# AGENTS.md

Pix Transaction Observability — instruções para agentes trabalharem neste repositório.

## Commandos de verificação

- Testes (dois módulos Maven):
  - `transaction-service`: `./mvnw test` dentro de `transaction-service/`
  - `external-partner-mock`: `./mvnw test` dentro de `external-partner-mock/`
- Rodar serviço: `./mvnw spring-boot:run` (transaction-service na porta `8080`; external-partner-mock na `8081`).

Quando terminar uma mudança, rode os testes do módulo afetado antes de declarar pronto.

## Agente de testes

Existe um agente dedicado (`tester`) em `.opencode/agents/tester.md` para rodar as suítes, escrever testes (JUnit 5 + Mockito + AssertJ) e aplicar TDD.

## Vocabulário de domínio

Leia `CONTEXT.md` na raiz antes de explorar o código. Use o vocabulário do glossário em issues, specs, nomes de teste e propostas. Não invente sinônimos para termos já definidos (ex.: use `status: PROCESSED/FAILED/ERROR` como está, não "sucesso/falha").

## Agent skills

### Issue tracker

Issues e specs vivem no GitHub Issues deste repositório; operações via `gh` CLI. Ver `docs/agents/issue-tracker.md`.

### Triage labels

Labels canônicos de triage: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. Ver `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` na raiz + ADRs em `docs/adr/` (ADR-001–007 ainda em `docs/system-design.md` §8). Ver `docs/agents/domain.md`.

## API e observabilidade

- Swagger UI: `http://localhost:8080/swagger-ui.html` (transaction-service) e `http://localhost:8081/swagger-ui.html` (external-partner-mock); contrato em `/v3/api-docs`.
- Docs manuais: `docs/API.md`, `docs/OBSERVABILITY.md`, `docs/architecture.md`, `docs/system-design.md`.