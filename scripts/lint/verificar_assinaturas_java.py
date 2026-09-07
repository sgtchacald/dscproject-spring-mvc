#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Verifica a convenção de quebra de linha em assinaturas de método/construtor Java.

Regra (guia-padroes-java.md, seção "Formatação"):

- Até 5 parâmetros  -> a lista de parâmetros fica numa única linha física.
- 6 ou mais         -> um parâmetro por linha.

A regra vale só para a *declaração*; chamadas de método não são checadas.
Contagem de parâmetros é o único gatilho — não há limite de coluna.

Uso:
    verificar_assinaturas_java.py [CAMINHO ...]   # arquivos e/ou diretórios
    verificar_assinaturas_java.py --staged        # só os .java no stage do git

Sem argumento: varre ./src/main/java e ./src/test/java a partir do diretório atual.

Saída: uma linha "arquivo:linha — motivo" por violação. Sai com 1 se houver
violação, 0 se estiver tudo certo.
"""
import os
import re
import subprocess
import sys

LIMITE_UMA_LINHA = 5

# Palavras que podem aparecer como "identificador seguido de (" sem serem
# declaração de método: estruturas de controle e chamadas especiais.
_NOMES_NAO_METODO = {
    "if", "for", "while", "switch", "catch", "synchronized",
    "return", "new", "throw", "assert", "yield", "super", "this",
}

# Token imediatamente ANTES do nome que descarta a construção como declaração.
_ANTES_NAO_DECL = {"new", "return", "throw", "yield", "case", "instanceof"}

_IDENT = re.compile(r"[A-Za-z_$][A-Za-z0-9_$]*")


def _remover_comentarios_e_strings(src: str) -> str:
    """Troca comentários, strings e chars por espaço/placeholder, preservando
    a posição de cada '\n' e o comprimento total (offsets continuam válidos)."""
    saida = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        prox = src[i + 1] if i + 1 < n else ""
        if c == "/" and prox == "/":
            j = src.find("\n", i)
            j = n if j == -1 else j
            saida.append(" " * (j - i))
            i = j
        elif c == "/" and prox == "*":
            j = src.find("*/", i + 2)
            j = n if j == -1 else j + 2
            trecho = src[i:j]
            saida.append("".join(ch if ch == "\n" else " " for ch in trecho))
            i = j
        elif c == '"' and src[i:i + 3] == '"""':
            j = src.find('"""', i + 3)
            j = n if j == -1 else j + 3
            trecho = src[i:j]
            saida.append("".join(ch if ch == "\n" else " " for ch in trecho))
            i = j
        elif c == '"':
            j = i + 1
            while j < n and src[j] != '"':
                j += 2 if src[j] == "\\" else 1
            j = min(j + 1, n)
            saida.append("x" * (j - i))
            i = j
        elif c == "'":
            j = i + 1
            while j < n and src[j] != "'":
                j += 2 if src[j] == "\\" else 1
            j = min(j + 1, n)
            saida.append("x" * (j - i))
            i = j
        else:
            saida.append(c)
            i += 1
    return "".join(saida)


def _fechar_parenteses(src: str, abre: int) -> int:
    """Índice do ')' que fecha o '(' em `abre`, ou -1."""
    profundidade = 0
    for k in range(abre, len(src)):
        ch = src[k]
        if ch == "(":
            profundidade += 1
        elif ch == ")":
            profundidade -= 1
            if profundidade == 0:
                return k
    return -1


def _contar_parametros(lista: str) -> int:
    """Nº de parâmetros de topo dentro dos parênteses (conteúdo sem os '()')."""
    if not lista.strip():
        return 0
    par = colch = chave = ang = 0
    params = 1
    for ch in lista:
        if ch == "(":
            par += 1
        elif ch == ")":
            par -= 1
        elif ch == "[":
            colch += 1
        elif ch == "]":
            colch -= 1
        elif ch == "{":
            chave += 1
        elif ch == "}":
            chave -= 1
        elif ch == "<":
            ang += 1
        elif ch == ">":
            ang = max(0, ang - 1)
        elif ch == "," and par == colch == chave == ang == 0:
            params += 1
    return params


