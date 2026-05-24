# Automação do Gitflow via Shell Scripts — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatizar o fluxo completo de release (versão → branch → tag → merge para main) via shell scripts, com versionamento semântico baseado em Conventional Commits.

**Architecture:** Orquestrador `scripts/gitflow.sh` delega para três módulos em `scripts/lib/gitflow/`: `semver.sh` calcula a versão, `git.sh` executa as operações git com portões de segurança, e `pom.sh` atualiza o `pom.xml`. Os módulos são puras coleções de funções (sem `set -e` próprio) para facilitar testes com bats.

**Tech Stack:** Bash 5+, bats-core (testes), git, sed, grep, awk (todos padrão Unix).

---

## Mapa de Arquivos

| Arquivo | Ação | Responsabilidade |
|---|---|---|
| `scripts/gitflow.sh` | Criar | Entry point; orquestra todas as etapas |
| `scripts/lib/gitflow/semver.sh` | Criar | Cálculo de versão por Conventional Commits |
| `scripts/lib/gitflow/git.sh` | Criar | Operações git com verificações de segurança |
| `scripts/lib/gitflow/pom.sh` | Criar | Atualização de `<version>` no pom.xml |
| `scripts/tests/test_semver.bats` | Criar | Testes unitários de semver.sh |
| `scripts/tests/test_pom.bats` | Criar | Testes unitários de pom.sh |

---

## Task 1: Setup — Estrutura de Diretórios e bats-core

**Files:**
- Create: `scripts/lib/gitflow/.gitkeep`
- Create: `scripts/tests/.gitkeep`

- [ ] **Step 1: Criar estrutura de diretórios**

```bash
mkdir -p scripts/lib/gitflow
mkdir -p scripts/tests
```

- [ ] **Step 2: Instalar bats-core como subdiretório local**

```bash
git clone --depth 1 https://github.com/bats-core/bats-core.git scripts/tests/bats-core
```

Resultado esperado: diretório `scripts/tests/bats-core/bin/bats` existe.

- [ ] **Step 3: Verificar instalação do bats**

```bash
scripts/tests/bats-core/bin/bats --version
```

Resultado esperado: `Bats 1.x.x`

- [ ] **Step 4: Adicionar bats-core ao .gitignore para não commitar**

Adicionar ao `.gitignore` (ou criar se não existir):

```
scripts/tests/bats-core/
```

- [ ] **Step 5: Commitar estrutura**

```bash
git add scripts/
git commit -m "chore(gitflow): criar estrutura de diretórios para scripts de automação"
```

---

## Task 2: Implementar `semver.sh` com TDD

**Files:**
- Create: `scripts/lib/gitflow/semver.sh`
- Create: `scripts/tests/test_semver.bats`

### Passo TDD: escrever os testes primeiro

- [ ] **Step 1: Criar o arquivo de testes**

Criar `scripts/tests/test_semver.bats`:

