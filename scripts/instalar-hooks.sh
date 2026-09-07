#!/usr/bin/env bash
# Aponta o git deste clone para os hooks versionados em scripts/hooks/.
# Rode uma vez após clonar o repositório.
set -euo pipefail

REPO_RAIZ="$(git rev-parse --show-toplevel)"
cd "${REPO_RAIZ}"

chmod +x scripts/hooks/* 2>/dev/null || true
git config core.hooksPath scripts/hooks

echo "core.hooksPath = scripts/hooks"
echo "Hooks ativos: $(ls scripts/hooks | tr '\n' ' ')"
