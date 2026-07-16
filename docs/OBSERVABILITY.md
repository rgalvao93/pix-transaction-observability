# Observabilidade

O projeto adota observabilidade como requisito de primeira classe: logs, métricas e health checks nativos em cada serviço.

## Logs Estruturados

- Biblioteca: `logstash-logback-encoder` (já presente no `pom.xml`).
- `TransactionService` já loga (via SLF4J) `transactionId`, o status resultante, o motivo de falha e o tempo total de processamento em cada transação; `HttpExternalPartnerClient` loga cada tentativa de chamada ao parceiro que falhar.
- **Pendente (Fase 4)**: configurar `src/main/resources/logback-spring.xml` para emitir esses logs em **JSON** (hoje saem no formato texto padrão do Spring Boot no console), pronto para ingestão por ferramentas como Loki, ELK ou similares.

Exemplo de linha de log (formato alvo após a Fase 4):
```json
{
  "timestamp": "2026-07-11T10:00:00.123Z",
  "level": "INFO",
  "logger": "com.rodrigogalvao.transaction.service.TransactionService",
  "message": "Transaction processed",
  "transactionId": "txn-123456",
  "type": "CASH_IN",
  "status": "PROCESSED",
  "durationMs": 142
}
```

## Métricas (Spring Boot Actuator + Micrometer + Prometheus)

Endpoint: `GET /actuator/prometheus`

Métricas planejadas (Fase 4):

| Métrica | Tipo | Descrição |
|---------|------|-----------|
| `http_server_requests_seconds` | histogram | Nativa do Spring — latência por endpoint |
| `transaction_processing_duration_seconds` | histogram | Tempo total de processamento de uma transação |
| `transaction_total` | counter | Total de transações, com tags `type` e `status` |
| `external_partner_calls_failed_total` | counter | Falhas de comunicação com o parceiro externo |
| `external_partner_call_duration_seconds` | histogram | Latência das chamadas ao parceiro |

## Health Checks

Endpoint: `GET /actuator/health`

- **Liveness**: confirma que o processo da aplicação está de pé.
- **Readiness**: confirma que o serviço consegue se comunicar com o `external-partner-mock` (custom `HealthIndicator`, Fase 4).

## Stack de Observabilidade (Docker Compose)

Diretório: `observability/` *(a criar na Fase 4)*

```
observability/
├── docker-compose.yml
├── prometheus/
│   └── prometheus.yml       # scrape configs para transaction-service e partner-mock
├── grafana/
│   └── dashboards/          # dashboards pré-configurados
└── alertmanager/
    └── alertmanager.yml     # regras de alerta (ex: taxa de erro > 5%)
```

Serviços:
| Serviço | Porta | Função |
|---------|-------|--------|
| Prometheus | 9090 | Coleta métricas de `/actuator/prometheus` de ambos os serviços |
| Grafana | 3000 | Dashboards de latência, taxa de erro, volume de transações |
| AlertManager | 9093 | Alertas configurados a partir de regras do Prometheus |

### Como subir a stack (após Fase 4)

```bash
cd observability
docker compose up -d
```

## Rastreamento (Traces)

Mencionado no README como objetivo (OpenTelemetry), mas **ainda não escopado em nenhuma fase do plano atual**. Pode ser adicionado como Fase 6 se houver necessidade de tracing distribuído entre `transaction-service` e `external-partner-mock`.