```bash
#!/usr/bin/env bats

setup() {
  source "${BATS_TEST_DIRNAME}/../lib/gitflow/semver.sh"
}

# ─── _detectar_tipo_bump ───────────────────────────────────────────────────

@test "_detectar_tipo_bump: feat → MINOR" {
  local result
  result=$(_detectar_tipo_bump "feat: adicionar tela de login")
  [[ "$result" == "MINOR" ]]
}

@test "_detectar_tipo_bump: feat com escopo → MINOR" {
  local result
  result=$(_detectar_tipo_bump "feat(auth): adicionar OAuth")
  [[ "$result" == "MINOR" ]]
}

@test "_detectar_tipo_bump: fix → PATCH" {
  local result
  result=$(_detectar_tipo_bump "fix: corrigir validação de CPF")
  [[ "$result" == "PATCH" ]]
}

@test "_detectar_tipo_bump: perf → PATCH" {
  local result
  result=$(_detectar_tipo_bump "perf: melhorar query de usuários")
  [[ "$result" == "PATCH" ]]
}

@test "_detectar_tipo_bump: BREAKING CHANGE no corpo → MAJOR" {
  local commits
  commits=$'feat: refatorar API\n\nBREAKING CHANGE: endpoint /v1 removido'
  local result
  result=$(_detectar_tipo_bump "$commits")
  [[ "$result" == "MAJOR" ]]
}

@test "_detectar_tipo_bump: feat! no subject → MAJOR" {
  local result
  result=$(_detectar_tipo_bump "feat!: novo modelo de dados")
  [[ "$result" == "MAJOR" ]]
}

@test "_detectar_tipo_bump: fix! no subject → MAJOR" {
  local result
  result=$(_detectar_tipo_bump "fix(auth)!: remover suporte a JWT v1")
  [[ "$result" == "MAJOR" ]]
}

@test "_detectar_tipo_bump: chore → NOCHANGE" {
  local result
  result=$(_detectar_tipo_bump "chore: atualizar dependências")
  [[ "$result" == "NOCHANGE" ]]
}

@test "_detectar_tipo_bump: docs → NOCHANGE" {
  local result
  result=$(_detectar_tipo_bump "docs: atualizar README")
  [[ "$result" == "NOCHANGE" ]]
}

@test "_detectar_tipo_bump: MAJOR tem prioridade sobre feat" {
  local commits
  commits=$'feat: nova feature\nBREAKING CHANGE: algo quebrou'
  local result
  result=$(_detectar_tipo_bump "$commits")
  [[ "$result" == "MAJOR" ]]
}

@test "_detectar_tipo_bump: MINOR tem prioridade sobre fix" {
  local commits
  commits=$'fix: corrigir bug\nfeat: adicionar feature'
  local result
  result=$(_detectar_tipo_bump "$commits")
  [[ "$result" == "MINOR" ]]
}

# ─── _aplicar_bump ────────────────────────────────────────────────────────

@test "_aplicar_bump: MAJOR em 1.2.3 → 2.0.0" {
  local result
  result=$(_aplicar_bump "1.2.3" "MAJOR")
  [[ "$result" == "2.0.0" ]]
}

@test "_aplicar_bump: MINOR em 1.2.3 → 1.3.0" {
  local result
  result=$(_aplicar_bump "1.2.3" "MINOR")
  [[ "$result" == "1.3.0" ]]
}

@test "_aplicar_bump: PATCH em 1.2.3 → 1.2.4" {
  local result
  result=$(_aplicar_bump "1.2.3" "PATCH")
  [[ "$result" == "1.2.4" ]]
}

@test "_aplicar_bump: MAJOR em 0.0.0 → 1.0.0" {
  local result
  result=$(_aplicar_bump "0.0.0" "MAJOR")
  [[ "$result" == "1.0.0" ]]
}

@test "_aplicar_bump: MINOR em 0.0.0 → 0.1.0" {
  local result
  result=$(_aplicar_bump "0.0.0" "MINOR")
  [[ "$result" == "0.1.0" ]]
}

@test "_aplicar_bump: PATCH em 0.0.0 → 0.0.1" {
  local result
  result=$(_aplicar_bump "0.0.0" "PATCH")
  [[ "$result" == "0.0.1" ]]
}
```

- [ ] **Step 2: Rodar os testes para confirmar que falham (arquivo semver.sh ainda não existe)**

```bash
scripts/tests/bats-core/bin/bats scripts/tests/test_semver.bats
```

Resultado esperado: todos os testes falham com erro de "command not found" ou similar.

- [ ] **Step 3: Criar `scripts/lib/gitflow/semver.sh`**

