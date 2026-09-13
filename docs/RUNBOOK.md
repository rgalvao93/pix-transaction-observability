# Runbook — Operação e Diagnóstico

Guia de operação do MVP: como reproduzir cada estado de transação, como diagnosticar incidentes com logs/métricas/alertas e o que fazer em cada alerta. Detalhe dos pilares de observabilidade em [`OBSERVABILITY.md`](OBSERVABILITY.md); métricas e alertas em [`system-design.md`](system-design.md) (§7).

## 1. Subir o ambiente

Terminal 1 — mock do parceiro (porta 8081):

```bash
cd external-partner-mock && ./mvnw spring-boot:run
```

Terminal 2 — transaction-service (porta 8080):

```bash
cd transaction-service && ./mvnw spring-boot:run
```

Opcional — stack de observabilidade:

```bash
cd observability && docker compose up -d
```

Prometheus `http://localhost:9090` · Grafana `http://localhost:3000` (admin/admin) · AlertManager `http://localhost:9093`.

## 2. Catálogo de cenários reproduzíveis

Obtenha um token de teste:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"clientId": "dev"}' | jq -r .token)
echo "$TOKEN"
```

| Cenário | Como reproduzir | Resultado esperado |
| --- | --- | --- |
| Cash-in PROCESSED | `CASH_IN` 50.00 em `acc-123` | `status: PROCESSED` |
| Cash-out PROCESSED | `CASH_OUT` 100.00 em `acc-789` (saldo 500.00) | `status: PROCESSED` |
| Saldo insuficiente | `CASH_OUT` 99999.00 em `acc-789` | `FAILED` — `"saldo insuficiente"` |
| Limite diário excedido | transaction-service com `TRANSACTION_CASH_OUT_DAILY_LIMIT=100` e `CASH_OUT` 150.00 em `acc-123` | `FAILED` — `"limite diário excedido"` |
| Conta não disponível | `CASH_IN` em `acc-inexistente` | `FAILED` — `"conta não disponível"` |
| Idempotência | reenviar o **mesmo** `transactionId` duas vezes | 2ª chamada retorna a mesma resposta, sem chamar o parceiro de novo |
| Falha simulada | mock com `PARTNER_MOCK_FAILURE_RATE=1`; cash-in | `FAILED` — `"falha na transferência"` (não dispara retry) |
| Falha de comunicação | parar o mock; qualquer transação | `ERROR` — `"falha de comunicação com o parceiro externo"` (após os retries) |

Exemplos de `curl`:

```bash
# Cash-in PROCESSED
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"transactionId":"txn-cashin-1","type":"CASH_IN","amount":50.00,"accountId":"acc-123"}' | jq

# Cash-out → saldo insuficiente
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"transactionId":"txn-out-1","type":"CASH_OUT","amount":99999.00,"accountId":"acc-789"}' | jq

# Idempotência — rode o mesmo payload duas vezes seguidas (2ª vez não re-chama o parceiro)
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"transactionId":"txn-idem-1","type":"CASH_IN","amount":10.00,"accountId":"acc-123"}' | jq

# Sem token → HTTP 401
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"transactionId":"txn-x","type":"CASH_IN","amount":10.00,"accountId":"acc-123"}'

# Payload inválido → HTTP 400 com lista de erros
curl -s -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"transactionId":"","type":"CASH_IN","amount":0,"accountId":"acc-123"}' | jq
```

Como forçar estados via variáveis de ambiente:

```bash
# mock: 100% de falha no transfer + latência alta (exercita FAILED e p95)
PARTNER_MOCK_FAILURE_RATE=1 PARTNER_MOCK_LATENCY_MIN_MS=300 PARTNER_MOCK_LATENCY_MAX_MS=800 ./mvnw spring-boot:run

# transaction-service: limite diário de cash-out reduzido (demonstra "limite diário excedido")
TRANSACTION_CASH_OUT_DAILY_LIMIT=100 ./mvnw spring-boot:run
```

## 3. Diagnóstico — sintomas → causa → verificação

| Sintoma | Causa provável | Verificar |
| --- | --- | --- |
| `ERROR` — "falha de comunicação com o parceiro externo" | mock fora do ar ou timeout mesmo após retries | `up{job="external-partner-mock"}`; `external_partner_calls_failed_total`; logs do `HttpExternalPartnerClient` |
| `FAILED` — "falha na transferência" | taxa de falha simulada alta (`partner.mock.failure-rate`) ou recusa de negócio | painel "Falhas de chamada ao parceiro"; config do mock |
| `FAILED` — "saldo insuficiente" sem motivo aparente | saldo em memória do mock zerado por testes anteriores | `verify-balance` manual; restart do mock (recarrega `acc-123`=1000.00, `acc-789`=500.00) |
| p95 de processamento alto | latência simulada do mock elevada ou timeouts no limite | `partner.mock.latency.*`; `histogram_quantile` de `transaction_processing_duration_seconds` |
| Dashboard sem dados | targets fora do Prometheus ou serviços sem `/actuator/prometheus` | Prometheus → Status → Targets; `curl -s localhost:8080/actuator/prometheus` |
| Restart "perdeu" idempotência/limite/saldo | estado em memória (`ConcurrentHashMap`) — comportamento esperado do MVP | ver GAP-2/GAP-3 em `system-design.md` (persistência) |

Consultas Prometheus úteis:

```promql
# proporção de ERROR sobre o total
sum(rate(transaction_total{status="ERROR"}[5m])) / sum(rate(transaction_total[5m]))
# latência p95 de processamento
histogram_quantile(0.95, sum(rate(transaction_processing_duration_seconds_bucket[5m])) by (le))
# falhas de chamada ao parceiro por operação
sum by (operation) (rate(external_partner_calls_failed_total[5m]))
```

## 4. Alertas — o que fazer

| Alerta | Condição | Severidade | Ação |
| --- | --- | --- | --- |
| `HighTransactionErrorRate` | taxa de `ERROR` > 5% em 5m | warning | checar se o mock está no ar (`up`); subir se baixou; revisar `partner.*-timeout-ms` |
| `PartnerCallFailuresHigh` | >5 falhas de chamada ao parceiro em 10m | warning | mock fora/instável ou latência alta; testar `curl -s localhost:8081/partner/verify-balance` |
| `TransactionServiceDown` | `up{job="transaction-service"} == 0` por 1m | critical | verificar processo (8080), logs de boot, `SECURITY_JWT_SECRET` |
| `ExternalPartnerMockDown` | `up{job="external-partner-mock"} == 0` por 1m | critical | verificar processo (8081); sem mock, transações viram `ERROR` |

## 5. Notas operacionais (MVP)

- **Estado em memória**: idempotência e limite diário (transaction-service) e saldos (mock) são perdidos em restart. Cenários consecutivos que gastam saldo/limite podem precisar de restart para resetar.
- **Quirk RN-05**: no cash-out, falha na transferência **não reverte** a reserva do limite diário — falhas contam contra o limite (correção planejada em GAP-8).
- **Falha simulada ≠ retry**: `success: false` do mock não dispara retry; apenas erro de transporte/HTTP. Para exercitar o caminho de retry, pare o mock ou suba a latência acima do timeout de leitura (`partner.read-timeout-ms`=2s).
- **AlertManager sem canal real**: o receiver `default` é demo e não notifica — configurar webhook/e-mail é item da Fase 5 do [`PLAN.md`](../PLAN.md).