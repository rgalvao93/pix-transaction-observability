# Observabilidade

O projeto adota observabilidade como requisito de primeira classe: logs, métricas e health checks nativos em cada serviço.

## Logs Estruturados

- Biblioteca: `logstash-logback-encoder` (nos dois serviços).
- Configuração: `src/main/resources/logback-spring.xml` (transaction-service e external-partner-mock) — `LogstashEncoder` no `ConsoleAppender`, com o campo customizado `service` (via `spring.application.name`).
- `TransactionService` loga (via SLF4J) `transactionId`, o status resultante, o motivo de falha e o tempo total de processamento em cada transação; `HttpExternalPartnerClient` loga cada tentativa de chamada ao parceiro que falhar; `PartnerService` loga cada `transfer`/`verify-balance` com falha ou conta desconhecida.

Exemplo real de linha de log (saída de `transaction-service`, execução local pós-Fase 6):

```json
{
  "@timestamp": "2026-11-15T20:13:15.798999824-03:00",
  "@version": "1",
  "message": "Transaction fase6-verify-1 processed as PROCESSED in 584ms",
  "logger_name": "com.rodrigogalvao.transaction.service.TransactionService",
  "thread_name": "http-nio-8080-exec-4",
  "level": "INFO",
  "level_value": 20000,
  "appName": "transaction-service",
  "traceId": "58812d276c15f5d0ad97db1b5dfe1f2a",
  "spanId": "18aecd72b1b137e1",
  "service": "transaction-service"
}
```

> `traceId`/`spanId` são incluídos automaticamente desde a Fase 6 (Micrometer Tracing popula o MDC).

## Métricas (Spring Boot Actuator + Micrometer + Prometheus)

Endpoint: `GET /actuator/prometheus` (exposto via `management.endpoints.web.exposure.include=health,prometheus`, dependência `io.micrometer:micrometer-registry-prometheus`).

| Métrica | Tipo | Descrição |
| --- | --- | --- |
| `http_server_requests_seconds` | histogram | Nativa do Spring — latência por endpoint |
| `transaction_processing_duration_seconds` | histogram (percentile histogram habilitado) | Tempo total de processamento de uma transação, tags `type`/`status` |
| `transaction_total` | counter | Total de transações, com tags `type` e `status` |
| `external_partner_calls_failed_total` | counter | Falhas de comunicação com o parceiro externo (após esgotar os retries), tag `operation` |
| `external_partner_call_duration_seconds` | histogram (percentile histogram habilitado) | Latência de cada tentativa de chamada ao parceiro, tags `operation`/`outcome` |

O histograma habilitado (`.publishPercentileHistogram()`) permite calcular percentis (`histogram_quantile`) no Prometheus/Grafana sem depender de `summary` client-side.

## Health Checks

Endpoint: `GET /actuator/health`, com grupos `liveness` e `readiness` (`management.endpoint.health.probes.enabled=true`).

- **Liveness**: confirma que o processo da aplicação está de pé.
- **Readiness** (`transaction-service`): inclui o indicator customizado `partnerConnectivity` (`PartnerConnectivityHealthIndicator`), que faz um `GET /actuator/health` direto no `external-partner-mock` (sem autenticação, sem retry) — `/actuator/health/readiness` fica `DOWN` se o parceiro estiver fora do ar.

## Stack de Observabilidade (Docker Compose)

Diretório: `observability/`

```text
observability/
├── docker-compose.yml
├── prometheus/
│   ├── prometheus.yml       # scrape configs para transaction-service e partner-mock
│   └── rules.yml            # regras de alerta
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/datasource.yml   # datasources Prometheus (default) e Tempo, provisionados automaticamente
│   │   └── dashboards/dashboard.yml     # provider de dashboards via arquivo
│   └── dashboards/
│       ├── pix-overview.json            # dashboard com os painéis abaixo
│       └── pix-traces.json              # tracing & latência por operação (p50/p95/p99 + guia Explore → Tempo)
├── tempo/
│   └── tempo.yaml           # config mínima do Tempo (OTLP 4317/4318, query 3200)
└── alertmanager/
    └── alertmanager.yml     # roteamento de alertas (receiver "default" → e-mail demo no Mailpit)
```

Serviços:

