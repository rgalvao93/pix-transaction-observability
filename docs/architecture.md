# Arquitetura — Pix Transaction Observability

## Visão Geral

O sistema simula um serviço transacional Pix operando em ambiente regulamentado, com observabilidade nativa desde o design. É composto por dois serviços independentes e uma stack de observabilidade.

```
                         ┌─────────────────────────┐
                         │   Cliente / Consumidor   │
                         └────────────┬────────────┘
                                      │ HTTP (JWT Bearer)
                                      ▼
                         ┌─────────────────────────┐
                         │   transaction-service     │  :8080
                         │  (cash-in / cash-out)     │
                         └────────────┬────────────┘
                                      │ HTTP (verify-balance, transfer)
                                      ▼
                         ┌─────────────────────────┐
                         │  external-partner-mock    │  :8081
                         │ (simula PSP / Banco Central)│
                         │  latência/erros configuráveis│
                         └─────────────────────────┘

     Ambos os serviços emitem logs (JSON) e métricas (Prometheus)
                                      │
                                      ▼
                         ┌─────────────────────────┐
                         │  observability stack      │
                         │  Prometheus + Grafana +   │
                         │  AlertManager (Docker Compose)│
                         └─────────────────────────┘
```

## Componentes

### transaction-service
Microsserviço principal (Spring Boot 4 / Java 21). Recebe requisições de transação Pix, valida regras de negócio, orquestra a chamada ao parceiro externo e retorna o resultado. Responsável por:
- Validação de payload (`jakarta.validation`)
- Lógica de cash-in / cash-out
- Autenticação via JWT (Spring Security) — *planejado, Fase 2*
- Logs estruturados e métricas de negócio

### external-partner-mock
Serviço separado (Spring Boot) que simula o comportamento de um parceiro externo (PSP ou Banco Central). Permite configurar latência e taxa de falha para testar resiliência do `transaction-service`. *Ainda não implementado — Fase 3.*

### observability
Configuração Docker Compose com Prometheus (scrape de métricas), Grafana (dashboards) e AlertManager (alertas). *Ainda não implementado — Fase 4.*

## Fluxo — Cash-In

```
1. Cliente envia POST /transactions { type: CASH_IN, accountId, amount }
2. transaction-service valida o payload (campos obrigatórios, amount > 0)
3. transaction-service chama external-partner-mock: POST /partner/verify-balance
4. Se OK → transação marcada como PENDING (log estruturado)
5. transaction-service chama external-partner-mock: POST /partner/transfer
6. Se OK → status = PROCESSED; se falhar → status = FAILED ou ERROR
7. Resposta retornada ao cliente + logs/métricas emitidos em cada etapa
```

## Fluxo — Cash-Out

Mesmo fluxo do cash-in, acrescido de:
- Verificação de limite diário de saque antes de acionar o parceiro externo
- Regra de negócio adicional: saldo insuficiente → `FAILED` (não `ERROR`, que é reservado a falhas técnicas)

## Status de Transação

| Status | Significado |
|--------|-------------|
| `PROCESSED` | Transação concluída com sucesso |
| `FAILED` | Falha de negócio (ex: saldo insuficiente, limite excedido) |
| `ERROR` | Falha técnica (timeout, erro de comunicação com o parceiro) |

## Known Issues (a resolver na Fase 2)

- **Duplicação de classes**: hoje existem duas versões de `TransactionRequest`/`TransactionResponse`:
  - `dto.TransactionRequest` / `dto.TransactionResponse` — usadas pelo `TransactionController`, com anotações de validação.
  - `model.TransactionRequest` / `model.TransactionResponse` / `model.TransactionStatus` — usadas pelo `TransactionService`, sem validação.
  - O `TransactionController` **não chama** o `TransactionService` atualmente — monta a resposta diretamente. Isso será unificado na Fase 2, consolidando em um único conjunto de classes (`dto` para transporte HTTP, `model` para status/domínio) e conectando o controller ao service real.

## Estrutura de Diretórios

| Diretório | Descrição | Status |
|-----------|-----------|--------|
| `transaction-service/` | Microsserviço principal | Estrutura básica implementada |
| `external-partner-mock/` | Mock do parceiro externo | Não implementado |
| `observability/` | Stack Prometheus/Grafana | Não implementado |
| `docs/` | Documentação e diagramas | Em construção |
