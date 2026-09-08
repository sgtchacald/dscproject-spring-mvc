# dscproject — Análise de Sistemas
## Módulo Despesas — USER / ADMIN — Manter Despesa

**Gerado em:** 08/09/2026
**Versão:** 1.0
**Projeto:** `dscproject-spring-mvc` (geração 2)

---

## Informações sobre o Documento

| Órgão | dscproject | Setor | Pessoal |
|---|---|---|---|
| Plataforma | dscproject-spring-mvc | Disciplina | Documentação |
| Área Cliente | Diego Cordeiro | Versão do Modelo | 2 (padrão híbrido) |

## Revisões e Aprovações

| Responsável por | Nome | Data de Execução |
|---|---|---|
| Revisão | Diego dos Santos Cordeiro | — |

## Histórico de Versões

| Versão | Data | Analista Responsável | Descrição da Alteração |
|---|---|---|---|
| 1.0 | 08/09/2026 | Diego dos Santos Cordeiro | Criação do documento. CRUD das **despesas do próprio usuário** (tela "Finanças > Despesas") para a geração 2 — sucessor do CRUD REST de `Despesa` da geração 1 (`dsc-backend`), agora sobre a tabela `DESPESAS` ([QUADRO_DESCRITIVO_10 do Documento 0](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-10)) e a associativa de rateio `DESPESAS_USUARIO` ([QUADRO_DESCRITIVO_11](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-11)). Cobre os dois pontos de complexidade do módulo: **parcelamento** (série de `DESPESAS` ligadas por `DESP_ID_PARCELA_PAI`, melhoria F) e **rateio entre usuários** (`DESPESAS_USUARIO`, recurso de plano pago via `DESPESA_RATEAR_MULTIUSUARIO`). Introduz a forma de pagamento (`DESP_MEIO_PAGAMENTO`), o vínculo com cartão de crédito (`CACR_ID`), a origem do lançamento (`DESP_ORIGEM`), a baixa de pagamento individual e em lote, e o escopo *row-level* por usuário derivado da conta **ou** do cartão. Remove os enums `DESP_TIPO_TRANSACAO` e `DESP_TIPO_RECEITA_DESPESA` da geração 1 (categoria passa a ser `CATE_ID`). Este documento **referencia** os QUADRO_DESCRITIVO do Documento 0 e **não introduz tabela nova**. |

---

## Diretrizes para Elaboração do Documento

| Nº | DIRETRIZ |
|---|---|
| D01 | As responsabilidades de camada são documentadas como **Regra de Tela (RT)** e **Regra de Negócio (RN)** — nunca "o backend deve" / "o frontend deve". |
| D02 | O termo `endpoint` é aceito na Seção 8. Fora dela, "chamada ao serviço". |
| D03 | A estrutura de dados é a do Documento 0 (`00 - analise-geral`). Este documento **referencia** os QUADRO_DESCRITIVO do Documento 0 e **não introduz tabela nova** nem altera schema. |
| D04 | `DESPESAS` não tem `USU_ID` próprio ([QUADRO_DESCRITIVO_10](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-10)); o dono da despesa é o `USU_ID` da conta (`DESP.CTA_ID → CONTAS.USU_ID`) **ou** do cartão (`DESP.CACR_ID → CARTOES_CREDITO.USU_ID`). O escopo por usuário (*row-level*) é sempre resolvido **no serviço**, a partir do contexto de segurança — nunca de um parâmetro da requisição. |
| D05 | **Importação de extrato / OFX / planilha** (`DESP_ORIGEM = IMPORTACAO`) está **fora do escopo** deste documento — é tema de um documento futuro. Os métodos `importarDadosCartaoCredito*` da geração 1 não são portados aqui. |

---

## 1. Introdução

Este documento descreve a funcionalidade **Manter Despesa** do `dscproject-spring-mvc` — o cadastro e a manutenção das **despesas do próprio usuário**: saídas de dinheiro previstas ou já pagas (compra à vista, compra parcelada, compra no cartão de crédito, conta de consumo, boleto).

Na **geração 1** (API REST + SPA Angular), isto é o CRUD REST de `Despesa` (`dsc-backend`): `GET /despesas` devolve **apenas as despesas do usuário autenticado** (`buscarTodosPorUsuario()` — resolve o usuário pelo token e consulta por `despesa.instituicaoFinanceiraUsuario.usuario`), com `inserir`, `editar`, `excluir`, geração de parcelamento (`gerarParcelamento`), rateio de gastos entre usuários (`DespesaUsuario`, métodos `inserir`/`editar`/`compartilharDespesas`) e baixa de pagamento individual e em lote (`pagarDespesa` / `pagarDespesas`).

O **Documento 0** ([QUADRO_DESCRITIVO_10](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-10) e [QUADRO_DESCRITIVO_11](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-11); Observações 8, 9, 12, 15, 16, 18 e 24) travou a reescrita das tabelas `DESPESAS` e `DESPESAS_USUARIO` para a geração 2:

- a categoria deixa de ser enum e passa a ser `CATE_ID` (FK para `CATEGORIAS`, documento `04 - manter-categoria`); os enums `DESP_TIPO_TRANSACAO` (`TipoRegistroFinanceiro`) e `DESP_TIPO_RECEITA_DESPESA` (`CategoriaRegistroFinanceiro`) são **removidos**;
- `DESP_VALOR_PARCELADO` + `DESP_VALOR_TOTAL_A_DIVIDIR` são consolidados em **`DESP_VALOR_TOTAL_COMPRA`** (valor cheio quando parcelada); `DESP_VALOR` passa a ser o valor **desta despesa/parcela**;
- `DESP_ID_PARCELA_PAI` deixa de ser um `Long` solto e passa a ser **FK real** (auto-relacionamento `@ManyToOne Despesa parcelaPai` — melhoria F, Observação 12);
- a competência (`DESP_COMPETENCIA`) vira `CHAR(7)` no formato `yyyy-MM`, com `CHECK`, mapeada em Java como `java.time.YearMonth` via `YearMonthConverter` (Documento 0, Seção 7.4); o *default* `"0000-00"` da geração 1 é removido;
- `DESP_DT_LANCAMENTO` passa a `LocalDate`;
- entram os campos novos **`DESP_MEIO_PAGAMENTO`** (enum `MeioPagamento`: `DINHEIRO` / `DEBITO` / `CREDITO` / `PIX` / `BOLETO` / `TRANSFERENCIA`), **`DESP_FL_PAGAMENTO_FATURA`** (`BOOLEAN`), **`DESP_ORIGEM`** (`MANUAL` / `OPEN_FINANCE` / `IMPORTACAO`, *default* `MANUAL`), **`CACR_ID`** (FK para `CARTOES_CREDITO`, nulo quando à vista/débito/dinheiro) e **`FTCA_ID`** (FK para `FATURAS_CARTAO`, preenchida pelo documento `11`);
- `DEPU_IND_STATUS_PAGAMENTO` de `DESPESAS_USUARIO` passa de `BOOLEAN` para o enum `StatusPagamento` (`SIM` / `NAO` / `NAO_SE_APLICA`), alinhado com `DESPESAS`; entra `DEPU_DT_ACERTO` (data em que a pessoa acertou a parte dela);
- `Despesa` passa a herdar da superclasse `LancamentoFinanceiro` (`@MappedSuperclass`, Documento 0, Seção 7.2), compartilhando valor, competência, data de lançamento, origem, conta e categoria com `Receita` e `TransacaoBancaria`.

Este documento cobre:

