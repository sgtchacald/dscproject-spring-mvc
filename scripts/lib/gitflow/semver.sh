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
    # NOCHANGE: fallback conservador — orquestrador intercepta antes de chegar aqui
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
# Para NOCHANGE aplica PATCH conservador — o orquestrador usa tipo_bump() para detectar
# esse caso e pedir confirmação interativa antes de prosseguir.
calcular_proxima_versao() {
  local ultima_tag versao_base commits tipo
  ultima_tag=$(_get_ultima_tag)
  versao_base="${ultima_tag:+${ultima_tag#v}}"
  versao_base="${versao_base:-0.0.0}"
  commits=$(_get_commits_log "$ultima_tag")
  tipo=$(_detectar_tipo_bump "$commits")
  _aplicar_bump "$versao_base" "$tipo"
}