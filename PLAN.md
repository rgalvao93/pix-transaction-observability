# PLAN.md — Pix Transaction Observability

Plano geral do projeto, organizado em fases. Cada fase tem objetivo, entregáveis e critérios de aceite. O objetivo central é um MVP de serviço transacional Pix com **observabilidade nativa** (logs, métricas e health checks desde o Dia Um).

## Objetivos do Projeto

- **Observabilidade desde o Dia Um**: logs, métricas e health checks fazem parte do design, não são adicionados depois.
- **Resiliência**: tratar latência e erros provenientes de parceiros externos (retry, timeout, circuit breaker).
- **Simulação realista**: ambiente autônomo para testar respostas operacionais (cash-in/cash-out, casos de borda, falhas simuladas).

## Fases Concluídas

| Fase | Descrição | Status | Referência |
|------|-----------|--------|------------|
| 1 | Setup do projeto, scaffolding do `transaction-service`, DTOs e modelos iniciais | ✅ Concluída | commits iniciais |
| 2 | Lógica de negócio (cash-in/cash-out), idempotência, limite diário, segurança JWT e testes | ✅ Concluída | `7984f4e` |
| 3 | `external-partner-mock` — PSP/Banco Central simulado (latência e falha configuráveis) | ✅ Concluída | `0416368` |
| 4 | Observabilidade completa — logs estruturados, métricas Prometheus, health checks, stack Docker (Prometheus/Grafana/AlertManager) | ✅ Concluída | `363e44a` |
| 6 | OpenTelemetry (tracing distribuído) — OTel nos dois serviços, Tempo na stack, correlação trace → log → métrica, dashboard de latência por operação | ✅ Concluída | ADR-011 |

> Design e arquitetura detalhados em [docs/architecture.md](docs/architecture.md). Documentação de API, observabilidade e desenvolvimento em [docs/](docs/).

## Fases Futuras (Roadmap)

### Fase 5 — Backlog / Endurecimento

Cadência de itens de baixo acoplamento que fecham pontas soltas do MVP antes de acrescentar features novas.

- [x] **Verificação real da stack de observabilidade** — `docker compose up` completo executado e validado (2026-11-14): Prometheus/Grafana/AlertManager sobem limpos, `promtool`/`amtool` aprovam config e as 4 regras, targets `up`, Grafana provisionado consultando o Prometheus e alerta de teste propagou até o AlertManager.
- [x] **Canal de notificação no AlertManager** — receiver `default` envia e-mail para o Mailpit (SMTP catcher local, UI em `localhost:8025`); notificação validada de ponta a ponta (2026-11-14). Em produção, trocar `smtp_smarthost` por um provedor SMTP real ou usar webhook/Slack.
- [x] **Persistência real** — PostgreSQL/JPA/Flyway para transações, limite diário e contas do mock; idempotência e reservas diárias persistem entre reinícios.
- [ ] **Provedor de identidade real** — `/auth/token` emite JWT sem validar credenciais (conveniência de dev). Integrar com IdP/OAuth2 e remover o endpoint de teste.
- [ ] **Limpeza de código** — subir cobertura de testes (health indicator e casos de borda), adicionar CI (build + testes + verificação de alertas), revisar regras de alerta (`rules.yml`) sob carga.

**Critérios de aceite da Fase 5**: stack de observabilidade sobe com `docker compose up` sem ajustes manuais; alertas chegam ao canal configurado; transações sobrevivem a restart (persistência); endpoints protegidos não dependem mais do token de teste; CI verde no repositório.

> **Fase 6 — OpenTelemetry (Tracing Distribuído) — ✅ Concluída (2026-11-15)**: OTel nos dois serviços (`spring-boot-starter-opentelemetry`), exportação direta OTLP → Tempo na stack, correlação trace → log → métrica (`traceId`/`spanId` no MDC; span de negócio `transaction-process`) e dashboard `pix-traces.json`. Validado em 2026-11-15: `POST /transactions` (cash-in) gerou trace distribuído completo `transaction-service` → `external-partner-mock` visível no Tempo (traceID `58812d276c15f5d0ad97db1b5dfe1f2a`). Decisões em [ADR-011](docs/adr/0011-opentelemetry-tempo-tracing.md); operacional em [docs/OBSERVABILITY.md](docs/OBSERVABILITY.md).

### Fase 7 — Resiliência Avançada (opcional)

- [ ] Circuit breaker e bulkhead nas chamadas ao parceiro (ex: Resilience4j) — hoje há retry simples.
- [ ] Retry com jitter/backoff exponencial configurável por operação.
- [ ] Simulação de pico de carga (script de geração de tráfego) para validar alertas e percentis p95/p99.

**Critérios de aceite da Fase 7**: degradação do parceiro não derruba o `transaction-service`; cenário de carga dispara os alertas existentes e mantém p99 dentro de SLO definido.

## Status Atual (checkpoint)

- Branch `main` sincronizada com `origin/main`.
- `transaction-service` (8080) e `external-partner-mock` (8081) implementados e testados — ver [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) para executar localmente.
- Stack de observabilidade validada com `docker compose up` completo (boot real + propagação de alerta até o AlertManager).
- Fase 6 (OpenTelemetry → Tempo) implementada; traces distribuídos validados de ponta a ponta e dashboard `Pix — Tracing & Latência por operação` provisionado — ver [ADR-011](docs/adr/0011-opentelemetry-tempo-tracing.md).
- Documentação completa em [docs/](docs/): `architecture.md`, `API.md`, `OBSERVABILITY.md`, `DEVELOPMENT.md`.

## Riscos e Observações

- **Spring Boot 4 / Jackson 3**: dependências modulares (ex: `spring-boot-starter-jackson`, `spring-boot-webmvc-test`, `@MockitoBean`). Mudanças de API podem surpreender durante upgrades — ver [docs/architecture.md](docs/architecture.md).
- **Segredos**: `security.jwt.secret` é **obrigatória e sem default** desde a issue #11 — o boot falha sem `SECURITY_JWT_SECRET` (mesmo valor nos dois serviços; gerar com `openssl rand -base64 64`).
- **Endpoint de token de dev**: `POST /auth/token` continua para desenvolvimento, desabilitável via `SECURITY_DEV_TOKEN_ENABLED=false` — decisão registrada no [ADR-010](docs/adr/0010-dev-token-endpoint.md); a remoção definitiva vem com o IdP real (GAP-4).
- **Escopo MVP**: saldos e contas persistem no banco do mock — o `transaction-service` não é fonte de verdade de saldo.

## Convenções

- Commits em português, no imperativo (ex: `feat: implementa observabilidade completa (Fase 4)`).
- Logs estruturados via SLF4J; nunca `System.out.println`.
- Pacotes Java seguem `com.rodrigogalvao.<serviço>.<camada>`.