- a **tela Despesas** — listar, cadastrar, editar, registrar o pagamento (individual e em lote) e excluir (exclusão lógica) as despesas do usuário autenticado;
- o **parcelamento** — ao marcar "parcelada" e informar o número de parcelas e o valor total da compra, a tela cria uma **série de registros `DESPESAS`** ligados por `DESP_ID_PARCELA_PAI` ([RN12](#rn12));
- o **rateio entre usuários** — dividir uma despesa com outros usuários reais, cada um responsável por uma fatia (`DEPU_VALOR`), com controle de acerto ([RN15](#rn15) a [RN20](#rn20)); recurso liberado pela permissão `DESPESA_RATEAR_MULTIUSUARIO`, concedível por plano pago;
- o **escopo por usuário**: um usuário nunca vê nem altera a despesa de outro — toda consulta e todo comando filtram pelo `USU_ID` da conta **ou** do cartão da despesa, no serviço;
- o tratamento das despesas **importadas do Open Finance** (`DESP_ORIGEM != MANUAL`), que aparecem nesta tela mas têm conta, cartão, origem e exclusão bloqueadas.

**Escopo deste documento:**
- Tela de **listagem das despesas do usuário** (grid client-side), com filtro por modal (intervalo de competência, status de pagamento, conta/cartão, categoria, parcelada, texto).
- **Cadastro e edição** de despesa via modal único, com as seções: dados básicos, forma de pagamento (conta × cartão × dinheiro/`CARTEIRA`), parcelamento e rateio (esta última só quando o plano permite).
- **Parcelamento**: geração da série, regras de arredondamento, e o comportamento de editar e de excluir uma despesa parcelada.
- **Rateio**: adicionar co-participantes, definir fatias, marcar o acerto de cada fatia, replicar o rateio nas parcelas, e a trava de plano pago.
- **Baixa de pagamento**: ação "Registrar pagamento" no grid (individual) e em lote, marcando `DESP_IND_STATUS_PAGAMENTO = SIM` e gravando `DESP_DT_PAGAMENTO`.
- **Exclusão lógica** de despesa (e de série parcelada).
- Definição das permissões `DESPESAS_LISTAR`, `DESPESAS_MANTER` e `DESPESA_RATEAR_MULTIUSUARIO`, e da regra de escopo por usuário.

**Não contempla:**
- CRUD de **Conta** (`CONTAS`) — documento `06 - manter-conta`. Aqui, a conta é apenas escolhida num combobox alimentado pelo endpoint de opções daquele documento ([EDP07 do `06`](../06%20-%20manter-conta/documento-analise-manter-conta.md#edp07)).
- CRUD de **Cartão de Crédito** (`CARTOES_CREDITO`) — documento `07 - manter-cartao-credito`. Aqui, o cartão é apenas escolhido num combobox alimentado pelo [EDP07 do `07`](../07%20-%20manter-cartao-credito/documento-analise-manter-cartao-credito.md#edp07).
- CRUD de **Categoria** (`CATEGORIAS`) — documento `04 - manter-categoria`. Aqui, a categoria é escolhida num combobox alimentado pelo [EDP07 do `04`](../04%20-%20manter-categoria/documento-analise-manter-categoria.md#edp07), filtrado por `aplicaA=DESPESA`.
- **Fatura de cartão** (`FATURAS_CARTAO`) — ciclo de vida (`ABERTA` → `FECHADA` → `PAGA`), fechamento, cálculo do total, pagamento da fatura e o vínculo `DESP.FTCA_ID` são do documento `11 - manter-fatura-cartao`. Aqui, a compra no cartão só grava `CACR_ID`; `FTCA_ID` fica nulo até o documento `11` existir ([RN23](#rn23)).
- **Importação de extrato / OFX / planilha** (`DESP_ORIGEM = IMPORTACAO`) — documento futuro.
- **Sincronização e conciliação de Open Finance** — a conciliação (documento `15`) pode **criar** uma despesa com `DESP_ORIGEM = OPEN_FINANCE`; esta tela **não** faz sincronização, apenas exibe e edita parcialmente o resultado.
- Cálculo de **saldo por competência, total de gastos por categoria e efeito de `DESP_FL_PAGAMENTO_FATURA` / `DESP_IND_STATUS_PAGAMENTO` no painel** — documento `13 - dashboard`. Aqui só se mantém o dado.
- Despesa **recorrente** (geração automática de ocorrências mês a mês) — fora do escopo da v1.0 (ver Seção 17).
- **Visão do co-participante do rateio** (tela "compartilhado comigo", o outro usuário acertando a própria fatia) — fora do escopo da v1.0; na v1.0 só o dono vê e gerencia o rateio ([RN19](#rn19), Seção 17).

**Perfis com acesso:** [PERF01](#perf01) (ADMIN) e [PERF02](#perf02) (USER). A tela é do **próprio usuário** — cada um opera somente sobre as suas despesas ([RN02](#rn02)). Dado financeiro é privado do dono: o ADMIN opera como um USER comum, sem qualquer visão administrativa das despesas de terceiros. O rateio exige, além do perfil, a permissão `DESPESA_RATEAR_MULTIUSUARIO` — concedida por plano pago ([RN15](#rn15)).

---

## 2. Observações

| Nº | OBSERVAÇÃO | REFERÊNCIA / IMPACTO |
|---|---|---|
| 1 | **`DESPESAS` é dado do próprio usuário, mas o dono é indireto e tem duas âncoras.** A tabela **não tem `USU_ID`** ([QUADRO_DESCRITIVO_10](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-10)); o dono é o `USU_ID` da conta (`DESP.CTA_ID → CONTAS.USU_ID`) **ou** do cartão (`DESP.CACR_ID → CARTOES_CREDITO.USU_ID`). Toda despesa tem exatamente **uma** das duas ([RN03](#rn03)). A tela vive em "Finanças > Despesas", não em "Administração". | [RN02](#rn02), [RN03](#rn03) |
| 2 | **`DESP.CTA_ID` e `DESP.CACR_ID` são anuláveis no schema, mas a aplicação exige um deles.** O Documento 0 mantém as duas colunas anuláveis (uma despesa é à vista **ou** no cartão). A v1.0 exige, na camada de serviço, que **conta e cartão não sejam ambos nulos nem ambos preenchidos** ([RN03](#rn03)). Sem nenhum dos dois, a despesa ficaria órfã (sem dono, invisível em qualquer grid). **O Documento 0 não é alterado.** | [RN03](#rn03), [RNF02](#rnf02) |
| 2a | **Dinheiro em espécie é modelado como conta do tipo `CARTEIRA`.** Não há despesa "sem conta e sem cartão": para registrar um gasto em dinheiro vivo, o usuário escolhe a forma de pagamento "Dinheiro", que aponta para uma conta do tipo `CARTEIRA` (`TipoConta.CARTEIRA`, documento `06 - manter-conta`) e fixa `DESP_MEIO_PAGAMENTO = DINHEIRO`. Decisão herdada do documento `08 - manter-receita`. | [RN03](#rn03), [RN22](#rn22), documento `06` |
| 3 | **Escopo por usuário (*row-level*).** Toda consulta e todo comando de `DESPESAS` filtram pelo `USU_ID` da conta **ou** do cartão, resolvido do contexto de segurança — nunca de um parâmetro. Um endpoint com `{id}` de despesa de outro usuário responde como "não encontrada" ([MSG05](#msg05)). Espelha o `findDespesasByUsuarioId(...)` da geração 1. | [RN02](#rn02), [RNF01](#rnf01), [RNF02](#rnf02) |
| 4 | **Mudanças de estrutura vs geração 1.** Saem os enums `DESP_TIPO_TRANSACAO` e `DESP_TIPO_RECEITA_DESPESA` (categoria vira `CATE_ID`). `DESP_VALOR_PARCELADO` + `DESP_VALOR_TOTAL_A_DIVIDIR` viram `DESP_VALOR_TOTAL_COMPRA`. `DESP_ID_PARCELA_PAI` vira FK. `DEPU_IND_STATUS_PAGAMENTO` vira enum. `DESP_DT_LANCAMENTO` passa a `LocalDate`; `DESP_COMPETENCIA` passa a `YearMonth`. Entram `DESP_MEIO_PAGAMENTO`, `DESP_FL_PAGAMENTO_FATURA`, `DESP_ORIGEM`, `CACR_ID`, `FTCA_ID`, `DEPU_DT_ACERTO`. **[Requer código]** | Documento 0, [QUADRO_DESCRITIVO_10](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-10), [QUADRO_DESCRITIVO_11](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-11) |
| 5 | **Parcelamento é uma série de `DESPESAS`.** Ao marcar "parcelada", a tela informa o **número de parcelas** e o **valor total da compra**; o serviço cria **N registros `DESPESAS`** — a **1ª parcela é a despesa-mãe** (`DESP_ID_PARCELA_PAI` nula, `DESP_NRO_PARCELA = 1`) e as demais apontam para ela ([RN12](#rn12)). `DESP_VALOR` de cada parcela = total ÷ N, com o resíduo do arredondamento na **última** parcela ([RN12](#rn12), parâmetro `DESPESA_RESIDUO_PARCELA_NA_ULTIMA`). `DESP_VALOR_TOTAL_COMPRA` = total em **todas** as parcelas. Competência e data de vencimento avançam **1 mês** por parcela; a data de lançamento (data da compra) é a mesma em todas. | [RN12](#rn12), [RN13](#rn13), [RN14](#rn14) |
| 6 | **Rateio entre usuários (`DESPESAS_USUARIO`).** A despesa **é do criador** (dono via conta/cartão). O rateio adiciona **outros usuários reais** como responsáveis por uma fatia (`DEPU_VALOR`), com status de acerto (`DEPU_IND_STATUS_PAGAMENTO`) e data de acerto (`DEPU_DT_ACERTO`). O par (`DESP_ID`, `USU_ID`) é único. | [RN15](#rn15) a [RN20](#rn20) |
| 7 | **Rateio é recurso de plano pago.** A permissão `DESPESA_RATEAR_MULTIUSUARIO` tem `PERM_FL_CONCEDIVEL_POR_PLANO = TRUE` (Documento 0, Observação 24 e [QUADRO_DESCRITIVO_26](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-26)). Sem essa **permissão efetiva** (perfil ∪ plano), a seção de rateio do modal **não aparece** e os endpoints de rateio **recusam** o corpo ([MSG16](#msg16)). | [RN15](#rn15), Seção 13.1 |
| 8 | **Privacidade do co-participante (decisão da v1.0).** Na v1.0, **somente o dono** enxerga e gerencia o rateio; quem foi adicionado como co-participante de uma despesa que não é dele **não vê essa despesa** nesta tela — a regra de escopo *row-level* ([RN02](#rn02)) continua estritamente pela conta/cartão do dono, sem vazar a despesa para o co-participante. O acerto de cada fatia é registrado **pelo dono** ([RN20](#rn20)). A visão do co-participante ("compartilhado comigo", acertar a própria fatia) fica para uma versão futura ([Seção 17](#17-workshop-de-análise)). | [RN02](#rn02), [RN19](#rn19), [RN20](#rn20) |
| 9 | **Soma das fatias ≤ `DESP_VALOR`.** A soma das fatias dos co-participantes pode ser **menor ou igual** ao valor da despesa; a diferença é a **cota implícita do dono**. A soma **não pode exceder** `DESP_VALOR` ([RN17](#rn17) → [MSG17](#msg17)). Exigir soma exata é item "A Confirmar" (Seção 17). | [RN17](#rn17) |
| 10 | **Rateio replicado nas parcelas.** Quando a despesa é parcelada, o rateio informado é replicado em **cada** parcela da série (`DEPU_VALOR` proporcional ao valor da parcela), como na geração 1. | [RN18](#rn18) |
| 11 | **Compra no cartão × fatura.** Uma despesa com `CACR_ID` preenchido é uma compra no cartão. O **vínculo com a fatura** (`DESP.FTCA_ID`), o fechamento e o cálculo do total da fatura são do documento `11 - manter-fatura-cartao` (Documento 0, Observação 15). Nesta tela, a despesa só grava `CACR_ID`; `FTCA_ID` fica nulo. Para compra no cartão, o serviço fixa `DESP_MEIO_PAGAMENTO = CREDITO` e `DESP_IND_STATUS_PAGAMENTO = NAO_SE_APLICA` ([RN22](#rn22), [RN23](#rn23)). | [RN23](#rn23), documento `11` |
| 12 | **`DESP_FL_PAGAMENTO_FATURA` — caso de borda.** Marca a despesa que **representa o pagamento de uma fatura** de cartão, quando lançada como despesa em vez de `TransacaoBancaria` (Documento 0, Observação 16). Essa despesa é **excluída** do total de gastos por categoria (regra detalhada no documento `13 - dashboard`). A v1.0 **não** oferece esse lançamento pelo fluxo normal da tela; o campo existe para o caminho alternativo e é mencionado aqui sem detalhamento ([RN24](#rn24)). | [RN24](#rn24), documentos `11` e `13` |
| 13 | **Status de pagamento.** `DESP_IND_STATUS_PAGAMENTO` (enum `StatusPagamento`): `NAO` (previsto, ainda não pago — *default* para à vista/boleto/PIX), `SIM` (pago — gravado na baixa, com `DESP_DT_PAGAMENTO`), `NAO_SE_APLICA` (compra no cartão — o pagamento acontece no fechamento da fatura, documento `11`; ou lançamento meramente informativo). Quando se aplica `NAO_SE_APLICA` fora da compra no cartão é item "A Confirmar" (Seção 17). | [RN21](#rn21), [RN23](#rn23) |
| 14 | **Baixa de pagamento — individual e em lote.** Ação "Registrar pagamento" no grid marca uma despesa como paga (`SIM` + `DESP_DT_PAGAMENTO`). A seleção múltipla no grid permite baixar várias de uma vez ([EDP08](#edp08), espelha `pagarDespesas` da geração 1). Numa série parcelada, a baixa é **por parcela**. | [RN21](#rn21) |
| 15 | **Meio de pagamento — derivação.** `DESP_MEIO_PAGAMENTO` (enum `MeioPagamento`) é **derivado da forma de pagamento escolhida e ajustável**: cartão → `CREDITO` (fixo); conta `CARTEIRA` (Dinheiro) → `DINHEIRO` (fixo); conta corrente/poupança → *default* `DEBITO`, ajustável para `PIX` / `BOLETO` / `TRANSFERENCIA` ([RN22](#rn22)). O campo é opcional no schema. Se deve permanecer derivado ou virar campo livre é item "A Confirmar". | [RN22](#rn22) |
| 16 | **Competência é `YearMonth` e independente.** `DESP_COMPETENCIA` é `CHAR(7)` `yyyy-MM`, com `CHECK` e `YearMonthConverter`. É um campo **próprio**, apenas **pré-preenchido** com o mês da data de lançamento e reajustado enquanto o usuário não a editar ([RT13](#rt13), parâmetro `DESPESA_COMPETENCIA_SEGUE_DATA_LANCAMENTO`). Pode divergir do mês da data de lançamento (ex.: compra de 28/01 que "cai" na fatura de fevereiro → competência `2026-02`). **[Requer código]** | [RN04](#rn04), Documento 0, Seção 7.4 |
| 17 | **Origem do lançamento.** `DESP_ORIGEM` (`MANUAL` / `OPEN_FINANCE` / `IMPORTACAO`, *default* `MANUAL` — Documento 0, Observação 18). Despesas criadas por esta tela são **sempre `MANUAL`**; o serviço fixa o valor e ignora qualquer origem recebida ([RN07](#rn07)). Não há migração da geração 1 (Observação 18a). | [RN07](#rn07) |
| 18 | **Despesa importada do Open Finance** (`DESP_ORIGEM != MANUAL`): aparece nesta tela e tem os campos de negócio editáveis (nome, descrição, valor, categoria, competência, datas, status de pagamento, rateio), mas **conta, cartão e origem ficam bloqueados** e ela **não é excluível** por esta tela ([RN08](#rn08)). Espelha a decisão do documento `08 - manter-receita`. | [RN08](#rn08) |
| 19 | **Datas podem ser futuras.** `DESP_DT_LANCAMENTO`, `DESP_DT_VENCIMENTO` e `DESP_DT_PAGAMENTO` aceitam datas posteriores a hoje — a tela serve para planejar despesas previstas (parcela do mês que vem, boleto agendado). O serviço **não** rejeita data futura. | [RN06](#rn06) |
| 20 | **Grid client-side.** A tela carrega a lista das despesas do usuário e pagina/ordena/filtra no navegador (DataTables). Uma despesa parcelada aparece como **N linhas** (uma por parcela), agrupáveis. O volume por usuário é da ordem de centenas de linhas por ano; a v1.0 não faz paginação server-side (ver Seção 17). | [RNF05](#rnf05) |
| 21 | **Auditoria.** `DESPESAS` e `DESPESAS_USUARIO` são auditadas via Hibernate Envers (`@Audited`), conforme o Documento 0. Criação, edição, geração de parcelamento, rateio, baixa de pagamento, acerto de fatia e exclusão lógica ficam registrados. | [RNF03](#rnf03) |
| 22 | **Sem regra de unicidade.** Não há restrição de nome/valor/competência únicos — é normal ter "Mercado" repetido na mesma competência. A deduplicação de importação é tema do documento futuro de importação. | [RN05](#rn05) |
| 23 | **Valor positivo.** `DESP_VALOR` é obrigatório e deve ser **maior que zero** ([RN05](#rn05)). Estorno com valor negativo não é escopo da v1.0. | [RN05](#rn05) |
| 24 | **Conta e cartão só do próprio usuário.** O combobox de conta ([EDP07 do `06`](../06%20-%20manter-conta/documento-analise-manter-conta.md#edp07)) e o de cartão ([EDP07 do `07`](../07%20-%20manter-cartao-credito/documento-analise-manter-cartao-credito.md#edp07)) já devolvem apenas os registros **ativos do usuário autenticado**. O serviço revalida ([C3](#c3), [C4](#c4)). | [SB01](#sb01), [SB02](#sb02) |

---

## 3. Requisitos

### 3.1 Requisitos Funcionais

| ID | DESCRIÇÃO | PRIORIDADE | SITUAÇÃO |
|---|---|---|---|
| <a id="rf01"></a>RF01 | O sistema deve listar as despesas do usuário autenticado, com: competência, nome, categoria, conta ou cartão, valor da despesa/parcela, parcela (ex.: "3/6"), data de lançamento, data de vencimento, status de pagamento, data de pagamento, rateio (indicador) e origem. | Alta | Em análise |
| <a id="rf02"></a>RF02 | O sistema deve permitir filtrar a listagem por texto (nome/descrição), intervalo de competência, status de pagamento, conta/cartão, categoria e "parcelada", por meio de um modal acionado pelo botão "Filtrar". | Média | Em análise |
| <a id="rf03"></a>RF03 | O sistema deve permitir cadastrar uma nova despesa via modal, com dados básicos (nome, descrição, valor, data de lançamento, data de vencimento, competência, categoria), forma de pagamento (conta à vista / cartão de crédito / dinheiro) e status de pagamento. | Alta | Em análise |
| <a id="rf04"></a>RF04 | O sistema deve permitir marcar a despesa como **parcelada**, informando o número de parcelas e o valor total da compra, e gerar automaticamente a série de parcelas (uma despesa por mês). | Alta | Em análise |
| <a id="rf05"></a>RF05 | O sistema deve permitir **ratear** a despesa entre outros usuários reais, definindo a fatia de cada um, desde que o usuário tenha a permissão `DESPESA_RATEAR_MULTIUSUARIO`. | Alta | Em análise |
| <a id="rf06"></a>RF06 | O sistema deve permitir registrar o **acerto** de cada fatia do rateio (status e data), pelo dono da despesa. | Média | Em análise |
| <a id="rf07"></a>RF07 | O sistema deve permitir editar uma despesa existente via modal; numa despesa parcelada, a edição afeta apenas a parcela selecionada. | Alta | Em análise |
| <a id="rf08"></a>RF08 | O sistema deve permitir a exclusão lógica de uma despesa; ao excluir a despesa-mãe de uma série parcelada, deve excluir toda a série. | Alta | Em análise |
| <a id="rf09"></a>RF09 | O sistema deve permitir **registrar o pagamento** de uma despesa por ação dedicada no grid (individual) e de várias despesas em lote (seleção múltipla), gravando a data de pagamento e o status `SIM`. | Alta | Em análise |
| <a id="rf10"></a>RF10 | O sistema deve garantir que cada usuário só liste, consulte, edite, pague e exclua as **próprias** despesas (as vinculadas às suas contas ou aos seus cartões). | Alta | Em análise |
| <a id="rf11"></a>RF11 | O sistema deve exigir que toda despesa tenha **exatamente uma** âncora de pagamento: uma conta **ou** um cartão do próprio usuário, nunca ambos nem nenhum. | Alta | Em análise |
| <a id="rf12"></a>RF12 | O sistema deve exigir valor maior que zero e competência no formato AAAA-MM. | Alta | Em análise |
| <a id="rf13"></a>RF13 | O sistema deve tratar as despesas importadas do Open Finance (`DESP_ORIGEM != MANUAL`) com conta, cartão e origem bloqueados e sem permitir a exclusão por esta tela. | Média | Em análise |
| <a id="rf14"></a>RF14 | O sistema deve ocultar a seção de rateio e recusar operações de rateio para o usuário que não tem a permissão efetiva `DESPESA_RATEAR_MULTIUSUARIO`. | Alta | Em análise |

### 3.2 Requisitos Não Funcionais

| ID | CATEGORIA | DESCRIÇÃO | CRITÉRIO DE ACEITAÇÃO |
|---|---|---|---|
| <a id="rnf01"></a>RNF01 | Segurança | Cada endpoint desta tela exige a autoridade da sua operação (`PERM_DESPESAS_LISTAR`, `PERM_DESPESAS_MANTER` ou `PERM_DESPESA_RATEAR_MULTIUSUARIO` — ver Seção 13 e [RN01](#rn01)). O `USU_ID` usado no filtro e nas travas vem sempre do contexto de segurança, nunca da requisição. | Teste de acesso com ADMIN, com USER, com um perfil sem nenhuma das permissões e com um usuário sem a permissão de rateio. |
| <a id="rnf02"></a>RNF02 | Isolamento | Nenhum endpoint que recebe `{id}` retorna, edita, paga ou exclui a despesa de outro usuário — a resposta é "não encontrada" ([MSG05](#msg05)), sem revelar a existência do registro. O dono é resolvido por `DESP.CTA_ID → CONTAS.USU_ID` **ou** `DESP.CACR_ID → CARTOES_CREDITO.USU_ID`. Um co-participante do rateio **não** ganha acesso à despesa ([RN19](#rn19)). | Teste chamando `buscar/{id}`, `editar/{id}`, `registrar-pagamento/{id}` e `excluir/{id}` com o id de uma despesa de outro usuário, e chamando `listar-dados` com um usuário que é só co-participante. |
| <a id="rnf03"></a>RNF03 | Auditoria | `DESPESAS` e `DESPESAS_USUARIO` têm auditoria completa via Hibernate Envers. Criação, edição, parcelamento, rateio, acerto de fatia, baixa de pagamento e exclusão lógica são registrados. | Inspeção das tabelas `DESPESAS_aud` e `DESPESAS_USUARIO_aud` após as operações. |
| <a id="rnf04"></a>RNF04 | Integridade | A âncora conta-XOR-cartão ([RN03](#rn03)), o valor positivo ([RN05](#rn05)), o formato da competência ([RN04](#rn04)), o número mínimo de parcelas ([RN12](#rn12)), a soma das fatias do rateio ([RN17](#rn17)) e a aceitação de datas futuras ([RN06](#rn06)) são validados no serviço, não só na tela. | Teste chamando os endpoints diretamente, inclusive com conta e cartão juntos, soma de fatias acima do valor e data no futuro. |
| <a id="rnf05"></a>RNF05 | Desempenho | A listagem ([EDP02](#edp02)) responde em menos de 1 s carregando a lista do usuário uma vez. A geração de uma série de até 72 parcelas ([EDP04](#edp04)) responde em menos de 2 s. | Medição em homologação com carga de ~1.000 despesas e uma compra em 72x. |
| <a id="rnf06"></a>RNF06 | Usabilidade | A interface segue o padrão do projeto (Thymeleaf + Tabler + DataTables + AJAX) e é responsiva. O campo de valor usa máscara monetária; a competência usa um seletor mês/ano; a seção de parcelamento mostra o valor da parcela calculado em tempo real. | Revisão visual do protótipo. |

---

## 4. Casos de Uso

[Inserir o diagrama de casos de uso — `prototipo/manter-despesa-casos-uso.drawio` + `images/manter-despesa-casos-uso.png` — quando gerado.]

| CÓDIGO | NOME | ATOR PRINCIPAL | DESCRIÇÃO |
|---|---|---|---|
| <a id="caus01"></a>CAUS01 | Listar Minhas Despesas | [PERF01](#perf01), [PERF02](#perf02) | O usuário acessa o menu e visualiza a lista das suas despesas, incluindo as parcelas das compras parceladas. ([RF01](#rf01), [RF10](#rf10)) |
| <a id="caus02"></a>CAUS02 | Filtrar Despesas | [PERF01](#perf01), [PERF02](#perf02) | O usuário abre o modal de filtro, informa os critérios e aplica. ([RF02](#rf02)) |
| <a id="caus03"></a>CAUS03 | Cadastrar Despesa à Vista | [PERF01](#perf01), [PERF02](#perf02) | O usuário abre o modal, preenche os dados básicos, escolhe a forma de pagamento "Conta à vista" ou "Dinheiro" e confirma. ([RF03](#rf03), [RF11](#rf11), [RF12](#rf12)) |
| <a id="caus04"></a>CAUS04 | Cadastrar Despesa no Cartão | [PERF01](#perf01), [PERF02](#perf02) | O usuário escolhe a forma de pagamento "Cartão de crédito", seleciona o cartão e confirma; o status de pagamento fica `NAO_SE_APLICA`. ([RF03](#rf03), [RN23](#rn23)) |
| <a id="caus05"></a>CAUS05 | Cadastrar Despesa Parcelada | [PERF01](#perf01), [PERF02](#perf02) | O usuário marca "Parcelada", informa o número de parcelas e o valor total; o sistema gera a série de parcelas. ([RF04](#rf04), [RN12](#rn12)) |
| <a id="caus06"></a>CAUS06 | Ratear Despesa entre Usuários | [PERF01](#perf01), [PERF02](#perf02) com `DESPESA_RATEAR_MULTIUSUARIO` | O usuário adiciona co-participantes e define a fatia de cada um; a soma não pode exceder o valor da despesa. ([RF05](#rf05), [RN15](#rn15), [RN17](#rn17)) |
| <a id="caus07"></a>CAUS07 | Registrar Acerto de Fatia | [PERF01](#perf01), [PERF02](#perf02) com `DESPESA_RATEAR_MULTIUSUARIO` | O dono marca a fatia de um co-participante como acertada, gravando a data. ([RF06](#rf06), [RN20](#rn20)) |
| <a id="caus08"></a>CAUS08 | Editar Despesa | [PERF01](#perf01), [PERF02](#perf02) | O usuário abre o modal de edição, altera os dados e confirma; numa série parcelada, a alteração afeta só a parcela selecionada. ([RF07](#rf07), [RN13](#rn13)) |
| <a id="caus09"></a>CAUS09 | Excluir Despesa ou Série | [PERF01](#perf01), [PERF02](#perf02) | O usuário exclui logicamente uma despesa `MANUAL`; ao excluir a despesa-mãe de uma série, exclui todas as parcelas; a despesa importada do Open Finance não é excluível. ([RF08](#rf08), [RN14](#rn14), [RF13](#rf13)) |
| <a id="caus10"></a>CAUS10 | Registrar Pagamento | [PERF01](#perf01), [PERF02](#perf02) | O usuário registra o pagamento de uma despesa pela ação do grid, ou de várias em lote pela seleção múltipla. ([RF09](#rf09), [RN21](#rn21)) |

---

## 5. Localização / Critérios de Aceitação

**Caminho de Navegação:**
- Menu principal > Finanças > Despesas

**Critérios de Aceitação:**
- O menu 'Despesas' é visível apenas para quem tem [PERM01](#perm01).
- Ao acessar a tela, a listagem das despesas do usuário autenticado é carregada automaticamente.
- A listagem nunca traz despesa de outro usuário, nem mesmo aquelas em que o usuário é apenas co-participante do rateio.
- O filtro é aplicado por um modal acionado pelo botão "Filtrar".
- O cadastro e a edição são feitos num modal único, com as seções: dados básicos, forma de pagamento, parcelamento e rateio.
- A forma de pagamento é uma escolha entre "Conta à vista", "Cartão de crédito" e "Dinheiro"; a despesa é salva com **conta ou cartão**, nunca os dois.
- Ao marcar "Parcelada", os campos "Número de parcelas" e "Valor total da compra" aparecem, e o valor da parcela é exibido calculado.
- A seção de rateio só aparece para quem tem [PERM03](#perm03); a soma das fatias não pode exceder o valor da despesa.
- Ao gerar uma série parcelada, são criadas N despesas, com competências e vencimentos consecutivos e o valor total dividido, com o resíduo na última parcela.
- Registrar o pagamento por uma ação do grid grava a data e marca a despesa como paga; a seleção múltipla permite baixar várias.
- Uma despesa importada do Open Finance abre em edição com Conta, Cartão e Origem desabilitados e sem o botão de excluir.
- Chamar `buscar/{id}`, `editar/{id}`, `registrar-pagamento/{id}` ou `excluir/{id}` com o id de uma despesa de outro usuário responde "não encontrada".

---

## 6. Banco de Dados

Toda a estrutura está no **Documento 0** (`00 - analise-geral`). Este documento **não introduz tabela nova** nem executa `ALTER TABLE`.

| Tabela | Onde | Papel nesta tela |
|---|---|---|
| `DESPESAS` | Documento 0 — [QUADRO_DESCRITIVO_10](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-10) | CRUD + parcelamento (auto-relacionamento `DESP_ID_PARCELA_PAI`) + baixa de pagamento, sempre no escopo do `USU_ID` da conta **ou** do cartão |
| `DESPESAS_USUARIO` | Documento 0 — [QUADRO_DESCRITIVO_11](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-11) | Rateio: uma linha por (`DESP_ID`, `USU_ID`); fatia, status e data de acerto |
| `CONTAS` | Documento 0 — [QUADRO_DESCRITIVO_5](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-5) | Somente leitura: âncora de dono (`CONTAS.USU_ID`) e combobox de conta (endpoint do documento `06`) |
| `CARTOES_CREDITO` | Documento 0 — [QUADRO_DESCRITIVO_6](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-6) | Somente leitura: âncora de dono (`CARTOES_CREDITO.USU_ID`) e combobox de cartão (endpoint do documento `07`) |
| `CATEGORIAS` | Documento 0 — [QUADRO_DESCRITIVO_3](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-3) | Somente leitura: combobox de categoria (endpoint do documento `04`, `aplicaA=DESPESA`) |
| `FATURAS_CARTAO` | Documento 0 — [QUADRO_DESCRITIVO_8](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-8) | Contexto: `DESP.FTCA_ID` referencia a fatura em que a compra entrou — preenchido pelo documento `11`, não por esta tela ([RN23](#rn23)) |
| `USUARIOS` | Documento 0 — [QUADRO_DESCRITIVO_2](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-2) | Somente leitura: dono da despesa (via `CONTAS`/`CARTOES_CREDITO`) e co-participantes do rateio |
| `PERMISSOES` | Documento 0 — [QUADRO_DESCRITIVO_26](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-26) | Contexto: `DESPESA_RATEAR_MULTIUSUARIO` com `PERM_FL_CONCEDIVEL_POR_PLANO = TRUE` ([RN15](#rn15)) |

> Nenhum `ALTER TABLE` neste documento. As tabelas `DESPESAS` e `DESPESAS_USUARIO`, a FK de auto-relacionamento `DESP_ID_PARCELA_PAI`, as FKs `CTA_ID` / `CACR_ID` / `FTCA_ID` / `CATE_ID` (todas anuláveis) e o `UNIQUE (DESP_ID, USU_ID)` já existem no Documento 0 ([DDL_10](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-10) e [DDL_11](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-11)). A obrigatoriedade de conta-XOR-cartão na aplicação ([RN03](#rn03)) é validada no serviço — o schema fica como está.

### 6.1 Diagrama ER

[Inserir `images/manter-despesa-der.png` quando gerado — subconjunto do DER do Documento 0: `DESPESAS` (com o auto-relacionamento `DESP_ID_PARCELA_PAI`), `DESPESAS_USUARIO`, `CONTAS`, `CARTOES_CREDITO`, `CATEGORIAS`, `FATURAS_CARTAO` e `USUARIOS`. Fonte: `prototipo/manter-despesa-der.drawio`.]

### 6.2 Auditoria de Tabelas

| TABELA PRINCIPAL | TABELA DE AUDITORIA | CAMPOS AUDITADOS |
|---|---|---|
| DESPESAS | DESPESAS_aud | Competência, nome, descrição, valor, valor total da compra, datas (lançamento, vencimento, pagamento), flags de parcelada e de pagamento de fatura, número e quantidade de parcelas, parcela-pai, meio de pagamento, status de pagamento, origem, conta, cartão, fatura e categoria. Registra criação, edição, parcelamento, baixa de pagamento e exclusão lógica |
| DESPESAS_USUARIO | DESPESAS_USUARIO_aud | Valor da fatia, status de pagamento, data de acerto, despesa e usuário. Registra inclusão no rateio, alteração de fatia, acerto e remoção |

### 6.3 Procedures / Views / Triggers / Functions

Nenhuma. A resolução do dono pela conta ou pelo cartão, a validação de conta-XOR-cartão, a geração da série de parcelas, o cálculo das fatias do rateio, a baixa de pagamento e a exclusão lógica ficam na camada de serviço.

### 6.4 Carga Inicial

Nenhuma para `DESPESAS` / `DESPESAS_USUARIO` (Documento 0, Seção 6.4 — sem *seed*). A permissão `DESPESA_RATEAR_MULTIUSUARIO` entra na carga inicial de `PERMISSOES` pelo catálogo do código, com `PERM_FL_CONCEDIVEL_POR_PLANO = TRUE` e **sem vínculo a perfil** ([RN15](#rn15), Seção 13.1).

---

## 7. Protótipos de Interface

Protótipo navegável e wireframes: `prototipo/manter-despesa-prototipo.html` e `prototipo/manter-despesa-prototipo.drawio` (a gerar). Os números em destaque nas telas correspondem aos IDs dos itens do respectivo QUADRO_DESCRITIVO.

### <a id="quadro-descritivo-1"></a>7.1 Tela: Despesas (Listagem) — QUADRO_DESCRITIVO_1

[Inserir `images/md-tela-1.png` quando gerado.]

> OBSERVAÇÕES: Tela acessada via 'Finanças > Despesas'. Restrita a quem tem [PERM01](#perm01). Grid client-side, carregado apenas com as despesas do usuário autenticado ([EDP02](#edp02) → [C1](#c1)). Cada parcela de uma compra parcelada é uma linha. O filtro é acionado por um modal (botão "Filtrar"). A seleção múltipla habilita a baixa em lote.

| ID | NOME | PROPRIEDADES | OBSERVAÇÕES |
|---|---|---|---|
| <a id="qdd1-0"></a>0 | LINK | Caminho: "/despesas/listar" | — |
| <a id="qdd1-1"></a>1 | BREADCRUMB | Tipo: Texto<br>Texto: Finanças > Despesas | — |
| <a id="qdd1-2"></a>2 | TÍTULO DA TELA | Tipo: Texto<br>Texto: Despesas | — |
| <a id="qdd1-3"></a>3 | DESCRIÇÃO | Tipo: Texto<br>Texto: Cadastre e acompanhe as suas despesas, parcelamentos e divisões de conta. | — |
| <a id="qdd1-4"></a>4 | BOTÃO FILTRAR | Tipo: Botão<br>Texto: Filtrar<br>Ícone: filter | Ao clicar, executar [RT01](#rt01). |
| <a id="qdd1-5"></a>5 | BOTÃO NOVA DESPESA | Tipo: Botão (primário)<br>Texto: Nova despesa<br>Ícone: plus | Visível a quem tem [PERM02](#perm02). Ao clicar, executar [RT04](#rt04). |
| <a id="qdd1-6"></a>6 | BOTÃO REGISTRAR PAGAMENTO EM LOTE | Tipo: Botão<br>Texto: Registrar pagamento<br>Ícone: cash | Visível a quem tem [PERM02](#perm02). Habilitado só quando há linhas selecionadas ([ID8](#qdd1-8)). Ao clicar, executar [RT12](#rt12). |
| <a id="qdd1-7"></a>7 | GRID DE LISTAGEM | Tipo: Grid (DataTables, client-side)<br>Colunas: [ID9](#qdd1-9)…[ID21](#qdd1-21)<br>Itens por página: 10, 25, 50<br>Ordenação padrão: Competência decrescente, depois Data de vencimento crescente<br>Endpoint: [EDP02](#edp02) | Carrega a lista completa das despesas do usuário uma vez. Filtra em memória conforme [RT02](#rt02). |
| <a id="qdd1-8"></a>8 | SELEÇÃO DE LINHA | Tipo: Checkbox por linha + "selecionar todas" | Visível a quem tem [PERM02](#perm02). Só habilita linhas com status de pagamento `NAO`. Alimenta [RT12](#rt12). |
| <a id="qdd1-9"></a>9 | COMPETÊNCIA | Tipo: Coluna<br>Ordenação: Sim | Exibe [C1](#c1).competencia formatada como "MM/AAAA". |
| <a id="qdd1-10"></a>10 | NOME | Tipo: Coluna<br>Ordenação: Sim | Exibe [C1](#c1).nome. |
| <a id="qdd1-11"></a>11 | PARCELA | Tipo: Coluna<br>Ordenação: Não | Exibe "{nroParcela}/{qtdParcelas}" quando [C1](#c1).parcelada; vazio caso contrário. |
| <a id="qdd1-12"></a>12 | CATEGORIA | Tipo: Coluna (badge)<br>Ordenação: Sim | Exibe [C1](#c1).categoriaNome; vazio quando sem categoria. |
| <a id="qdd1-13"></a>13 | PAGAMENTO | Tipo: Coluna<br>Ordenação: Sim | Exibe [C1](#c1).contaDescricao ou [C1](#c1).cartaoDescricao, com um ícone que distingue conta de cartão. |
| <a id="qdd1-14"></a>14 | MEIO | Tipo: Coluna (badge)<br>Ordenação: Sim | Exibe [C1](#c1).meioPagamento ("Dinheiro", "Débito", "Crédito", "PIX", "Boleto", "Transferência"); vazio quando nulo. |
| <a id="qdd1-15"></a>15 | VALOR | Tipo: Coluna (moeda, alinhada à direita)<br>Ordenação: Sim | Exibe [C1](#c1).valor (valor desta despesa/parcela) formatado em BRL. |
| <a id="qdd1-16"></a>16 | DATA DE LANÇAMENTO | Tipo: Coluna (data)<br>Ordenação: Sim | Exibe [C1](#c1).dataLancamento. |
| <a id="qdd1-17"></a>17 | DATA DE VENCIMENTO | Tipo: Coluna (data)<br>Ordenação: Sim | Exibe [C1](#c1).dataVencimento; vazio quando nulo. |
| <a id="qdd1-18"></a>18 | STATUS DE PAGAMENTO | Tipo: Coluna (badge)<br>Ordenação: Sim | "Pago" (verde) quando [C1](#c1).statusPagamento = `SIM`; "Em aberto" (amarelo) quando `NAO`; "N/A" (cinza) quando `NAO_SE_APLICA`. "Excluída" quando há `audit_data_exclusao`. |
| <a id="qdd1-19"></a>19 | DATA DE PAGAMENTO | Tipo: Coluna (data)<br>Ordenação: Sim | Exibe [C1](#c1).dataPagamento; vazio quando não pago. |
| <a id="qdd1-20"></a>20 | RATEIO | Tipo: Coluna (ícone)<br>Ordenação: Não | Ícone "users" quando [C1](#c1).temRateio; tooltip "Dividida com N pessoa(s)". Visível a quem tem [PERM03](#perm03). |
| <a id="qdd1-21"></a>21 | ORIGEM | Tipo: Coluna (badge)<br>Ordenação: Sim | "Manual" / "Open Finance" / "Importação", de [C1](#c1).origem. |
| <a id="qdd1-22"></a>22 | AÇÃO | Tipo: Coluna | Visível a quem tem [PERM02](#perm02). Ícones [ID23](#qdd1-23). |
| <a id="qdd1-23"></a>23 | ÍCONES DE AÇÃO | Tipo: Ícones<br>Editar (ícone: edit, tooltip: Editar despesa)<br>Registrar pagamento (ícone: cash-banknote, tooltip: Registrar pagamento)<br>Ratear (ícone: users, tooltip: Dividir despesa)<br>Excluir (ícone: trash, tooltip: Excluir despesa) | Editar → [RT05](#rt05). Registrar pagamento → [RT11](#rt11); oculto quando [C1](#c1).statusPagamento ≠ `NAO`. Ratear → [RT10](#rt10); visível só com [PERM03](#perm03). Excluir → [RT07](#rt07); oculto quando a despesa já está excluída ou quando [C1](#c1).origem ≠ "MANUAL" ([RN08](#rn08)). |

### <a id="quadro-descritivo-2"></a>7.2 Modal: Filtrar Despesas — QUADRO_DESCRITIVO_2

[Inserir `images/md-tela-2.png` quando gerado.]

> OBSERVAÇÕES: Todos os campos são opcionais. O filtro é aplicado em memória sobre a lista já carregada ([RT02](#rt02)).

| ID | NOME | PROPRIEDADES | OBSERVAÇÕES |
|---|---|---|---|
| <a id="qdd2-1"></a>1 | TÍTULO DO MODAL | Tipo: Texto<br>Texto: Filtrar Despesas | — |
| <a id="qdd2-2"></a>2 | FILTRO – BUSCA | Tipo: Input Text<br>Obrigatório: Não<br>Placeholder: Nome ou descrição<br>Tooltip: Filtre por parte do nome ou da descrição. | Filtro parcial e sem acento sobre nome e descrição. |
| <a id="qdd2-3"></a>3 | FILTRO – COMPETÊNCIA INICIAL | Tipo: Seletor mês/ano<br>Obrigatório: Não | Filtra [C1](#c1).competencia ≥ valor. Ver [SB05](#sb05). |
| <a id="qdd2-4"></a>4 | FILTRO – COMPETÊNCIA FINAL | Tipo: Seletor mês/ano<br>Obrigatório: Não | Filtra [C1](#c1).competencia ≤ valor. Ver [SB05](#sb05). |
| <a id="qdd2-5"></a>5 | FILTRO – STATUS DE PAGAMENTO | Tipo: Combobox<br>Obrigatório: Não<br>Valor default: Todos<br>Domínio: Todos / Em aberto / Pago / Não se aplica / Excluída | Filtra por [C1](#c1).statusPagamento e pela presença de `audit_data_exclusao`. Ver [SB04](#sb04). |
| <a id="qdd2-6"></a>6 | FILTRO – FORMA DE PAGAMENTO | Tipo: Combobox<br>Obrigatório: Não<br>Placeholder: Todas<br>Domínio: "Todas" + contas do usuário + cartões do usuário | Filtra por [C1](#c1).contaId / [C1](#c1).cartaoId. Ver [SB01](#sb01) e [SB02](#sb02). |
| <a id="qdd2-7"></a>7 | FILTRO – CATEGORIA | Tipo: Combobox<br>Obrigatório: Não<br>Placeholder: Todas as categorias<br>Domínio: "Todas" + categorias de despesa | Filtra por [C1](#c1).categoriaId. Ver [SB03](#sb03). |
| <a id="qdd2-8"></a>8 | FILTRO – PARCELADA | Tipo: Combobox<br>Obrigatório: Não<br>Valor default: Todas<br>Domínio: Todas / Sim / Não | Filtra por [C1](#c1).parcelada. |
| <a id="qdd2-9"></a>9 | BOTÃO APLICAR | Tipo: Botão<br>Texto: Aplicar | Ao clicar, executar [RT02](#rt02). |
| <a id="qdd2-10"></a>10 | BOTÃO LIMPAR | Tipo: Botão<br>Texto: Limpar | Ao clicar, executar [RT03](#rt03). |

### <a id="quadro-descritivo-3"></a>7.3 Modal: Cadastro / Edição de Despesa — QUADRO_DESCRITIVO_3

[Inserir `images/md-tela-3.png` quando gerado.]

> OBSERVAÇÕES: Modal único de cadastro e edição, restrito a [PERM02](#perm02). Organizado em quatro seções: **Dados básicos**, **Forma de pagamento**, **Parcelamento** e **Rateio**. A seção Parcelamento fica oculta no modo edição de uma parcela existente ([RT09](#rt09)). A seção Rateio só aparece para quem tem [PERM03](#perm03) ([RT10](#rt10)). Ao editar uma despesa importada do Open Finance, os campos Forma de pagamento e Origem ficam desabilitados e o aviso ([ID24](#qdd3-24)) é exibido ([RN08](#rn08)).

| ID | NOME | PROPRIEDADES | OBSERVAÇÕES |
|---|---|---|---|
| <a id="qdd3-1"></a>1 | TÍTULO DO MODAL | Tipo: Texto<br>Texto: Nova despesa / Editar despesa | Varia conforme o modo. No modo edição de parcela, mostra "Editar despesa — parcela {n}/{qtd}". |
| <a id="qdd3-2"></a>2 | SEÇÃO – DADOS BÁSICOS | Tipo: Título de seção | — |
| <a id="qdd3-3"></a>3 | CAMPO – NOME | Tipo: Input Text<br>Tamanho: 100<br>Obrigatório: Sim | Grava `DESP_NOME`. Ex.: "MERCADO", "NOTEBOOK". |
| <a id="qdd3-4"></a>4 | CAMPO – DESCRIÇÃO | Tipo: Textarea<br>Tamanho: 512<br>Obrigatório: Não | Grava `DESP_DESCRICAO`. |
| <a id="qdd3-5"></a>5 | CAMPO – VALOR | Tipo: Input monetário<br>Obrigatório: Sim | Quando não parcelada, grava `DESP_VALOR`. Quando parcelada, o rótulo muda para "Valor total da compra" e alimenta `DESP_VALOR_TOTAL_COMPRA` ([RT09](#rt09)). Deve ser maior que zero ([RN05](#rn05)). |
| <a id="qdd3-6"></a>6 | CAMPO – DATA DE LANÇAMENTO | Tipo: Input Date<br>Obrigatório: Sim<br>Valor default: hoje | Grava `DESP_DT_LANCAMENTO`. Ao mudar, pode reajustar a Competência ([RT13](#rt13)). |
| <a id="qdd3-7"></a>7 | CAMPO – DATA DE VENCIMENTO | Tipo: Input Date<br>Obrigatório: Não | Grava `DESP_DT_VENCIMENTO`. Numa série parcelada, é a data da 1ª parcela e avança 1 mês por parcela ([RN12](#rn12)). |
| <a id="qdd3-8"></a>8 | CAMPO – COMPETÊNCIA | Tipo: Seletor mês/ano<br>Obrigatório: Sim<br>Valor default: mês da data de lançamento | Grava `DESP_COMPETENCIA` (`yyyy-MM`). Ver [SB05](#sb05) e [RN04](#rn04). |
| <a id="qdd3-9"></a>9 | CAMPO – CATEGORIA | Tipo: Combobox<br>Obrigatório: Não<br>Domínio: categorias de despesa ativas | Grava `CATE_ID`. Ver [SB03](#sb03). |
| <a id="qdd3-10"></a>10 | SEÇÃO – FORMA DE PAGAMENTO | Tipo: Título de seção | — |
| <a id="qdd3-11"></a>11 | CAMPO – FORMA DE PAGAMENTO | Tipo: Radio / segmented<br>Obrigatório: Sim<br>Domínio: Conta à vista / Cartão de crédito / Dinheiro | Não persiste diretamente; define qual campo abaixo aparece e o `DESP_MEIO_PAGAMENTO` derivado ([RT08](#rt08), [RN22](#rn22)). **Desabilitado** ao editar despesa com origem ≠ MANUAL ([RN08](#rn08)). |
| <a id="qdd3-12"></a>12 | CAMPO – CONTA | Tipo: Combobox<br>Obrigatório: Sim quando Forma = "Conta à vista"<br>Domínio: contas ativas do usuário | Grava `CTA_ID`. Ver [SB01](#sb01) e [RN03](#rn03). Oculto nas outras formas. |
| <a id="qdd3-13"></a>13 | CAMPO – CARTÃO | Tipo: Combobox<br>Obrigatório: Sim quando Forma = "Cartão de crédito"<br>Domínio: cartões ativos do usuário | Grava `CACR_ID`. Ver [SB02](#sb02) e [RN03](#rn03). Oculto nas outras formas. |
| <a id="qdd3-14"></a>14 | CAMPO – CONTA CARTEIRA | Tipo: Combobox<br>Obrigatório: Sim quando Forma = "Dinheiro"<br>Domínio: contas ativas do usuário do tipo `CARTEIRA` | Grava `CTA_ID`. Fixa `DESP_MEIO_PAGAMENTO = DINHEIRO` ([RN22](#rn22)). Se o usuário não tem conta `CARTEIRA`, exibir [MSG18](#msg18). |
| <a id="qdd3-15"></a>15 | CAMPO – MEIO DE PAGAMENTO | Tipo: Combobox<br>Obrigatório: Não<br>Domínio: Débito / PIX / Boleto / Transferência<br>Exibição: só quando Forma = "Conta à vista" | Grava `DESP_MEIO_PAGAMENTO`. Default `DEBITO`. Fixo em `CREDITO` (cartão) ou `DINHEIRO` (dinheiro), sem exibir o campo ([RN22](#rn22)). |
| <a id="qdd3-16"></a>16 | CAMPO – STATUS DE PAGAMENTO | Tipo: Toggle "Já paguei"<br>Valor default: Não<br>Exibição: oculto quando Forma = "Cartão de crédito" | Quando "Sim", grava `DESP_IND_STATUS_PAGAMENTO = SIM` e exibe [ID17](#qdd3-17). Quando Forma = "Cartão de crédito", o serviço fixa `NAO_SE_APLICA` ([RN23](#rn23)). |
| <a id="qdd3-17"></a>17 | CAMPO – DATA DE PAGAMENTO | Tipo: Input Date<br>Obrigatório: Sim quando "Já paguei" = Sim<br>Valor default: hoje<br>Exibição: só quando "Já paguei" = Sim | Grava `DESP_DT_PAGAMENTO`. Ver [RN21](#rn21). |
| <a id="qdd3-18"></a>18 | SEÇÃO – PARCELAMENTO | Tipo: Título de seção<br>Exibição: oculta no modo edição de parcela ([RT09](#rt09)) | — |
| <a id="qdd3-19"></a>19 | CAMPO – PARCELADA | Tipo: Toggle (Sim/Não)<br>Valor default: Não | Grava `DESP_FL_PARCELADA`. Ao ligar, exibe [ID20](#qdd3-20) e [ID21](#qdd3-21) e muda o rótulo de [ID5](#qdd3-5) ([RT09](#rt09)). |
| <a id="qdd3-20"></a>20 | CAMPO – NÚMERO DE PARCELAS | Tipo: Input numérico<br>Obrigatório: Sim quando Parcelada = Sim<br>Mínimo: 2<br>Máximo: parâmetro `DESPESA_PARCELAS_MAXIMO` | Grava `DESP_QTD_PARCELAS`. Ver [RN12](#rn12). |
| <a id="qdd3-21"></a>21 | VALOR DA PARCELA (CALCULADO) | Tipo: Texto (somente leitura) | Exibe "valor total ÷ nº de parcelas", em BRL, com nota "a última parcela pode diferir alguns centavos" ([RN12](#rn12)). |
| <a id="qdd3-22"></a>22 | SEÇÃO – RATEIO | Tipo: Título de seção<br>Exibição: só com [PERM03](#perm03) ([RT10](#rt10)) | — |
| <a id="qdd3-23"></a>23 | LISTA DE CO-PARTICIPANTES | Tipo: Tabela editável<br>Colunas: Usuário (suggestion box), Valor da fatia (monetário), Status de acerto (badge), remover | Cada linha grava uma `DESPESAS_USUARIO`. Usuário via [SB06](#sb06). Soma das fatias validada por [RT10](#rt10) e [RN17](#rn17). "Minha cota" = valor − soma das fatias, exibida como linha somente leitura. |
| <a id="qdd3-24"></a>24 | AVISO – DESPESA IMPORTADA | Tipo: Texto informativo | Exibido no modo edição quando [C1](#c1).origem ≠ "MANUAL": "Esta despesa foi importada do Open Finance. A forma de pagamento e a origem não podem ser alteradas, e ela não pode ser excluída por esta tela." |
| <a id="qdd3-25"></a>25 | BOTÃO SALVAR | Tipo: Botão (primário)<br>Texto: Salvar<br>Endpoint: [EDP04](#edp04) (criação) ou [EDP05](#edp05) (edição) | Ao clicar, executar [RT06](#rt06). |
| <a id="qdd3-26"></a>26 | BOTÃO CANCELAR | Tipo: Botão<br>Texto: Cancelar | Fecha sem salvar. |

### <a id="quadro-descritivo-4"></a>7.4 Modal: Registrar Pagamento — QUADRO_DESCRITIVO_4

[Inserir `images/md-tela-4.png` quando gerado.]

> OBSERVAÇÕES: Acionado pelo ícone "Registrar pagamento" do grid ([ID23](#qdd1-23)), restrito a [PERM02](#perm02). Marca `DESP_IND_STATUS_PAGAMENTO = SIM` e grava `DESP_DT_PAGAMENTO`. Oculto para despesas já pagas ou com status `NAO_SE_APLICA`.

| ID | NOME | PROPRIEDADES | OBSERVAÇÕES |
|---|---|---|---|
| <a id="qdd4-1"></a>1 | TÍTULO DO MODAL | Tipo: Texto<br>Texto: Registrar pagamento — {nome da despesa} | — |
| <a id="qdd4-2"></a>2 | VALOR DA DESPESA | Tipo: Texto (somente leitura) | Exibe [C1](#c1).valor formatado em BRL; numa parcela, mostra "parcela {n}/{qtd}". |
| <a id="qdd4-3"></a>3 | CAMPO – DATA DE PAGAMENTO | Tipo: Input Date<br>Obrigatório: Sim<br>Valor default: hoje ou a data de vencimento, o que for mais recente | Grava `DESP_DT_PAGAMENTO`. |
| <a id="qdd4-4"></a>4 | BOTÃO SALVAR | Tipo: Botão (primário)<br>Texto: Registrar<br>Endpoint: [EDP07](#edp07) | Ao clicar, executar [RT11](#rt11). |
| <a id="qdd4-5"></a>5 | BOTÃO CANCELAR | Tipo: Botão<br>Texto: Cancelar | Fecha sem salvar. |

### <a id="quadro-descritivo-5"></a>7.5 Modal: Confirmar Pagamento em Lote — QUADRO_DESCRITIVO_5

[Inserir `images/md-tela-5.png` quando gerado.]

> OBSERVAÇÕES: Acionado pelo botão "Registrar pagamento" da barra ([ID6](#qdd1-6)) quando há linhas selecionadas. Espelha `pagarDespesas` da geração 1.

| ID | NOME | PROPRIEDADES | OBSERVAÇÕES |
|---|---|---|---|
| <a id="qdd5-1"></a>1 | TÍTULO DO MODAL | Tipo: Texto<br>Texto: Registrar pagamento em lote | — |
| <a id="qdd5-2"></a>2 | RESUMO DA SELEÇÃO | Tipo: Texto (somente leitura) | "N despesas selecionadas — total {soma} em BRL". Lista as despesas selecionadas com nome, competência e valor. |
| <a id="qdd5-3"></a>3 | CAMPO – DATA DE PAGAMENTO | Tipo: Input Date<br>Obrigatório: Sim<br>Valor default: hoje | Aplicada a todas as despesas selecionadas. |
| <a id="qdd5-4"></a>4 | BOTÃO CONFIRMAR | Tipo: Botão (primário)<br>Texto: Registrar<br>Endpoint: [EDP08](#edp08) | Ao clicar, executar [RT12](#rt12). |
| <a id="qdd5-5"></a>5 | BOTÃO CANCELAR | Tipo: Botão<br>Texto: Cancelar | Fecha sem salvar. |

### 7.6 Suggestion Boxes

| ID | NOME | DESCRIÇÃO |
|---|---|---|
| <a id="sb01"></a>SB01 | CONTA | Itens carregados de `CONTAS` via o endpoint de opções do documento `06 - manter-conta` ([EDP07 daquele documento](../06%20-%20manter-conta/documento-analise-manter-conta.md#edp07) — `GET /contas/opcoes`), que já devolve apenas as contas **ativas do usuário autenticado**. No modal, sem opção vazia; na forma "Dinheiro" ([ID14](#qdd3-14)), a lista é recortada em memória para o tipo `CARTEIRA`. No filtro do grid ([ID6](#qdd2-6)), a opção "Todas" é adicional. Nunca `<option>` fixo no HTML. |
| <a id="sb02"></a>SB02 | CARTÃO | Itens carregados de `CARTOES_CREDITO` via o endpoint de opções do documento `07 - manter-cartao-credito` ([EDP07 daquele documento](../07%20-%20manter-cartao-credito/documento-analise-manter-cartao-credito.md#edp07) — `GET /cartoes/opcoes`), que devolve os cartões **ativos do usuário autenticado**. Nunca `<option>` fixo no HTML. |
| <a id="sb03"></a>SB03 | CATEGORIA | Itens carregados de `CATEGORIAS` via o endpoint de opções do documento `04 - manter-categoria` ([EDP07 daquele documento](../04%20-%20manter-categoria/documento-analise-manter-categoria.md#edp07) — `GET /categorias/opcoes?aplicaA=DESPESA`), que devolve as categorias ativas com `CATE_APLICA_A` em `DESPESA` ou `AMBOS`, ordenadas por nome. No modal, opcional (opção "Sem categoria"). No filtro ([ID7](#qdd2-7)), "Todas as categorias" é adicional. Nunca `<option>` fixo no HTML. |
| <a id="sb04"></a>SB04 | FILTRO STATUS DE PAGAMENTO | Domínio fixo do próprio filtro: Todos, Em aberto, Pago, Não se aplica, Excluída. Não é entidade. |
| <a id="sb05"></a>SB05 | COMPETÊNCIA | Campo mês/ano (`YearMonth`), renderizado como seletor mês/ano ou `<input type="month">`. Não é entidade. |
| <a id="sb06"></a>SB06 | USUÁRIO DO RATEIO | Campo de texto com filtragem em tempo real que consome [EDP10](#edp10) (`GET /despesas/usuarios-rateio?termo=`), devolvendo usuários **ativos** (id, nome, e-mail) exceto o próprio. Só disponível para quem tem [PERM03](#perm03). Não é `<select>` estático. O escopo de quem pode ser adicionado (qualquer usuário × lista de contatos) é item "A Confirmar" (Seção 17). |

### 7.7 Regras de Tela

| ID | DESCRIÇÃO |
|---|---|
| <a id="rt01"></a>RT01 | Ao clicar em "Filtrar" ([ID4](#qdd1-4)), abrir o modal de filtro ([QUADRO_DESCRITIVO_2](#quadro-descritivo-2)) com os valores atualmente aplicados. |
| <a id="rt02"></a>RT02 | Ao clicar em "Aplicar" ([ID9](#qdd2-9)), filtrar **em memória** a lista já carregada: busca parcial e sem acento sobre nome/descrição, competência dentro do intervalo, e correspondência exata de status de pagamento, forma de pagamento, categoria e parcelada. Fechar o modal. Se nada restar, exibir [MSG08](#msg08) na área do grid. |
| <a id="rt03"></a>RT03 | Ao clicar em "Limpar" ([ID10](#qdd2-10)), voltar todos os campos do filtro para vazio (status e parcelada para "Todos") e reaplicar conforme [RT02](#rt02). |
| <a id="rt04"></a>RT04 | Ao clicar em "Nova despesa" ([ID5](#qdd1-5)) — visível só com [PERM02](#perm02) —, abrir o modal ([QUADRO_DESCRITIVO_3](#quadro-descritivo-3)) em modo criação: campos vazios, Data de lançamento em hoje, Competência no mês corrente, Forma de pagamento em "Conta à vista", "Já paguei" em "Não", Parcelada em "Não", seção Rateio vazia. Carregar os comboboxes conforme [RT14](#rt14). |
| <a id="rt05"></a>RT05 | Ao clicar no ícone Editar ([ID23](#qdd1-23)) — visível só com [PERM02](#perm02) —, chamar [EDP03](#edp03) com o id e abrir o modal em modo edição, com os campos preenchidos. Se a despesa é uma parcela de série (`DESP_ID_PARCELA_PAI` não nula ou tem parcelas-filhas), ocultar a seção Parcelamento e mostrar o título com "parcela {n}/{qtd}" ([RN13](#rn13)). Se [C1](#c1).origem ≠ "MANUAL", desabilitar Forma de pagamento e Origem e exibir o aviso [ID24](#qdd3-24) ([RN08](#rn08)). Se há rateio, carregar as linhas. |
| <a id="rt06"></a>RT06 | Ao clicar em "Salvar" ([ID25](#qdd3-25)): validar Nome, Valor, Data de lançamento, Competência obrigatórios ([MSG02](#msg02)); Valor maior que zero ([MSG03](#msg03)); formato da Competência ([MSG11](#msg11)); conforme a Forma de pagamento, Conta **ou** Cartão obrigatório ([MSG12](#msg12)); se Parcelada, Número de parcelas ≥ 2 ([MSG13](#msg13)); se há rateio, soma das fatias ≤ Valor ([MSG17](#msg17)) e cada co-participante válido ([MSG19](#msg19)); se "Já paguei" = Sim, Data de pagamento preenchida ([MSG09](#msg09)). Em criação, chamar [EDP04](#edp04); em edição, [EDP05](#edp05). Em sucesso, exibir [MSG01](#msg01) (criação, ou [MSG14](#msg14) quando gerou série) ou [MSG04](#msg04) (edição), fechar o modal e recarregar o grid via [EDP02](#edp02). |
| <a id="rt07"></a>RT07 | Ao clicar no ícone Excluir ([ID23](#qdd1-23)) — visível só com [PERM02](#perm02) e oculto quando [C1](#c1).origem ≠ "MANUAL" —: se a despesa é a **mãe** de uma série parcelada, exibir a confirmação [MSG15](#msg15) (informando quantas parcelas); se é uma parcela isolada ou despesa avulsa, exibir [MSG06](#msg06). Ao confirmar, chamar [EDP06](#edp06). Despesa importada do Open Finance → [MSG16b](#msg16b). Em sucesso, exibir [MSG07](#msg07) e recarregar o grid. |
| <a id="rt08"></a>RT08 | Ao mudar a Forma de pagamento ([ID11](#qdd3-11)): "Conta à vista" → exibir Conta ([ID12](#qdd3-12)) e Meio de pagamento ([ID15](#qdd3-15), default `DEBITO`), ocultar Cartão e Conta Carteira, exibir o toggle "Já paguei"; "Cartão de crédito" → exibir Cartão ([ID13](#qdd3-13)), ocultar os demais, fixar meio `CREDITO`, ocultar o toggle "Já paguei" ([RN23](#rn23)); "Dinheiro" → exibir Conta Carteira ([ID14](#qdd3-14)), fixar meio `DINHEIRO`, exibir o toggle "Já paguei" com default "Sim". Limpar os campos das formas não escolhidas ([RN03](#rn03), [RN22](#rn22)). |
| <a id="rt09"></a>RT09 | Ao ligar o toggle Parcelada ([ID19](#qdd3-19)): mudar o rótulo do campo Valor ([ID5](#qdd3-5)) para "Valor total da compra", exibir Número de parcelas ([ID20](#qdd3-20)) e o Valor da parcela calculado ([ID21](#qdd3-21)), recalculado a cada mudança de valor ou de número de parcelas. Ao desligar, reverter. No modo edição de uma parcela existente, a seção Parcelamento fica **oculta** — não se reparcelam parcelas ([RN13](#rn13)). |
| <a id="rt10"></a>RT10 | A seção Rateio ([ID22](#qdd3-22)) só é renderizada quando o usuário tem [PERM03](#perm03). Ao adicionar uma linha, o campo Usuário usa [SB06](#sb06). A cada mudança de fatia, recalcular "Minha cota" = Valor − soma das fatias e validar que a soma não excede o Valor ([RN17](#rn17) → [MSG17](#msg17)). O acerto de uma fatia (marcar como acertada + data) é editável na própria linha e persistido junto com o Salvar, ou pelo ícone "Ratear" do grid ([ID23](#qdd1-23)) que reabre o modal na seção Rateio. Quando a despesa é parcelada, avisar que o rateio será replicado em todas as parcelas ([RN18](#rn18)). |
| <a id="rt11"></a>RT11 | Ao clicar no ícone Registrar pagamento ([ID23](#qdd1-23)) — visível só com [PERM02](#perm02) e oculto quando [C1](#c1).statusPagamento ≠ `NAO` —, abrir o modal ([QUADRO_DESCRITIVO_4](#quadro-descritivo-4)) com o Valor ([ID2](#qdd4-2)) preenchido e a Data de pagamento ([ID3](#qdd4-3)) iniciada em hoje. Ao clicar em "Registrar": validar a data preenchida ([MSG02](#msg02)) e chamar [EDP07](#edp07). Em sucesso, exibir [MSG10](#msg10), fechar o modal e recarregar o grid. |
| <a id="rt12"></a>RT12 | Ao clicar em "Registrar pagamento" da barra ([ID6](#qdd1-6)) com linhas selecionadas ([ID8](#qdd1-8)), abrir o modal ([QUADRO_DESCRITIVO_5](#quadro-descritivo-5)) com o resumo da seleção. Ao confirmar, validar a data ([MSG02](#msg02)) e chamar [EDP08](#edp08) com a lista de ids e a data. Em sucesso, exibir [MSG10b](#msg10b), limpar a seleção e recarregar o grid. |
| <a id="rt13"></a>RT13 | Aplicar máscara monetária ao campo Valor ([ID5](#qdd3-5)) e formatar a coluna Valor do grid ([ID15](#qdd1-15)) em BRL. Ao alterar a Data de lançamento ([ID6](#qdd3-6)), e enquanto o usuário não tiver editado a Competência ([ID8](#qdd3-8)) manualmente, atualizar a Competência para o mês da nova data — comportamento controlado pelo parâmetro `DESPESA_COMPETENCIA_SEGUE_DATA_LANCAMENTO` ([Seção 12](#12-parâmetros-de-sistema)). |
| <a id="rt14"></a>RT14 | Ao abrir o modal de despesa ([QUADRO_DESCRITIVO_3](#quadro-descritivo-3)), carregar o combobox Conta pelo endpoint de opções do documento `06` ([SB01](#sb01)), o combobox Cartão pelo endpoint do documento `07` ([SB02](#sb02)) e o combobox Categoria pelo endpoint do documento `04` com `aplicaA=DESPESA` ([SB03](#sb03)). Nunca renderizar `<option>` fixo no HTML. |

---

## 8. Endpoints

| CÓDIGO | HTTP | PERMISSÃO | PATH | FINALIZADO? |
|---|---|---|---|---|
| <a id="edp01"></a>EDP01 | GET | [PERM01](#perm01) | /despesas/listar | N |
| Retorna a página da listagem de despesas (Thymeleaf). O grid é carregado por [EDP02](#edp02). | | | | |
| <a id="edp02"></a>EDP02 | GET | [PERM01](#perm01) | /despesas/listar-dados | N |
| Lista das despesas do usuário autenticado para o grid, em JSON. Executa [C1](#c1) com o `USU_ID` do contexto de segurança ([RN02](#rn02)). Campos por linha: id, competencia, nome, descricao, valor, valorTotalCompra, parcelada, nroParcela, qtdParcelas, idParcelaPai, meioPagamento, statusPagamento, dataLancamento, dataVencimento, dataPagamento, origem, contaId, contaDescricao, cartaoId, cartaoDescricao, categoriaId, categoriaNome, temRateio (boolean), qtdCoParticipantes, excluido (boolean). Sem paginação (client-side). | | | | |
| <a id="edp03"></a>EDP03 | GET | [PERM02](#perm02) | /despesas/buscar/{id} | N |
| Retorna uma despesa do usuário autenticado para edição. Executa [RN02](#rn02) (via [C2](#c2)) — se não for do usuário, 404 ([MSG05](#msg05)). Campos: os de [EDP02](#edp02) mais o rateio (lista de {usuarioId, usuarioNome, valor, statusPagamento, dataAcerto}, via [C7](#c7)). | | | | |
| <a id="edp04"></a>EDP04 | POST | [PERM02](#perm02) | /despesas/inserir | N |
| Cria uma despesa para o usuário autenticado. Executa, nesta ordem: [RN03](#rn03) (via [C3](#c3) e/ou [C4](#c4) — conta XOR cartão, ativos e do usuário), [RN04](#rn04) (competência), [RN05](#rn05) (valor > 0), [RN06](#rn06) (datas), [C5](#c5) (categoria, quando informada), [RN22](#rn22) (deriva `DESP_MEIO_PAGAMENTO`), [RN23](#rn23) (compra no cartão → `NAO_SE_APLICA`), [RN07](#rn07) (fixa `DESP_ORIGEM = MANUAL`); se rateado, [RN15](#rn15) (permissão efetiva), [C8](#c8) (co-participantes válidos), [RN17](#rn17) (soma das fatias); persiste a despesa-mãe; se parcelada, [RN12](#rn12) (gera as demais parcelas); se rateado, [RN18](#rn18) (replica o rateio em cada parcela). Dados: competencia, nome, descricao, valor, valorTotalCompra, dataLancamento, dataVencimento, dataPagamento, formaPagamento, contaId, cartaoId, meioPagamento, statusPagamento, categoriaId, parcelada, qtdParcelas, rateio[]. Retorno: 200 ([MSG01](#msg01) / [MSG14](#msg14)) ou 422 ([MSG02](#msg02) / [MSG03](#msg03) / [MSG09](#msg09) / [MSG11](#msg11) / [MSG12](#msg12) / [MSG13](#msg13) / [MSG16](#msg16) / [MSG17](#msg17) / [MSG18](#msg18) / [MSG19](#msg19)). | | | | |
| <a id="edp05"></a>EDP05 | PUT | [PERM02](#perm02) | /despesas/editar/{id} | N |
| Edita **uma** despesa do usuário autenticado (uma parcela isolada, quando série). Executa, nesta ordem: [RN02](#rn02) (via [C2](#c2) — 404 se não for do usuário), [RN08](#rn08) (se origem ≠ MANUAL, ignora forma de pagamento e origem), [RN13](#rn13) (não altera número/valor total de parcelas de uma série; esses campos são ignorados), [RN03](#rn03) (via [C3](#c3)/[C4](#c4)), [RN04](#rn04), [RN05](#rn05), [RN06](#rn06), [C5](#c5) (categoria), [RN22](#rn22); se rateio no corpo, [RN15](#rn15), [C8](#c8), [RN17](#rn17); persiste. Dados: os de [EDP04](#edp04) exceto `parcelada`/`qtdParcelas`/`valorTotalCompra`. Retorno: 200 ([MSG04](#msg04)) ou 422 (mesmas de [EDP04](#edp04)). | | | | |
| <a id="edp06"></a>EDP06 | DELETE | [PERM02](#perm02) | /despesas/excluir/{id} | N |
| Exclusão lógica da despesa do usuário autenticado. Executa [RN02](#rn02) (via [C2](#c2) — 404 se não for do usuário), [RN08](#rn08) (recusa se origem ≠ MANUAL → [MSG16b](#msg16b)) e [RN14](#rn14): se o `{id}` é a **despesa-mãe** de uma série, exclui logicamente **todas** as parcelas (via [C6](#c6)) e as `DESPESAS_USUARIO` de cada uma; se é parcela isolada ou avulsa, exclui só ela. Preenche `audit_data_exclusao` / `audit_excluido_por`. Retorno: 200 ([MSG07](#msg07)) ou 422 ([MSG16b](#msg16b)). | | | | |
| <a id="edp07"></a>EDP07 | PUT | [PERM02](#perm02) | /despesas/registrar-pagamento/{id} | N |
| Registra o pagamento de **uma** despesa do usuário autenticado. Executa [RN02](#rn02) (via [C2](#c2) — 404 se não for do usuário) e [RN21](#rn21). Dados: dataPagamento. Grava `DESP_IND_STATUS_PAGAMENTO = SIM` e `DESP_DT_PAGAMENTO = dataPagamento`. Recusa quando o status já é `NAO_SE_APLICA` → [MSG20](#msg20). O Envers registra a revisão. Retorno: 200 ([MSG10](#msg10)) ou 422 ([MSG02](#msg02) / [MSG20](#msg20)). | | | | |
| <a id="edp08"></a>EDP08 | POST | [PERM02](#perm02) | /despesas/registrar-pagamento-lote | N |
| Registra o pagamento de **várias** despesas do usuário autenticado (espelha `pagarDespesas` da geração 1). Executa [C9](#c9) (todas as despesas do lote são do usuário; qualquer id fora do escopo → 404 sem baixar nenhuma) e, por despesa, [RN21](#rn21). Dados: idDespesaList[], dataPagamento. Retorno: 200 ([MSG10b](#msg10b)) ou 422. | | | | |
| <a id="edp09"></a>EDP09 | PUT | [PERM03](#perm03) | /despesas/{id}/rateio-acerto | N |
| Registra ou desfaz o **acerto** de uma fatia do rateio (pelo dono). Executa [RN02](#rn02) (via [C2](#c2) — 404), [RN15](#rn15) (permissão efetiva) e [RN20](#rn20). Dados: usuarioId, acertado (boolean), dataAcerto. Grava `DEPU_IND_STATUS_PAGAMENTO` (`SIM`/`NAO`) e `DEPU_DT_ACERTO` na linha `(DESP_ID = {id}, USU_ID = usuarioId)`. Retorno: 200 ([MSG21](#msg21)) ou 422 ([MSG16](#msg16) / [MSG19](#msg19)). | | | | |
| <a id="edp10"></a>EDP10 | GET | [PERM03](#perm03) | /despesas/usuarios-rateio?termo= | N |
| Busca usuários **ativos** para adicionar ao rateio, por parte do nome ou do e-mail (mínimo 3 caracteres), excluindo o próprio usuário. Executa [RN15](#rn15). Campos: id, nome, email. Máx. 20 itens. Alimenta [SB06](#sb06). O critério de elegibilidade (qualquer usuário × lista de contatos) é item "A Confirmar" (Seção 17). | | | | |

> Os comboboxes de conta, cartão e categoria são alimentados pelos endpoints de opções dos documentos `06`, `07` e `04` respectivamente ([EDP07 do `06`](../06%20-%20manter-conta/documento-analise-manter-conta.md#edp07), [EDP07 do `07`](../07%20-%20manter-cartao-credito/documento-analise-manter-cartao-credito.md#edp07), [EDP07 do `04`](../04%20-%20manter-categoria/documento-analise-manter-categoria.md#edp07)). Este documento não os redefine.

---

## 9. Regras de Negócio

| ID | DESCRIÇÃO |
|---|---|
| <a id="rn01"></a>RN01 | Cada endpoint exige a autoridade da sua operação: [EDP01](#edp01) / [EDP02](#edp02) → `PERM_DESPESAS_LISTAR`; [EDP03](#edp03) / [EDP04](#edp04) / [EDP05](#edp05) / [EDP06](#edp06) / [EDP07](#edp07) / [EDP08](#edp08) → `PERM_DESPESAS_MANTER`; [EDP09](#edp09) / [EDP10](#edp10) → `PERM_DESPESA_RATEAR_MULTIUSUARIO`. As autoridades são resolvidas pelo `getAuthorities()` do `Usuario` como **permissão efetiva = permissões do perfil ∪ permissões do plano** (Documento 0, Observação 24). `DESPESAS_MANTER` pressupõe `DESPESAS_LISTAR`. A permissão **habilita a tela / o recurso**; o recorte por dono é a [RN02](#rn02). |
| <a id="rn02"></a>RN02 | **Escopo por usuário (*row-level*).** Toda consulta e todo comando de `DESPESAS` são restritos às despesas cuja conta (`DESP.CTA_ID → CONTAS.USU_ID`) **ou** cujo cartão (`DESP.CACR_ID → CARTOES_CREDITO.USU_ID`) pertence ao usuário autenticado, obtido do contexto de segurança — nunca de um parâmetro. Em [EDP03](#edp03), [EDP05](#edp05), [EDP06](#edp06), [EDP07](#edp07), [EDP08](#edp08) e [EDP09](#edp09), se a despesa do `{id}` não for do usuário, o serviço responde **404** com [MSG05](#msg05), sem distinguir "não existe" de "é de outro usuário". Executa [C2](#c2). Aparecer como co-participante do rateio **não** dá acesso à despesa ([RN19](#rn19)). Espelha o `findDespesasByUsuarioId(...)` da geração 1. |
| <a id="rn03"></a>RN03 | **Âncora de pagamento: conta XOR cartão.** Toda despesa tem **exatamente uma** de `CTA_ID` ou `CACR_ID` — nunca ambas, nunca nenhuma (embora o schema permita as duas anuláveis — [QUADRO_DESCRITIVO_10](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-10)). A conta informada deve existir, estar ativa (`CTA_FL_ATIVO = TRUE`) e pertencer ao usuário (executa [C3](#c3)); o cartão, idem (`CACR_FL_ATIVO = TRUE`, executa [C4](#c4)). Conta/cartão inválidos → [MSG12](#msg12); conta e cartão juntos (ou nenhum) → [MSG12b](#msg12b). Dinheiro em espécie usa uma conta do tipo `CARTEIRA` ([Observação 2a](#2-observações)). |
| <a id="rn04"></a>RN04 | `DESP_COMPETENCIA` é obrigatória, `CHAR(7)` no formato `yyyy-MM` (validada pelo `CHECK` no banco e pelo `YearMonthConverter` no mapeamento — Documento 0, Seção 7.4). Fora do formato → [MSG11](#msg11). É um campo **próprio e independente**: a tela pré-preenche com o mês da data de lançamento ([RT13](#rt13)), mas o usuário ajusta livremente e ela pode divergir do mês da data de lançamento. Não há validação que force a igualdade. **[Requer código]** |
| <a id="rn05"></a>RN05 | `DESP_VALOR` é obrigatório, `DECIMAL(15,2)`, e deve ser **maior que zero** — ausente, zero ou negativo → [MSG03](#msg03). Numa despesa parcelada, o campo informado pela tela é o **valor total da compra** (`DESP_VALOR_TOTAL_COMPRA`), também maior que zero; `DESP_VALOR` de cada parcela é derivado ([RN12](#rn12)). Não há regra de unicidade. |
| <a id="rn06"></a>RN06 | **Datas.** `DESP_DT_LANCAMENTO` é obrigatória; `DESP_DT_VENCIMENTO` e `DESP_DT_PAGAMENTO` são opcionais. Todas aceitam datas **posteriores a hoje** — a tela serve para planejar despesas previstas. O serviço não rejeita data futura; a única validação de data é o formato. |
| <a id="rn07"></a>RN07 | `DESP_ORIGEM` das despesas criadas por esta tela é **sempre `MANUAL`** — o serviço fixa o valor em [EDP04](#edp04) e ignora qualquer origem no corpo. `OPEN_FINANCE` só chega pela conciliação (documento `15`); `IMPORTACAO`, pela rotina de importação (documento futuro). Não há migração da geração 1 (Documento 0, Observação 18a). |
| <a id="rn08"></a>RN08 | **Despesa importada** (`DESP_ORIGEM` ≠ `MANUAL`): na edição ([EDP05](#edp05)), `CTA_ID`, `CACR_ID` e `DESP_ORIGEM` **não** são alteráveis — valores divergentes no corpo são ignorados. Na exclusão ([EDP06](#edp06)), a operação é **recusada** → [MSG16b](#msg16b), porque a transação de *staging* de Open Finance guarda um vínculo lógico com a despesa gerada (Documento 0, Observações 19 e 21). Os demais campos de negócio (nome, descrição, valor, competência, datas, categoria, status de pagamento, rateio) **são** editáveis. |
| <a id="rn09"></a>RN09 | Exclusão de despesa: é sempre **lógica** (`audit_data_exclusao` / `audit_excluido_por`) e **não checa uso** por FK externa — nada no domínio referencia `DESPESAS` além de `DESPESAS_USUARIO` (rateio, excluído junto — [RN14](#rn14)) e, quando o documento `11` existir, `FATURAS_CARTAO` via `DESP.FTCA_ID` (o efeito da exclusão de uma compra sobre a fatura é tratado lá). A única restrição desta tela é a da [RN08](#rn08). |
| <a id="rn10"></a>RN10 | `DESPESAS` e `DESPESAS_USUARIO` são auditadas via Hibernate Envers (`@Audited`). Cada criação, edição, geração de parcelamento, rateio, acerto de fatia, baixa de pagamento e exclusão lógica gera uma revisão com autor e data. |
| <a id="rn11"></a>RN11 | **Mudanças de estrutura vs geração 1 (requer código):** removidos os enums `DESP_TIPO_TRANSACAO` (`TipoRegistroFinanceiro`) e `DESP_TIPO_RECEITA_DESPESA` (`CategoriaRegistroFinanceiro`); categoria vira `@ManyToOne Categoria` (`CATE_ID`). `DESP_VALOR_PARCELADO` + `DESP_VALOR_TOTAL_A_DIVIDIR` → `DESP_VALOR_TOTAL_COMPRA`. `idParcelaPai` `Long` solto → `@ManyToOne Despesa parcelaPai` (`DESP_ID_PARCELA_PAI`, FK). `DespesaUsuario.statusPagamento` `boolean` → `StatusPagamento` (enum). `dtLancamento` → `LocalDate`; `competencia` `String "0000-00"` → `YearMonth` (via `YearMonthConverter`). Entram `meioPagamento` (`MeioPagamento`), `flPagamentoFatura` (`boolean`), `origem` (`OrigemLancamento`), `cartao` (`@ManyToOne CartaoCredito`, `CACR_ID`), `fatura` (`@ManyToOne FaturaCartao`, `FTCA_ID`), `DespesaUsuario.dataAcerto` (`LocalDate`). `Despesa` passa a herdar de `LancamentoFinanceiro` (Documento 0, Seção 7.2). Os métodos `importarDadosCartaoCredito*` / `setDespesaImportacao` / `validaExistenciaDespesaImportacao` **não** são portados (fora do escopo — D05). **[Requer código]** |
| <a id="rn12"></a>RN12 | **Parcelamento — geração da série.** Quando `parcelada = true`, [EDP04](#edp04) recebe o **valor total da compra** (`T`) e o **número de parcelas** (`N`, inteiro ≥ 2 e ≤ `DESPESA_PARCELAS_MAXIMO`; fora disso → [MSG13](#msg13)). O serviço: (1) calcula a parcela base `P = arredonda_para_baixo(T / N, 2 casas)`; (2) grava a **despesa-mãe** como a **1ª parcela** — `DESP_NRO_PARCELA = 1`, `DESP_QTD_PARCELAS = N`, `DESP_FL_PARCELADA = TRUE`, `DESP_ID_PARCELA_PAI = NULL`, `DESP_VALOR = P`, `DESP_VALOR_TOTAL_COMPRA = T`; (3) cria as parcelas `2..N` copiando a mãe, com `DESP_ID_PARCELA_PAI = {id da mãe}`, `DESP_NRO_PARCELA = i`, `DESP_VALOR_TOTAL_COMPRA = T`, competência e data de vencimento avançando **1 mês** por parcela (a data de lançamento é a mesma da compra em todas); (4) a **última** parcela (`i = N`) recebe `DESP_VALOR = T − P × (N−1)` para fechar exatamente o total (parâmetro `DESPESA_RESIDUO_PARCELA_NA_ULTIMA` — se `false`, o resíduo vai na primeira). Cada parcela nasce com `DESP_IND_STATUS_PAGAMENTO = NAO` (ou `NAO_SE_APLICA` se for compra no cartão — [RN23](#rn23)). Espelha o `gerarParcelamento` da geração 1, corrigindo a divisão do valor. |
| <a id="rn13"></a>RN13 | **Parcelamento — edição.** [EDP05](#edp05) edita **uma parcela isoladamente** — nome, descrição, valor, categoria, datas, status de pagamento e rateio daquela parcela. **Não** reprocessa a série: `DESP_QTD_PARCELAS`, `DESP_VALOR_TOTAL_COMPRA` e `DESP_FL_PARCELADA` são imutáveis pela edição e valores no corpo são ignorados. Para mudar o número de parcelas ou o valor total, o usuário exclui a série ([RN14](#rn14)) e cria de novo. O comportamento alternativo (reprocessar a série ao editar a mãe) é item "A Confirmar" (Seção 17). |
| <a id="rn14"></a>RN14 | **Parcelamento — exclusão.** [EDP06](#edp06) sobre a **despesa-mãe** (`DESP_NRO_PARCELA = 1` e com parcelas-filhas) exclui logicamente **toda a série** — a mãe e todas as parcelas com `DESP_ID_PARCELA_PAI = {id da mãe}` (via [C6](#c6)), mais as `DESPESAS_USUARIO` de cada uma. [EDP06](#edp06) sobre uma **parcela-filha** exclui **só aquela parcela**, deixando a série incompleta (a tela avisa com [MSG06](#msg06)). Excluir a série pela mãe é o caminho recomendado; a possibilidade de "reajustar as parcelas restantes" ao apagar uma do meio é item "A Confirmar" (Seção 17). |
| <a id="rn15"></a>RN15 | **Rateio é recurso de plano pago.** As operações de rateio ([EDP09](#edp09), [EDP10](#edp10) e o bloco `rateio[]` de [EDP04](#edp04)/[EDP05](#edp05)) exigem a **permissão efetiva** `DESPESA_RATEAR_MULTIUSUARIO` (`PERM_FL_CONCEDIVEL_POR_PLANO = TRUE` — Documento 0, Observação 24). Sem ela: a seção de rateio do modal não é renderizada ([RT10](#rt10)), o ícone "Ratear" do grid não aparece, e qualquer `rateio[]` no corpo de [EDP04](#edp04)/[EDP05](#edp05) é **recusado** → [MSG16](#msg16). Na carga inicial a permissão **não** é vinculada a nenhum perfil (Seção 13.1) — chega só pelo plano. |
| <a id="rn16"></a>RN16 | **Rateio — a despesa é do criador.** O rateio não transfere a propriedade: a despesa continua sendo do usuário dono da conta/cartão ([RN02](#rn02)). Os co-participantes são **usuários reais** (`USUARIOS`, [C8](#c8)), adicionados como linhas em `DESPESAS_USUARIO` com uma fatia (`DEPU_VALOR > 0`). O par (`DESP_ID`, `USU_ID`) é único (Documento 0, [QUADRO_DESCRITIVO_11](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-11)); o próprio dono pode ou não figurar como linha explícita ([RN17](#rn17)). |
| <a id="rn17"></a>RN17 | **Rateio — soma das fatias.** A soma dos `DEPU_VALOR` das linhas de co-participantes deve ser **≤ `DESP_VALOR`** da despesa (na parcelada, ≤ `DESP_VALOR` de cada parcela). Se a soma exceder → [MSG17](#msg17). A diferença (`DESP_VALOR` − soma) é a **cota implícita do dono** e não precisa de linha própria. Exigir soma **exatamente** igual ao valor é item "A Confirmar" (Seção 17). |
| <a id="rn18"></a>RN18 | **Rateio — replicação nas parcelas.** Quando a despesa é parcelada, o rateio informado é replicado em **cada** parcela da série: para cada co-participante, cria-se uma `DESPESAS_USUARIO` por parcela com `DEPU_VALOR` proporcional ao valor daquela parcela (mesma proporção da fatia sobre o total). O acerto ([RN20](#rn20)) é por (parcela, usuário). Espelha a geração 1. |
| <a id="rn19"></a>RN19 | **Rateio — privacidade do co-participante (v1.0).** Um usuário adicionado ao rateio de uma despesa que não é dele **não a vê** nesta tela — [EDP02](#edp02) e [C1](#c1) filtram estritamente pela conta/cartão do dono, sem incluir despesas em que o usuário só participa do rateio. A visão do co-participante (tela "compartilhado comigo", com a fatia dele e a opção de marcar o próprio acerto) fica para uma versão futura ([Seção 17](#17-workshop-de-análise)). |
| <a id="rn20"></a>RN20 | **Rateio — acerto da fatia.** O **dono** registra, por [EDP09](#edp09) ou pelo modal ([RT10](#rt10)), que um co-participante acertou a parte dele: grava `DEPU_IND_STATUS_PAGAMENTO = SIM` e `DEPU_DT_ACERTO` na linha correspondente. Desfazer volta para `NAO` e limpa a data. Não altera o `DESP_IND_STATUS_PAGAMENTO` da despesa (que é sobre o pagamento ao credor, não sobre o acerto entre as pessoas). |
| <a id="rn21"></a>RN21 | **Baixa de pagamento.** [EDP07](#edp07) (individual) e [EDP08](#edp08) (lote) marcam `DESP_IND_STATUS_PAGAMENTO = SIM` e gravam `DESP_DT_PAGAMENTO` (data informada; no lote, a mesma para todas). Só se aplica a despesas com status `NAO` — despesa já `SIM` é ignorada (idempotente); despesa `NAO_SE_APLICA` é recusada → [MSG20](#msg20). Numa série parcelada, a baixa é **por parcela** — não há "pagar a série toda" na v1.0. Espelha `pagarDespesa` / `pagarDespesas` da geração 1. |
| <a id="rn22"></a>RN22 | **Meio de pagamento — derivação.** O serviço deriva `DESP_MEIO_PAGAMENTO` da forma de pagamento: "Cartão de crédito" → `CREDITO`; "Dinheiro" (conta `CARTEIRA`) → `DINHEIRO`; "Conta à vista" → o valor informado no campo Meio de pagamento ([ID15](#qdd3-15)), *default* `DEBITO`, entre `DEBITO` / `PIX` / `BOLETO` / `TRANSFERENCIA`. Nas duas primeiras, qualquer valor divergente no corpo é ignorado. Se `DESP_MEIO_PAGAMENTO` deve permanecer derivado ou virar campo totalmente livre é item "A Confirmar" (Seção 17). |
| <a id="rn23"></a>RN23 | **Compra no cartão — fronteira com a fatura.** Quando `CACR_ID` está preenchido, o serviço fixa `DESP_MEIO_PAGAMENTO = CREDITO` e `DESP_IND_STATUS_PAGAMENTO = NAO_SE_APLICA` (parâmetro `DESPESA_COMPRA_CARTAO_STATUS_NAO_SE_APLICA`), e a ação "Registrar pagamento" não se aplica ([RT11](#rt11)). O **vínculo com a fatura** (`DESP.FTCA_ID`), o fechamento e o cálculo do total da fatura são do documento `11 - manter-fatura-cartao` (Documento 0, Observação 15) — nesta tela `FTCA_ID` fica nulo. O pagamento da fatura em si é uma `TransacaoBancaria` com `TRBA_FL_PAGAMENTO_FATURA` (documento `10`, Documento 0, Observação 16). |
| <a id="rn24"></a>RN24 | **`DESP_FL_PAGAMENTO_FATURA` — caso de borda.** É o caminho alternativo de registrar o pagamento de uma fatura **como despesa** em vez de transação (Documento 0, Observação 16). A v1.0 **não** oferece esse lançamento pelo fluxo da tela; toda despesa criada aqui nasce com `DESP_FL_PAGAMENTO_FATURA = FALSE`. O tratamento dessas despesas no total por categoria é do documento `13 - dashboard`. |

---

## 10. Mensagens de Sistema

| CÓDIGO | DESCRIÇÃO |
|---|---|
| <a id="msg01"></a>MSG01 | Despesa cadastrada com sucesso. |
| <a id="msg02"></a>MSG02 | O campo {campo} é obrigatório. |
| <a id="msg03"></a>MSG03 | Informe um valor maior que zero. |
| <a id="msg04"></a>MSG04 | Despesa atualizada com sucesso. |
| <a id="msg05"></a>MSG05 | Despesa não encontrada. |
| <a id="msg06"></a>MSG06 | Confirma a exclusão da despesa "{nome}"? |
| <a id="msg07"></a>MSG07 | Despesa excluída com sucesso. |
| <a id="msg08"></a>MSG08 | Nenhuma despesa encontrada com os filtros informados. |
| <a id="msg09"></a>MSG09 | Informe a data de pagamento para marcar a despesa como paga. |
| <a id="msg10"></a>MSG10 | Pagamento registrado com sucesso. |
| <a id="msg10b"></a>MSG10b | Pagamento de {n} despesas registrado com sucesso. |
| <a id="msg11"></a>MSG11 | A competência deve estar no formato AAAA-MM. |
| <a id="msg12"></a>MSG12 | A {conta/cartão} selecionado não está disponível. Escolha uma opção ativa. |
| <a id="msg12b"></a>MSG12b | Escolha uma forma de pagamento: a despesa deve ter uma conta ou um cartão, não os dois. |
| <a id="msg13"></a>MSG13 | O número de parcelas deve ser um inteiro entre 2 e {máximo}. |
| <a id="msg14"></a>MSG14 | Compra parcelada cadastrada: {n} parcelas geradas. |
| <a id="msg15"></a>MSG15 | Esta é a primeira parcela de uma compra em {qtd}x. Excluir vai apagar todas as {qtd} parcelas. Confirma? |
| <a id="msg16"></a>MSG16 | Dividir despesas entre usuários é um recurso do seu plano. Faça o upgrade para habilitar. |
| <a id="msg16b"></a>MSG16b | Esta despesa foi importada do Open Finance e não pode ser excluída por esta tela. |
| <a id="msg17"></a>MSG17 | A soma das fatias ({soma}) não pode ser maior que o valor da despesa ({valor}). |
| <a id="msg18"></a>MSG18 | Você ainda não tem uma conta do tipo Carteira. Crie uma em "Minhas Contas" para lançar despesas em dinheiro. |
| <a id="msg19"></a>MSG19 | Um dos usuários do rateio não foi encontrado ou está inativo. |
| <a id="msg20"></a>MSG20 | Esta despesa é uma compra no cartão — o pagamento é controlado pela fatura, não aqui. |
| <a id="msg21"></a>MSG21 | Acerto da fatia registrado com sucesso. |

---

## 11. Consultas

| CÓDIGO | DESCRIÇÃO |
|---|---|
| <a id="c1"></a>C1 | Listagem das despesas do usuário autenticado para o grid ([EDP02](#edp02)), com conta ou cartão, categoria e o resumo do rateio.<br>`SELECT d.DESP_ID, d.DESP_COMPETENCIA, d.DESP_NOME, d.DESP_DESCRICAO, d.DESP_VALOR,`<br>`       d.DESP_VALOR_TOTAL_COMPRA, d.DESP_FL_PARCELADA, d.DESP_NRO_PARCELA, d.DESP_QTD_PARCELAS,`<br>`       d.DESP_ID_PARCELA_PAI, d.DESP_MEIO_PAGAMENTO, d.DESP_IND_STATUS_PAGAMENTO,`<br>`       d.DESP_DT_LANCAMENTO, d.DESP_DT_VENCIMENTO, d.DESP_DT_PAGAMENTO, d.DESP_ORIGEM,`<br>`       c.CTA_ID, c.CTA_DESCRICAO, cc.CACR_ID, cc.CACR_DESCRICAO,`<br>`       cat.CATE_ID, cat.CATE_NOME,`<br>`       (SELECT COUNT(*) FROM DESPESAS_USUARIO du WHERE du.DESP_ID = d.DESP_ID AND du.audit_data_exclusao IS NULL) AS qtd_co_participantes,`<br>`       (d.audit_data_exclusao IS NOT NULL) AS excluido`<br>`FROM DESPESAS d`<br>`LEFT JOIN CONTAS c            ON c.CTA_ID  = d.CTA_ID`<br>`LEFT JOIN CARTOES_CREDITO cc  ON cc.CACR_ID = d.CACR_ID`<br>`LEFT JOIN CATEGORIAS cat      ON cat.CATE_ID = d.CATE_ID`<br>`WHERE c.USU_ID = :usuId OR cc.USU_ID = :usuId`<br>`ORDER BY d.DESP_COMPETENCIA DESC, d.DESP_DT_VENCIMENTO ASC;` |
| <a id="c2"></a>C2 | Verifica se a despesa pertence ao usuário autenticado ([RN02](#rn02)) — usada antes de editar, pagar, ratear, acertar ou excluir.<br>`SELECT COUNT(*) FROM DESPESAS d`<br>`LEFT JOIN CONTAS c           ON c.CTA_ID  = d.CTA_ID`<br>`LEFT JOIN CARTOES_CREDITO cc ON cc.CACR_ID = d.CACR_ID`<br>`WHERE d.DESP_ID = :despId`<br>`  AND (c.USU_ID = :usuId OR cc.USU_ID = :usuId)`<br>`  AND d.audit_data_exclusao IS NULL;` |
| <a id="c3"></a>C3 | Verifica se a conta informada existe, está ativa e é do usuário autenticado ([RN03](#rn03)).<br>`SELECT COUNT(*) FROM CONTAS c`<br>`WHERE c.CTA_ID = :ctaId AND c.USU_ID = :usuId`<br>`  AND c.CTA_FL_ATIVO = TRUE AND c.audit_data_exclusao IS NULL;` |
| <a id="c4"></a>C4 | Verifica se o cartão informado existe, está ativo e é do usuário autenticado ([RN03](#rn03)).<br>`SELECT COUNT(*) FROM CARTOES_CREDITO cc`<br>`WHERE cc.CACR_ID = :cacrId AND cc.USU_ID = :usuId`<br>`  AND cc.CACR_FL_ATIVO = TRUE AND cc.audit_data_exclusao IS NULL;` |
| <a id="c5"></a>C5 | Verifica se a categoria informada está ativa e se aplica a despesa.<br>`SELECT COUNT(*) FROM CATEGORIAS cat`<br>`WHERE cat.CATE_ID = :cateId AND cat.CATE_FL_ATIVO = TRUE`<br>`  AND cat.CATE_APLICA_A IN ('DESPESA', 'AMBOS')`<br>`  AND cat.audit_data_exclusao IS NULL;` |
| <a id="c6"></a>C6 | Busca todas as parcelas de uma série a partir da despesa-mãe ([RN14](#rn14)).<br>`SELECT d.DESP_ID FROM DESPESAS d`<br>`WHERE (d.DESP_ID = :maeId OR d.DESP_ID_PARCELA_PAI = :maeId)`<br>`  AND d.audit_data_exclusao IS NULL;` |
| <a id="c7"></a>C7 | Busca o rateio de uma despesa ([EDP03](#edp03)).<br>`SELECT du.USU_ID, u.USU_NOME, du.DEPU_VALOR, du.DEPU_IND_STATUS_PAGAMENTO, du.DEPU_DT_ACERTO`<br>`FROM DESPESAS_USUARIO du`<br>`JOIN USUARIOS u ON u.USU_ID = du.USU_ID`<br>`WHERE du.DESP_ID = :despId AND du.audit_data_exclusao IS NULL`<br>`ORDER BY u.USU_NOME;` |
| <a id="c8"></a>C8 | Verifica se os usuários informados no rateio existem e estão ativos ([RN16](#rn16)).<br>`SELECT COUNT(*) FROM USUARIOS u`<br>`WHERE u.USU_ID IN (:usuIds) AND u.USU_FL_ATIVO = TRUE`<br>`  AND u.audit_data_exclusao IS NULL;` |
| <a id="c9"></a>C9 | Verifica se todas as despesas de um lote de baixa são do usuário autenticado ([EDP08](#edp08)).<br>`SELECT COUNT(*) FROM DESPESAS d`<br>`LEFT JOIN CONTAS c           ON c.CTA_ID  = d.CTA_ID`<br>`LEFT JOIN CARTOES_CREDITO cc ON cc.CACR_ID = d.CACR_ID`<br>`WHERE d.DESP_ID IN (:despIds)`<br>`  AND (c.USU_ID = :usuId OR cc.USU_ID = :usuId)`<br>`  AND d.audit_data_exclusao IS NULL;` |
| <a id="c10"></a>C10 | Busca usuários ativos para o rateio, por parte do nome ou do e-mail ([EDP10](#edp10)).<br>`SELECT u.USU_ID, u.USU_NOME, u.USU_EMAIL FROM USUARIOS u`<br>`WHERE u.USU_FL_ATIVO = TRUE AND u.audit_data_exclusao IS NULL`<br>`  AND u.USU_ID <> :usuIdLogado`<br>`  AND (u.USU_NOME LIKE :termo OR u.USU_EMAIL LIKE :termo)`<br>`ORDER BY u.USU_NOME LIMIT 20;` |

> Os nomes de coluna de `USUARIOS` (`USU_NOME`, `USU_EMAIL`, `USU_FL_ATIVO`) seguem o [QUADRO_DESCRITIVO_2 do Documento 0](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-2); ajustar na implementação se divergirem.

---

## 12. Parâmetros de Sistema

| PARÂMETRO | VALOR PADRÃO | DESCRIÇÃO |
|---|---|---|
| DESPESA_COMPETENCIA_SEGUE_DATA_LANCAMENTO | true | Se `true`, a Competência do modal ([ID8](#qdd3-8)) é pré-preenchida e reajustada pelo mês da Data de lançamento enquanto o usuário não a editar à mão ([RT13](#rt13)). Se `false`, começa no mês corrente e nunca é reajustada. |
| DESPESA_PARCELAS_MAXIMO | 72 | Número máximo de parcelas aceito por [EDP04](#edp04) ([RN12](#rn12) / [MSG13](#msg13)). |
| DESPESA_RESIDUO_PARCELA_NA_ULTIMA | true | Se `true`, o resíduo do arredondamento da divisão do valor total vai na **última** parcela; se `false`, na primeira ([RN12](#rn12)). |
| DESPESA_COMPRA_CARTAO_STATUS_NAO_SE_APLICA | true | Se `true`, a compra no cartão (`CACR_ID` preenchido) nasce com `DESP_IND_STATUS_PAGAMENTO = NAO_SE_APLICA` ([RN23](#rn23)). Se `false`, nasce `NAO`. |
| DESPESA_RATEIO_SOMA_DEVE_FECHAR | false | Se `true`, a soma das fatias do rateio deve ser **exatamente** igual ao valor da despesa; se `false`, pode ser menor (o resto é a cota do dono — [RN17](#rn17)). |

---

## 13. Permissões

Três permissões relacionadas ao módulo **Despesas** (`PERM_MODULO = 'Despesas'`). Fazem parte do catálogo do código e da carga inicial (Documento 0, [QUADRO_DESCRITIVO_26](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-26)). Convenção domínio-primeiro; cada uma vira a autoridade `PERM_{CÓDIGO}`.

| CÓDIGO | DESCRIÇÃO | PERFIS COM ACESSO |
|---|---|---|
| <a id="perm01"></a>PERM01 | `DESPESAS_LISTAR` — abrir a tela Despesas, listar e filtrar as próprias despesas. Controla a visibilidade do menu 'Despesas'. | [PERF01](#perf01), [PERF02](#perf02) |
| <a id="perm02"></a>PERM02 | `DESPESAS_MANTER` — cadastrar, editar, parcelar, registrar o pagamento (individual e em lote) e excluir as próprias despesas. | [PERF01](#perf01), [PERF02](#perf02) |
| <a id="perm03"></a>PERM03 | `DESPESA_RATEAR_MULTIUSUARIO` — dividir uma despesa com outros usuários (adicionar co-participantes, definir fatias, registrar acertos, buscar usuários para o rateio). `PERM_FL_CONCEDIVEL_POR_PLANO = TRUE` — **não** é vinculada a perfil na carga inicial; chega pela permissão efetiva do **plano pago** (Documento 0, Observação 24). | Nenhum perfil por padrão — via plano |

> `DESPESAS_MANTER` pressupõe `DESPESAS_LISTAR`. `DESPESA_RATEAR_MULTIUSUARIO` pressupõe `DESPESAS_MANTER` (não há rateio sem manter a despesa). As permissões `DESPESAS_LISTAR` / `DESPESAS_MANTER` vêm de carga inicial e são concedidas a **ADMIN e USER** — a tela é do próprio usuário. O que cada usuário enxerga e altera é limitado às suas despesas pela [RN02](#rn02), não pela permissão.

### 13.1 Matriz Perfil × Permissão

| PERMISSÃO | ADMIN | USER | Concedível por plano |
|---|:-:|:-:|:-:|
| `DESPESAS_LISTAR` | ✓ | ✓ | · |
| `DESPESAS_MANTER` | ✓ | ✓ | · |
| `DESPESA_RATEAR_MULTIUSUARIO` | · | · | ✓ |

`ADMIN` e `USER` recebem `DESPESAS_LISTAR` e `DESPESAS_MANTER` na carga inicial: cada usuário gerencia as suas próprias despesas. O `ADMIN` **não** tem visão administrativa das despesas de outros usuários — dado financeiro é privado do dono, e o escopo por `USU_ID` da [RN02](#rn02) vale igual para os dois perfis. `DESPESA_RATEAR_MULTIUSUARIO` **não** é concedida por perfil: a permissão efetiva do usuário só a inclui quando o plano pago dele a concede (permissão efetiva = perfil ∪ plano). Enquanto o módulo de planos (`NN - planos-e-assinaturas`) não existe, nenhum usuário tem a permissão — a seção de rateio fica indisponível para todos, e os testes usam um perfil de homologação com a permissão vinculada manualmente.

---

## 14. Perfis

| CÓDIGO | NOME | DESCRIÇÃO |
|---|---|---|
| <a id="perf01"></a>PERF01 | ADMIN | Administrador do sistema. `PERF_FL_SISTEMA = TRUE`. Recebe todas as permissões na carga inicial, inclusive `DESPESAS_LISTAR` e `DESPESAS_MANTER` (Seção 13.1); **não** recebe `DESPESA_RATEAR_MULTIUSUARIO` por perfil. Opera apenas sobre as próprias despesas ([RN02](#rn02)). Corresponde a `ROLE_ADMIN`. |
| <a id="perf02"></a>PERF02 | USER | Usuário comum. `PERF_FL_SISTEMA = TRUE`. Recebe `DESPESAS_LISTAR` e `DESPESAS_MANTER` na carga inicial e gerencia as próprias despesas. `DESPESA_RATEAR_MULTIUSUARIO` só pela permissão efetiva do plano. Corresponde a `ROLE_USER`. |

---

## 15. Fluxo de Eventos

**Cadastrar uma compra parcelada em 6x:**

```
1. Usuário clica em "Nova despesa" → modal QUADRO_DESCRITIVO_3 em modo criação.
        Data de lançamento ← hoje    Competência ← mês corrente
        Forma de pagamento ← "Cartão de crédito".
2. Usuário informa nome "Notebook", seleciona o cartão, liga "Parcelada",
   informa Número de parcelas = 6 e Valor total da compra = 3.000,00.
        A tela mostra "Valor da parcela: 500,00".
3. Usuário clica em "Salvar" → EDP04.
        │
        ├─ Cartão inativo / de outro usuário (RN03 / C4)     → MSG12.
        ├─ Número de parcelas < 2 ou > 72 (RN12)             → MSG13.
        ├─ Competência fora do formato (RN04)                → MSG11.
        └─ OK →
             • grava a despesa-mãe: parcela 1/6, competência atual,
               DESP_VALOR = 500,00, DESP_VALOR_TOTAL_COMPRA = 3.000,00,
               DESP_ID_PARCELA_PAI = NULL, meio CREDITO, status NAO_SE_APLICA;
             • gera as parcelas 2..6, DESP_ID_PARCELA_PAI = id da mãe,
               competência e vencimento +1 mês por parcela;
             • parcela 6 recebe DESP_VALOR = 3.000,00 − 500,00 × 5 = 500,00
               (aqui divide exato; com 3.001,00 a última seria 501,00);
             • fixa DESP_ORIGEM = MANUAL, audita (Envers), retorna MSG14
               ("6 parcelas geradas"), recarrega o grid com as 6 linhas.
```

**Ratear uma despesa 50/50 e registrar o acerto do outro:**

```
1. Usuário com DESPESA_RATEAR_MULTIUSUARIO edita a despesa "Jantar" (R$ 200,00)
   → modal, seção Rateio visível (RT10).
2. Usuário adiciona o co-participante "Maria" (SB06 → EDP10), fatia 100,00.
        A tela mostra "Minha cota: 100,00".
3. Usuário clica em "Salvar" → EDP05.
        │
        ├─ Sem a permissão efetiva de rateio (RN15)          → MSG16.
        ├─ Maria inativa / inexistente (C8)                  → MSG19.
        ├─ Soma das fatias 100,00 > valor 200,00? não        → segue.
        │  (se fosse 250,00 > 200,00 → MSG17)
        └─ OK → cria DESPESAS_USUARIO (DESP_ID, USU_ID de Maria),
                 DEPU_VALOR = 100,00, DEPU_IND_STATUS_PAGAMENTO = NAO,
                 audita, retorna MSG04, recarrega o grid (ícone de rateio na linha).
4. Maria transfere os 100,00. Usuário abre a despesa (ícone "Ratear"),
   marca a fatia de Maria como acertada, data de hoje → EDP09.
        └─ OK → grava DEPU_IND_STATUS_PAGAMENTO = SIM e DEPU_DT_ACERTO,
                 audita, retorna MSG21.
   (Maria não vê essa despesa em lugar nenhum na v1.0 — RN19.)
```

**Registrar pagamento de várias despesas em lote:**

```
1. Usuário marca 4 despesas "Em aberto" no grid (ID8) → habilita o botão da barra (ID6).
2. Usuário clica em "Registrar pagamento" → modal QUADRO_DESCRITIVO_5,
   resumo "4 despesas — total 780,00", data ← hoje.
3. Usuário confirma → EDP08.
        │
        ├─ Alguma despesa do lote é de outro usuário (C9)    → MSG05 (404), nada é baixado.
        ├─ Alguma é compra no cartão (status NAO_SE_APLICA)  → ignorada (RN21), as demais seguem.
        └─ OK → grava DESP_IND_STATUS_PAGAMENTO = SIM e DESP_DT_PAGAMENTO
                 nas despesas elegíveis, audita, retorna MSG10b, recarrega o grid.
```

---

## 16. Critérios de Aceitação / BDD

### 16.0 Listar minhas despesas

Dado que estou autenticado com um usuário que tem a permissão [PERM01](#perm01).
E que tenho oito despesas vinculadas às minhas contas e aos meus cartões.
Quando eu acessar o menu "Finanças > Despesas".
Então o sistema deve exibir o grid com as minhas oito despesas, ordenadas por competência decrescente, mostrando a categoria, a conta ou o cartão, o valor, as datas, o status de pagamento e a origem.

### 16.1 Bloquear acesso de usuário sem a permissão

Dado que estou autenticado com um usuário de um perfil que não tem [PERM01](#perm01).
Quando eu tentar acessar "/despesas/listar" ou chamar "/despesas/listar-dados".
Então o sistema deve negar o acesso (HTTP 403).

### 16.2 A listagem traz só as despesas do próprio usuário

Dado que o usuário A tem cinco despesas e o usuário B tem três despesas.
Quando o usuário A abrir a tela Despesas.
Então o grid deve mostrar apenas as cinco despesas do usuário A.

### 16.3 Co-participante do rateio não vê a despesa

Dado que o usuário A criou a despesa "Jantar" e adicionou o usuário B ao rateio.
Quando o usuário B abrir a tela Despesas.
Então o grid do usuário B não deve mostrar a despesa "Jantar" ([RN19](#rn19)).

### 16.4 Acesso cruzado a buscar/editar/pagar/excluir é negado

Dado que estou autenticado como usuário A.
E que a despesa com id 90 é do usuário B.
Quando eu chamar "/despesas/buscar/90", "/despesas/editar/90", "/despesas/registrar-pagamento/90" ou "/despesas/excluir/90".
Então o sistema deve responder 404 com [MSG05](#msg05) e não alterar a despesa do usuário B.

### 16.5 Cadastrar despesa à vista em conta

Dado que estou na tela Despesas com [PERM01](#perm01) e [PERM02](#perm02) e clico em "Nova despesa".
Quando eu informar o nome "Mercado", o valor "250,00", a data de lançamento "05/09/2026", a forma de pagamento "Conta à vista", a conta "Nubank Conta Corrente", o meio "Débito", deixar "Já paguei" em "Sim" com data "05/09/2026" e clicar em "Salvar".
Então o sistema deve criar a despesa com `CTA_ID` da conta, `CACR_ID` nulo, `DESP_MEIO_PAGAMENTO = DEBITO`, `DESP_IND_STATUS_PAGAMENTO = SIM`, `DESP_ORIGEM = MANUAL`, competência "2026-09", exibir [MSG01](#msg01) e recarregar o grid.

### 16.6 Cadastrar despesa em dinheiro usa conta CARTEIRA

Dado que eu tenho uma conta do tipo `CARTEIRA` chamada "Carteira".
Quando eu cadastrar uma despesa com a forma de pagamento "Dinheiro", escolher a conta "Carteira" e salvar.
Então o sistema deve gravar `CTA_ID` da conta Carteira, `DESP_MEIO_PAGAMENTO = DINHEIRO` e `CACR_ID` nulo.

### 16.7 Cadastrar despesa no cartão fica sem conta e sem status de pagamento

Dado que estou cadastrando uma despesa.
Quando eu escolher a forma de pagamento "Cartão de crédito", selecionar o cartão "Nubank Ultravioleta" e salvar.
Então o sistema deve gravar `CACR_ID` do cartão, `CTA_ID` nulo, `DESP_MEIO_PAGAMENTO = CREDITO` e `DESP_IND_STATUS_PAGAMENTO = NAO_SE_APLICA`, e a ação "Registrar pagamento" não deve aparecer para essa linha.

### 16.8 Conta e cartão juntos são recusados

Dado que estou chamando "/despesas/inserir" diretamente.
Quando eu enviar `contaId` e `cartaoId` preenchidos ao mesmo tempo (ou os dois nulos).
Então o sistema deve recusar com [MSG12b](#msg12b).

### 16.9 Valor deve ser maior que zero

Dado que estou cadastrando uma despesa.
Quando eu informar o valor "0,00" e clicar em "Salvar".
Então o sistema deve impedir e exibir [MSG03](#msg03).

### 16.10 Competência pré-preenchida pela data de lançamento

Dado que o parâmetro `DESPESA_COMPETENCIA_SEGUE_DATA_LANCAMENTO` está em "true" e abri o modal de nova despesa.
E que ainda não editei o campo Competência.
Quando eu informar a data de lançamento "10/12/2026".
Então o campo Competência deve passar a "12/2026".

### 16.11 Competência diferente do mês da data de lançamento

Dado que cadastrei uma despesa com data de lançamento "28/01/2026".
Quando eu ajustar a Competência para "02/2026" e salvar.
Então o sistema deve gravar `DESP_COMPETENCIA = '2026-02'` e `DESP_DT_LANCAMENTO = 2026-01-28`, sem forçar a igualdade.

### 16.12 Lançar despesa prevista com data futura

Dado que hoje é 08/09/2026 e estou cadastrando uma despesa.
Quando eu informar a data de vencimento "10/10/2026", deixar "Já paguei" em "Não" e salvar.
Então o sistema deve aceitar o cadastro com a data futura e status de pagamento "Em aberto".

### 16.13 Criar compra parcelada em 6x

Dado que estou cadastrando a despesa "Notebook" no cartão.
Quando eu ligar "Parcelada", informar 6 parcelas, valor total "3.000,00" e salvar.
Então o sistema deve criar 6 despesas: a 1ª como despesa-mãe (`DESP_NRO_PARCELA = 1`, `DESP_ID_PARCELA_PAI` nula) e as demais apontando para ela, todas com `DESP_VALOR = 500,00` e `DESP_VALOR_TOTAL_COMPRA = 3.000,00`, competências de 6 meses consecutivos a partir da competência informada, e exibir [MSG14](#msg14).

### 16.14 Resíduo do arredondamento vai na última parcela

Dado que estou criando uma compra parcelada de valor total "100,00" em 3 parcelas, com `DESPESA_RESIDUO_PARCELA_NA_ULTIMA` em "true".
Quando o sistema gerar a série.
Então as parcelas 1 e 2 devem ter `DESP_VALOR = 33,33` e a parcela 3 deve ter `DESP_VALOR = 33,34`, somando exatamente 100,00.

### 16.15 Editar uma parcela afeta só ela

Dado que tenho uma compra em 6x e a parcela 3/6 tem valor "500,00".
Quando eu editar a parcela 3/6, mudar o valor para "480,00" e salvar.
Então o sistema deve alterar apenas a parcela 3/6, mantendo as outras 5 parcelas e o `DESP_VALOR_TOTAL_COMPRA` inalterados.

### 16.16 Excluir a série pela despesa-mãe

Dado que tenho uma compra em 6x.
Quando eu clicar em excluir na parcela 1/6 (a mãe) e confirmar [MSG15](#msg15).
Então o sistema deve fazer a exclusão lógica das 6 parcelas e das `DESPESAS_USUARIO` de cada uma, e exibir [MSG07](#msg07).

### 16.17 Excluir uma parcela do meio remove só ela

Dado que tenho uma compra em 6x.
Quando eu excluir a parcela 4/6 e confirmar [MSG06](#msg06).
Então o sistema deve excluir logicamente apenas a parcela 4/6, deixando a série com 5 parcelas.

### 16.18 Registrar pagamento individual

Dado que a minha despesa "Boleto de luz" está "Em aberto".
Quando eu clicar no ícone "Registrar pagamento", confirmar a data "12/09/2026" e clicar em "Registrar".
Então o sistema deve gravar `DESP_IND_STATUS_PAGAMENTO = SIM` e `DESP_DT_PAGAMENTO = 2026-09-12`, exibir [MSG10](#msg10) e recarregar o grid com o status "Pago".

### 16.19 Registrar pagamento em lote

Dado que tenho quatro despesas "Em aberto" selecionadas no grid.
Quando eu clicar em "Registrar pagamento" na barra, confirmar a data "12/09/2026" e confirmar.
Então o sistema deve marcar as quatro como pagas com a mesma data e exibir [MSG10b](#msg10b).

### 16.20 Não é possível pagar uma compra no cartão pela tela

Dado que a minha despesa "Compra no cartão" tem `DESP_IND_STATUS_PAGAMENTO = NAO_SE_APLICA`.
Quando eu chamar "/despesas/registrar-pagamento/{id}" diretamente.
Então o sistema deve recusar com [MSG20](#msg20).

### 16.21 Ratear uma despesa 50/50

Dado que estou editando a despesa "Jantar" (R$ 200,00) e tenho a permissão [PERM03](#perm03).
Quando eu adicionar o usuário "Maria" com a fatia "100,00" e salvar.
Então o sistema deve criar uma `DESPESAS_USUARIO` com `DEPU_VALOR = 100,00` e `DEPU_IND_STATUS_PAGAMENTO = NAO`, e o grid deve mostrar o ícone de rateio na linha.

### 16.22 A soma das fatias não pode exceder o valor

Dado que estou rateando a despesa "Jantar" (R$ 200,00).
Quando eu informar uma fatia de "250,00" para um co-participante e salvar.
Então o sistema deve impedir e exibir [MSG17](#msg17).

### 16.23 Marcar a fatia do co-participante como acertada

Dado que a despesa "Jantar" tem a fatia de "Maria" com `DEPU_IND_STATUS_PAGAMENTO = NAO`.
Quando eu, como dono, marcar a fatia de Maria como acertada com data "10/09/2026" (via [EDP09](#edp09)).
Então o sistema deve gravar `DEPU_IND_STATUS_PAGAMENTO = SIM` e `DEPU_DT_ACERTO = 2026-09-10`, e exibir [MSG21](#msg21).

### 16.24 Rateio replicado nas parcelas

Dado que estou criando uma compra parcelada em 3x de valor total "300,00" e adiciono "Maria" com metade da despesa.
Quando o sistema gerar a série.
Então cada uma das 3 parcelas (valor 100,00) deve ter uma `DESPESAS_USUARIO` de Maria com `DEPU_VALOR = 50,00`.

### 16.25 Rateio indisponível sem a permissão de plano

Dado que estou autenticado com um usuário que **não** tem a permissão efetiva [PERM03](#perm03).
Quando eu abrir o modal de despesa.
Então a seção de rateio não deve aparecer, o ícone "Ratear" do grid não deve aparecer, e se eu enviar `rateio[]` no corpo de "/despesas/inserir", o sistema deve recusar com [MSG16](#msg16).

### 16.26 Despesa do Open Finance: edição parcial, sem exclusão

Dado que a minha despesa "Compra PIX" tem `DESP_ORIGEM = OPEN_FINANCE`.
Quando eu abri-la em edição.
Então os campos Forma de pagamento e Origem devem estar desabilitados e o ícone de excluir não deve aparecer.
E se eu alterar a categoria e o nome e salvar, o sistema deve gravar os novos valores mantendo a forma de pagamento e a origem.
E se a exclusão for chamada diretamente, o sistema deve responder [MSG16b](#msg16b).

### 16.27 ADMIN também só vê as próprias despesas

Dado que estou autenticado como ADMIN.
E que existem despesas de outros usuários.
Quando eu abrir a tela Despesas.
Então o grid deve mostrar apenas as minhas despesas.

### 16.28 Auditoria da despesa

Dado que eu registro o pagamento de uma despesa e salvo.
Quando eu consultar a auditoria de `DESPESAS`.
Então deve haver o registro de quem alterou e quando, com o valor anterior e o novo de `DESP_IND_STATUS_PAGAMENTO` e `DESP_DT_PAGAMENTO`.

### 16.29 Filtrar por competência e status de pagamento

Dado que tenho despesas nas competências de 07/2026 a 10/2026, em aberto e pagas.
Quando eu filtrar por competência inicial "08/2026", competência final "09/2026" e status "Em aberto".
Então o grid deve mostrar apenas as despesas em aberto com competência entre 08/2026 e 09/2026.

---

## 17. Workshop de Análise

Data: —
Convidados: Diego Cordeiro
Participantes: Diego Cordeiro
Descrição: Levantamento a partir do Documento 0 ([QUADRO_DESCRITIVO_10](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-10) e [QUADRO_DESCRITIVO_11](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-11); Observações 8, 9, 12, 15, 16, 18, 18a, 19, 21 e 24), do CRUD REST de `Despesa` da geração 1 (`dsc-backend` — `domain/Despesa.java`, `domain/DespesaUsuario.java`, `controller/DespesaController.java`, `services/DespesaService.java`, `enums/StatusPagamento.java`; métodos `gerarParcelamento`, `inserir`/`editar` com `usuariosResponsaveis`, `pagarDespesa`/`pagarDespesas`, `compartilharDespesas`) e do padrão de tela dos documentos `08 - manter-receita` (par direto — escopo *row-level*, competência independente, datas futuras, origem, edição parcial de Open Finance, dinheiro = `CARTEIRA`), `06 - manter-conta` e `07 - manter-cartao-credito` (comboboxes e endpoints de opções).

**Decisões tomadas:**
- A tela **Despesas** vive em "Finanças > Despesas" e é acessível a `ADMIN` e `USER`; cada um opera só sobre as próprias despesas ([RN02](#rn02)).
- `DESPESAS` **não tem `USU_ID`**; o dono é o `USU_ID` da conta (`DESP.CTA_ID → CONTAS.USU_ID`) **ou** do cartão (`DESP.CACR_ID → CARTOES_CREDITO.USU_ID`). O escopo *row-level* é resolvido no serviço, a partir do contexto de segurança — nunca de um parâmetro. Acesso cruzado por `{id}` responde 404 ([MSG05](#msg05)). **O Documento 0 não é alterado** e `DESPESAS` não ganha `USU_ID`.
- **Toda despesa tem exatamente uma âncora de pagamento** — conta **ou** cartão, do próprio usuário, ativos ([RN03](#rn03)). Conta e cartão continuam anuláveis no schema; a regra conta-XOR-cartão fica na camada de serviço.
- **Dinheiro em espécie = conta do tipo `CARTEIRA`**, com `DESP_MEIO_PAGAMENTO = DINHEIRO` (decisão herdada do documento `08`).
- Permissões `DESPESAS_LISTAR` / `DESPESAS_MANTER` (domínio-primeiro) concedidas a `ADMIN` e `USER` na carga inicial; `DESPESA_RATEAR_MULTIUSUARIO` com `PERM_FL_CONCEDIVEL_POR_PLANO = TRUE`, **não** vinculada a perfil — só pela permissão efetiva do plano (Documento 0, Observação 24).
- **O `ADMIN` não tem visão administrativa de dado financeiro de terceiros.** Opera como um `USER` sobre as próprias despesas.
- **Competência — campo próprio e independente:** `YearMonth`, apenas pré-preenchida com o mês da data de lançamento ([RT13](#rt13) / parâmetro `DESPESA_COMPETENCIA_SEGUE_DATA_LANCAMENTO`). Pode divergir.
- **Datas podem ser futuras** ([RN06](#rn06)).
- **Origem:** despesas criadas por esta tela são sempre `MANUAL` ([RN07](#rn07)). `OPEN_FINANCE` / `IMPORTACAO` só chegam por outros documentos. Sem migração da geração 1 (Observação 18a).
- **Despesa importada do Open Finance** — edição parcial (campos de negócio editáveis; forma de pagamento e origem bloqueadas; sem exclusão) ([RN08](#rn08) / [MSG16b](#msg16b)).
- **Parcelamento** ([RN12](#rn12) a [RN14](#rn14)): a tela informa **número de parcelas** e **valor total da compra**; o serviço cria N registros `DESPESAS`. A **1ª parcela é a despesa-mãe** (`DESP_ID_PARCELA_PAI` nula), as demais apontam para ela (melhoria F — FK real). `DESP_VALOR` = total ÷ N com resíduo na última parcela; `DESP_VALOR_TOTAL_COMPRA` = total em todas; competência e vencimento avançam 1 mês por parcela; data de lançamento igual em todas. **Editar** uma parcela afeta só ela; mudar N ou o total exige excluir e recriar. **Excluir** a mãe apaga a série toda; excluir uma filha apaga só ela.
- **Rateio** ([RN15](#rn15) a [RN20](#rn20)): recurso de plano pago. A despesa continua sendo do criador; co-participantes são usuários reais com uma fatia (`DEPU_VALOR`). Soma das fatias ≤ `DESP_VALOR` (o resto é a cota do dono). Replicado em todas as parcelas. **Privacidade:** na v1.0 só o dono vê e gerencia o rateio; o co-participante não vê a despesa ([RN19](#rn19)). O **acerto** de cada fatia é registrado pelo dono (`DEPU_IND_STATUS_PAGAMENTO` + `DEPU_DT_ACERTO`).
- **Baixa de pagamento** ([RN21](#rn21)): ação individual no grid ([EDP07](#edp07)) e em lote ([EDP08](#edp08), espelha `pagarDespesas`). Marca `SIM` + `DESP_DT_PAGAMENTO`. Por parcela numa série.
- **Meio de pagamento** ([RN22](#rn22)): derivado da forma de pagamento e ajustável só na forma "Conta à vista".
- **Compra no cartão** ([RN23](#rn23)): grava `CACR_ID`, meio `CREDITO`, status `NAO_SE_APLICA`. O vínculo com a fatura (`FTCA_ID`), o fechamento e o pagamento da fatura são do documento `11`.
- **`DESP_FL_PAGAMENTO_FATURA`** ([RN24](#rn24)): caso de borda — a v1.0 não o oferece pelo fluxo da tela; toda despesa nasce com `FALSE`.
- **Importação de extrato / OFX / planilha** está **fora do escopo** deste documento (D05) — os métodos `importarDadosCartaoCredito*` da geração 1 não são portados aqui.

**A Confirmar:**
- **Despesa-mãe: 1ª parcela × registro separado.** A v1.0 usa a **1ª parcela como mãe** (`DESP_ID_PARCELA_PAI` nula), espelhando a geração 1. Confirmar se um registro-mãe sintético separado (que não entra no grid nem no total, só agrega a série) seria mais limpo para relatórios e para o comportamento de exclusão/edição.
- **Editar a série inteira.** A v1.0 só edita parcela a parcela ([RN13](#rn13)). Confirmar se deve existir um "editar toda a série" (ex.: mudar a categoria de todas as parcelas de uma vez, ou reprocessar valores ao alterar o total na mãe).
- **Excluir uma parcela do meio.** A v1.0 apaga só a parcela e deixa a série incompleta ([RN14](#rn14)). Confirmar se deve reajustar as parcelas restantes (renumerar, redistribuir o valor) ou bloquear a exclusão de parcela isolada, permitindo só "excluir a série".
- **Privacidade do co-participante do rateio.** A v1.0 não mostra a despesa ao co-participante ([RN19](#rn19)). Confirmar o modelo da versão futura: o co-participante vê só a fatia dele (valor, nome da despesa, competência, quem dividiu) numa tela "compartilhado comigo"? Ele pode marcar o próprio acerto, ou só o dono? Ele vê o valor total e as fatias das outras pessoas?
- **Soma das fatias = total × parcial.** A v1.0 permite soma ≤ valor, com a diferença como cota do dono ([RN17](#rn17), parâmetro `DESPESA_RATEIO_SOMA_DEVE_FECHAR = false`). Confirmar se o padrão deve ser exigir soma exata.
- **Rateio sem plano pago: seção some × aviso de upgrade.** A v1.0 **oculta** a seção ([RN15](#rn15)). Confirmar se deve aparecer um aviso/CTA discreto de upgrade ([MSG16](#msg16)) no lugar da seção, ou nada.
- **Arredondamento da última parcela.** A v1.0 joga o resíduo na **última** parcela (parâmetro `DESPESA_RESIDUO_PARCELA_NA_ULTIMA`). Confirmar se a preferência do produto é última, primeira, ou distribuir o centavo entre as primeiras parcelas.
- **`DESP_MEIO_PAGAMENTO` derivado × campo livre.** A v1.0 deriva da forma de pagamento e só deixa escolher na forma "Conta à vista" ([RN22](#rn22)). Confirmar se o usuário deveria poder informar o meio livremente em qualquer forma (ex.: PIX pago com o saldo de uma conta corrente já é coberto; e um "vale-refeição"?).
- **`NAO_SE_APLICA` de status de pagamento fora da compra no cartão.** A v1.0 usa `NAO_SE_APLICA` só para compra no cartão ([RN23](#rn23)). Confirmar se há outros casos (lançamento informativo, ajuste, estorno) que devem nascer `NAO_SE_APLICA` e se a tela deve oferecer essa escolha.
- **Elegibilidade do co-participante do rateio ([EDP10](#edp10)).** A v1.0 busca qualquer usuário ativo por nome/e-mail. Confirmar se deve ser restrito a uma lista de contatos/amigos, ou a usuários que já compartilharam algo com o solicitante, por privacidade.
- **Despesa recorrente.** Fora do escopo da v1.0 — registrado para um módulo/versão futura (gerar ocorrências mês a mês a partir de um modelo).
- **Volume e paginação.** Se o número de despesas por usuário crescer muito (compras em 72x multiplicam as linhas), o grid client-side deve passar a recortar por competência no servidor ([RNF05](#rnf05))?
- **Agrupamento visual da série no grid.** Confirmar se as N parcelas devem aparecer como N linhas soltas (v1.0), como uma linha-resumo expansível, ou com filtro "mostrar/ocultar parcelas futuras".

---

## 18. Anexos

- **Pendência (v1.0):** gerar o diagrama de casos de uso (`prototipo/manter-despesa-casos-uso.drawio` + PNG), o DER do subconjunto (`prototipo/manter-despesa-der.drawio` + `images/manter-despesa-der.png`), os wireframes das cinco telas/modais (`prototipo/manter-despesa-prototipo.drawio` + `images/md-tela-*.png`) e o protótipo navegável (`prototipo/manter-despesa-prototipo.html`).
- Documento 0 — Fundação: `../00 - analise-geral/documento-0-fundacao.md` ([QUADRO_DESCRITIVO_2](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-2), [QUADRO_DESCRITIVO_3](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-3), [QUADRO_DESCRITIVO_5](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-5), [QUADRO_DESCRITIVO_6](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-6), [QUADRO_DESCRITIVO_8](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-8), [QUADRO_DESCRITIVO_10](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-10), [QUADRO_DESCRITIVO_11](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-11), [QUADRO_DESCRITIVO_26](../00%20-%20analise-geral/documento-0-fundacao.md#quadro-descritivo-26); Seções 7.2, 7.3 e 7.4; Observações 8, 9, 12, 15, 16, 18, 24).
- Documento `04 - manter-categoria`: `../04 - manter-categoria/documento-analise-manter-categoria.md` — endpoint de opções de categoria (`GET /categorias/opcoes?aplicaA=DESPESA`).
- Documento `06 - manter-conta`: `../06 - manter-conta/documento-analise-manter-conta.md` — endpoint de opções de conta (`GET /contas/opcoes`), conta `CARTEIRA`, padrão de escopo *row-level*.
- Documento `07 - manter-cartao-credito`: `../07 - manter-cartao-credito/documento-analise-manter-cartao-credito.md` — endpoint de opções de cartão (`GET /cartoes/opcoes`).
- Documento `08 - manter-receita`: `../08 - manter-receita/documento-analise-manter-receita.md` — par direto (forma, voz, decisões espelhadas).
- Documentos `11 - manter-fatura-cartao`, `13 - dashboard`, `15 - open-finance-conciliacao` (a escrever) — fatura de cartão e `DESP.FTCA_ID`, total por categoria e `DESP_FL_PAGAMENTO_FATURA`, despesas de origem `OPEN_FINANCE`.
- Documento futuro de **importação de extrato** — `DESP_ORIGEM = IMPORTACAO`, métodos `importarDadosCartaoCredito*` da geração 1.
- Código de referência geração 1: `dsc-backend` (`domain/Despesa.java`, `domain/DespesaUsuario.java`, `controller/DespesaController.java`, `services/DespesaService.java`, `enums/StatusPagamento.java`, `enums/TipoRegistroFinanceiro.java`, `enums/CategoriaRegistroFinanceiro.java`).
