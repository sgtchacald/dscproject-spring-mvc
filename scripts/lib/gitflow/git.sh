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

# Verifica que a release existe no remoto e já foi mergeada em homologacao.
verificar_release_para_aprovacao() {
  local versao="$1"

  echo "Buscando estado remoto..."
  git fetch origin 2>/dev/null || true

  if ! git branch -r | grep -q "origin/release/${versao}$"; then
    echo "ERRO: Branch 'release/${versao}' não encontrada no remoto." >&2
    echo "       Execute './gitflow.sh' primeiro para criar a release." >&2
    return 1
  fi

  if ! git branch -r --merged origin/homologacao | grep -q "origin/release/${versao}$"; then
    echo "ERRO: Branch 'release/${versao}' ainda não foi mergeada em homologacao." >&2
    echo "       Execute './gitflow.sh' para enviar a release para homologacao." >&2
    return 1
  fi

  echo "OK: release/${versao} está em homologacao."
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

# Faz merge --no-ff da release em homologacao e push.
merge_para_homologacao() {
  local versao="$1"

  git checkout homologacao
  git pull origin homologacao
  git merge --no-ff "release/${versao}" -m "chore: Merge release/${versao} em homologacao"
  git push origin homologacao
  echo "Release '${versao}' mergeada em homologacao com sucesso."
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
