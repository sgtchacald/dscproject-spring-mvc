# Documentos de Análise — dscproject-spring-mvc

Padrão 72B (documento de análise híbrido, 17 seções). Uma pasta por módulo/tela,
nomeada `NN - nome-com-hifen` para ordenar sozinha.

| Pasta | Conteúdo | Situação |
|---|---|---|
| `00 - analise-geral` | Documento 0 — Fundação: 26 tabelas (RBAC + domínio financeiro + Open Finance), DDL MySQL, mapeamento JPA, DER e diagrama de classes. Base transversal de todos os demais. | Escrito (v1.2) |
| `01 - manter-usuario` | CRUD de Usuário (mescla gerações 1 e 2) + RBAC + recuperação de senha por token. Tela nova `USUARIOS_RECUPERACAO_SENHA`. Protótipo navegável + wireframes na Seção 7 | Escrito (v1.1) — telas a construir |
| `02 - manter-perfil-permissao` | Tela de RBAC: gerenciar perfis e vincular permissões. Catálogo de permissões vem do código (sincronizador). Travas anti-lockout | Escrito (v1.0) — telas a construir |
| `03 - manter-categoria` | CRUD de Categoria + mapa `CATEGORIAS_PROVEDOR` | A escrever |
| `04 - manter-instituicao-financeira` | CRUD de Instituição Financeira + `OPFI_INSTITUICAO_PROVEDOR` | A escrever |
| `05 - manter-conta` | CRUD de Conta | A escrever |
| `06 - manter-cartao-credito` | CRUD de Cartão de Crédito | A escrever |
| `07 - manter-receita` | CRUD de Receita | A escrever |
| `08 - manter-despesa` | CRUD de Despesa — parcelamento e rateio entre usuários | A escrever |
| `09 - manter-transacao-bancaria` | CRUD de Transação Bancária | A escrever |
| `10 - manter-fatura-cartao` | Fatura de Cartão — ciclo de vida, pagamento | A escrever |
| `11 - manter-investimento` | CRUD de Investimento | A escrever |
| `12 - dashboard` | Totais por competência, saldo consolidado, gráficos | A escrever |
| `13 - open-finance-conectar-conta` | Provedor, credencial cifrada, widget de conexão, consentimento | A escrever |
| `14 - open-finance-conciliacao` | Conciliação das transações de staging com o domínio | A escrever |

## Convenções

- Cada documento segue o template `72b-template-doc-analise.md`.
- Identificadores: `EDP_NNNNN` (endpoints), `RN_NNNNN` (regras de negócio), `MSG_NNNNN`
  (mensagens), `C1`/`C2` (consultas), `RT01` (regras de tela), `QUADRO_DESCRITIVO_N`.
- Referência cruzada é `ação + link + pontuação` — não repetir no ponto da citação o
  que o destino já diz.
- O documento 0 é a fonte única da estrutura de dados; os documentos de tela
  referenciam os `QUADRO_DESCRITIVO` dele, não redefinem tabelas.
- Diagramas: `.drawio` (fonte) + `images/*.png` (render, via `flatpak run com.jgraph.drawio.desktop -x -f png`).
