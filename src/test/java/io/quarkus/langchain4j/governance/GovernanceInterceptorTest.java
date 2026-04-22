package io.quarkus.langchain4j.governance;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies GovernanceInterceptor correctly evaluates policies before tool execution.
 */
class GovernanceInterceptorTest {

    private PolicyStore store;

    @BeforeEach
    void setUp() {
        store = new PolicyStore("governance-policies.yaml");
    }

    @Test
    void denyToolWhenPolicyDenies() {
        ToolExecutionRequest req = ToolExecutionRequest.builder()
                .id("call-x")
                .name("filesystem.delete")
                .arguments("/tmp/secret.txt")
                .build();

        PolicyDecision decision = store.evaluate(req, "developer");
        assertFalse(decision.allowed());
        assertEquals("DENY", decision.action());
        assertTrue(decision.reason().contains("deny-file-delete"));
    }

    @Test
    void allowToolWhenPolicyAllows() {
        ToolExecutionRequest req = ToolExecutionRequest.builder()
                .id("call-y")
                .name("filesystem.read")
                .arguments("/etc/passwd")
                .build();

        PolicyDecision decision = store.evaluate(req, "user");
        assertTrue(decision.allowed());
    }

    @Test
    void denySqlDropStatement() {
        ToolExecutionRequest req = ToolExecutionRequest.builder()
                .id("call-z")
                .name("database.execute")
                .arguments("DROP TABLE users;")
                .build();

        PolicyDecision decision = store.evaluate(req, "user");
        assertFalse(decision.allowed());
        assertEquals("DENY", decision.action());
    }

    @Test
    void allowNonDropSql() {
        ToolExecutionRequest req = ToolExecutionRequest.builder()
                .id("call-w")
                .name("database.execute")
                .arguments("INSERT INTO users VALUES ('Alice')")
                .build();

        PolicyDecision decision = store.evaluate(req, "user");
        assertTrue(decision.allowed());
    }

    @Test
    void denyExternalWebhook() {
        ToolExecutionRequest req = ToolExecutionRequest.builder()
                .id("call-v")
                .name("http.post")
                .arguments("{\"url\":\"https://external.example.com/webhook\"}")
                .build();

        PolicyDecision decision = store.evaluate(req, "user");
        assertFalse(decision.allowed());
    }

    @Test
    void defaultAllowUnknownTool() {
        ToolExecutionRequest req = ToolExecutionRequest.builder()
                .id("call-u")
                .name("unknown.tool.action")
                .arguments("{}")
                .build();

        PolicyDecision decision = store.evaluate(req, "user");
        assertTrue(decision.allowed());
    }
}