```bash
#!/usr/bin/env bash
# Módulo de cálculo de versão semântica baseado em Conventional Commits.
# Este arquivo define apenas funções — sem set -e — para ser sourceado com segurança.

# Retorna a última tag de versão (ex: "v1.2.3") ou string vazia se não houver tags.
_get_ultima_tag() {
  git tag --sort=-v:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -1
}

# Retorna as mensagens de commit (subject + body) desde a última tag.
# Se não houver tags, retorna todos os commits.
_get_commits_log() {
  local ultima_tag="$1"
  if [[ -n "$ultima_tag" ]]; then
    git log "${ultima_tag}..HEAD" --format="%s%n%b" 2>/dev/null || true
  else
    git log --format="%s%n%b" 2>/dev/null || true
  fi
}

# Analisa o texto de commits e retorna o tipo de bump: MAJOR, MINOR, PATCH ou NOCHANGE.
_detectar_tipo_bump() {
  local commits="$1"

  # BREAKING CHANGE no corpo ou ! após o tipo no subject
  if echo "$commits" | grep -qE '(^BREAKING CHANGE:|^[a-z]+(\([^)]+\))?!:)'; then
    echo "MAJOR"
    return
  fi

  # Nova feature
  if echo "$commits" | grep -qE '^feat(\([^)]+\))?:'; then
    echo "MINOR"
    return
  fi

  # Correção ou performance
  if echo "$commits" | grep -qE '^(fix|perf)(\([^)]+\))?:'; then
    echo "PATCH"
    return
  fi

  # Commits apenas de manutenção (docs, chore, style, etc.) — não geram bump automático
  echo "NOCHANGE"
}

# Aplica o bump sobre uma versão "X.Y.Z" e retorna a nova versão.
_aplicar_bump() {
  local versao="$1"
  local tipo="$2"
  local major minor patch
  IFS='.' read -r major minor patch <<< "$versao"

  case "$tipo" in
    MAJOR) echo "$((major + 1)).0.0" ;;
    MINOR) echo "${major}.$((minor + 1)).0" ;;
    PATCH|NOCHANGE) echo "${major}.${minor}.$((patch + 1))" ;;
    *) echo "$versao" ;;
  esac
}

# Retorna o tipo de bump detectado nos commits desde a última tag.
tipo_bump() {
  local ultima_tag commits
  ultima_tag=$(_get_ultima_tag)
  commits=$(_get_commits_log "$ultima_tag")
  _detectar_tipo_bump "$commits"
}

# Retorna a próxima versão calculada (ex: "1.3.0").
# Sempre retorna uma versão — NOCHANGE é tratado como PATCH pelo chamador após confirmação.
calcular_proxima_versao() {
  local ultima_tag versao_base tipo
  ultima_tag=$(_get_ultima_tag)

  if [[ -n "$ultima_tag" ]]; then
    versao_base="${ultima_tag#v}"
  else
    versao_base="0.0.0"
  fi

  tipo=$(tipo_bump)
  _aplicar_bump "$versao_base" "$tipo"
}
```

- [ ] **Step 4: Rodar os testes e confirmar que passam**

```bash
scripts/tests/bats-core/bin/bats scripts/tests/test_semver.bats
```

Resultado esperado:
```
 ✓ _detectar_tipo_bump: feat → MINOR
 ✓ _detectar_tipo_bump: feat com escopo → MINOR
 ...
16 tests, 0 failures
```

- [ ] **Step 5: Commitar**

```bash
git add scripts/lib/gitflow/semver.sh scripts/tests/test_semver.bats
git commit -m "feat(gitflow): implementar semver.sh com cálculo por conventional commits"
```

---

## Task 3: Implementar `pom.sh` com TDD

**Files:**
- Create: `scripts/lib/gitflow/pom.sh`
- Create: `scripts/tests/test_pom.bats`

- [ ] **Step 1: Criar o arquivo de testes**

Criar `scripts/tests/test_pom.bats`:

