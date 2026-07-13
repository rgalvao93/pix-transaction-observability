# Guia de Desenvolvimento

## Pré-requisitos

- Java 21+
- Maven Wrapper incluído (não precisa instalar Maven manualmente)
- Docker + Docker Compose *(necessário a partir da Fase 4, para subir a stack de observabilidade)*

## Estrutura de Pastas

```
pix-transaction-observability/
├── README.md
├── docs/                          # Documentação do projeto
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
├── external-partner-mock/         # Mock do parceiro externo (Fase 3)
└── observability/                 # Stack Prometheus/Grafana (Fase 4)
```

## Rodando o transaction-service localmente

```bash
cd transaction-service
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

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

Sem o `external-partner-mock` rodando (Fase 3), a chamada acima retorna `status: "ERROR"` — o `transaction-service` tenta se comunicar com `http://localhost:8081` e falha após as tentativas de retry.

## Rodando os testes

```bash
cd transaction-service
./mvnw test
```

## Rodando o external-partner-mock (a partir da Fase 3)

```bash
cd external-partner-mock
./mvnw spring-boot:run
```

Sobe em `http://localhost:8081`. O `transaction-service` deve ser configurado (via `application.properties`) para apontar para essa URL.

## Rodando a stack de observabilidade (a partir da Fase 4)

```bash
cd observability
docker compose up -d
```

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## Convenções de Código

- Pacotes Java seguem `com.rodrigogalvao.transaction.<camada>` (`controller`, `service`, `dto`, `model`).
- DTOs de request/response ficam em `dto/`; enums e regras de domínio ficam em `model/`.
- Validações de payload usam `jakarta.validation` diretamente nos DTOs.
- Logs devem ser estruturados (nunca `System.out.println`) — usar SLF4J (`LoggerFactory.getLogger`).

## Branches e Commits

- Branch de desenvolvimento atual: `claude/project-documentation-c3Fgv`.
- Commits em português, no imperativo, descrevendo a mudança (ex: `atualiza TransactionService com lógica de cash-in`).
