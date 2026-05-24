# Design: Automação do Gitflow via Shell Scripts

**Data:** 2026-05-20
**Projeto:** dscproject-spring-mvc
**Branch de implementação:** feat/dsc-versionamento-shellscript-20052026

---

## Problema

O fluxo de release documentado em `docs/gitflow/git-workflow.md` é executado manualmente, o que causa erros recorrentes: esquecimento de criar a branch de release, de atualizar o número de versão, de criar a tag, ou de mergear para `main`. Este design automatiza todo esse processo via shell scripts.

---

## Estrutura de Arquivos

```
scripts/
├── gitflow.sh                  ← entry point; executar com: ./scripts/gitflow.sh
└── lib/
    └── gitflow/
        ├── semver.sh           ← cálculo de próxima versão por conventional commits
        ├── git.sh              ← operações git (verificações, branch, merge, tag, push)
        └── pom.sh              ← atualização de <version> no pom.xml
```

---

## Fluxo de Execução (`gitflow.sh`)

O orquestrador executa as etapas na seguinte ordem, abortando com mensagem clara em qualquer falha:

1. Verificar pré-requisitos (`git`, `sed`, `grep`)
2. Verificar que está em uma branch de feature ou release (não em `main` nem `homologacao`)
3. Verificar que a branch atual já foi mergeada em `homologacao`
4. Calcular próxima versão pelos conventional commits (delega para `semver.sh`)
5. Exibir resumo e pedir confirmação: `"Será criada a release/X.Y.Z. Confirmar? (s/N)"`
6. Criar branch `release/X.Y.Z` a partir da branch atual
7. Atualizar `pom.xml` com a nova versão sem `-SNAPSHOT` (delega para `pom.sh`)
8. Commitar: `"chore: Atualizando o número de versão para X.Y.Z"`
9. Push da branch `release/X.Y.Z`
10. Criar tag anotada `vX.Y.Z` com mensagem `"Release X.Y.Z"`
11. Push da tag
12. Merge da `release/X.Y.Z` em `main` com `--no-ff` + push
13. Perguntar se deve deletar a branch `release/X.Y.Z` (local e remota)
14. Exibir resumo final de tudo que foi executado

---

## Módulo: Cálculo de Versão Semântica (`semver.sh`)

### Fonte de commits analisados

- Se existe ao menos uma tag: `git log <última-tag>..HEAD --oneline`
- Se não existe nenhuma tag: `git log --oneline` (todos os commits)
- A última tag é obtida com: `git tag --sort=-v:refname | head -1`

### Regras de bump (em ordem decrescente de prioridade)

| Condição no histórico de commits | Resultado |
|---|---|
| Qualquer commit com `BREAKING CHANGE:` no corpo **ou** `!` após o tipo (ex: `feat!:`, `fix!:`) | Bump `MAJOR` → `X+1.0.0` |
| Ao menos um commit com prefixo `feat:` ou `feat(escopo):` | Bump `MINOR` → `X.Y+1.0` |
| Commits com `fix:` ou `perf:` (sem feat ou breaking) | Bump `PATCH` → `X.Y.Z+1` |
| Apenas `refactor:`, `docs:`, `style:`, `test:`, `chore:`, `build:`, `ci:` | Sem bump automático — script exibe aviso e pede confirmação para PATCH |
| Nenhum commit convencional reconhecido | Script exibe aviso e pede confirmação para PATCH |

### Versão base quando não há tags

- Versão base assumida: `0.0.0`
- O bump é aplicado normalmente sobre ela

### Saída

A função retorna a próxima versão como string (ex: `1.2.0`) via `echo`, para ser capturada pelo orquestrador.

---

## Módulo: Operações Git (`git.sh`)

### `verificar_branch_homologacao`

- Executa: `git fetch origin homologacao` (garante ref atualizada)
- Verifica: `git branch -r --merged origin/homologacao` deve conter a branch atual
- Se não contiver: aborta com mensagem `"ERRO: A branch atual não foi mergeada em homologacao."`

### `criar_branch_release <versao>`

- Cria `release/<versao>` a partir do HEAD atual
- Se a branch já existir: aborta com `"ERRO: A branch release/<versao> já existe."`

### `criar_tag <versao>`

- Cria tag anotada: `git tag -a v<versao> -m "Release <versao>"`
- Verifica antes se a tag já existe para evitar duplicatas
- Push: `git push origin v<versao>`

### `merge_para_main <versao>`

- `git checkout main && git pull origin main`
- `git merge --no-ff release/<versao> -m "chore: Merge release/<versao> em main"`
- `git push origin main`
- O `--no-ff` preserva o nó de merge no grafo, tornando o histórico da release visível

### `deletar_branch_release <versao>` (condicional)

- Só executado mediante confirmação interativa do usuário
- Local: `git branch -d release/<versao>`
- Remota: `git push origin --delete release/<versao>`

### Comportamento em erro

- `set -euo pipefail` ativo em todos os scripts
- Qualquer comando que falhe interrompe o script imediatamente
- Uma função `on_error` via `trap ERR` exibe: qual etapa falhou, a branch atual, e orienta o desenvolvedor

---

## Módulo: Atualização do `pom.xml` (`pom.sh`)

### Estratégia

- Usa `sed` para localizar e substituir somente a tag `<version>` do projeto
- A substituição é delimitada para não atingir a `<version>` do `<parent>` do Spring Boot
- Padrão buscado após o bloco `</parent>`: primeira ocorrência de `<version>...</version>` fora do bloco parent

### Substituição

```
De: <version>QUALQUER_COISA</version>
Para: <version>X.Y.Z</version>
```

### Verificação

- Após a substituição, confirma via `grep` que `<version>X.Y.Z</version>` existe no arquivo
- Se não encontrar: aborta com mensagem de erro antes de qualquer commit

### Dependências

- Apenas `sed` e `grep` — sem `xmllint`, `mvn versions:set`, nem Python
- Compatível com qualquer ambiente Unix

---

## Decisões de Design

| Aspecto | Decisão | Justificativa |
|---|---|---|
| Estrutura | Modular com `lib/gitflow/` | Responsabilidade única por módulo; fácil adicionar novos artefatos |
| Gatilho | Manual (`./scripts/gitflow.sh`) | Controle explícito do desenvolvedor |
| Portão de segurança | Verifica merge em `homologacao` via git | Impede releases sem aprovação |
| Versionamento | Automático por conventional commits | Elimina decisão manual e inconsistências |
| Merge para main | `--no-ff` | Preserva nó de merge no grafo git |
| Limpeza de branch | Interativo | Flexibilidade para o desenvolvedor decidir |
| Tratamento de erros | `set -euo pipefail` + `trap ERR` | Falha rápida e visível; nunca deixa estado indefinido |

---

## Fora do Escopo

- Integração com CI/CD (GitHub Actions, Jenkins)
- Suporte a múltiplos artefatos (`package.json`, classes Java de versão)
- Rollback automatizado em caso de falha parcial
- Notificações (Slack, e-mail)