```bash
#!/usr/bin/env bats

setup() {
  source "${BATS_TEST_DIRNAME}/../lib/gitflow/pom.sh"
  TEST_POM="$(mktemp /tmp/test_pom_XXXX.xml)"
  cat > "$TEST_POM" <<'POMEOF'
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.5</version>
    <relativePath/>
  </parent>
  <groupId>br.com.diegocordeiro.dscproject</groupId>
  <artifactId>dscproject</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</project>
POMEOF
}

teardown() {
  rm -f "$TEST_POM"
}

@test "atualiza versão do projeto no pom.xml" {
  atualizar_versao_pom "1.2.3" "$TEST_POM"
  grep -q "<version>1.2.3</version>" "$TEST_POM"
}

@test "não altera versão do parent (Spring Boot) no pom.xml" {
  atualizar_versao_pom "1.2.3" "$TEST_POM"
  grep -q "<version>4.0.5</version>" "$TEST_POM"
}

@test "substitui versão SNAPSHOT corretamente" {
  atualizar_versao_pom "2.0.0" "$TEST_POM"
  grep -q "<version>2.0.0</version>" "$TEST_POM"
  ! grep -q "SNAPSHOT" "$TEST_POM"
}

@test "falha com mensagem clara quando pom.xml não existe" {
  run atualizar_versao_pom "1.0.0" "/tmp/nao_existe_${RANDOM}.xml"
  [ "$status" -ne 0 ]
  [[ "$output" == *"não encontrado"* ]]
}

@test "falha se versão não foi aplicada (pom.xml corrompido)" {
  # Simula um pom sem tag <version> após o parent
  local pom_sem_versao
  pom_sem_versao="$(mktemp /tmp/test_pom_broken_XXXX.xml)"
  echo "<project><groupId>test</groupId></project>" > "$pom_sem_versao"

  run atualizar_versao_pom "1.0.0" "$pom_sem_versao"
  [ "$status" -ne 0 ]

  rm -f "$pom_sem_versao"
}
```

- [ ] **Step 2: Rodar os testes para confirmar que falham**

```bash
scripts/tests/bats-core/bin/bats scripts/tests/test_pom.bats
```

Resultado esperado: todos falham com "command not found: atualizar_versao_pom".

- [ ] **Step 3: Criar `scripts/lib/gitflow/pom.sh`**

```bash
#!/usr/bin/env bash
# Módulo de atualização da versão no pom.xml.
# Este arquivo define apenas funções — sem set -e — para ser sourceado com segurança.

# Atualiza a tag <version> do projeto no pom.xml, preservando a versão do <parent>.
# Uso: atualizar_versao_pom "1.2.3" [caminho/para/pom.xml]
atualizar_versao_pom() {
  local versao="$1"
  local pom="${2:-pom.xml}"

  if [[ ! -f "$pom" ]]; then
    echo "ERRO: Arquivo '${pom}' não encontrado." >&2
    return 1
  fi

  # Usa awk para pular o bloco <parent>...</parent> e substituir apenas
  # a primeira <version> fora desse bloco (que é a versão do projeto).
  awk -v ver="$versao" '
    /<parent>/     { in_parent=1 }
    /<\/parent>/   { in_parent=0 }
    !in_parent && !done && /<version>/ {
      sub(/<version>[^<]*<\/version>/, "<version>" ver "<\/version>")
      done=1
    }
    { print }
  ' "$pom" > "${pom}.tmp" && mv "${pom}.tmp" "$pom"

  # Confirma que a versão foi aplicada
  if ! grep -q "<version>${versao}</version>" "$pom"; then
    echo "ERRO: Falha ao atualizar a versão em '${pom}'. Verifique a estrutura do arquivo." >&2
    return 1
  fi
}
```

- [ ] **Step 4: Rodar os testes e confirmar que passam**

```bash
scripts/tests/bats-core/bin/bats scripts/tests/test_pom.bats
```

Resultado esperado:
```
 ✓ atualiza versão do projeto no pom.xml
 ✓ não altera versão do parent (Spring Boot) no pom.xml
 ✓ substitui versão SNAPSHOT corretamente
 ✓ falha com mensagem clara quando pom.xml não existe
 ✓ falha se versão não foi aplicada (pom.xml corrompido)
5 tests, 0 failures
```

- [ ] **Step 5: Commitar**

