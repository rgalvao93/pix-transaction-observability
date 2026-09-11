# Observabilidade

O projeto adota observabilidade como requisito de primeira classe: logs, métricas e health checks nativos em cada serviço.

## Logs Estruturados

- Biblioteca: `logstash-logback-encoder` (nos dois serviços).
- Configuração: `src/main/resources/logback-spring.xml` (transaction-service e external-partner-mock) — `LogstashEncoder` no `ConsoleAppender`, com o campo customizado `service` (via `spring.application.name`).
- `TransactionService` loga (via SLF4J) `transactionId`, o status resultante, o motivo de falha e o tempo total de processamento em cada transação; `HttpExternalPartnerClient` loga cada tentativa de chamada ao parceiro que falhar; `PartnerService` loga cada `transfer`/`verify-balance` com falha ou conta desconhecida.

Exemplo real de linha de log (saída de `transaction-service`):
```json
{
  "@timestamp": "2026-07-15T13:21:12.566Z",
  "@version": "1",
  "message": "Transaction final-2 processed as PROCESSED in 1ms",
  "logger_name": "com.rodrigogalvao.transaction.service.TransactionService",
  "thread_name": "http-nio-8080-exec-6",
  "level": "INFO",
  "level_value": 20000,
  "appName": "transaction-service",
  "service": "transaction-service"
}
```

## Métricas (Spring Boot Actuator + Micrometer + Prometheus)

Endpoint: `GET /actuator/prometheus` (exposto via `management.endpoints.web.exposure.include=health,prometheus`, dependência `io.micrometer:micrometer-registry-prometheus`).

| Métrica | Tipo | Descrição |
|---------|------|-----------|
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

```
observability/
├── docker-compose.yml
├── prometheus/
│   ├── prometheus.yml       # scrape configs para transaction-service e partner-mock
│   └── rules.yml            # regras de alerta
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/datasource.yml   # datasource Prometheus, provisionado automaticamente
│   │   └── dashboards/dashboard.yml     # provider de dashboards via arquivo
│   └── dashboards/
│       └── pix-overview.json            # dashboard com os painéis abaixo
└── alertmanager/
    └── alertmanager.yml     # roteamento de alertas (receiver "default", sem canal real configurado)
```

Serviços:
| Serviço | Porta | Função |
|---------|-------|--------|
| Prometheus | 9090 | Coleta métricas de `/actuator/prometheus` de `transaction-service` e `external-partner-mock`, que rodam no host (`host.docker.internal:8080`/`:8081`) |
| Grafana | 3000 | Dashboard "Pix Transaction Observability" provisionado automaticamente (login `admin`/`admin`, ou acesso anônimo como Viewer) |
| AlertManager | 9093 | Recebe alertas do Prometheus; sem canal de notificação real configurado (demo) |

Regras de alerta (`prometheus/rules.yml`): `HighTransactionErrorRate` (taxa de `status=ERROR` > 5% em 5m), `PartnerCallFailuresHigh` (>5 falhas de chamada ao parceiro em 10m), `TransactionServiceDown` e `ExternalPartnerMockDown` (target fora do ar por 1m).

Painéis do dashboard: serviços disponíveis (`up`), transações por status, taxa de erro, latência p95 de processamento, latência p95 de chamada ao parceiro, falhas de chamada ao parceiro.

### Como subir a stack

Suba primeiro os dois serviços Java (fora do Docker) e depois a stack:

```bash
cd external-partner-mock && ./mvnw spring-boot:run   # terminal 1
cd transaction-service && ./mvnw spring-boot:run     # terminal 2
cd observability && docker compose up -d             # terminal 3
```

> **Nota**: a sintaxe do `docker-compose.yml` foi validada com `docker compose config`, mas o `docker compose up` em si não foi executado no ambiente onde este projeto foi desenvolvido (sem daemon Docker disponível). Caso encontre algum ajuste necessário ao rodar localmente, é esperado — a stack não teve um "primeiro boot" real verificado.

## Rastreamento (Traces)

Mencionado no README como objetivo (OpenTelemetry), mas **ainda não escopado em nenhuma fase do plano atual**. Pode ser adicionado como Fase 6 se houver necessidade de tracing distribuído entre `transaction-service` e `external-partner-mock`.
