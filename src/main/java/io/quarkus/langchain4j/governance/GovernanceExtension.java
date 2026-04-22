package io.quarkus.langchain4j.governance;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Quarkus extension recorder — wires up the governance beans at build time.
 */
@Recorder
public class GovernanceExtension {

    @ConfigProperty(defaultValue = "true")
    boolean enabled;

    @ConfigProperty(defaultValue = "governance-policies.yaml")
    String policyFile;

    @ConfigProperty(defaultValue = "jsonl")
    String auditLogFormat;

    public void register(BeanContainer container) {
        PolicyStore policyStore = new PolicyStore(policyFile);
        AuditLogger auditLogger = new AuditLogger();

        container.beanManager().createInstance()
                .getReference(GovernanceExtension.class);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public PolicyStore store() {
        return new PolicyStore(policyFile);
    }

    public AuditLogger audit() {
        return new AuditLogger();
    }

    public PolicyDecision evaluate(dev.langchain4j.agent.tool.ToolExecutionRequest request,
                                  String callerIdentity) {
        return store().evaluate(request, callerIdentity);
    }
}