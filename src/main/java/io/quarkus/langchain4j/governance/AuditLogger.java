package io.quarkus.langchain4j.governance;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes structured audit logs for every tool execution decision.
 * Output format: newline-delimited JSON (JSONL) — one entry per tool call.
 */
@ApplicationScoped
public class AuditLogger {

    private final Path auditFile;
    private final ObjectMapper mapper;
    private final Map<String, Long> counter = new ConcurrentHashMap<>();

    public AuditLogger() {
        this("/tmp/quarkus-langchain4j-governance-audit.jsonl");
    }

    public AuditLogger(String auditPath) {
        this.auditFile = Path.of(auditPath);
        this.mapper = new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .findAndRegisterModules();
        try {
            Files.createDirectories(auditFile.getParent());
        } catch (IOException e) {
            System.err.println("AuditLogger: could not create directory: " + auditFile.getParent());
        }
    }

    public synchronized void log(ToolExecutionRequest request, String callerIdentity,
                                  String verdict, long startMs, Object result) {
        long durationMs = System.currentTimeMillis() - startMs;
        AuditEntry entry = new AuditEntry(
                Instant.now().toString(),
                request.name(),
                request.arguments(),
                callerIdentity,
                verdict,
                durationMs,
                result != null ? result.toString() : null
        );
        try {
            String json = mapper.writeValueAsString(entry);
            try (PrintWriter w = new PrintWriter(new FileWriter(auditFile.toFile(), true))) {
                w.println(json);
            }
        } catch (IOException e) {
            System.err.println("AuditLogger: failed to write entry: " + e.getMessage());
        }
    }

    public long getCount(String verdict) {
        return counter.getOrDefault(verdict, 0L);
    }

    private record AuditEntry(
            String timestamp,
            String toolName,
            String toolArgs,
            String callerIdentity,
            String verdict,
            long durationMs,
            String resultSummary
    ) {}
}