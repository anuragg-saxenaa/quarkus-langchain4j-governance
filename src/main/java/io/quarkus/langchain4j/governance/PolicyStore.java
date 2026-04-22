package io.quarkus.langchain4j.governance;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.*;

/**
 * Evaluates tool execution requests against loaded policy rules.
 */
@ApplicationScoped
public class PolicyStore {

    private final List<PolicyRule> rules = new ArrayList<>();

    public PolicyStore() {
        // Load default policy
        loadDefaultPolicies();
    }

    public PolicyStore(String policyFile) {
        this();
        if (policyFile != null) {
            loadFromFile(policyFile);
        }
    }

    private void loadDefaultPolicies() {
        // Default: allow all
        rules.add(new PolicyRule("default-allow", ".*", "ALLOW", null));
    }

    public void loadFromFile(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("PolicyStore: file not found: " + path);
                return;
            }
            Map<String, Object> doc = new Yaml().load(is);
            parsePolicies(doc);
        } catch (Exception e) {
            System.err.println("PolicyStore: failed to load " + path + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void parsePolicies(Map<String, Object> doc) {
        List<Map<String, Object>> policies = (List<Map<String, Object>>) doc.get("policies");
        if (policies == null) return;
        for (Map<String, Object> p : policies) {
            String name = (String) p.get("name");
            String tool = (String) p.get("tool");
            String action = (String) p.getOrDefault("action", "ALLOW");
            String condition = (String) p.get("condition");
            rules.add(new PolicyRule(name, tool, action, condition));
        }
    }

    public synchronized PolicyDecision evaluate(ToolExecutionRequest request, String callerIdentity) {
        for (PolicyRule rule : rules) {
            if (rule.matches(request.name())) {
                // TODO: evaluate condition against callerIdentity/session context
                if ("DENY".equals(rule.action())) {
                    return PolicyDecision.deny(rule.name() + ": tool '" + request.name() + "' denied");
                }
                return PolicyDecision.allow();
            }
        }
        return PolicyDecision.deny("No matching policy for tool: " + request.name());
    }

    private record PolicyRule(String name, String toolPattern, String action, String condition) {
        boolean matches(String toolName) {
            return toolName.matches(toolPattern.replace("*", ".*"));
        }
    }
}