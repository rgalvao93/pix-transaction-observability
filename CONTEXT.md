# Contexto — Pix Transaction Observability

> Glossário e contexto de domínio do repositório. Use este vocabulário em issues, specs, nomes de teste e propostas. Leia antes de explorar o código.

## O que é

MVP de um **serviço transacional Pix** que processa operações de **cash-in** (crédito) e **cash-out** (débito) em um ambiente regulamentado. A premissa central é **observabilidade**: logs JSON, métricas e health checks são requisitos de primeira classe. O serviço orquestra um **parceiro externo** (PSP / Banco Central) simulado, que é a fonte de verdade do saldo.

## Atores

| Ator | Papel |
| --- | --- |
| Cliente / Consumidor | Aplica transações Pix via API, autenticado com JWT |
| Parceiro externo (PSP / Banco Central) | Fonte de verdade de saldo; verifica e transfere (simulado pelo `external-partner-mock`) |
| Operador SRE / Suporte | Consome logs, métricas, alertas e dashboards |
| Desenvolvedor | Evolui o sistema seguindo as convenções documentadas |

## Glossário

| Termo | Definição | Uso |
| --- | --- | --- |
| **Cash-in** | Operação de crédito (depósito) em conta Pix | `type: CASH_IN` |
| **Cash-out** | Operação de débito (saque/transferência) de conta Pix | `type: CASH_OUT` |
| **Transaction-service** | Microsserviço que processa transações e orquestra o parceiro | porta `8080` |
| **External-partner-mock** | Parceiro simulado com contas em memória, latência e falha configuráveis | porta `8081` |
| **Parceiro externo** | PSP ou Banco Central, fonte de verdade do saldo | consultado via `verify-balance`/`transfer` |
| **PROCESSED** | Transação concluída com sucesso no parceiro | status de negócio |
| **FAILED** | Falha de negócio (saldo, limite, conta, recusa) — esperada e categorizada | status de negócio |
| **ERROR** | Falha técnica: comunicação exaurida com o parceiro após retries | status de negócio |
| **Idempotência** | Reenvios do mesmo `transactionId` não reprocessam nem duplicam efeitos | chaveada por `transactionId` para o cache in-memory |
| **Limite diário de cash-out** | Total cash-out permitido por conta por dia; reserva feita antes de transferir | `DailyLimitTracker` in-memory |
| **Retry com backoff** | Repetição de chamada de comunicação ao parceiro com backoff crescente (200ms · attempt) até `max-attempts` (3) | apenas falhas de comunicação disparam retry |
| **Readiness / Liveness** | Sinais de saúde do serviço; readiness de `transaction-service` inclui `partnerConnectivity` | `/actuator/health` |
| **SLI / SLO** | Indicador / objetivo de nível de serviço | SLIs instrumentados; SLOs formais não definidos (gap) |
| **Idempotency cache** | `TransactionRepository`, `ConcurrentHashMap` em memória | perdido em restart |
| **Saldo** | Fonte de verdade: o parceiro, não o serviço | contas demo `acc-123` (1000.00) e `acc-789` (500.00) |

## Regras de negócio centrais

- `amount` > 0 e `type` ∈ {`CASH_IN`, `CASH_OUT`}; violação → HTTP 400.
- Conta indisponível → `FAILED` ("conta não disponível"); saldo insuficiente → `FAILED` ("saldo insuficiente"); limite excedido → `FAILED` ("limite diário excedido"); parceiro recusa → `FAILED` ("falha na transferência").
- Comunicação exaurida após retries → `ERROR` ("falha de comunicação com o parceiro externo").
- Resposta de transação é **sempre HTTP 200** com `PROCESSED`/`FAILED`/`ERROR` no corpo; 400 (validação) e 401 (autenticação) são os únicos códigos HTTP de erro (ADR-005).
- o serviço **não** é a fonte de verdade do saldo (RN-09).
- **Quirk conhecido**: no cash-out, falha de transferência **não reverte** a reserva do limite diário — falhas contam contra o limite (RN-05 edge case, GAP-8, risco R-1).

## Decisões de arquitetura

- ADR-001 a ADR-007: registrados em `docs/system-design.md` (§8). A partir do ADR-008, novos ADRs são arquivos individuais em `docs/adr/`.
- Resumo em uma linha: estado em memória no MVP, JWT demo com segredo compartilhado, operação síncrona REST, HTTP 200 + status de negócio, retry com backoff, Java 21 + Spring Boot 4.