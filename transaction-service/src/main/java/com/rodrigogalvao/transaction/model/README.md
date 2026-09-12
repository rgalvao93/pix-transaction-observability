# Model

Modelo de domínio do serviço de transações. Concentra as enumerações que descrevem os conceitos centrais do negócio.

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `TransactionType` | Direção da operação: `CASH_IN` (entrada) ou `CASH_OUT` (saída). |
| `TransactionStatus` | Estado final de uma transação: |
|        | - `PROCESSED` — processada com sucesso. |
|        | - `FAILED` — falha de negócio (ex: saldo insuficiente, limite diário excedido). |
|        | - `ERROR` — erro técnico / instabilidade (ex: falha de comunicação com o parceiro). |

## Observação

O serviço usa enums para o domínio, mas expõe os valores como `String` no `TransactionResponse` (via `name()`) — o que facilita a serialização JSON sem acoplamento direto com o DTO de saída.