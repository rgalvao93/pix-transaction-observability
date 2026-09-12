# Issue tracker: GitHub

Issues e specs deste repositório vivem como issues do GitHub. Use a CLI `gh` para todas as operações.

## Convenções

- **Criar issue**: `gh issue create --title "..." --body "..."`. Use heredoc para corpos multilinha.
- **Ler issue**: `gh issue view <number> --comments`, filtrando comentários com `jq` e também buscando labels.
- **Listar issues**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` com `--label`/`--state` conforme necessário.
- **Comentar issue**: `gh issue comment <number> --body "..."`
- **Aplicar/remover labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Fechar**: `gh issue close <number> --comment "..."`

O repositório é inferido de `git remote -v`; `gh` faz isso automaticamente quando executado dentro do clone.

## Pull requests como superfície de triage

**PRs como superfície de requisição: não.** _(Defina `yes` se este repositório tratar PRs externos como requisições de feature; `/triage` lê este flag.)_

Quando `yes`, PRs passam pelos mesmos labels e estados das issues, usando os equivalentes `gh pr`:

- **Ler PR**: `gh pr view <number> --comments` e `gh pr diff <number>` para o diff.
- **Listar PRs externos para triage**: `gh pr list --state open --json number,title,body,labels,author,authorAssociation,comments` mantendo apenas `authorAssociation` de `CONTRIBUTOR`, `FIRST_TIME_CONTRIBUTOR` ou `NONE` (descartando `OWNER`/`MEMBER`/`COLLABORATOR`).
- **Comentar / label / fechar**: `gh pr comment`, `gh pr edit --add-label`/`--remove-label`, `gh pr close`.

GitHub compartilha um único espaço de números entre issues e PRs, então um `#42` pode ser qualquer um dos dois: resolva com `gh pr view 42` e, se falhar, `gh issue view 42`.

## Quando uma skill diz "publique no issue tracker"

Crie uma issue no GitHub.

## Quando uma skill diz "busque o ticket relevante"

Rode `gh issue view <number> --comments`.

## Operações de wayfinding

Usado pelo `/wayfinder`. O **mapa** é uma única issue com **child issues** como tickets.

- **Mapa**: uma única issue com label `wayfinder:map`, contendo o corpo Notes / Decisions-so-far / Fog. `gh issue create --label wayfinder:map`.
- **Ticket filho**: issue vinculada ao mapa como sub-issue do GitHub (`gh api` no endpoint de sub-issues). Onde sub-issues não estiverem habilitadas, adicione o filho a uma task list no corpo do mapa e coloque `Part of #<map>` no topo do corpo do filho. Labels: `wayfinder:<type>` (`research`/`prototype`/`grilling`/`task`). Quando reivindicado, o ticket é atribuído ao dev que vai executar.
- **Blocking**: dependências nativas de issues do GitHub, representação canônica e visível na UI. Adicione a aresta com `gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`, onde `<blocker-db-id>` é o **id numérico de banco** do bloqueador (`gh api repos/<owner>/<repo>/issues/<n> --jq .id`, _não_ o `#number` nem o `node_id`). O GitHub reporta `issue_dependencies_summary.blocked_by` (apenas bloqueadores abertos, o gate vivo). Onde dependências não estiverem disponíveis, use como fallback uma linha `Blocked by: #<n>, #<n>` no topo do corpo do filho. Um ticket está desbloqueado quando todos os bloqueadores estão fechados.
- **Query de fronteira**: liste os filhos abertos do mapa (`gh issue list --state open`, escopo nos sub-issues / task list do mapa), descarte os que tiverem bloqueador aberto (`issue_dependencies_summary.blocked_by > 0`, ou uma issue aberta na linha `Blocked by`) ou assignee; o primeiro na ordem do mapa vence.
- **Reivindicação**: `gh issue edit <n> --add-assignee @me`, primeira escrita da sessão.
- **Resolução**: `gh issue comment <n> --body "<resposta>"`, depois `gh issue close <n>`, e então anexe um ponteiro de contexto (gist + link) nas Decisions-so-far do mapa.