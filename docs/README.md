# Documentação — Pix Transaction Observability

Mapa rápido dos documentos de projeto, organizados por finalidade. Se você está começando, siga a hierarquia de leitura no fim desta página.

---

## Documentos

| Documento | Finalidade | Quando consultar |
| --- | --- | --- |
| [CONTEXT.md](../CONTEXT.md) | Glossário de domínio, atores e regras de negócio centrais; vocabulário canônico (`PROCESSED`/`FAILED`/`ERROR`, `CASH_IN`/`CASH_OUT`) | Antes de qualquer issue, spec ou mudança; ao nomear testes e propostas |
| [PLAN.md](../PLAN.md) | Plano em fases (concluídas e roadmap), critérios de aceite, status atual e convenções | Decidir prioridades; saber o que está feito e o que vem a seguir |
| [system-design.md](system-design.md) | Levantamento de requisitos (funcionais e não-funcionais), regras de negócio, casos de uso, diagramas C4/sequence, ADRs (001–007), matriz de cobertura e gaps | Começo do projeto; revisão de escopo; antes de propor mudanças estruturais |
| [architecture.md](architecture.md) | Componentes do sistema, fluxos cash-in/cash-out, estrutura de diretórios, notas de compatibilidade do Spring Boot 4 | Entender o que cada componente faz; onboarding; revisão de código |
| [API.md](API.md) | Endpoints REST, schemas de request/response, exemplos com `curl`, autorização JWT | Integrar com a API; escrever testes de integração; debug de chamadas |
| [OBSERVABILITY.md](OBSERVABILITY.md) | Logs estruturados, métricas (Micrometer/Prometheus), health checks, stack Prometheus/Grafana/AlertManager e alertas | Investigar alertas; criar dashboards; entender métricas; subir a stack |
| [RUNBOOK.md](RUNBOOK.md) | Operação: cenários reproduzíveis de teste, sintomas → causa → verificação e o que fazer em cada alerta | Diagnóstico de incidentes; demonstração/fluxos manuais; suporte |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Setup local, estrutura de pastas, como rodar testes, convenções de código e branches | Primeiro contato com o código; onboarding; criar uma branch de feature |
| [DEPLOY.md](DEPLOY.md) | Deploy dos dois serviços no Google Cloud Run: variáveis obrigatórias, ordem e verificação | Publicar em Cloud Run; revisar configuração de produção |
| [adr/README.md](adr/README.md) | Architecture Decision Records — ADR-001–007 em `system-design.md` §8; ADR-008+ como arquivos individuais em `docs/adr/` | Entender o *porquê* de decisões de arquitetura |
| [AGENTS.md](../AGENTS.md) | Instruções para agentes: comandos de verificação, vocabulário de domínio e skills | Trabalhar no repositório com uma IA assistente |

## Hierarquia de leitura sugerida

```
CONTEXT.md                  ← vocabulário de domínio (o QUE significam os termos)
   ↓
PLAN.md                     ← fases e roadmap (para onde o projeto vai)
   ↓
system-design.md            ← requisitos e decisões (o PORQUÊ dos requisitos)
   ↓
architecture.md             ← como está organizado, o QUE cada componente faz
   ↓
API.md · OBSERVABILITY.md · DEVELOPMENT.md · DEPLOY.md · RUNBOOK.md   ← detalhes operacionais
```

Para quem entra no projeto pela primeira vez: `CONTEXT.md` → `PLAN.md` → `system-design.md` para entender o todo, depois aprofundar no documento específico da tarefa — `API.md` para integrar, `DEVELOPMENT.md` para codar, `RUNBOOK.md` para operar, `DEPLOY.md` para publicar.
