# Controller

Camada de apresentação da API REST. Recebe requisições HTTP, delega o processamento para a camada de serviço e devolve as respostas.

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `TransactionController` | Exponha o endpoint `POST /transactions` para criação/processamento de transações Pix. |
| `AuthController` | Endpoint de conveniência `POST /auth/token` que emite um JWT para testes locais. **Apenas para desenvolvimento** — não valida credenciais nem substitui um provedor de identidade real. |

## Endpoints

| Método | Caminho | Descrição |
|--------|---------|-----------|
| `POST` | `/transactions` | Processa uma transação (`TransactionRequest`). Requer autenticação via `Authorization: Bearer <token>`. |
| `POST` | `/auth/token` | Emite um token JWT para um `clientId` informado. Público (permitido no `SecurityConfig`). |