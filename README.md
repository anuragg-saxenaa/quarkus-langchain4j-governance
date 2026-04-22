# quarkus-langchain4j-governance

Enterprise-grade policy enforcement for Quarkus LangChain4j AI agents — powered by Microsoft Agent Governance Toolkit (AGT).

## Features

- **Policy-as-code**: YAML-based policy definitions, version-controlled
- **Zero-trust identity**: Every tool call verified against caller identity + session context
- **Audit logging**: JSONL audit trail with timestamps, tool names, verdicts, latencies
- **Policy transform**: Modify arguments before execution (e.g., PII masking)
- **Micrometer metrics**: `policy_checks_total`, `policy_violations_total`, `policy_check_latency_ms`

## Quick Start

```xml
<dependency>
    <groupId>io.quarkus.langchain4j</groupId>
    <artifactId>quarkus-langchain4j-governance</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```properties
quarkus.langchain4j.governance.enabled=true
quarkus.langchain4j.governance.policy-file=governance-policies.yaml
quarkus.langchain4j.governance.audit-log=jsonl
```

## Policy DSL

```yaml
policies:
  - name: restrict-file-write
    tool: filesystem.write
    action: DENY

  - name: allow-read-only
    tool: filesystem.read
    action: ALLOW
```

## Architecture

- `GovernanceInterceptor` — CDI interceptor wrapping tool execution
- `PolicyStore` — evaluates rules from YAML config
- `AuditLogger` — writes JSONL audit entries per call
- `GovernanceExtension` — Quarkus extension recorder

## Build

```bash
./mvnw clean install
```

## Test

```bash
./mvnw test
```