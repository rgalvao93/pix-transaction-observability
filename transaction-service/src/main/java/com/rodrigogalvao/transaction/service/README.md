# Service

Camada de lógica de negócio do processamento de transações Pix.

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `TransactionService` | Orquestra o processamento de uma transação: idempotência, fluxos de cash-in/cash-out, verificação de saldo, limite diário, transferência no parceiro e registro de métricas. |

## Fluxo

1. **Idempotência**: se o `transactionId` já foi processado, retorna o resultado em cache.
2. **Cash-in** (`CASH_IN`): verifica disponibilidade da conta no parceiro → executa a transferência → `PROCESSED`.
3. **Cash-out** (`CASH_OUT`): verifica saldo suficiente no parceiro → reserva o valor no limite diário (`DailyLimitTracker`) → executa a transferência → `PROCESSED`.
4. **Falha no parceiro** (`ExternalPartnerException`): retorna `ERROR` com `"falha de comunicação com o parceiro externo"`.

## Estados de saída

| Status | Cenário |
|--------|---------|
| `PROCESSED` | Operação concluída com sucesso. |
| `FAILED` | Falha de negócio: conta não disponível, saldo insuficiente, limite diário excedido ou transferência recusada. |
| `ERROR` | Falha técnica: comunicação com o parceiro externo. |

## Métricas

- `transaction_total` — contador por `type` e `status`.
- `transaction_processing_duration_seconds` — duração do processamento por `type` e `status` (histograma de percentis).