| Serviço | Porta | Função |
| --- | --- | --- |
| Prometheus | 9090 | Coleta métricas de `/actuator/prometheus` de `transaction-service` e `external-partner-mock`, que rodam no host (`host.docker.internal:8080`/`:8081`) |
| Grafana | 3000 | Dashboards "Pix Transaction Observability" e "Pix — Tracing & Latência por operação" provisionados automaticamente (login `admin`/`admin`, ou acesso anônimo como Viewer) |
| Tempo | 3200 (query) · 4317 (OTLP gRPC) · 4318 (OTLP HTTP) | Backend de traces; recebe OTLP direto dos dois serviços e serve o datasource Tempo do Grafana |
| AlertManager | 9093 | Recebe alertas do Prometheus e envia notificações por e-mail via Mailpit (canal demo) |
| Mailpit | 8025 (UI) · 1025 (SMTP) | SMTP catcher local que recebe os e-mails do AlertManager — ver mensagens em `http://localhost:8025` |

Regras de alerta (`prometheus/rules.yml`): `HighTransactionErrorRate` (taxa de `status=ERROR` > 5% em 5m), `PartnerCallFailuresHigh` (>5 falhas de chamada ao parceiro em 10m), `TransactionServiceDown` e `ExternalPartnerMockDown` (target fora do ar por 1m).

> As regras que avaliam métricas de negócio (`transaction_total`, `external_partner_*`) dependem do `transaction-service`, o único que emite essas séries; o `external-partner-mock` contribui apenas com métricas HTTP nativas do Actuator e com o alvo `up{job="external-partner-mock"}`.

Painéis do dashboard: serviços disponíveis (`up`), transações por status, taxa de erro, latência p95 de processamento, latência p95 de chamada ao parceiro, falhas de chamada ao parceiro.

### Como subir a stack

Suba primeiro os dois serviços Java (fora do Docker) e depois a stack:

```bash
cd external-partner-mock && ./mvnw spring-boot:run   # terminal 1
cd transaction-service && ./mvnw spring-boot:run     # terminal 2
cd observability && docker compose up -d             # terminal 3
```

> **Nota**: primeiro boot real validado em 2026-11-14 — `docker compose up` completo, `promtool check config/rules` e `amtool check-config` aprovando, targets `up`, datasource e dashboard do Grafana provisionados e um alerta de teste propagando do Prometheus até o AlertManager. O canal de e-mail demo (receiver `default` → Mailpit) foi validado com notificação recebida na UI do Mailpit.

## Rastreamento (Traces)

Tracing distribuído com **OpenTelemetry → Grafana Tempo** (Fase 6). Racional e alternativas em [ADR-011](adr/0011-opentelemetry-tempo-tracing.md); aqui fica só o operacional —

- Sampling **100%** (`management.tracing.sampling.probability=1.0`); apps exportam OTLP **direto** para o Tempo (`localhost:4318`, env `OTLP_TRACES_ENDPOINT`); métricas seguem no Prometheus (push OTLP de métricas desabilitado).
- Logs JSON carregam `traceId`/`spanId` automaticamente (MDC).
- Spans: HTTP server (mock) e HTTP client (transaction-service, via `RestClient.Builder` auto-configurado + `RestClientCustomizer` de timeouts), mais o span de negócio `transaction-process` com tags `transaction.id`/`transaction.type`/`transaction.status`.
- Backend: Tempo no `docker-compose.yml` (query em `http://localhost:3200`; OTLP 4317/4318); datasource **Tempo** provisionado no Grafana (`uid: tempo`).
- Dashboard "Pix — Tracing & Latência por operação" (`pix-traces.json`): p50/p95/p99 por operação (`verify-balance` × `transfer`; `CASH_IN` × `CASH_OUT`) + guia para abrir os traces no Explore → Tempo. A listagem de traces em painel de dashboard não é suportada por esta combinação (Grafana 11.4 + Tempo 2.10) — use o Explore.

Como validar: suba os dois serviços e a stack, faça um POST de transação e abra Grafana → Explore → Tempo (TraceQL `{}` ou `{ span.transaction.id = "<transactionId>" }`) para ver o trace completo `transaction-service` → `external-partner-mock`; o `traceId` do trace filtra os logs JSON de ambos. Validado em 2026-11-15 (trace `58812d276c15f5d0ad97db1b5dfe1f2a`).
