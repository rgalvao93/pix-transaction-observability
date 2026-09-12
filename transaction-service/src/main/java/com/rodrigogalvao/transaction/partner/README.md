# Partner

Integração com o parceiro externo que executa as operações reais de verificação de saldo e transferência (simulado pelo `external-partner-mock`).

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `ExternalPartnerClient` | Interface que define o contrato de integração: `verifyBalance(accountId)` e `transfer(accountId, amount, type)`. |
| `HttpExternalPartnerClient` | Implementação via `RestClient`. Gera um JWT próprio (`transaction-service`) para autenticar no parceiro, aplica **retry com backoff** linear e mede chamadas com métricas Micrometer. |
| `PartnerClientConfig` | Configura o `RestClient.Builder` com timeouts de conexão (`partner.connect-timeout-ms`, padrão 1000) e leitura (`partner.read-timeout-ms`, padrão 2000). |
| `ExternalPartnerException` | Exceção lançada quando todas as tentativas de chamada ao parceiro falham. |

## Operações

| Operação | Endpoint do parceiro | Descrição |
|----------|----------------------|-----------|
| `verifyBalance` | `POST /partner/verify-balance` | Consulta saldo e disponibilidade da conta. |
| `transfer` | `POST /partner/transfer` | Executa a transferência (cash-in ou cash-out). |

## Configuração

| Propriedade | Padrão | Descrição |
|-------------|--------|-----------|
| `partner.base-url` | `http://localhost:8081` | URL base do parceiro. |
| `partner.retry.max-attempts` | `3` | Máximo de tentativas por chamada. |
| `partner.retry.backoff-ms` | `200` | Backoff base linear (multiplicado pela tentativa). |
| `partner.connect-timeout-ms` | `1000` | Timeout de conexão. |
| `partner.read-timeout-ms` | `2000` | Timeout de leitura. |

## Métricas

- `external_partner_call_duration_seconds` — duração por operação/resultado (`operation`, `outcome=success|failure`).
- `external_partner_calls_failed_total` — contador de falhas após esgotar as tentativas.