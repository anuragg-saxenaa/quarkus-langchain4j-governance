package io.quarkus.langchain4j.governance;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AuditLoggerTest {

    @TempDir
    Path tempDir;

    @Test
    void writesJsonlEntry() throws Exception {
        Path auditPath = tempDir.resolve("audit.jsonl");
        AuditLogger logger = new AuditLogger(auditPath.toString());

        ToolExecutionRequest req = ToolExecutionRequest.builder()
                .id("call-1")
                .name("database.query")
                .arguments("SELECT * FROM users")
                .build();

        logger.log(req, "user@example.com", "ALLOWED", System.currentTimeMillis() - 50, "result");

        String content = Files.readString(auditPath);
        assertTrue(content.contains("\"toolName\":\"database.query\""));
        assertTrue(content.contains("\"verdict\":\"ALLOWED\""));
        assertTrue(content.contains("\"callerIdentity\":\"user@example.com\""));
    }

    @Test
    void multipleEntriesOnSeparateLines() throws Exception {
        Path auditPath = tempDir.resolve("audit2.jsonl");
        AuditLogger logger = new AuditLogger(auditPath.toString());

        for (int i = 0; i < 3; i++) {
            ToolExecutionRequest req = ToolExecutionRequest.builder()
                    .id("call-" + i)
                    .name("tool-" + i)
                    .arguments("{}")
                    .build();
            logger.log(req, "user", "ALLOWED", 10L, null);
        }

        String[] lines = Files.readString(auditPath).split("\n");
        assertEquals(3, lines.length);
    }
}