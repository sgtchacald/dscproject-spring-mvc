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
      sub(/<version>[^<]*<\/version>/, "<version>" ver "</version>")
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