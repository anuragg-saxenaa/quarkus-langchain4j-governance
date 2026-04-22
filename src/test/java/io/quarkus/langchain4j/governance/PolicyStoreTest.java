package io.quarkus.langchain4j.governance;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PolicyStoreTest {

    private PolicyStore store;

    @BeforeEach
    void setUp() {
        store = new PolicyStore();
    }

    @Test
    void allowAllowsMatchingTool() {
        ToolExecutionRequest req = ToolExecutionRequest.builder()
                .id("call-1")
                .name("database.query")
                .arguments("SELECT * FROM users")
                .build();

        PolicyDecision decision = store.evaluate(req, "user@example.com");
        assertTrue(decision.allowed());
        assertEquals("ALLOW", decision.action());
    }

    @Test
    void denyBlocksConfiguredTool() {
        store.loadFromFile("governance-policies.yaml");

        ToolExecutionRequest req = ToolExecutionRequest.builder()
                .id("call-2")
                .name("filesystem.delete")
                .arguments("/tmp/sensitive.txt")
                .build();

        PolicyDecision decision = store.evaluate(req, "developer");
        assertFalse(decision.allowed());
        assertEquals("DENY", decision.action());
        assertTrue(decision.reason().contains("deny-file-delete"));
    }

    @Test
    void sqlInjectionDeniesRawDrop() {
        ToolExecutionRequest req = ToolExecutionRequest.builder()
                .id("call-3")
                .name("database.execute")
                .arguments("DROP TABLE users;")
                .build();

        PolicyDecision decision = store.evaluate(req, "user");
        assertFalse(decision.allowed());
    }
}