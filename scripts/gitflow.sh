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
