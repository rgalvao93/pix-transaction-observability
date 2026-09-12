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

> Design e arquitetura detalhados em [docs/architecture.md](docs/architecture.md). Documentação de API, observabilidade e desenvolvimento em [docs/](docs/).

## Fases Futuras (Roadmap)

### Fase 5 — Backlog / Endurecimento

Cadência de itens de baixo acoplamento que fecham pontas soltas do MVP antes de acrescentar features novas.

- [ ] **Verificação real da stack de observabilidade** — o `docker compose up` completo nunca foi executado (só `docker compose config` foi validado). Validar primeiro boot de Prometheus/Grafana/AlertManager e ajustar o que for necessário.
- [ ] **Canal de notificação no AlertManager** — hoje o receiver é `default` sem canal real (demo). Configurar webhook ou e-mail (demonstração) e validar alertas de ponta a ponta.
- [ ] **Persistência real** — substituir `TransactionRepository` e `DailyLimitTracker` (em memória) por um banco (ex: PostgreSQL), preservando a idempotência e o limite diário.
- [ ] **Provedor de identidade real** — `/auth/token` emite JWT sem validar credenciais (conveniência de dev). Integrar com IdP/OAuth2 e remover o endpoint de teste.
- [ ] **Limpeza de código** — subir cobertura de testes (health indicator e casos de borda), adicionar CI (build + testes + verificação de alertas), revisar regras de alerta (`rules.yml`) sob carga.

**Critérios de aceite da Fase 5**: stack de observabilidade sobe com `docker compose up` sem ajustes manuais; alertas chegam ao canal configurado; transações sobrevivem a restart (persistência); endpoints protegidos não dependem mais do token de teste; CI verde no repositório.

### Fase 6 — OpenTelemetry (Tracing Distribuído)

- [ ] Adicionar tracing distribuído com OpenTelemetry (SDK + `otlp`) nos dois serviços.
- [ ] Conectar a um collector/backend de traces (ex: Jaeger ou Tempo do Grafana).
- [ ] Correlacionar trace → log → métrica (ex: incluir `traceId`/`spanId` nos logs estruturados).
- [ ] Dashboard no Grafana com análise de latência por operação (verify-balance × transfer, cash-in × cash-out).

**Critérios de aceite da Fase 6**: uma transação cash-in/cash-out gera um trace distribuído completo (transaction-service → external-partner-mock) com spans anotados, visível no backend de traces; logs carregam `traceId` para correlação.

### Fase 7 — Resiliência Avançada (opcional)

- [ ] Circuit breaker e bulkhead nas chamadas ao parceiro (ex: Resilience4j) — hoje há retry simples.
- [ ] Retry com jitter/backoff exponencial configurável por operação.
- [ ] Simulação de pico de carga (script de geração de tráfego) para validar alertas e percentis p95/p99.

**Critérios de aceite da Fase 7**: degradação do parceiro não derruba o `transaction-service`; cenário de carga dispara os alertas existentes e mantém p99 dentro de SLO definido.

## Status Atual (checkpoint)

- Branch `main` sincronizada com `origin/main`.
- `transaction-service` (8080) e `external-partner-mock` (8081) implementados e testados — ver [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) para executar localmente.
- Stack de observabilidade pronta em `observability/` (pendente primeiro boot real).
- Documentação completa em [docs/](docs/): `architecture.md`, `API.md`, `OBSERVABILITY.md`, `DEVELOPMENT.md`.

## Riscos e Observações

- **Spring Boot 4 / Jackson 3**: dependências modulares (ex: `spring-boot-starter-jackson`, `spring-boot-webmvc-test`, `@MockitoBean`). Mudanças de API podem surpreender durante upgrades — ver [docs/architecture.md](docs/architecture.md).
- **Segredos**: `security.jwt.secret` tem valor padrão de demonstração. Em qualquer ambiente compartilhado, definir via `SECURITY_JWT_SECRET` (mesmo valor nos dois serviços).
- **Escopo MVP**: saldos e contas vivem no mock, em memória — o `transaction-service` não é fonte de verdade de saldo.

## Convenções

- Commits em português, no imperativo (ex: `feat: implementa observabilidade completa (Fase 4)`).
- Logs estruturados via SLF4J; nunca `System.out.println`.
- Pacotes Java seguem `com.rodrigogalvao.<serviço>.<camada>`.