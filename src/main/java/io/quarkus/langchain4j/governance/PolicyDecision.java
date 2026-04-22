package io.quarkus.langchain4j.governance;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import java.util.List;

/**
 * Result of a policy evaluation for a tool call.
 */
public record PolicyDecision(
    boolean allowed,
    String reason,
    String action,
    Object[] transformedArgs
) {
    public boolean deny() { return !allowed; }

    public static PolicyDecision allow() {
        return new PolicyDecision(true, null, "ALLOW", null);
    }

    public static PolicyDecision deny(String reason) {
        return new PolicyDecision(false, reason, "DENY", null);
    }

    public static PolicyDecision allowWithTransform(Object[] transformed, String reason) {
        return new PolicyDecision(true, reason, "ALLOW_TRANSFORM", transformed);
    }
}