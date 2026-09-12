# Exception

Tratamento centralizado de erros da API. Converte exceções de validação e de requisição em respostas HTTP padronizadas com `ErrorResponse`.

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `GlobalExceptionHandler` | `@RestControllerAdvice` que intercepta: |
|        | - `MethodArgumentNotValidException` → `400` com as mensagens de erro de validação de campo. |
|        | - `HttpMessageNotReadableException` → `400` com `"malformed request body"`. |

## Comportamento

- Todas as respostas de erro seguem o contrato de `ErrorResponse` (timestamp, status HTTP e lista de mensagens).
- Erros de negócio do `TransactionService` **não** passam por aqui — são refletidos no campo `status`/`reason` do `TransactionResponse`.