# ADR-010 — Endpoint de token de dev (`/auth/token`) mantido, comutável via config

- **Status**: Aceita (2026-11-14)
- **Escopo**: `transaction-service`
- **Relacionada**: ADR-003 (JWT demo), issue #11 (endurecer defaults), GAP-4 (IdP real)

## Contexto

`POST /auth/token` emite um JWT para qualquer `clientId` **sem validar credenciais**. É
conveniência de desenvolvimento (README, Postman, testes manuais), mas num deploy real
qualquer um poderia forjar tokens para os endpoints protegidos. A issue #11 exigia uma
decisão registrada sobre o endpoint **antes de qualquer deploy real**, e a substituição
por um provedor de identidade (GAP-4) é maior que o endurecimento dos defaults.

## Alternativas consideradas

1. **Remover o endpoint imediatamente** — segurança máxima, mas quebra o fluxo de
   desenvolvimento local e a coleção Postman antes de existir um IdP substituto.
2. **Manter como está (sempre ligado)** — sem custo de DX, mas nenhum mecanismo
   operacional para desligá-lo em produção; depende de lembrete humano.
3. **Manter, mas comutável por configuração** — escolhido: mesmo DX, com um corte
   explícito e testável que o checklist de deploy pode aplicar sem mudança de código.

## Decisão

- O endpoint permanece por enquanto, comutável pela propriedade
  `security.dev-token-enabled` (env `SECURITY_DEV_TOKEN_ENABLED`), default `true`
  para não quebrar desenvolvimento local. Com `false`, o bean não é carregado e
  `POST /auth/token` responde **404** (coberto por `DevTokenEndpointDisabledTest`).
- Qualquer deploy real **deve** definir `SECURITY_DEV_TOKEN_ENABLED=false` — exigido
  em [`docs/DEPLOY.md`](../../DEPLOY.md).
- A remoção definitiva acontece junto com a integração do provedor de identidade
  (GAP-4 / Fase 5 do [`PLAN.md`](../../PLAN.md)).
- Complemento da issue #11: `SECURITY_JWT_SECRET` **não tem mais default** em
  nenhum dos dois serviços — o boot falha sem a env.

## Consequências

**Positivas**

- Operações pode desabilitar o endpoint por variável de ambiente, sem rebuild.
- A decisão fica registrada e verificável por teste; o checklist de deploy tem o passo.
- O segredo JWT deixou de ter valor demo conhecido num repo público.

**Negativas / riscos**

- O default `true` continua exposto se um deploy esquecer a flag — mitigado pelo
  checklist em `DEPLOY.md` e pela remoção futura no GAP-4.
- Enquanto o IdP real não chegar, quem precisa de tokens em dev continua dependendo
  desse endpoint (comportamento conhecido, documentado).
