# Pix Transaction Observability

Este repositório implementa um MVP projetado com **princípios de observabilidade em primeiro lugar** para um serviço transacional Pix. Ele simula operações financeiras de alta confiabilidade, incluindo integração com parceiros externos e tratamento de casos de borda.

## 🏗 Contexto
O componente principal é um serviço transacional Pix que lida com operações de **cash-in** e **cash-out**. Ele opera em um ambiente regulamentado onde a confiabilidade e a rápida resolução de incidentes são fundamentais.

## 🎯 Objetivos
- **Observabilidade desde o Dia Um**: Logs, métricas e traces são nativos, não uma reflexão tardia.
- **Resiliência**: Tratamento de latência e erros provenientes de parceiros externos.
- **Simulação Realista**: Um ambiente autônomo para testar respostas operacionais.

## 🚀 Tecnologias
- **Java 21**
- **Spring Boot 3+**
- **Spring Boot Actuator**
- **Logstash Logback Encoder**
- **Maven** (Wrapper incluído)

## 📂 Estrutura do Projeto
| Diretório | Descrição |
|-----------|-----------|
| `transaction-service` | Microsserviço principal lidando com a lógica de transação Pix. |
| `external-partner-mock` | Simula o parceiro externo (ex: Banco Central ou PSP) com latência/erros configuráveis. |
| `observability` | Configuração para a stack de observabilidade (Prometheus, Grafana, OpenTelemetry). |
| `docs` | Documentação e diagramas de arquitetura. |

## ⚡ Começando

### Pré-requisitos
- Java 21+

### Executando o Serviço de Transação
O projeto usa o Maven Wrapper, então você não precisa instalar o Maven manualmente.

1. Navegue até o diretório do serviço:
   ```bash
   cd transaction-service
   ```

2. Execute a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```

A aplicação iniciará na porta `8080` (padrão).

## 🔭 Funcionalidades de Observabilidade
- **Logs Estruturados**: Logs em JSON prontos para ingestão (via `logstash-logback-encoder`).
- **Métricas**: Expostas via Spring Boot Actuator (ex: `/actuator/prometheus`).
- **Verificações de Saúde**: Probes de Readiness e Liveness configurados.

## 📖 Documentação

- [System Design](docs/system-design.md) — Levantamento de requisitos funcionais e não-funcionais, regras de negócio, casos de uso, decisões de arquitetura e gaps
- [Arquitetura](docs/architecture.md) — Componentes, fluxos de cash-in/cash-out e known issues
- [API](docs/API.md) — Endpoints, schemas e exemplos de requisição/resposta
- [Observabilidade](docs/OBSERVABILITY.md) — Logs, métricas, health checks e stack Prometheus/Grafana
- [Guia de Desenvolvimento](docs/DEVELOPMENT.md) — Setup local, estrutura de pastas e convenções

A **API também é documentada por contrato OpenAPI 3.1** (springdoc):

- Swagger UI: `http://localhost:8080/swagger-ui.html` (transaction-service) e `http://localhost:8081/swagger-ui.html` (external-partner-mock)
- Contrato canônico em runtime: `/v3/api-docs` em cada serviço
- Contrato versionado (fonte de verdade): [`api-spec/`](api-spec/) — regerado por `scripts/export-openapi.sh` e verificado por drift check no CI