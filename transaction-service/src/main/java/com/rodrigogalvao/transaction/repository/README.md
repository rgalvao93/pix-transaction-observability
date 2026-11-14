# Repository

Camada de acesso/armazenamento de dados. Os dados são persistidos no PostgreSQL via Spring Data JPA e as tabelas são criadas pelo Flyway.

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `TransactionRepository` | Persiste as transações por `transactionId` em `transactions`. Permite buscar (`findByTransactionId`) e salvar (`save`). A chave primária garante unicidade e permite recuperar a mesma resposta após reinício. |
| `DailyLimitTracker` | Persiste o limite diário de cash-out por conta/data em `daily_limits`. `tryReserve` bloqueia a linha do limite para serializar reservas concorrentes entre instâncias; na primeira reserva do dia, usa uma linha de coordenação para evitar inserções duplicadas. |

## Limite diário

| Propriedade | Padrão |
|-------------|--------|
| `transaction.cash-out.daily-limit` | `5000.00` |

## Persistência e configuração

- Banco padrão: `jdbc:postgresql://localhost:5432/pix_observability`.
- Configuração por `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`.
- Migration inicial: `src/main/resources/db/migration/V1__create_transaction_tables.sql`.
- Testes usam H2; não exigem banco PostgreSQL ativo.