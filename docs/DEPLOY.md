# Deploy no Google Cloud Run

Este projeto consiste em dois serviços Spring Boot (Java 25) que devem ser
deploiados como containers no Cloud Run. Cada serviço tem seu próprio
`Dockerfile` e `Docker image`.

## Variáveis de ambiente obrigatórias

Ao fazer o deploy (via `gcloud run deploy` ou console), configure as seguintes
variáveis de ambiente:

| Variável | Descrição | Padrão |
|---|---|---|
| `PORT` | Porta injetada pelo Cloud Run (geralmente 8080). O Dockerfile a respeita automaticamente. | 8080 (injetado) |
| `SECURITY_JWT_SECRET` | Segredo JWT compartilhado entre os dois serviços. **Igual em ambos.** Obrigatória — **sem default**: o boot falha sem ela (issue #11). Gere um valor forte, ex.: `openssl rand -base64 64`; em produção, forneça por Secret Manager. | *sem default* |
| `SECURITY_DEV_TOKEN_ENABLED` | `false` desabilita o endpoint demo `POST /auth/token` (emite JWT sem credenciais). **Obrigatória = `false` em produção** — ver [ADR-010](./adr/0010-dev-token-endpoint.md). | `true` (apenas dev) |
| `PARTNER_BASE_URL` | URL do `external-partner-mock` apontada pelo `transaction-service`. Ex: `https://mock-xxx-uc.a.run.app` | `http://localhost:8081` |
| `DATABASE_URL` | JDBC URL de uma instância PostgreSQL acessível pelos dois serviços, por exemplo `jdbc:postgresql://HOST:5432/pix_observability` | `jdbc:postgresql://localhost:5432/pix_observability` |
| `DATABASE_USERNAME` | Usuário do banco PostgreSQL | `pix` (desenvolvimento local) |
| `DATABASE_PASSWORD` | Senha do banco PostgreSQL; em produção, forneça por Secret Manager | `pix_local_dev` (desenvolvimento local) |

O deploy de produção precisa de PostgreSQL durável e conectividade de rede a partir de ambos os serviços Cloud Run. Configure a mesma `DATABASE_URL` e credenciais nos dois serviços. As migrations Flyway são aplicadas no startup; `spring.jpa.hibernate.ddl-auto=validate` valida o schema. Os valores padrão de banco são apenas para desenvolvimento local.

## Ordem de deploy

**Importante:** Faça o deploy do `external-partner-mock` **primeiro** e anote a URL gerada,
pois o `transaction-service` precisa da URL via `PARTNER_BASE_URL`.

## Comandos de deploy

### 1. Build e push das imagens (opcional — Cloud Run pode build a partir do git)

```bash
# Do repo raiz
gcloud builds submit --tag gcr.io/PROJECT_ID/transaction-service transaction-service/
gcloud builds submit --tag gcr.io/PROJECT_ID/external-partner-mock external-partner-mock/
```

> **Nota:** `gcr.io` é o Container Registry legado. Projetos novos frequentemente usam o Artifact Registry (ex.: `us-central1-docker.pkg.dev/PROJECT_ID/...`). Ajuste o domínio do registry conforme a configuração do seu projeto.

### 2. Deploy do `external-partner-mock`

```bash
gcloud run deploy external-partner-mock \
  --image gcr.io/PROJECT_ID/external-partner-mock \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars=SECURITY_JWT_SECRET=seu-secreto-aqui,PARTNER_BASE_URL=https://mock-xxx-uc.a.run.app
```

### 3. Deploy do `transaction-service`

```bash
gcloud run deploy transaction-service \
  --image gcr.io/PROJECT_ID/transaction-service \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars=SECURITY_JWT_SECRET=seu-secreto-aqui,PARTNER_BASE_URL=https://mock-xxx-uc.a.run.app
```

## Verificação pós-deploy

```bash
# URLs após deploy — substitua pelo domínio real gerado pelo Cloud Run
https://<transaction-service>-<hash>-<region>.run.app/actuator/health/readiness
https://<external-partner-mock>-<hash>-<region>.run.app/actuator/health/readiness
```

O domínio exato (`*.run.app`/`*.a.run.app`) varia conforme o projeto. Obtenha a URL real com:

```bash
gcloud run services describe transaction-service --region us-central1 --format='value(status.url)'
gcloud run services describe external-partner-mock --region us-central1 --format='value(status.url)'
```

Ambos devem retornar `200 OK` com `{"status":"UP",...}`.

## Observação sobre o JWT

O segredo está hardcoded em `application.properties` como valor de demonstração.
`SECURITY_JWT_SECRET` é obrigatória (sem default — o serviço não sobe sem ela) e `SECURITY_DEV_TOKEN_ENABLED=false` deve ser definida em ambos os serviços para desativar o endpoint demo de emissão de token ([ADR-010](./adr/0010-dev-token-endpoint.md)).
O mesmo valor deve ser usado nos dois.