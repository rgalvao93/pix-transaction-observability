# Repository

Camada de acesso/armazenamento de dados. Neste MVP as implementações são **em memória** (thread-safe), sem banco de dados externo.

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `TransactionRepository` | Armazena transações processadas por `transactionId` em um `ConcurrentHashMap`. Permite buscar (`findByTransactionId`) e salvar (`save`). Suporta **idempotência**: uma transação já processada retorna o resultado em cache. |
| `DailyLimitTracker` | Controla o **limite diário de cash-out** por conta. Mantém o total acumulado por `accountId` + data em um `ConcurrentHashMap`, com **reset natural à meia-noite** (a chave inclui `LocalDate.now()`). O método `tryReserve` verifica e reserva o valor atomicamente. |

## Limite diário

| Propriedade | Padrão |
|-------------|--------|
| `transaction.cash-out.daily-limit` | `5000.00` |

## Limitações (MVP)

- Dados voláteis: o estado é perdido no reinício da aplicação.
- Não é adequado para múltiplas instâncias (estado local por JVM) nem para histórico de longo prazo.