```bash
git add scripts/lib/gitflow/pom.sh scripts/tests/test_pom.bats
git commit -m "feat(gitflow): implementar pom.sh com atualização segura de versão"
```

---

## Task 4: Implementar `git.sh`

**Files:**
- Create: `scripts/lib/gitflow/git.sh`

As funções deste módulo dependem de um repositório git real, portanto são testadas manualmente no Task 6.

- [ ] **Step 1: Criar `scripts/lib/gitflow/git.sh`**

```bash
#!/usr/bin/env bash
# Módulo de operações git para o fluxo de release.
# Este arquivo define apenas funções — sem set -e — para ser sourceado com segurança.

# Aborta se estiver em uma branch protegida (main, homologacao) ou já em uma release branch.
verificar_branch_atual() {
  local branch_atual
  branch_atual=$(git rev-parse --abbrev-ref HEAD)

  if [[ "$branch_atual" == "main" || "$branch_atual" == "homologacao" ]]; then
    echo "ERRO: Você está em '${branch_atual}'." >&2
    echo "       Execute o script a partir de uma branch de feature." >&2
    return 1
  fi

  if [[ "$branch_atual" == release/* ]]; then
    echo "ERRO: Você já está em uma branch de release ('${branch_atual}')." >&2
    echo "       Para continuar uma release existente, execute as etapas manualmente." >&2
    return 1
  fi
}

# Verifica via git que a branch atual já foi mergeada em homologacao.
verificar_branch_homologacao() {
  local branch_atual
  branch_atual=$(git rev-parse --abbrev-ref HEAD)

  echo "Buscando estado remoto de homologacao..."
  if ! git fetch origin homologacao 2>/dev/null; then
    echo "ERRO: Branch 'homologacao' não encontrada no repositório remoto." >&2
    echo "       Crie a branch homologacao antes de usar este script." >&2
    return 1
  fi

  if ! git branch -r --merged origin/homologacao | grep -q "origin/${branch_atual}$"; then
    echo "ERRO: A branch '${branch_atual}' não foi mergeada em homologacao." >&2
    echo "       Faça o merge, aguarde a aprovação e tente novamente." >&2
    return 1
  fi

  echo "OK: '${branch_atual}' já está em homologacao."
}

# Cria a branch release/<versao> a partir do HEAD atual.
criar_branch_release() {
  local versao="$1"

  if git branch --list "release/${versao}" | grep -q "release/${versao}"; then
    echo "ERRO: A branch 'release/${versao}' já existe." >&2
    return 1
  fi

  git checkout -b "release/${versao}"
  echo "Branch 'release/${versao}' criada."
}

# Cria a tag anotada vX.Y.Z e faz push.
criar_tag() {
  local versao="$1"

  if git tag --list "v${versao}" | grep -q "v${versao}"; then
    echo "ERRO: A tag 'v${versao}' já existe." >&2
    return 1
  fi

  git tag -a "v${versao}" -m "Release ${versao}"
  git push origin "v${versao}"
  echo "Tag 'v${versao}' criada e enviada."
}

# Faz merge --no-ff da release em main e push.
merge_para_main() {
  local versao="$1"

  git checkout main
  git pull origin main
  git merge --no-ff "release/${versao}" -m "chore: Merge release/${versao} em main"
  git push origin main
  echo "Release '${versao}' mergeada em main com sucesso."
}

# Pergunta ao usuário e deleta a branch de release local e remota se confirmado.
deletar_branch_release() {
  local versao="$1"

  read -r -p "Deseja deletar a branch 'release/${versao}' (local e remota)? (s/N) " resposta
  if [[ "${resposta,,}" == "s" ]]; then
    git branch -d "release/${versao}" 2>/dev/null || git branch -D "release/${versao}"
    git push origin --delete "release/${versao}" 2>/dev/null \
      || echo "AVISO: Branch remota 'release/${versao}' não encontrada (pode já ter sido deletada)."
    echo "Branch 'release/${versao}' deletada."
  else
    echo "Branch 'release/${versao}' mantida."
  fi
}
```

