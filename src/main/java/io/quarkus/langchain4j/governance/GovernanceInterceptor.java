package io.quarkus.langchain4j.governance;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;

/**
 * CDI interceptor that wraps every LangChain4j tool execution with policy checks.
 * Fires before each ToolExecutionRequest is passed to the actual tool method.
 */
@Priority(Interceptor.Priority.APPLICATION)
@GovernanceBinding
public class GovernanceInterceptor {

    private static final String EXECUTION_RESULT_KEY = "governance.tool.result";

    @AroundInvoke
    public Object intercept(InvocationContext ic) throws Exception {
        Method method = ic.getMethod();
        Object[] params = ic.getParameters();

        // Only intercept tool execution methods
        if (!isToolMethod(method)) {
            return ic.proceed();
        }

        Optional<ToolExecutionRequest> toolRequest = extractToolRequest(params);
        if (toolRequest.isEmpty()) {
            return ic.proceed();
        }

        GovernanceExtension ext = Arc.container()
                .instance(GovernanceExtension.class)
                .get();

        if (!ext.isEnabled()) {
            return ic.proceed();
        }

        long start = System.currentTimeMillis();
        String callerIdentity = resolveCallerIdentity();

        // Evaluate policy
        PolicyDecision decision = ext.evaluate(toolRequest.get(), callerIdentity);

        if (decision.deny()) {
            ext.audit().log(toolRequest.get(), callerIdentity, "DENIED", start, null);
            throw new PolicyViolationException(
                    "Tool '" + toolRequest.get().name() + "' denied by policy: " + decision.reason());
        }

        // Apply transforms if any
        Object[] transformedParams = applyTransforms(decision, params);
        if (transformedParams != params) {
            ic.setParameters(transformedParams);
        }

        try {
            Object result = ic.proceed();
            ext.audit().log(toolRequest.get(), callerIdentity, "ALLOWED", start, result);
            return result;
        } catch (Exception e) {
            ext.audit().log(toolRequest.get(), callerIdentity, "ERROR", start, e);
            throw e;
        }
    }

    private boolean isToolMethod(Method method) {
        // Tools are typically named doExecute or similar in LangChain4j invocation handlers
        // This checks for tool-call method signatures
        return method.getName().contains("Tool") ||
               method.isAnnotationPresent(dev.langchain4j.agent.tool.Tool.class);
    }

    private Optional<ToolExecutionRequest> extractToolRequest(Object[] params) {
        if (params == null) return Optional.empty();
        for (Object p : params) {
            if (p instanceof ToolExecutionRequest req) {
                return Optional.of(req);
            }
        }
        return Optional.empty();
    }

    private String resolveCallerIdentity() {
        // In a real impl, extract from Quarkus SecurityContext or session
        return "unknown";
    }

    private Object[] applyTransforms(PolicyDecision decision, Object[] params) {
        if (decision.transformedArgs() == null) return params;
        // Return transformed args - in practice this would be a deep copy
        return decision.transformedArgs();
    }
}