# Deploy no Google Cloud Run

Este projeto consiste em dois serviços Spring Boot (Java 21) que devem ser
deploiados como containers no Cloud Run. Cada serviço tem seu próprio
`Dockerfile` e `Docker image`.

## Variáveis de ambiente obrigatórias

Ao fazer o deploy (via `gcloud run deploy` ou console), configure as seguintes
variáveis de ambiente:

| Variável | Descrição | Padrão |
|---|---|---|
| `PORT` | Porta injetada pelo Cloud Run (geralmente 8080). O Dockerfile a respeita automaticamente. | 8080 (injetado) |
| `SECURITY_JWT_SECRET` | Segredo JWT compartilhado entre os dois serviços. **Igual em ambos.** | `demo-only-jwt-secret-change-me-in-production-1234567890` |
| `PARTNER_BASE_URL` | URL do `external-partner-mock` apontada pelo `transaction-service`. Ex: `https://mock-xxx-uc.a.run.app` | `http://localhost:8081` |

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
# URLs após deploy
https://transaction-service-xxx-uc.a.run.dev/actuator/health/readiness
https://external-partner-mock-xxx-uc.a.run.dev/actuator/health/readiness
```

Ambos devem retornar `200 OK` com `{"status":"UP",...}`.

## Observação sobre o JWT

O segredo está hardcoded em `application.properties` como valor de demonstração.
Em produção, **sempre** override via `SECURITY_JWT_SECRET` env var em ambos os serviços.
O mesmo valor deve ser usado nos dois.