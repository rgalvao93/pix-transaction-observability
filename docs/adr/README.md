# ADRs

_Architecture Decision Records_ deste repositório.

## Convenção

- Nome de arquivo: `NNNN-titulo-curto-kebab-case.md`, onde `NNNN` é o próximo número sequencial.
- Formato: Contexto → Alternativas consideradas → Decisão → Consequências.
- Ledgeres de escopo maiores podem ser listados em `docs/system-design.md` §8 antes de migrarem para cá.

## Registros

- **ADR-001 a ADR-007** — arquivados na seção "Decisões de Arquitetura (ADRs)" de [`docs/system-design.md`](../system-design.md):
  - ADR-001 Observability-first desde o Dia Um
  - ADR-002 Estado em memória no MVP (sem persistência)
  - ADR-003 Autenticação JWT demo com segredo compartilhado
  - ADR-004 Operação síncrona REST (em vez de fila assíncrona)
  - ADR-005 HTTP 200 sempre + status de negócio no corpo
  - ADR-006 Retry com backoff crescente no cliente HTTP
  - ADR-007 Java 21 + Spring Boot 4

A partir do ADR-008, novos registros são arquivados aqui como arquivos individuais.

- [**ADR-008 — Contrato OpenAPI 3.1 + spec-driven**](0008-openapi-contract.md)