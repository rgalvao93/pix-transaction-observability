# DTO (Data Transfer Objects)

Objetos usados nas fronteiras da aplicação para transportar dados entre a camada de apresentação e o restante do sistema. Mantêm o modelo de domínio encapsulado e isolado do formato exposto pela API.

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `TransactionRequest` | Payload de entrada de `POST /transactions` (`transactionId`, `type`, `amount`, `accountId`). Validações via Bean Validation (`@NotBlank`, `@NotNull`, `@Positive`). |
| `TransactionResponse` | Payload de saída de uma transação (`transactionId`, `status`, `reason`). |
| `ErrorResponse` | Corpo padrão de erro (`timestamp`, `status`, `errors`) devolvido pelo `GlobalExceptionHandler`. |