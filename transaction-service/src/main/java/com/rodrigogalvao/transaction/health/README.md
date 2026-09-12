# Health

Indicadores de saúde do serviço, expostos via Spring Boot Actuator.

## Classes

| Classe | Responsabilidade |
|--------|------------------|
| `PartnerConnectivityHealthIndicator` | Indicador `partnerConnectivity` (readiness). Verifica a conectividade com o parceiro externo chamando diretamente `/actuator/health` do `external-partner-mock`. Retorna `UP`/`DOWN` sem aplicar a política de retry/backoff do fluxo de negócio, para que uma checagem de saúde não fique bloqueada esperando tolerância a falhas. |

## Configuração

| Propriedade | Padrão | Descrição |
|-------------|--------|-----------|
| `partner.base-url` | `http://localhost:8081` | URL base do parceiro externo. |