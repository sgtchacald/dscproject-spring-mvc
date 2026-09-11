# Documentos de Análise — dscproject-spring-mvc

Padrão 72B (documento de análise híbrido, 17 seções). Uma pasta por módulo/tela,
nomeada `NN - nome-com-hifen` para ordenar sozinha.

## Ciclo de vida do documento

Cada documento carrega um campo **`Status`** no cabeçalho (logo abaixo de
`Versão`); a coluna **Situação** desta tabela repete o mesmo valor. Progressão:

| Status | Significado | Quem marca |
|---|---|---|
| `A escrever` | Pasta/tema previstos, documento ainda não redigido | analista-requisitos / usuário |
| `Em análise` | Documento em redação — seções incompletas ou decisões de escopo em aberto | analista-requisitos |
| `Analisado` | Corpo das 17 seções fechado e versionado; pronto para desenvolvimento. Itens menores podem seguir listados em "A Confirmar" na Seção 17 sem bloquear | analista-requisitos |
| `Desenvolvido` | **Todos** os requisitos funcionais (RF) e não funcionais (RNF) do documento implementados e cobertos por teste; branch de feature commitada no repo-alvo | **desenvolvedor-java** |
| `Homologado` | Tela publicada em homologação, testada e aprovada pelo usuário | **usuário** (ou Claude, a pedido do usuário) |

### Diretrizes

- **D-CICLO-01** — O `desenvolvedor-java` só move o Status para `Desenvolvido`
  quando **todos** os RF (Seção 3.1) e RNF (Seção 3.2) do documento estiverem
  implementados e com teste passando. Cobertura parcial mantém `Analisado`; a
  pendência vai para o relatório de execução.
- **D-CICLO-02** — `Desenvolvido → Homologado` é decisão do usuário, depois de
  validar a tela no ambiente de homologação. O usuário marca ou pede ao Claude que
  marque.
- **D-CICLO-03** — Ao mudar o Status, atualizar **os dois lugares**: o cabeçalho do
  documento e a coluna Situação deste README. Mudança de Status é metadado — não
  gera nova versão nem linha no Histórico de Versões, a menos que venha junto de
  alteração de conteúdo.

## Documentos

