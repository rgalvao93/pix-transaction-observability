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
- Validação de payload (`jakarta.validation`), incluindo o enum `TransactionType` (`CASH_IN`/`CASH_OUT`)
- Lógica de cash-in / cash-out, com idempotência por `transactionId` (`TransactionRepository`, em memória)
- Limite diário de cash-out por conta (`DailyLimitTracker`, em memória, configurável via `transaction.cash-out.daily-limit`)
- Chamada ao parceiro externo via `ExternalPartnerClient`/`HttpExternalPartnerClient` (RestClient), com retry e timeout configuráveis (`partner.retry.*`, `partner.connect-timeout-ms`, `partner.read-timeout-ms`)
- Autenticação via JWT (Spring Security) — endpoint `POST /auth/token` emite tokens de teste (ver [`API.md`](./API.md))
- Logs estruturados via SLF4J em cada etapa crítica

### external-partner-mock
Serviço separado (Spring Boot) que simula o comportamento de um parceiro externo (PSP ou Banco Central). Permite configurar latência e taxa de falha para testar resiliência do `transaction-service`. *Ainda não implementado — Fase 3.*

### observability
Configuração Docker Compose com Prometheus (scrape de métricas), Grafana (dashboards) e AlertManager (alertas). *Ainda não implementado — Fase 4.*

## Fluxo — Cash-In

```
1. Cliente autentica via POST /auth/token e envia POST /transactions { type: CASH_IN, accountId, amount } com Bearer token
2. transaction-service valida o payload (campos obrigatórios, amount > 0, type é CASH_IN/CASH_OUT)
3. Se transactionId já foi processado antes → retorna a resposta em cache (idempotência), sem repetir os passos abaixo
4. transaction-service chama external-partner-mock: POST /partner/verify-balance
5. Se a conta não estiver disponível → status = FAILED ("conta não disponível")
6. transaction-service chama external-partner-mock: POST /partner/transfer
7. Se o parceiro aceitar → status = PROCESSED; se recusar → status = FAILED; se a comunicação falhar após as tentativas de retry → status = ERROR
8. Resposta retornada ao cliente (sempre 200 OK, com o status refletido no corpo) + logs emitidos em cada etapa
```

## Fluxo — Cash-Out

Mesmo fluxo do cash-in, com verificações adicionais entre os passos 4 e 6:
- **Saldo insuficiente**: se `balance < amount` → `FAILED` ("saldo insuficiente")
- **Limite diário**: se a soma do dia para a conta ultrapassar `transaction.cash-out.daily-limit` → `FAILED` ("limite diário excedido")
- Falhas técnicas de comunicação continuam mapeadas para `ERROR` (nunca `FAILED`, que é reservado a regras de negócio)

## Status de Transação

| Status | Significado |
|--------|-------------|
| `PROCESSED` | Transação concluída com sucesso |
| `FAILED` | Falha de negócio (ex: saldo insuficiente, limite excedido) |
| `ERROR` | Falha técnica (timeout, erro de comunicação com o parceiro) |

## Estrutura de Diretórios

| Diretório | Descrição | Status |
|-----------|-----------|--------|
| `transaction-service/` | Microsserviço principal | Lógica de negócio, segurança JWT e testes implementados (Fase 2) |
| `external-partner-mock/` | Mock do parceiro externo | Não implementado — Fase 3 |
| `observability/` | Stack Prometheus/Grafana | Não implementado — Fase 4 |
| `docs/` | Documentação e diagramas | Em construção |

## Notas de Compatibilidade (Spring Boot 4)

O `transaction-service` usa Spring Boot 4, que modularizou dependências antes agrupadas em `spring-boot-starter-web`/`spring-boot-starter-test`. Pontos relevantes para quem for mexer no projeto:
- Jackson requer o starter explícito `spring-boot-starter-jackson`, que traz o **Jackson 3** (`tools.jackson.*`, não mais `com.fasterxml.jackson.*`) como implementação padrão.
- Suporte a `MockMvc`/`@AutoConfigureMockMvc` em testes está no artefato `spring-boot-webmvc-test`, sob o pacote `org.springframework.boot.webmvc.test.autoconfigure`.
- `@MockBean` foi substituído por `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean`).
