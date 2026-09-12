# Documentação — Pix Transaction Observability

Mapa rápido dos documentos de projeto, organizados por finalidade.

---

## Documentos

| Documento | Finalidade | Quando consultar |
| --- | --- | --- |
| [system-design.md](system-design.md) | Levantamento de requisitos (funcionais e não-funcionais), regras de negócio, casos de uso, diagramas C4/sequence, decisões de arquitetura (ADRs), matriz de cobertura e gaps | Começo do projeto; revisão de escopo; antes de propor mudanças estruturais |
| [architecture.md](architecture.md) | Componentes do sistema, fluxos cash-in/cash-out, estrutura de diretórios, notas de compatibilidade do Spring Boot 4 | Entender o que cada componente faz;.onboarding; revisão de código |
| [API.md](API.md) | Endpoints REST, schemas de request/response, exemplos de chamada com `curl`, autorização | Integrar com a API; escrever testes de integração; debug de chamadas |
| [OBSERVABILITY.md](OBSERVABILITY.md) | Logs estruturados, métricas (Micrometer/Prometheus), health checks, stack Prometheus/Grafana/AlertManager | Investigar alertas; criar dashboards; entender o que cada métrica mede; subir a stack de observabilidade |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Setup local, estrutura de pastas, como rodar testes, convenções de código e branches | Primeiro contato com o projeto; onboarding; criar uma branch de feature |

## Hierarquia de leitura sugerida

```
system-design.md   ← contexto, requisitos e decisões (o PORQUÊ)
       ↓
  architecture.md   ← como está organizado, o QUE cada componente faz
       ↓
      API.md / OBSERVABILITY.md / DEVELOPMENT.md   ← detalhes operacionais
```

Para quem entra no projeto pela primeira vez, comece por `system-design.md` para entender o todo, depois aprofunde no documento específico da tarefa.