| Pasta | Conteúdo | Situação |
|---|---|---|
| `00 - analise-geral` | Documento 0 — Fundação: tabelas (RBAC + domínio financeiro + Open Finance + parâmetros globais), DDL MySQL, mapeamento JPA, DER e diagrama de classes. Base transversal de todos os demais. | `Analisado` · v1.4 |
| `01 - manter-usuario` | CRUD de Usuário (mescla gerações 1 e 2) + RBAC + recuperação de senha por token. Tela nova `USUARIOS_RECUPERACAO_SENHA`. v1.3: troca de senha vira ação dedicada no grid (ADMIN) + tela self-service **Configurações da Conta** (`/minha-conta`). Protótipo navegável + wireframes na Seção 7 | `Homologado` · v1.3.1 |
| `02 - manter-perfil-permissao` | Tela de RBAC: gerenciar perfis e vincular permissões. Catálogo de permissões vem do código (sincronizador). Travas anti-lockout | `Homologado` · v1.2 |
| `03 - manter-parametro-global` | Tela **só de edição** dos parâmetros globais do sistema (`PARAMETROS_GLOBAIS`, prefixo `PAGL_`). Os parâmetros são semeados por um **loader no código** (mesmo padrão do catálogo de permissões) — a tela não cria nem exclui. v1.1: `tipo` e `valor` são editados **inline no grid** (a célula vira o editor do tipo da linha em tempo de execução); a confirmação de cada alteração pede o `motivo` num modal enxuto ("Confirmar Alteração") — não há tela/modal de edição. Histórico de revisões (Envers) no escopo. Engenharia reversa do `portal-lgpd-api`. Nova tabela no Documento 0: `PARAMETROS_GLOBAIS` (QUADRO_DESCRITIVO_28, v1.4) | `Desenvolvido` · v1.1 |
| `04 - manter-categoria` | CRUD de Categoria + tela do mapa `CATEGORIAS_PROVEDOR`. Categoria de sistema (carga inicial do enum da geração 1) não excluível e de código imutável (`CATE_FL_SISTEMA`). Permissões granulares por operação (`CATEGORIAS_LISTAR`/`_INSERIR`/`_EDITAR`/`_EXCLUIR` e `CATEGORIAS_PROVEDOR_LISTAR`/`_INSERIR`/`_EDITAR`/`_EXCLUIR`). CRUD server-side sem AJAX. | `Desenvolvido` · v1.1 |
| `05 - manter-instituicao-financeira` | CRUD de Instituição Financeira (catálogo global de bancos/corretoras, ADMIN-only, dá tela ao `InstituicaoFinanceiraController` REST da geração 1 + `INFI_FL_ATIVO`) + tela do mapa `OPFI_INSTITUICAO_PROVEDOR`. Instituição de carga inicial protegida por `INFI_FL_SISTEMA` (padrão de `CATE_FL_SISTEMA`); trava de exclusão por uso em `CONTAS`/`INVESTIMENTOS`/`OPFI_CONEXOES`. Permissões `INSTITUICOES_LISTAR`/`_MANTER` e `INSTITUICOES_PROVEDOR_LISTAR`/`_MANTER` | `Desenvolvido` · v1.0 |
| `06 - manter-conta` | CRUD das contas **do usuário** (tela "Minhas Contas") sobre `CONTAS` (prefixo `CTA_`, ex-`INSTITUICOES_FINANCEIRAS_USUARIO` da geração 1): descrição, tipo (enum `TipoConta` novo), instituição (combobox do `05`), agência, número, moeda, saldo inicial, "considera no saldo geral", situação. Escopo *row-level* por `USU_ID` no serviço — **inclusive para o ADMIN**, sem visão administrativa de dados financeiros de outros; acesso cruzado por id responde 404. Saldo manual com ação dedicada "Ajustar saldo" (Envers registra). Exclusão lógica com trava de uso (oferta de desativar). Endpoint de contas ativas alimenta `07`/`08`/`09`/`10`. Permissões `CONTAS_LISTAR`/`_MANTER` a ADMIN e USER — o escopo é RN, não permissão | `Desenvolvido` · v1.0 |
| `07 - manter-cartao-credito` | CRUD dos cartões de crédito **do usuário** (tela "Meus Cartões"): descrição, bandeira, final do cartão, limite, dia de fechamento/vencimento, conta de débito opcional, situação. Escopo por `USU_ID` no serviço — **inclusive para o ADMIN**; acesso cruzado por id responde 404. Permissões `CARTOES_LISTAR`/`_MANTER` a ADMIN e USER — o escopo é RN, não permissão. Sem "cartão de sistema"; exclusão lógica com trava por faturas/despesas vinculadas (oferta de desativar). Depende do `06` (combobox de conta de débito) e alimenta o `09` (cartões ativos); fatura de cartão fica no `11` | `Analisado` · v1.0 — pendente: diagramas/wireframes/protótipo; itens "A Confirmar" na Seção 17 |
| `08 - manter-receita` | CRUD das receitas **do usuário** (tela "Finanças > Receitas") sobre `RECEITAS` (prefixo `RECE_`): nome, descrição, valor, data de lançamento, competência (`YearMonth`), conta (combobox do `06`), categoria opcional (combobox do `04`, `aplicaA=RECEITA`). Introduz **prevista × recebida** (`RECE_FL_RECEBIDO` + `RECE_DT_RECEBIMENTO`, com ação dedicada "Registrar recebimento") e `RECE_ORIGEM`. Escopo *row-level* por usuário derivado da conta (`RECE.CTA_ID → CONTAS.USU_ID`) — **inclusive para o ADMIN**; acesso cruzado por id responde 404. Conta obrigatória na aplicação. Receita `OPEN_FINANCE` editável nos campos de negócio, sem conta/origem e não excluível pela tela. Sem trava de uso (receita é folha); exclusão lógica. Remove os enums de tipo/categoria da geração 1. Permissões `RECEITAS_LISTAR`/`_MANTER` a ADMIN e USER — o escopo é RN, não permissão. Recomendação ao Documento 0 (avaliar `USU_ID` no `LancamentoFinanceiro`) na Seção 17 | `Analisado` · v1.0 — pendente: diagramas/wireframes/protótipo; itens "A Confirmar" na Seção 17 |
| `09 - manter-despesa` | CRUD das despesas **do usuário** (tela "Finanças > Despesas") sobre `DESPESAS` (prefixo `DESP_`) e a associativa `DESPESAS_USUARIO` (`DEPU_`). Escopo *row-level* pelo `USU_ID` da conta **ou** do cartão (conta XOR cartão obrigatório; dinheiro = conta `CARTEIRA`); ADMIN sem visão de terceiros. Dois pontos de complexidade: **parcelamento** (série de `DESPESAS` ligada por `DESP_ID_PARCELA_PAI` — melhoria F; 1ª parcela é a mãe; valor total ÷ N com resíduo na última; competência/vencimento +1 mês; editar afeta só a parcela, excluir a mãe apaga a série) e **rateio entre usuários** (`DESPESAS_USUARIO`, fatia + acerto `DEPU_DT_ACERTO`; recurso de plano pago via `DESPESA_RATEAR_MULTIUSUARIO` concedível por plano; co-participante não vê a despesa na v1.0). Baixa de pagamento individual e em lote (`pagarDespesas`). Compra no cartão grava `CACR_ID`; fatura/`FTCA_ID` são do doc `11`. Despesa `OPEN_FINANCE` com edição parcial. Remove os enums de tipo/categoria da geração 1. Permissões `DESPESAS_LISTAR`/`_MANTER` a ADMIN e USER; importação de extrato fora do escopo | `Analisado` · v1.0 — pendente: diagramas/wireframes/protótipo; itens "A Confirmar" na Seção 17 |
| `10 - manter-transacao-bancaria` | CRUD de Transação Bancária | `A escrever` |
| `11 - manter-fatura-cartao` | Fatura de Cartão — ciclo de vida, pagamento | `A escrever` |
| `12 - manter-investimento` | CRUD de Investimento | `A escrever` |
| `13 - dashboard` | Totais por competência, saldo consolidado, gráficos | `A escrever` |
| `14 - open-finance-conectar-conta` | Provedor, credencial cifrada, widget de conexão, consentimento | `A escrever` |
| `15 - open-finance-conciliacao` | Conciliação das transações de staging com o domínio | `A escrever` |

## Convenções

- Cada documento segue o template `72b-template-doc-analise.md`.
- Identificadores: `EDP_NNNNN` (endpoints), `RN_NNNNN` (regras de negócio), `MSG_NNNNN`
  (mensagens), `C1`/`C2` (consultas), `RT01` (regras de tela), `QUADRO_DESCRITIVO_N`.
- Referência cruzada é `ação + link + pontuação` — não repetir no ponto da citação o
  que o destino já diz.
- O documento 0 é a fonte única da estrutura de dados; os documentos de tela
  referenciam os `QUADRO_DESCRITIVO` dele, não redefinem tabelas.
- Diagramas: `.drawio` (fonte) + `images/*.png` (render, via `flatpak run com.jgraph.drawio.desktop -x -f png`).
