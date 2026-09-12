# Guia de Desenvolvimento

## Pré-requisitos

- Java 21+
- Maven Wrapper incluído (não precisa instalar Maven manualmente)
- Docker + Docker Compose *(necessário apenas para subir a stack de observabilidade — Prometheus/Grafana/AlertManager)*

## Estrutura de Pastas

```
pix-transaction-observability/
├── README.md
├── docs/                          # Documentação do projeto
│   ├── system-design.md           # Requisitos (funcionais e não-funcionais), regras de negócio, ADRs e gaps
│   ├── architecture.md
│   ├── API.md
│   ├── OBSERVABILITY.md
│   └── DEVELOPMENT.md
├── transaction-service/           # Microsserviço principal
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/rodrigogalvao/transaction/
│       │   ├── TransactionServiceApplication.java
│       │   ├── controller/        # Endpoints REST (TransactionController, AuthController)
│       │   ├── service/           # Lógica de negócio
│       │   ├── dto/               # Objetos de transporte HTTP (request/response/erro)
│       │   ├── model/             # Domínio (TransactionType, TransactionStatus)
│       │   ├── partner/           # Cliente HTTP do parceiro externo (retry/timeout)
│       │   ├── repository/        # Repositórios em memória (idempotência, limite diário)
│       │   ├── security/          # JWT (provider, filtro, configuração)
│       │   └── exception/         # Tratamento global de erros (400/401)
│       └── test/                  # Testes unitários e de integração
├── external-partner-mock/         # Mock do parceiro externo
│   ├── pom.xml
│   └── src/main/java/com/rodrigogalvao/partnermock/
│       ├── PartnerMockApplication.java
│       ├── controller/             # PartnerController (verify-balance, transfer)
│       ├── service/                # PartnerService (contas em memória, latência/falha)
│       ├── dto/, model/, security/, exception/  # mesmos papéis do transaction-service
└── observability/                 # Stack Prometheus + Grafana + AlertManager (Docker Compose)
    ├── docker-compose.yml
    ├── prometheus/                 # prometheus.yml (scrape config) + rules.yml (alertas)
    ├── grafana/                    # provisioning (datasource/dashboard) + dashboards/*.json
    └── alertmanager/                # alertmanager.yml
```

## Rodando os dois serviços localmente

Suba primeiro o `external-partner-mock` (porta 8081), depois o `transaction-service` (porta 8080) — ambos precisam do **mesmo** `SECURITY_JWT_SECRET` para que os tokens emitidos por um sejam aceitos pelo outro (por padrão os dois usam o mesmo valor de demonstração em `application.properties`, então basta rodar sem sobrescrever nada):

```bash
cd external-partner-mock
./mvnw spring-boot:run
```

```bash
# em outro terminal
cd transaction-service
./mvnw spring-boot:run
```

### Testando manualmente

Endpoints sob `/transactions` exigem um JWT. Obtenha um token de teste em `/auth/token` (ver [`API.md`](./API.md) — não é autenticação real, apenas conveniência de desenvolvimento):

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"clientId": "dev"}' | jq -r .token)

curl -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "transactionId": "txn-001",
    "type": "CASH_IN",
    "amount": 100.00,
    "accountId": "acc-123"
  }'
```

Com o `external-partner-mock` rodando, a resposta deve ser `{"status":"PROCESSED",...}` (`acc-123` já vem pré-carregada com saldo 1000.00). Sem o mock rodando, a mesma chamada retorna `status: "ERROR"` — o `transaction-service` tenta se comunicar com `http://localhost:8081`, falha após as tentativas de retry, e reporta o erro no corpo da resposta.

## Rodando os testes

```bash
cd transaction-service && ./mvnw test
cd external-partner-mock && ./mvnw test
```

## Rodando a stack de observabilidade

Com `transaction-service` e `external-partner-mock` já rodando localmente (ver acima), suba a stack:

```bash
cd observability
docker compose up -d
```

- **Prometheus**: `http://localhost:9090` — em Status → Targets, confira que `transaction-service` e `external-partner-mock` aparecem `UP`
- **Grafana**: `http://localhost:3000` (login `admin`/`admin`) — dashboard "Pix Transaction Observability" já provisionado, com painéis de taxa de erro, latência p95 e falhas de comunicação com o parceiro
- **AlertManager**: `http://localhost:9093`

Gere tráfego de teste (sucesso, saldo insuficiente, falha simulada) para ver os painéis reagirem — ver exemplos de `curl` em [Testando manualmente](#testando-manualmente) acima. Para forçar falhas simuladas do parceiro, suba o `external-partner-mock` com `partner.mock.failure-rate` mais alto, ex: `PARTNER_MOCK_FAILURE_RATE=0.5 ./mvnw spring-boot:run` (ou ajuste a propriedade diretamente).

> A sintaxe do `docker-compose.yml` foi validada com `docker compose config`; o `docker compose up` completo (baixar as imagens e subir os três containers) não foi executado no ambiente de desenvolvimento original, que não tinha o daemon Docker disponível. Ver detalhes em [`OBSERVABILITY.md`](./OBSERVABILITY.md).

## Convenções de Código

- Pacotes Java seguem `com.rodrigogalvao.transaction.<camada>` (`controller`, `service`, `dto`, `model`).
- DTOs de request/response ficam em `dto/`; enums e regras de domínio ficam em `model/`.
- Validações de payload usam `jakarta.validation` diretamente nos DTOs.
- Logs devem ser estruturados (nunca `System.out.println`) — usar SLF4J (`LoggerFactory.getLogger`).

## Branches e Commits

- Branch de desenvolvimento atual: `claude/project-documentation-c3Fgv`.
- Commits em português, no imperativo, descrevendo a mudança (ex: `atualiza TransactionService com lógica de cash-in`).
