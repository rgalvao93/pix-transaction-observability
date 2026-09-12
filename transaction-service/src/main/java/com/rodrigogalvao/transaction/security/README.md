# Security

Autenticação e autorização da API via **JWT stateless**.

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `JwtTokenProvider` | Gera e valida tokens JWT (HS256) usando o segredo `security.jwt.secret`. Expira após `security.jwt.expiration-ms` (padrão `3600000` = 1h). E expõe o `clientId` (subject) do token. |
| `JwtAuthenticationFilter` | Filtro `OncePerRequestFilter` que lê o header `Authorization: Bearer <token>`, valida o token e popula o `SecurityContextHolder` com o `clientId`. |
| `SecurityConfig` | Configura a cadeia de segurança: CSRF desabilitado, sessão stateless, `/actuator/**` e `/auth/**` públicos, demais rotas autenticadas, retorno `401` para acesso não autorizado e registro do filtro JWT. |

## Configuração

| Propriedade | Padrão | Descrição |
|-------------|--------|-----------|
| `security.jwt.secret` | *(obrigatório)* | Segredo usado na assinatura HS256. |
| `security.jwt.expiration-ms` | `3600000` | Validade do token em milissegundos. |

## Rotas

| Rota | Acesso |
|------|--------|
| `/auth/**` | Público (emissão de tokens para testes locais). |
| `/actuator/**` | Público. |
| Qualquer outra | Exige `Authorization: Bearer <token>` válido. |