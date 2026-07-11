# API — transaction-service

> Base URL: `http://localhost:8080`

## Autenticação

*Planejado para a Fase 2.* Todos os endpoints sob `/transactions/**` exigirão um header `Authorization: Bearer <jwt>`. Endpoints de infraestrutura (`/actuator/**`) permanecem públicos.

## POST /transactions

Cria e processa uma transação Pix (cash-in ou cash-out).

### Request

```http
POST /transactions
Content-Type: application/json
Authorization: Bearer <jwt>   # a partir da Fase 2

{
  "transactionId": "txn-123456",
  "type": "CASH_IN",
  "amount": 150.00,
  "accountId": "acc-789"
}
```

| Campo | Tipo | Obrigatório | Regra |
|-------|------|--------------|-------|
| `transactionId` | string | Sim | Não vazio |
| `type` | string | Sim | `CASH_IN` ou `CASH_OUT` |
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
| `reason` | Motivo da falha, quando aplicável (ex: `"saldo insuficiente"`) |

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

*A partir da Fase 2.* Retornado quando o token JWT está ausente ou é inválido.

### Response — 504 / erro tratado

*A partir da Fase 2.* Retornado (ou convertido em `status: ERROR` no corpo) quando o `external-partner-mock` não responde dentro do timeout configurado, mesmo após as tentativas de retry.

## Endpoints de Infraestrutura (Spring Boot Actuator)

| Endpoint | Descrição |
|----------|-----------|
| `GET /actuator/health` | Status geral da aplicação (liveness/readiness) |
| `GET /actuator/prometheus` | Métricas no formato Prometheus |

Ver detalhes em [`OBSERVABILITY.md`](./OBSERVABILITY.md).

## API — external-partner-mock (planejado, Fase 3)

> Base URL: `http://localhost:8081`

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

Ambos os endpoints podem responder com latência artificial ou erro simulado, conforme configuração descrita em [`architecture.md`](./architecture.md).
