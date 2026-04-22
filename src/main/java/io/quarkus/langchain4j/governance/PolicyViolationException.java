package io.quarkus.langchain4j.governance;

import java.util.Map;

/**
 * Thrown when a tool call violates a governance policy.
 */
public class PolicyViolationException extends RuntimeException {

    private final String toolName;
    private final String policyName;

    public PolicyViolationException(String message) {
        super(message);
        this.toolName = null;
        this.policyName = null;
    }

    public PolicyViolationException(String toolName, String policyName, String reason) {
        super("Policy violation: tool=" + toolName + ", policy=" + policyName + ", reason=" + reason);
        this.toolName = toolName;
        this.policyName = policyName;
    }

    public String toolName() { return toolName; }
    public String policyName() { return policyName; }
}