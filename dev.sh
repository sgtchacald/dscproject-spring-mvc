#!/usr/bin/env bash
# Carrega o .env no ambiente e roda o Maven wrapper.
# Uso:  ./dev.sh spring-boot:run -Dspring-boot.run.profiles=dev
#       ./dev.sh test
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "Falta o arquivo .env — copie de .env.example e preencha as credenciais." >&2
  exit 1
fi

# Parser tolerante: aceita valor com espaços, '=' e '#'; ignora comentários e
# linhas em branco. NÃO faz eval (não quebra com valor colado do Gmail).
while IFS= read -r line || [ -n "$line" ]; do
  case "$line" in
    ''|'#'*) continue ;;
  esac
  key=${line%%=*}
  val=${line#*=}
  # tira aspas envolventes, se houver
  val=${val#\"}; val=${val%\"}
  val=${val#\'}; val=${val%\'}
  export "$key=$val"
done < .env

exec ./mvnw "$@"
