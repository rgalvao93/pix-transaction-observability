# Domain Docs

Como as skills de engenharia devem consumir a documentação de domínio deste repositório ao explorar o código.

## Antes de explorar, leia

- **`CONTEXT.md`** na raiz do repositório, ou
- **`CONTEXT-MAP.md`** na raiz, caso exista: aponta para um `CONTEXT.md` por contexto. Leia cada um relevante ao tópico.
- **`docs/adr/`**: leia os ADRs que tocam a área em que você vai trabalhar. Em repositórios multi-contexto, verifique também `src/<contexto>/docs/adr/` para decisões específicas do contexto.

Se qualquer um desses arquivos não existir, **siga em silêncio**. Não sinalize a ausência; não sugira criá-los de antemão. A skill `/domain-modeling` (alcançada via `/grill-with-docs` e `/improve-codebase-architecture`) os cria de forma tardia quando termos ou decisões são resolvidos de fato.

## Estrutura de arquivos

Repositório de contexto único (a maioria):

```
/
├── CONTEXT.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

Repositório multi-contexto (presença de `CONTEXT-MAP.md` na raiz):

```
/
├── CONTEXT-MAP.md
├── docs/adr/                          ← decisões do sistema
└── src/
    ├── ordering/
    │   ├── CONTEXT.md
    │   └── docs/adr/                  ← decisões específicas do contexto
    └── billing/
        ├── CONTEXT.md
        └── docs/adr/
```

## Use o vocabulário do glossário

Quando sua saída nomear um conceito de domínio (em título de issue, proposta de refactor, hipótese, nome de teste), use o termo definido no `CONTEXT.md`. Não derive para sinônimos que o glossário evita explicitamente.

Se o conceito que você precisa ainda não está no glossário, isso é um sinal: ou você está inventando uma linguagem que o projeto não usa (reconsidere) ou existe um gap real (anote para `/domain-modeling`).

## Sinalize conflitos de ADR

Se sua saída contradizer um ADR existente, exponha isso explicitamente em vez de sobrepor em silêncio:

> _Contradiz o ADR-0007 (event-sourced orders), mas vale reabrir porque…_

## Nota deste repositório

Os ADRs 001–007 estão registrados na seção "Decisões de Arquitetura (ADRs)" de `docs/system-design.md`. A partir do ADR-008, novos ADRs devem ser arquivados como arquivos individuais em `docs/adr/` (ver `docs/adr/README.md`).