def _cada_param_em_sua_linha(lista: str) -> bool:
    """True se toda vírgula de topo é seguida de quebra de linha."""
    par = colch = chave = ang = 0
    for idx, ch in enumerate(lista):
        if ch == "(":
            par += 1
        elif ch == ")":
            par -= 1
        elif ch == "[":
            colch += 1
        elif ch == "]":
            colch -= 1
        elif ch == "{":
            chave += 1
        elif ch == "}":
            chave -= 1
        elif ch == "<":
            ang += 1
        elif ch == ">":
            ang = max(0, ang - 1)
        elif ch == "," and par == colch == chave == ang == 0:
            # depois da vírgula só pode vir espaço/tab até a quebra de linha
            if not re.match(r"[ \t]*\n", lista[idx + 1:]):
                return False
    return True


def _token_anterior(src: str, pos: int) -> str:
    j = pos
    while j > 0 and src[j - 1] in " \t\n\r":
        j -= 1
    fim = j
    while j > 0 and (src[j - 1].isalnum() or src[j - 1] in "_$"):
        j -= 1
    return src[j:fim]


def _eh_declaracao(src: str, nome: str, ini_nome: int, fim_par: int) -> bool:
    """Heurística: identificador seguido de '(' cujo ')' é seguido de '{',
    ';' ou 'throws', e que não é chamada, anotação nem 'new X('."""
    if nome in _NOMES_NAO_METODO:
        return False
    antes = src[max(0, ini_nome - 1):ini_nome].strip()
    if antes == "@" or antes == ".":
        return False
    if _token_anterior(src, ini_nome) in _ANTES_NAO_DECL:
        return False
    depois = src[fim_par + 1:fim_par + 200].lstrip()
    if depois.startswith("{") or depois.startswith("throws"):
        return True
    if depois.startswith(";"):
        # abstract / interface: exige modificador ou tipo de retorno antes do nome
        prefixo = src[max(0, ini_nome - 120):ini_nome]
        return bool(re.search(r"(public|private|protected|abstract|default|static)\s+$", prefixo)
                    or re.search(r"[\w>\]]\s+$", prefixo))
    return False


def violacoes_no_arquivo(caminho: str):
    with open(caminho, "r", encoding="utf-8") as fh:
        original = fh.read()
    src = _remover_comentarios_e_strings(original)
    achados = []
    for m in _IDENT.finditer(src):
        nome = m.group(0)
        p = m.end()
        while p < len(src) and src[p] in " \t\n\r":
            p += 1
        if p >= len(src) or src[p] != "(":
            continue
        fecha = _fechar_parenteses(src, p)
        if fecha == -1:
            continue
        if not _eh_declaracao(src, nome, m.start(), fecha):
            continue
        lista = src[p + 1:fecha]
        n_params = _contar_parametros(lista)
        linha = original.count("\n", 0, m.start()) + 1
        uma_linha = "\n" not in original[p:fecha + 1]
        if n_params <= LIMITE_UMA_LINHA:
            if not uma_linha:
                achados.append((linha, nome, n_params,
                                f"{n_params} parâmetro(s): a assinatura deve caber numa única linha"))
        else:
            if uma_linha or not _cada_param_em_sua_linha(lista):
                achados.append((linha, nome, n_params,
                                f"{n_params} parâmetros: cada parâmetro em sua própria linha"))
    return achados


def _arquivos_java(caminhos):
    for c in caminhos:
        if os.path.isfile(c) and c.endswith(".java"):
            yield c
        elif os.path.isdir(c):
            for raiz, _, arquivos in os.walk(c):
                for a in sorted(arquivos):
                    if a.endswith(".java"):
                        yield os.path.join(raiz, a)


def _java_staged():
    out = subprocess.run(
        ["git", "diff", "--cached", "--name-only", "--diff-filter=ACMR"],
        capture_output=True, text=True, check=True,
    ).stdout.split()
    return [a for a in out if a.endswith(".java") and os.path.isfile(a)]


def main(argv):
    if argv == ["--staged"]:
        arquivos = _java_staged()
    elif argv:
        arquivos = list(_arquivos_java(argv))
    else:
        arquivos = list(_arquivos_java([p for p in ("src/main/java", "src/test/java")
                                        if os.path.isdir(p)]))

    total = 0
    for arq in arquivos:
        for linha, _nome, _n, motivo in violacoes_no_arquivo(arq):
            print(f"{arq}:{linha} — {motivo}")
            total += 1

    if total:
        print(f"\n{total} violação(ões) da convenção de assinatura "
              f"(guia-padroes-java.md, seção Formatação).", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
