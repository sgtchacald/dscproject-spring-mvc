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

# Fase 1: feature → release → homologacao
_fase_release() {
  echo "[Pré-verificação] Verificando branch atual..."
  verificar_branch_atual

  local branch_feature
  branch_feature=$(git rev-parse --abbrev-ref HEAD)

  echo ""
  echo "Analisando commits para calcular próxima versão..."
  local tipo nova_versao
  tipo=$(tipo_bump)
  nova_versao=$(calcular_proxima_versao)

  echo ""

  # Detecta se a release já existe no remoto (novos commits na feature com release pendente)
  git fetch origin 2>/dev/null || true
  if git branch -r 2>/dev/null | grep -q " *origin/release/${nova_versao}$"; then
    echo "Branch release/${nova_versao} já existe."
    echo "Novos commits de '${branch_feature}' serão incorporados e reenviados para homologacao."
    echo ""
    _confirmar "Atualizar release/${nova_versao} com os commits mais recentes?" \
      || { echo "Operação cancelada pelo usuário."; exit 0; }

    echo ""
    echo "[1/3] Incorporando '${branch_feature}' em release/${nova_versao}..."
    if git branch --list "release/${nova_versao}" | grep -q "release/${nova_versao}"; then
      git checkout "release/${nova_versao}"
      git pull origin "release/${nova_versao}"
    else
      git checkout -b "release/${nova_versao}" "origin/release/${nova_versao}"
    fi
    git merge --no-ff "$branch_feature" -m "chore: Incorporando commits de ${branch_feature} em release/${nova_versao}"

    echo "[2/3] Enviando release/${nova_versao} atualizada para o remoto..."
    git push origin "release/${nova_versao}"

    echo "[3/3] Mergeando release/${nova_versao} em homologacao..."
    merge_para_homologacao "$nova_versao"
  else
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
    echo "[1/5] Criando branch release/${nova_versao}..."
    criar_branch_release "$nova_versao"

    echo "[2/5] Atualizando versão no pom.xml..."
    atualizar_versao_pom "$nova_versao" "$POM_XML"

    echo "[3/5] Commitando alteração de versão..."
    git add "$POM_XML"
    git commit -m "chore: Atualizando o número de versão para ${nova_versao}"

    echo "[4/5] Enviando branch release/${nova_versao} para o remoto..."
    git push origin "release/${nova_versao}"

    echo "[5/5] Mergeando release/${nova_versao} em homologacao..."
    merge_para_homologacao "$nova_versao"
  fi

  echo ""
  echo "══════════════════════════════════════════════════════════════"
  echo "  Release ${nova_versao} enviada para homologacao."
  echo ""
  echo "  Faça o deploy em homologacao e aguarde a aprovação."
  echo "  Quando aprovada, execute:"
  echo ""
  echo "    ./gitflow.sh aprovar ${nova_versao}"
  echo ""
  echo "══════════════════════════════════════════════════════════════"
  echo ""
}

# Fase 2: aprovação → tag → main
_fase_aprovacao() {
  local versao="${1:-}"

  if [[ -z "$versao" ]]; then
    echo "ERRO: Informe a versão a aprovar. Ex: ./gitflow.sh aprovar 1.0.0" >&2
    exit 1
  fi

  echo "[Pré-verificação] Verificando release/${versao} em homologacao..."
  verificar_release_para_aprovacao "$versao"

  echo ""
  _confirmar "Confirmar criação de tag e merge em main para release/${versao}?" \
    || { echo "Operação cancelada."; exit 0; }

  echo ""
  git checkout "release/${versao}"

  echo "[1/2] Criando e enviando tag v${versao}..."
  criar_tag "$versao"

  echo "[2/2] Mergeando release/${versao} em main..."
  merge_para_main "$versao"

  echo ""
  deletar_branch_release "$versao"

  echo ""
  echo "══════════════════════════════════════════"
  echo "  Release ${versao} em produção!"
  echo ""
  echo "  Tag  : v${versao}"
  echo "  Main : atualizado"
  echo "══════════════════════════════════════════"
  echo ""
}

main() {
  _verificar_prerequisitos

  case "${1:-}" in
    aprovar)
      echo ""
      echo "══════════════════════════════════════════"
      echo "   DSC Gitflow — Aprovação de Release     "
      echo "══════════════════════════════════════════"
      echo ""
      _fase_aprovacao "${2:-}"
      ;;
    *)
      echo ""
      echo "══════════════════════════════════════════"
      echo "   DSC Gitflow — Automação de Release     "
      echo "══════════════════════════════════════════"
      echo ""
      _fase_release
      ;;
  esac
}

main "$@"