- [ ] **Step 2: Commitar**

```bash
git add scripts/lib/gitflow/git.sh
git commit -m "feat(gitflow): implementar git.sh com operações de release e portões de segurança"
```

---

## Task 5: Implementar `gitflow.sh` (Orquestrador)

**Files:**
- Create: `scripts/gitflow.sh`

- [ ] **Step 1: Criar `scripts/gitflow.sh`**

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="${SCRIPT_DIR}/lib/gitflow"
POM_XML="${SCRIPT_DIR}/../pom.xml"

# shellcheck source=/dev/null
source "${LIB_DIR}/semver.sh"
# shellcheck source=/dev/null
source "${LIB_DIR}/git.sh"
# shellcheck source=/dev/null
source "${LIB_DIR}/pom.sh"

_on_error() {
  local line_number="$1"
  local branch_atual
  branch_atual=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "desconhecido")
  echo ""
  echo "═══════════════════════════════════════════════════════"
  echo "  ERRO inesperado na linha ${line_number}"
  echo "  Branch atual: ${branch_atual}"
  echo "  Nenhuma alteração foi enviada ao repositório remoto."
  echo "  Corrija o problema e tente novamente."
  echo "═══════════════════════════════════════════════════════"
}
trap '_on_error $LINENO' ERR

_verificar_prerequisitos() {
  local cmds=("git" "sed" "grep" "awk")
  for cmd in "${cmds[@]}"; do
    if ! command -v "$cmd" &>/dev/null; then
      echo "ERRO: Comando '${cmd}' não encontrado. Instale-o antes de continuar." >&2
      exit 1
    fi
  done
}

_confirmar() {
  local mensagem="$1"
  local resposta
  read -r -p "${mensagem} (s/N) " resposta
  [[ "${resposta,,}" == "s" ]]
}

main() {
  echo ""
  echo "══════════════════════════════════════════"
  echo "   DSC Gitflow — Automação de Release     "
  echo "══════════════════════════════════════════"
  echo ""

  _verificar_prerequisitos

  echo "[Pré-verificação 1/2] Verificando branch atual..."
  verificar_branch_atual

  echo "[Pré-verificação 2/2] Verificando merge em homologacao..."
  verificar_branch_homologacao

  echo ""
  echo "Analisando commits para calcular próxima versão..."
  local tipo nova_versao
  tipo=$(tipo_bump)
  nova_versao=$(calcular_proxima_versao)

  echo ""
  if [[ "$tipo" == "NOCHANGE" ]]; then
    echo "⚠  AVISO: Nenhum commit de versão encontrado (feat:, fix:, perf:, BREAKING CHANGE)."
    echo "   Commits encontrados são apenas de manutenção (chore:, docs:, style:, etc.)."
    echo "   Versão sugerida por PATCH conservador: ${nova_versao}"
    echo ""
    _confirmar "Deseja prosseguir com a versão ${nova_versao}?" \
      || { echo "Operação cancelada pelo usuário."; exit 0; }
  else
    echo "Tipo de bump detectado : ${tipo}"
    echo "Próxima versão         : ${nova_versao}"
    echo ""
    _confirmar "Confirmar criação da release/${nova_versao}?" \
      || { echo "Operação cancelada pelo usuário."; exit 0; }
  fi

  echo ""
  echo "[1/6] Criando branch release/${nova_versao}..."
  criar_branch_release "$nova_versao"

  echo "[2/6] Atualizando versão no pom.xml..."
  atualizar_versao_pom "$nova_versao" "$POM_XML"

  echo "[3/6] Commitando alteração de versão..."
  git add "$POM_XML"
  git commit -m "chore: Atualizando o número de versão para ${nova_versao}"

  echo "[4/6] Enviando branch release/${nova_versao} para o remoto..."
  git push origin "release/${nova_versao}"

  echo "[5/6] Criando e enviando tag v${nova_versao}..."
  criar_tag "$nova_versao"

  echo "[6/6] Mergeando release/${nova_versao} em main..."
  merge_para_main "$nova_versao"

  echo ""
  deletar_branch_release "$nova_versao"

  echo ""
  echo "══════════════════════════════════════════"
  echo "  Release ${nova_versao} concluída com sucesso!"
  echo ""
  echo "  Branch : release/${nova_versao}"
  echo "  Tag    : v${nova_versao}"
  echo "  Main   : atualizado"
  echo "══════════════════════════════════════════"
  echo ""
}

