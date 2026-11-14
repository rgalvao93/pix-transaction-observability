# ADR-009 — Persistência PostgreSQL nos dois serviços

- **Status**: Aceita (2026-11-12)
- **Escopo**: `transaction-service` e `external-partner-mock`

## Contexto

O MVP mantinha em memória os resultados de transação, reservas do limite diário de cash-out e saldos do parceiro. Reiniciar qualquer serviço apagava esse estado e instâncias distintas não compartilhavam os dados. Era necessário tornar duráveis as informações que controlam idempotência, limite diário e saldo simulado.

## Alternativas consideradas

1. **Manter estado em memória** — simples, mas perde dados em reinícios e não permite compartilhar estado entre instâncias.
2. **PostgreSQL com Spring Data JPA e Flyway** — armazenamento relacional durável, migrations versionadas e locks transacionais; escolhido para os dois serviços.
3. **Redis** — adequado a estado volátil e baixa latência, mas não oferece o histórico relacional necessário sem adicionar outra persistência.

## Decisão

- Usar PostgreSQL como armazenamento durável, Spring Data JPA para acesso e Flyway para migrations; Hibernate apenas valida o schema no startup.
- Em desenvolvimento, os dois serviços compartilham o banco `pix_observability`, isolados nos schemas `transaction_service` e `partner_mock`, cada um com seu histórico Flyway.
- `transaction-service` persiste os dados da requisição e o status/motivo para recuperar a resposta de idempotência após reinícios.
- O limite diário é atualizado sob lock pessimista na linha da conta/data. Uma linha de coordenação impede corrida na criação do primeiro limite de um dia.
- O mock persiste os saldos; `acc-123` e `acc-789` são inseridas pela migration inicial e não são resemeadas em cada startup.
- O banco é configurado por `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`. Testes de integração usam H2; o Compose local fornece PostgreSQL.

## Consequências

**Positivas**

- Transações, reservas diárias e saldos sobrevivem a reinícios enquanto o banco permanecer disponível.
- O limite diário é coordenado entre instâncias; a inicialização de novos registros é serializada pelo banco.
- Mudanças de schema são explícitas e ordenadas por migration.

**Negativas / riscos**

- Cada serviço depende do PostgreSQL para iniciar e processar transações; readiness do `transaction-service` verifica o banco.
- A consulta de idempotência e a chamada ao parceiro ainda não são uma operação atômica. Requisições simultâneas com o mesmo `transactionId`, ou crash entre transferência e gravação do resultado, ainda podem repetir o efeito no parceiro; acompanhar em GAP-2.
- O primeiro limite diário de qualquer conta é serializado pela linha de coordenação compartilhada; depois que o registro da conta/data existe, o lock é por linha.
- Em produção, é necessário configurar uma instância PostgreSQL durável e acessível pelos serviços e fornecer credenciais por um gerenciador de segredos.
