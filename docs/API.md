# API — transaction-service

> Base URL: `http://localhost:8080`

## Autenticação

Todos os endpoints sob `/transactions/**` exigem um header `Authorization: Bearer <jwt>`. Endpoints de infraestrutura (`/actuator/**`) e `/auth/**` são públicos.

### POST /auth/token

Endpoint de **desenvolvimento/teste** que emite um JWT para o `clientId` informado, sem validar credenciais. Não é autenticação real — serve apenas para obter um token e testar os endpoints protegidos localmente, até que um provedor de identidade seja integrado.

```http
POST /auth/token
Content-Type: application/json

{ "clientId": "test-client" }
```

```json
// Response — 200 OK
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

O segredo usado para assinar o token é configurado via `security.jwt.secret` (env var `SECURITY_JWT_SECRET`); o valor padrão em `application.properties` é apenas para demonstração.

## POST /transactions

Cria e processa uma transação Pix (cash-in ou cash-out).

### Request

```http
POST /transactions
Content-Type: application/json
Authorization: Bearer <jwt>

{
  "transactionId": "txn-123456",
  "type": "CASH_IN",
  "amount": 150.00,
  "accountId": "acc-789"
}
```

| Campo | Tipo | Obrigatório | Regra |
|-------|------|--------------|-------|
| `transactionId` | string | Sim | Não vazio. Reenviar o mesmo `transactionId` retorna a resposta já processada (idempotência), sem chamar o parceiro externo novamente. |
| `type` | string | Sim | `CASH_IN` ou `CASH_OUT` (enum `TransactionType`); qualquer outro valor é rejeitado com 400 |
| `amount` | decimal | Sim | Maior que zero |
| `accountId` | string | Sim | Não vazio |

### Response — 200 OK

```json
{
  "transactionId": "txn-123456",
  "status": "PROCESSED",
  "reason": null
}
```

| Campo | Descrição |
|-------|-----------|
| `status` | `PROCESSED`, `FAILED` (falha de negócio) ou `ERROR` (falha técnica) |
| `reason` | Motivo da falha, quando aplicável |

Motivos de `FAILED` conhecidos: `"conta não disponível"` (cash-in), `"saldo insuficiente"` (cash-out), `"limite diário excedido"` (cash-out), `"falha na transferência"` (parceiro recusou). Motivo de `ERROR`: `"falha de comunicação com o parceiro externo"`.

### Response — 400 Bad Request

Retornado quando a validação do payload falha (campo ausente, `amount` <= 0, etc).

```json
{
  "timestamp": "2026-07-11T10:00:00Z",
  "status": 400,
  "errors": [
    "amount must be greater than zero"
  ]
}
```

### Response — 401 Unauthorized

Retornado quando o header `Authorization` está ausente ou o token JWT é inválido/expirado.

### Falha de comunicação com o parceiro externo

Quando o `external-partner-mock` não responde dentro do timeout configurado (mesmo após as tentativas de retry — ver `partner.retry.*` em [`architecture.md`](./architecture.md)), a resposta continua **200 OK**, mas com `status: "ERROR"` no corpo — não há um código HTTP de erro dedicado para esse caso.

## Endpoints de Infraestrutura (Spring Boot Actuator)

| Endpoint | Descrição |
|----------|-----------|
| `GET /actuator/health` | Status geral da aplicação (liveness/readiness) |
| `GET /actuator/prometheus` | *Planejado, Fase 4.* Métricas no formato Prometheus — hoje o `pom.xml` não inclui `micrometer-registry-prometheus` e o endpoint não está exposto em `application.properties`. |

Ver detalhes em [`OBSERVABILITY.md`](./OBSERVABILITY.md).

## API — external-partner-mock

> Base URL: `http://localhost:8081`

Assim como o `transaction-service`, todos os endpoints sob `/partner/**` exigem `Authorization: Bearer <jwt>`. O token é validado com o **mesmo segredo** (`security.jwt.secret` / env var `SECURITY_JWT_SECRET`) usado pelo `transaction-service` para assiná-lo — o `HttpExternalPartnerClient` gera um token novo a cada chamada.

Duas contas de demonstração já vêm pré-carregadas em memória: `acc-123` (saldo 1000.00) e `acc-789` (saldo 500.00). Qualquer outra `accountId` é tratada como desconhecida (`available: false`).

### POST /partner/verify-balance

```json
// Request
{ "accountId": "acc-789" }

// Response
{ "accountId": "acc-789", "balance": 500.00, "available": true }
```

### POST /partner/transfer

```json
// Request
{ "accountId": "acc-789", "amount": 150.00, "type": "CASH_IN" }

// Response
{ "success": true, "partnerReference": "psp-ref-987" }
```

`transfer` aplica o delta (`CASH_IN` soma, `CASH_OUT` subtrai) ao saldo em memória quando `success: true`. Ambos os endpoints simulam latência artificial e uma taxa de falha configuráveis (`partner.mock.latency.min-ms`/`max-ms`, `partner.mock.failure-rate` — ver [`architecture.md`](./architecture.md)); uma falha simulada retorna `success: false` com HTTP 200, não um erro HTTP.