main "$@"
```

- [ ] **Step 2: Tornar o script executável**

```bash
chmod +x scripts/gitflow.sh
```

- [ ] **Step 3: Commitar**

```bash
git add scripts/gitflow.sh
git commit -m "feat(gitflow): implementar orquestrador gitflow.sh"
```

---

## Task 6: Smoke Test e Documentação

**Files:**
- Modify: `docs/gitflow/git-workflow.md` (adicionar seção sobre o script)

- [ ] **Step 1: Rodar todos os testes unitários**

```bash
scripts/tests/bats-core/bin/bats scripts/tests/
```

Resultado esperado: todos os testes passam, 0 falhas.

- [ ] **Step 2: Preparar ambiente para smoke test**

O smoke test exige que a branch `homologacao` exista no remoto e que a branch atual já tenha sido mergeada nela. Se ainda não existir:

```bash
# Criar homologacao a partir da branch atual (apenas para teste)
git checkout -b homologacao
git push origin homologacao
git checkout feat/dsc-versionamento-shellscript-20052026
```

- [ ] **Step 3: Executar o script em modo dry-run (responder N na confirmação)**

```bash
./scripts/gitflow.sh
```

Verificar que:
- As pré-verificações são executadas
- A versão é calculada e exibida corretamente
- Ao responder `N`, nada é alterado no repositório

- [ ] **Step 4: Verificar o help / uso incorreto**

Executar o script em uma branch protegida para confirmar que o portão funciona:

```bash
git checkout main
./scripts/gitflow.sh
```

Resultado esperado:
```
ERRO: Você está em 'main'.
      Execute o script a partir de uma branch de feature.
```

Voltar para a branch de feature:
```bash
git checkout feat/dsc-versionamento-shellscript-20052026
```

- [ ] **Step 5: Adicionar instruções de uso ao git-workflow.md**

Adicionar seção ao final de `docs/gitflow/git-workflow.md`:

```markdown
---

## Automação via Script (Recomendado)

Em vez de executar o fluxo manualmente, use o script de automação após concluir
a aprovação em homologação:

```bash
./scripts/gitflow.sh
```

O script:
1. Verifica que você está em uma branch de feature
2. Verifica que a branch já foi mergeada em `homologacao`
3. Calcula a próxima versão automaticamente pelos Conventional Commits
4. Cria a branch `release/X.Y.Z`, atualiza o `pom.xml`, cria a tag e mergeia em `main`

**Pré-requisito:** A branch `homologacao` deve existir no repositório remoto.
```

- [ ] **Step 6: Commitar documentação**

```bash
git add docs/gitflow/git-workflow.md
git commit -m "docs(gitflow): documentar uso do script de automação no git-workflow.md"
```

- [ ] **Step 7: Rodar todos os testes uma última vez**

```bash
scripts/tests/bats-core/bin/bats scripts/tests/
```

Resultado esperado: todos passam.

---

## Resumo

| Task | Entrega |
|---|---|
| Task 1 | Estrutura de diretórios + bats-core instalado |
| Task 2 | `semver.sh` + 16 testes unitários passando |
| Task 3 | `pom.sh` + 5 testes unitários passando |
| Task 4 | `git.sh` com 6 funções e portões de segurança |
| Task 5 | `gitflow.sh` orquestrador completo |
| Task 6 | Smoke test validado + documentação atualizada |
