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

@test "_aplicar_bump: NOCHANGE em 1.2.3 → 1.2.4 (fallback conservador)" {
  local result
  result=$(_aplicar_bump "1.2.3" "NOCHANGE")
  [[ "$result" == "1.2.4" ]]
}

@test "_aplicar_bump: tipo inválido → retorna versão sem alteração" {
  local result
  result=$(_aplicar_bump "1.2.3" "INVALIDO")
  [[ "$result" == "1.2.3" ]]
}

@test "_detectar_tipo_bump: string vazia → NOCHANGE" {
  local result
  result=$(_detectar_tipo_bump "")
  [[ "$result" == "NOCHANGE" ]]
}