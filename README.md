# Pix Transaction Observability

Este repositório implementa um MVP projetado com **princípios de observabilidade em primeiro lugar** para um serviço transacional Pix. Ele simula operações financeiras de alta confiabilidade, incluindo integração com parceiros externos e tratamento de casos de borda.

## 🏗 Contexto
O componente principal é um serviço transacional Pix que lida com operações de **cash-in** e **cash-out**. Ele opera em um ambiente regulamentado onde a confiabilidade e a rápida resolução de incidentes são fundamentais. O vocabulário de domínio e o glossário ficam em [`CONTEXT.md`](CONTEXT.md) — leia antes de explorar o código.

## 🎯 Objetivos
- **Observabilidade desde o Dia Um**: Logs, métricas e health checks são nativos, não uma reflexão tardia (tracing distribuído está no roadmap — Fase 6 de [`PLAN.md`](PLAN.md)).
- **Resiliência**: Tratamento de latência e erros provenientes de parceiros externos (retry com backoff, timeout).
- **Simulação Realista**: Um ambiente autônomo para testar respostas operacionais.

## 🚀 Tecnologias
- **Java 21**
- **Spring Boot 4**
- **Spring Boot Actuator + Micrometer** (métricas no formato Prometheus)
- **Spring Security + JWT (jjwt)** — autenticação Bearer nos endpoints de transação
- **springdoc-openapi** — Swagger UI e contrato **OpenAPI 3.1**
- **Logstash Logback Encoder** — logs estruturados em JSON
- **Docker** — imagens multi-stage para deploy (Google Cloud Run)

## 📂 Estrutura do Projeto
| Diretório | Descrição |
|-----------|-----------|
| `transaction-service` | Microsserviço principal lidando com a lógica de transação Pix (porta `8080`). |
| `external-partner-mock` | Simula o parceiro externo (ex: Banco Central ou PSP) com latência/erros configuráveis (porta `8081`). |
| `observability` | Stack de observabilidade (Prometheus, Grafana, AlertManager) via Docker Compose. |
| `api-spec` | Contrato OpenAPI 3.1 versionado de cada serviço (fonte de verdade do contrato). |
| `postman` | Coleção Postman para testes manuais dos fluxos. |
| `scripts` | Scripts de apoio (`export-openapi.sh` para regenerar o contrato versionado). |
| `docs` | Documentação e diagramas de arquitetura. |
| `CONTEXT.md` | Glossário e contexto de domínio do repositório. |

## ⚡ Começando

### Pré-requisitos
- Java 21+
- Maven Wrapper incluído (não precisa instalar o Maven manualmente)
- Docker + Docker Compose *(apenas para subir a stack de observabilidade)*

### Executando os serviços

Suba primeiro o mock do parceiro (porta `8081`), depois o `transaction-service` (porta `8080`) — ambos usam o mesmo `SECURITY_JWT_SECRET` por padrão:

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

Os endpoints sob `/transactions` exigem um JWT. Obtenha um token de teste em `/auth/token` (conveniência de desenvolvimento — não é autenticação real):

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

A resposta é **sempre 200 OK** com o status de negócio no corpo: `PROCESSED` (sucesso), `FAILED` (falha de negócio — saldo, limite, conta) ou `ERROR` (falha técnica de comunicação após retries). Os códigos HTTP 400 (validação) e 401 (autenticação) são os únicos códigos de erro.

### Rodando os testes

```bash
cd transaction-service && ./mvnw test
cd external-partner-mock && ./mvnw test
```

## 🔭 Funcionalidades de Observabilidade
- **Logs Estruturados**: Logs em JSON prontos para ingestão (via `logstash-logback-encoder`).
- **Métricas**: Expostas via Spring Boot Actuator + Micrometer (`/actuator/prometheus`).
- **Verificações de Saúde**: Probes de Readiness e Liveness configurados (`/actuator/health`).
- **Stack completo**: Prometheus + Grafana + AlertManager via `observability/docker-compose.yml` — dashboard provisionado com taxa de erro, latência p95 e falhas de comunicação.

```bash
cd observability
docker compose up -d
```

Prometheus: `http://localhost:9090` · Grafana: `http://localhost:3000` (admin/admin) · AlertManager: `http://localhost:9093`

## 🐳 Docker e Deploy

Cada serviço possui um `Dockerfile` multi-stage com usuário não-root e suporte à variável `PORT` injetada pelo Cloud Run. O guia completo de deploy no **Google Cloud Run** (build, ordem de deploy e variáveis de ambiente obrigatórias) está em [`docs/DEPLOY.md`](docs/DEPLOY.md).

## 📖 Documentação

- [Índice da Documentação](docs/README.md) — Mapa de navegação e hierarquia de leitura
- [Plano do Projeto](PLAN.md) — Fases, roadmap e status atual
- [Contexto do Domínio](CONTEXT.md) — Glossário e regras de negócio centrais
- [System Design](docs/system-design.md) — Levantamento de requisitos funcionais e não-funcionais, regras de negócio, casos de uso, decisões de arquitetura e gaps
- [Arquitetura](docs/architecture.md) — Componentes, fluxos de cash-in/cash-out e known issues
- [API](docs/API.md) — Endpoints, schemas e exemplos de requisição/resposta
- [Observabilidade](docs/OBSERVABILITY.md) — Logs, métricas, health checks e stack Prometheus/Grafana
- [Guia de Operação / Runbook](docs/RUNBOOK.md) — Cenários reproduzíveis e diagnóstico de incidentes
- [Guia de Desenvolvimento](docs/DEVELOPMENT.md) — Setup local, estrutura de pastas e convenções
- [Deploy](docs/DEPLOY.md) — Deploy no Google Cloud Run
- [ADRs](docs/adr/README.md) — Architecture Decision Records (ADR-001–007 em `system-design.md` §8; ADR-008+ em `docs/adr/`)

## 📜 Contrato OpenAPI 3.1 (spec-driven)

A API é documentada por contrato **OpenAPI 3.1** gerado pelo `springdoc-openapi` a partir do código:

- Swagger UI: `http://localhost:8080/swagger-ui.html` (transaction-service) e `http://localhost:8081/swagger-ui.html` (external-partner-mock)
- Contrato canônico em runtime: `/v3/api-docs` em cada serviço
- Contrato versionado (fonte de verdade): [`api-spec/`](api-spec/) — regerado por `scripts/export-openapi.sh` e verificado por drift check no CI ([`.github/workflows/openapi-drift.yml`](.github/workflows/openapi-drift.yml))
- Instruções para agentes: [`AGENTS.md`](AGENTS.md)