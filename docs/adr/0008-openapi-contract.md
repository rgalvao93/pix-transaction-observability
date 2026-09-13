# ADR-008 — Contrato OpenAPI 3.1 + spec-driven

- **Status**: Aceita (2026-09-12)
- **Escopo**: Ambos os serviços (`transaction-service`, `external-partner-mock`)
- **Issue de origem**: [#4 — Add OpenAPI contract (springdoc + versioned spec)](https://github.com/rgalvao93/pix-transaction-observability/issues/4)

## Contexto

Até aqui a API era documentada só por `docs/API.md` (manual) e pelo Swagger ausente. Não havia uma fonte única de verdade de contrato: nada gerava a especificação a partir do código, e não existia verificação de que a documentação continuava atual conforme a API evoluía. Isso dificulta consumo por parceiros e validação de contratos (spec-first / schema-first).

## Alternativas consideradas

1. **OpenAPI manual (arquivo escrito à mão)** — contrato desacoplado do código; risco alto de divergência silenciosa; rejeitado.
2. **springdoc-openapi no runtime (`/v3/api-docs`) + contrato versionado em `api-spec/` + drift check no CI** — contrato canônico gerado a partir das anotações/reflexão (schema-first), sempre fiel à implementação; fornece Swagger UI grátis; drift detecta mudanças deliberadas antes do merge. **Escolhida.**
3. **Contract test dedicado só (sem versionamento)** — mantido como smoke test, mas insuficiente sozinho.

## Decisão

- Adicionar `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1` (compatível com Spring Boot 4) nos dois serviços.
- Liberar no `SecurityConfig` de cada serviço: `/v3/api-docs/**`, `/swagger-ui.html`, `/swagger-ui/**` (público; junto com `/actuator/**` e `/auth/**` já existentes).
- Contrato canônico em runtime: `GET /v3/api-docs` (JSON) por serviço. Swagger UI em `/swagger-ui.html`.
- Contrato versionado em `api-spec/`:
  - `transaction-service.openapi.yaml` e `external-partner-mock.openapi.yaml`;
  - regenerados por `scripts/export-openapi.sh` — que busca o JSON canônico e o converte para YAML (PyYAML) de forma determinística.
- Drift check em `.github/workflows/openapi-drift.yml`: sobe os dois serviços, regera `api-spec/` e falha se `git diff --exit-code api-spec/` acusar diferença.
- Autenticação aparece como `securitySchemes.bearerAuth` (HTTP Bearer JWT); `/auth/token` permanece público.
- `servers` fixo via `@OpenAPIDefinition(servers = @Server(url = "http://localhost:8080"/"8081"))` — impossibilita vazamento de host/porta do ambiente gerador no contrato versionado.

## Consequências

**Positivas**
- Fonte única de verdade do contrato, gerada do código (schema-first na prática).
- Swagger UI interativo por serviço para testes manuais.
- Mudança de contrato exige atualizar `api-spec/` de propósito — drift vira erro de CI, não surpresa em prod.
- Vocabulário de domínio (ADRs de domínio/CONTEXT.md) refletido nas `description`/`example` dos schemas.

**Negativas / riscos**
- No springdoc v3 (Boot 4), `GET /v3/api-docs.yaml` e `Accept: application/yaml` não servem YAML — o caminho não é liberado pela regra `/v3/api-docs/**` e o `?format=yaml` ignora o formato. Por isso o script converte JSON→YAML localmente.
- Em motores, adicionar `description`/`example` em DTOs deixa o contrato mais verboso; requer cuidado para não divergir da implementação real (ex.: enums `PROCESSED/FAILED/ERROR`, `CASH_IN/CASH_OUT`).
- Drift check sobe os serviços completos no CI — custo de alguns minutos por execução.