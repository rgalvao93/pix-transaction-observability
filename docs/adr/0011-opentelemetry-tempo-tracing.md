# ADR-011 — Tracing distribuído com OpenTelemetry → Grafana Tempo

- **Status**: Aceita (2026-11-15)
- **Escopo**: `transaction-service`, `external-partner-mock`, stack `observability/`
- **Relacionada**: PLAN.md Fase 6, ADR-001 (Observability-first), GAP-1

## Contexto

Até a Fase 5, a correlação entre uma transação e seus efeitos no parceiro era
**manual**, via `transactionId` embutido nas mensagens de log (RNF-09 parcial).
Não havia `traceId`/`spanId` nos logs, nenhuma propagação entre os serviços e
nenhum backend de traces. A Fase 6 do [`PLAN.md`](../../PLAN.md) pedia tracing
distribuído de ponta a ponta: `transaction-service` → `external-partner-mock`,
com correlação trace → log → métrica e análise de latência por operação.

## Alternativas consideradas

1. **Jaeger all-in-one** — clássico e autocontido, mas adiciona uma UI separada
   à stack; métricas e traces ficariam em Grafana (métricas) e Jaeger (traces),
   com propagação de contexto igualmente necessária.
2. **Tempo + OTel Collector** — topologia mais "production-like" (apps → collector
   → backend), mas adiciona um componente e config a manter sem ganho para o MVP.
3. **Tempo, exportação direta OTLP dos apps** — escolhido: um serviço a mais no
   compose (`grafana/tempo`), mesmo ecossistema Grafana (datasource provisionado,
   uma UI para métricas e traces), sem collector intermediário.
4. **Brave + Zipkin** — suportado pelo Spring Boot, mas o ecossistema OTel é o
   caminho padrão e o PLAN.md pedia OpenTelemetry explicitamente.

## Decisão

- Dependência `org.springframework.boot:spring-boot-starter-opentelemetry` nos dois
  serviços (Micrometer Tracing → ponte OTel → exporter OTLP, versões gerenciadas
  pelo BOM do Spring Boot 4.0.2).
- Export **direto** dos apps para o Tempo: `management.opentelemetry.tracing.export.otlp.endpoint`
  (env `OTLP_TRACES_ENDPOINT`, default `http://localhost:4318/v1/traces`).
- **Sampling 100%** no MVP (`management.tracing.sampling.probability=1.0`) — volume
  baixo de tráfego; revisar ao aproximar carga real.
- **Métricas continuam via Prometheus** (scrape); a exportação OTLP de métricas do
  starter é desabilitada (`management.otlp.metrics.export.enabled=false`).
- Logs JSON (`logstash-logback-encoder`) passam a carregar `traceId`/`spanId`
  automaticamente via MDC — sem mudança no `logback-spring.xml`.
- `PartnerClientConfig` deixa de publicar um bean `RestClient.Builder` próprio e
  passa a publicar um `RestClientCustomizer` (timeouts 1000/2000ms). O bean
  original **substituía** o builder auto-configurado e derrubava a instrumentação
  de observação; com o customizer, timeouts e propagação `traceparent` coexistem.
- Span de negócio `transaction-process` criado via API `Tracer` (span-only, sem
  duplicar métricas), com tags `transaction.id`, `transaction.type`,
  `transaction.status`. Spans HTTP (server no mock, client no transaction-service)
  vêm da auto-configuração.
- Tempo sobe no `docker-compose.yml` da stack (`grafana/tempo:2.10.10`; OTLP HTTP
  4318, gRPC 4317, query 3200; config `observability/tempo/tempo.yaml`, volume
  `tempo-data`, retenção de blocos de 48h). Datasource Tempo provisionado no
  Grafana com `uid: tempo` e dashboard separado `pix-traces.json`.
- Nos testes, a exportação OTLP de métricas é desabilitada em
  `src/test/resources/application.properties` (sem efeito no caminho de traces,
  que já não reporta sob `@SpringBootTest`).

## Consequências

**Positivas**

- Uma transação gera um trace completo `transaction-service` → `external-partner-mock`
  visível no Grafana (Explore → Tempo), com spans anotados.
- `traceId`/`spanId` aparecem nos logs JSON dos dois serviços — correlação
  trace → log sem mudança de código de log nem de appenders.
- Dashboard `Pix — Tracing & Latência por operação` com p50/p95/p99 por operação
  (`verify-balance` × `transfer`; `CASH_IN` × `CASH_OUT`).

**Negativas / riscos**

- Exportação direta sem collector: sem tail-sampling, sem batching central nem
  enriquecimento no caminho — aceitável no MVP; revisitar com carga real (Fase 7).
- Sampling 100% não escala para volume de produção; parametrizar via config quando
  o tráfego crescer.
- O Tempo local usa armazenamento `local` (filesystem) — adequado à demo; produção
  pediria object storage (S3/GCS) e retenção adequada.
- O health check `partnerConnectivity` passou a gerar um span HTTP client por
  probe de readiness (antes não havia instrumentação) — ruído moderado nos
  traces; sampling por rota fica como refinamento futuro.
- `micrometer-tracing-test` entrou como dependência de teste (unit tests de
  `TransactionService` usam `SimpleTracer`).
