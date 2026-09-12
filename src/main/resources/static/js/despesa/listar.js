import { getJson, enviar } from '../comum/http.js';
import { semAcento, dataBr, toast, abrirModal, fecharModal } from '../comum/ui.js';
import { parseDecimal, formatarMoeda, definirValorMoeda } from '../comum/mascara.js';
import { inicializarFiltro, obterFiltroAtual, abrirModalFiltro } from './modal-filtro.js';
import { inicializarForm, abrirEdicao, excluir, EVENTO_ALTERADO } from './modal-form.js';
import { inicializarPagamento, abrirPagamento, EVENTO_PAGAMENTO_REGISTRADO } from './modal-pagamento.js';
import { inicializarPagamentoLote, abrirPagamentoLote, EVENTO_PAGAMENTO_LOTE_REGISTRADO } from './modal-lote.js';

const cfg = () => document.getElementById('dadosTelaDespesa').dataset;

let todas = [];
let ordenacao = { col: 'competencia', asc: false };
let selecionadasMap = new Map();
let idsParaDuplicar = [];

const corpo = document.getElementById('corpoTabelaDespesas');
const rodape = document.getElementById('rodapeContagemDespesas');
const chkTodos = document.getElementById('chkTodos');
const btnPagarLote = document.getElementById('btnPagarLote');
const btnDuplicarLote = document.getElementById('btnDuplicarLote');

const podeInserir = () => !!document.querySelector('[data-perm="inserir"]');
const podeEditar = () => !!document.querySelector('[data-perm="editar"]');
const podeExcluir = () => !!document.querySelector('[data-perm="excluir"]');
const podePagar = () => !!document.querySelector('[data-perm="pagar"]');
const podeImportar = () => !!document.querySelector('[data-perm="importar"]');
const podeRatear = () => !!document.querySelector('[data-perm="ratear"]') || cfg().podeRatear === 'true';

async function carregar() {
    try {
        todas = await getJson(cfg().urlDados);
        selecionadasMap.clear();
        atualizarBotoesLote();
        if (chkTodos) chkTodos.checked = false;
        render();
    } catch (e) {
        corpo.innerHTML = '<tr><td colspan="12" class="text-center text-danger">Erro ao carregar despesas.</td></tr>';
    }
}

function situacaoCodigo(d) {
    if (d.excluido) return 'EXCLUIDA';
    return d.statusPagamento || 'NAO';
}

function filtrar(lista) {
    const f = obterFiltroAtual();
    const buscaNorm = semAcento(f.busca);

    return lista.filter(d => {
        if (d.excluido) return false;

        if (buscaNorm) {
            const nomeNorm = semAcento(d.nome);
            const descNorm = semAcento(d.descricao);
            if (!nomeNorm.includes(buscaNorm) && !descNorm.includes(buscaNorm)) {
                return false;
            }
        }

        if (f.competenciaInicio && d.competencia < f.competenciaInicio) return false;
        if (f.competenciaFim && d.competencia > f.competenciaFim) return false;

        if (f.status && situacaoCodigo(d) !== f.status) return false;
        if (f.forma && d.formaPagamento !== f.forma) return false;
        if (f.categoriaId && String(d.categoriaId) !== String(f.categoriaId)) return false;

        if (f.parcelada === 'SIM' && !d.parcelada) return false;
        if (f.parcelada === 'NAO' && d.parcelada) return false;

        if (f.recorrente === 'SIM' && !d.recorrente) return false;
        if (f.recorrente === 'NAO' && d.recorrente) return false;

        return true;
    });
}

function ordenar(lista) {
    const { col, asc } = ordenacao;
    return lista.slice().sort((a, b) => {
        let cmp = 0;

        if (col === 'categoria') {
            const va = a.categoriaNome || '';
            const vb = b.categoriaNome || '';
            cmp = va.localeCompare(vb, 'pt-BR');
        } else if (col === 'forma') {
            const va = a.formaPagamento || '';
            const vb = b.formaPagamento || '';
            cmp = va.localeCompare(vb, 'pt-BR');
        } else if (col === 'valor') {
            const va = a.valor != null ? Number(a.valor) : 0;
            const vb = b.valor != null ? Number(b.valor) : 0;
            cmp = va - vb;
        } else if (col === 'status') {
            const va = situacaoCodigo(a);
            const vb = situacaoCodigo(b);
            cmp = va.localeCompare(vb, 'pt-BR');
        } else if (col === 'rateio') {
            const va = a.qtdCoParticipantes != null ? Number(a.qtdCoParticipantes) : 0;
            const vb = b.qtdCoParticipantes != null ? Number(b.qtdCoParticipantes) : 0;
            cmp = va - vb;
        } else {
            let va = a[col];
            let vb = b[col];
            if (va == null) va = '';
            if (vb == null) vb = '';
            if (typeof va === 'number' && typeof vb === 'number') {
                cmp = va - vb;
            } else {
                cmp = String(va).localeCompare(String(vb), 'pt-BR');
            }
        }

        if (cmp !== 0) {
            return asc ? cmp : -cmp;
        }

        // --- Desempate determinístico (para parcelas e registros com campos iguais) ---
        // 1. Número da parcela
        const parcelaA = a.nroParcela || 0;
        const parcelaB = b.nroParcela || 0;
        if (parcelaA !== parcelaB) {
            return asc ? parcelaA - parcelaB : parcelaB - parcelaA;
        }

        // 2. Competência
        const compA = a.competencia || '';
        const compB = b.competencia || '';
        const compCmp = compA.localeCompare(compB);
        if (compCmp !== 0) {
            return asc ? compCmp : -compCmp;
        }

        // 3. Vencimento
        const vencA = a.dataVencimento || '';
        const vencB = b.dataVencimento || '';
        const vencCmp = vencA.localeCompare(vencB);
        if (vencCmp !== 0) {
            return asc ? vencCmp : -vencCmp;
        }

        // 4. Identificador único
        return (a.id || 0) - (b.id || 0);
    });
}

function formatarCompetencia(competencia) {
    if (!competencia) return '';
    const [ano, mes] = competencia.split('-');
    return `${mes}/${ano}`;
}

function badgeStatus(d) {
    const st = situacaoCodigo(d);
    if (st === 'EXCLUIDA') return `<span class="badge bg-red text-red-fg">${cfg().labelExcluida || 'Excluída'}</span>`;
    if (st === 'SIM') return `<span class="badge bg-green text-green-fg">${cfg().labelPaga || 'Paga'}</span>`;
    if (st === 'NAO_SE_APLICA') return `<span class="badge bg-secondary-lt">${cfg().labelNaoSeAplica || 'Não se aplica'}</span>`;
    return `<span class="badge bg-yellow text-yellow-fg">${cfg().labelEmAberto || 'Em aberto'}</span>`;
}

function badgeForma(d) {
    if (d.formaPagamento === 'CARTAO') {
        const desc = d.cartaoDescricao || cfg().labelFormaCartao || 'Cartão';
        return `<span class="badge bg-purple-lt" title="${desc}"><i class="ph ph-credit-card me-1"></i>${desc}</span>`;
    }
    if (d.formaPagamento === 'DINHEIRO') {
        return `<span class="badge bg-green-lt"><i class="ph ph-money me-1"></i>${cfg().labelFormaDinheiro || 'Dinheiro'}</span>`;
    }
    const conta = d.contaDescricao || cfg().labelFormaConta || 'Conta';
    const meio = d.meioPagamento ? ` (${d.meioPagamento})` : '';
    return `<span class="badge bg-azure-lt" title="${conta}${meio}"><i class="ph ph-bank me-1"></i>${conta}</span>`;
}

function labelOrigem(origem) {
    if (origem === 'OPEN_FINANCE') return cfg().labelOrigemOpenFinance || 'Open Finance';
    if (origem === 'IMPORTACAO') return cfg().labelOrigemImportacao || 'Importação';
    return cfg().labelOrigemManual || 'Manual';
}

function atualizarBotoesLote() {
    if (btnDuplicarLote) {
        btnDuplicarLote.disabled = selecionadasMap.size === 0;
    }
    if (btnPagarLote) {
        const temParaPagar = Array.from(selecionadasMap.values()).some(d => d.statusPagamento === 'NAO');
        btnPagarLote.disabled = !temParaPagar;
    }
}

function atualizarTotalizador(filtradas) {
    const f = obterFiltroAtual();
    const cardTotalizador = document.getElementById('cardTotalizadorDespesa');
    if (!cardTotalizador) return;

    if (f.competenciaInicio && f.competenciaInicio === f.competenciaFim) {
        const totalPago = filtradas
            .filter(d => !d.excluido && d.statusPagamento === 'SIM')
            .reduce((acc, d) => acc + (d.valor != null ? Number(d.valor) : 0), 0);
        const totalPendente = filtradas
            .filter(d => !d.excluido && d.statusPagamento === 'NAO')
            .reduce((acc, d) => acc + (d.valor != null ? Number(d.valor) : 0), 0);
        const totalGeral = totalPago + totalPendente;

        const elPago = document.getElementById('totalDespesaPago');
        const elPendente = document.getElementById('totalDespesaPendente');
        const elGeral = document.getElementById('totalDespesaGeral');

        if (elPago) elPago.textContent = formatarMoeda(totalPago);
        if (elPendente) elPendente.textContent = formatarMoeda(totalPendente);
        if (elGeral) elGeral.textContent = formatarMoeda(totalGeral);
        cardTotalizador.style.display = '';
    } else {
        cardTotalizador.style.display = 'none';
    }
}

function render() {
    atualizarCabecalhoOrdenacao();
    const filtradas = filtrar(todas);
    const lista = ordenar(filtradas);
    corpo.innerHTML = '';

    atualizarTotalizador(filtradas);

    const colspan = podeRatear() ? 12 : 11;

    if (lista.length === 0) {
        corpo.innerHTML = `<tr><td colspan="${colspan}" class="text-center text-secondary">${cfg().msgFiltroVazio}</td></tr>`;
    } else {
        lista.forEach(d => {
            const tr = document.createElement('tr');

            const categoriaHtml = d.categoriaNome
                ? `<span class="badge bg-blue-lt">${d.categoriaNome}</span>`
                : '<span class="text-muted">—</span>';

            const parcelaBadge = d.parcelada
                ? `<span class="badge bg-teal-lt ms-1">${d.nroParcela || 1}/${d.qtdParcelas || 1}x</span>`
                : '';

            const recorrenteBadge = d.recorrente
                ? `<span class="badge bg-cyan-lt ms-1" title="Despesa Recorrente"><i class="ph ph-arrows-clockwise me-1"></i>Fixa</span>`
                : '';

            const descHtml = d.descricao ? `<br><small class="text-muted">${d.descricao}</small>` : '';

            const podeSelecionar = !d.excluido;
            const checkHtml = podeSelecionar
                ? `<input class="form-check-input chk-despesa" type="checkbox" data-id="${d.id}" ${selecionadasMap.has(d.id) ? 'checked' : ''}>`
                : '';

            let acaoHtml = '';
            if (!d.excluido) {
                // Registrar Pagamento (somente se status for NAO e tiver permissão)
                if (d.statusPagamento === 'NAO' && podePagar()) {
                    acaoHtml += `<button type="button" class="btn btn-action text-success" data-acao="pagamento"
                        data-id="${d.id}" data-valor="${d.valor}" title="${cfg().acaoPagamento || 'Registrar pagamento'}" aria-label="Registrar pagamento">
                        <i class="ph ph-currency-dollar" aria-hidden="true"></i>
                    </button> `;
                }

                // Duplicar
                if (podeInserir()) {
                    acaoHtml += `<button type="button" class="btn btn-action" data-acao="duplicar"
                        data-id="${d.id}" title="Duplicar despesa" aria-label="Duplicar despesa">
                        <i class="ph ph-copy" aria-hidden="true"></i>
                    </button> `;
                }

                // Ratear (se tiver permissão)
                if (podeRatear()) {
                    acaoHtml += `<button type="button" class="btn btn-action text-info" data-acao="ratear"
                        data-id="${d.id}" title="${cfg().acaoRatear || 'Dividir despesa'}" aria-label="Dividir despesa">
                        <i class="ph ph-users-three" aria-hidden="true"></i>
                    </button> `;
                }

                // Editar
                if (podeEditar()) {
                    acaoHtml += `<button type="button" class="btn btn-action" data-acao="editar"
                        data-id="${d.id}" title="${cfg().acaoEditar || 'Editar despesa'}" aria-label="Editar despesa">
                        <i class="ph ph-pencil-simple" aria-hidden="true"></i>
                    </button> `;
                }

                // Excluir (somente MANUAL e tiver permissão)
                if (d.origem === 'MANUAL' && podeExcluir()) {
                    acaoHtml += `<button type="button" class="btn btn-action text-danger" data-acao="excluir"
                        data-id="${d.id}" data-nome="${d.nome}" data-parcelada="${d.parcelada}"
                        data-nro="${d.nroParcela}" data-qtd="${d.qtdParcelas}"
                        data-recorrente="${d.recorrente}" data-recorrente-pai="${d.idRecorrentePai || ''}"
                        title="Excluir despesa" aria-label="Excluir despesa">
                        <i class="ph ph-trash" aria-hidden="true"></i>
                    </button>`;
                }
            }

            let rateioColHtml = '';
            if (podeRatear()) {
                const qtd = d.qtdCoParticipantes || 0;
                rateioColHtml = qtd > 0
                    ? `<td class="text-center"><span class="badge bg-blue-lt" title="${qtd} co-participante(s)"><i class="ph ph-users me-1"></i>${qtd}</span></td>`
                    : '<td class="text-center text-muted">—</td>';
            }

            const podeEditarCompetencia = podeEditar() && !d.excluido;
            const classeCompetencia = podeEditarCompetencia ? 'cursor-pointer celula-competencia' : '';
            const titleCompetencia = podeEditarCompetencia ? 'Clique para editar a competência' : '';

            const podeEditarValor = podeEditar() && !d.excluido;
            const classeValor = podeEditarValor ? 'text-end fw-bold cursor-pointer celula-valor' : 'text-end fw-bold';
            const titleValor = podeEditarValor ? 'Clique para editar o valor' : '';

            tr.innerHTML = `
                <td>${checkHtml}</td>
                <td class="${classeCompetencia}" data-id="${d.id}" title="${titleCompetencia}">${formatarCompetencia(d.competencia)}</td>
                <td><strong>${d.nome}</strong>${parcelaBadge}${recorrenteBadge}${descHtml}</td>
                <td>${categoriaHtml}</td>
                <td>${badgeForma(d)}</td>
                <td class="${classeValor}" data-id="${d.id}" title="${titleValor}">${formatarMoeda(d.valor)}</td>
                <td>${d.dataVencimento ? dataBr(d.dataVencimento) : '<span class="text-muted">—</span>'}</td>
                <td>${badgeStatus(d)}</td>
                <td>${d.dataPagamento ? dataBr(d.dataPagamento) : '<span class="text-muted">—</span>'}</td>
                ${rateioColHtml}
                <td>${labelOrigem(d.origem)}</td>
                <td><div class="d-flex gap-1">${acaoHtml}</div></td>
            `;

            corpo.appendChild(tr);
        });
    }

    rodape.textContent = `Mostrando ${lista.length} de ${todas.length} despesas`;
}

function inicializarEdicaoInline() {
    corpo.addEventListener('click', function (e) {
        const celula = e.target.closest('td.celula-valor');
        if (!celula || celula.querySelector('input')) return;

        const id = Number(celula.dataset.id);
        const d = todas.find(item => item.id === id);
        if (!d) return;

        const valorOriginal = d.valor != null ? Number(d.valor) : 0;
        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'form-control form-control-sm text-end mascara-moeda';
        input.style.minWidth = '110px';
        input.style.maxWidth = '140px';
        input.style.display = 'inline-block';
        definirValorMoeda(input, valorOriginal);

        celula.innerHTML = '';
        celula.appendChild(input);
        input.focus();
        input.select();

        let finalizado = false;

        function restaurar() {
            if (finalizado) return;
            finalizado = true;
            celula.innerHTML = formatarMoeda(d.valor);
        }

        async function salvar() {
            if (finalizado) return;
            finalizado = true;
            const novoValor = parseDecimal(input.value);
            if (novoValor <= 0) {
                toast('O valor da despesa deve ser maior que zero.', true);
                celula.innerHTML = formatarMoeda(d.valor);
                return;
            }

            if (Math.abs(novoValor - Number(d.valor)) < 0.001) {
                celula.innerHTML = formatarMoeda(d.valor);
                return;
            }

            try {
                const url = `${cfg().urlAtualizarValor}/${id}/valor`;
                const body = new URLSearchParams();
                body.append('valor', novoValor.toFixed(2));
                const res = await enviar(url, 'PATCH', body);
                if (res.sucesso) {
                    d.valor = novoValor;
                    toast(res.mensagem || 'Valor atualizado com sucesso.');
                    celula.innerHTML = formatarMoeda(novoValor);
                    const tr = celula.closest('tr');
                    if (tr) {
                        const btnPag = tr.querySelector('[data-acao="pagamento"]');
                        if (btnPag) btnPag.dataset.valor = novoValor;
                    }
                    atualizarTotalizador(filtrar(todas));
                } else {
                    const erroMsg = res.errosCampos?.valor || res.mensagem || 'Erro ao atualizar valor.';
                    toast(erroMsg, true);
                    celula.innerHTML = formatarMoeda(d.valor);
                }
            } catch (err) {
                toast('Erro de comunicação ao atualizar valor.', true);
                celula.innerHTML = formatarMoeda(d.valor);
            }
        }

        input.addEventListener('keydown', function (evt) {
            if (evt.key === 'Enter') {
                evt.preventDefault();
                salvar();
            } else if (evt.key === 'Escape') {
                evt.preventDefault();
                restaurar();
            }
        });

        input.addEventListener('blur', function () {
            salvar();
        });
    });
}

function inicializarEdicaoInlineCompetencia() {
    corpo.addEventListener('click', function (e) {
        const celula = e.target.closest('td.celula-competencia');
        if (!celula || celula.querySelector('input')) return;

        const id = Number(celula.dataset.id);
        const d = todas.find(item => item.id === id);
        if (!d) return;

        const competenciaOriginal = d.competencia || '';
        const input = document.createElement('input');
        input.type = 'month';
        input.className = 'form-control form-control-sm';
        input.style.minWidth = '130px';
        input.style.maxWidth = '160px';
        input.style.display = 'inline-block';
        input.value = competenciaOriginal;

        celula.innerHTML = '';
        celula.appendChild(input);
        input.focus();

        let finalizado = false;

        function restaurar() {
            if (finalizado) return;
            finalizado = true;
            celula.innerHTML = formatarCompetencia(d.competencia);
        }

        async function salvar() {
            if (finalizado) return;
            finalizado = true;
            const novaCompetencia = (input.value || '').trim();
            if (!novaCompetencia || !/^\d{4}-\d{2}$/.test(novaCompetencia)) {
                toast('A competência deve estar no formato AAAA-MM.', true);
                celula.innerHTML = formatarCompetencia(d.competencia);
                return;
            }

            if (novaCompetencia === d.competencia) {
                celula.innerHTML = formatarCompetencia(d.competencia);
                return;
            }

            try {
                const urlBase = cfg().urlAtualizarCompetencia || cfg().urlAtualizarValor || `${cfg().urlBase || ''}/despesas`;
                const url = `${urlBase}/${id}/competencia`;
                const body = new URLSearchParams();
                body.append('competencia', novaCompetencia);
                const res = await enviar(url, 'PATCH', body);
                if (res.sucesso) {
                    d.competencia = novaCompetencia;
                    toast(res.mensagem || 'Competência atualizada com sucesso.');
                    celula.innerHTML = formatarCompetencia(novaCompetencia);
                    render();
                } else {
                    const erroMsg = res.errosCampos?.competencia || res.mensagem || 'Erro ao atualizar competência.';
                    toast(erroMsg, true);
                    celula.innerHTML = formatarCompetencia(d.competencia);
                }
            } catch (err) {
                toast('Erro de comunicação ao atualizar competência.', true);
                celula.innerHTML = formatarCompetencia(d.competencia);
            }
        }

        input.addEventListener('keydown', function (evt) {
            if (evt.key === 'Enter') {
                evt.preventDefault();
                salvar();
            } else if (evt.key === 'Escape') {
                evt.preventDefault();
                restaurar();
            }
        });

        input.addEventListener('blur', function () {
            salvar();
        });
    });
}

export function abrirModalDuplicar(despesas) {
    if (!despesas || despesas.length === 0) return;
    idsParaDuplicar = despesas.map(d => d.id);

    const resumoEl = document.getElementById('duplicarResumoSelecao');
    if (resumoEl) {
        if (despesas.length === 1) {
            resumoEl.textContent = `1 despesa selecionada: ${despesas[0].nome}`;
        } else {
            resumoEl.textContent = `${despesas.length} despesas selecionadas para duplicação.`;
        }
    }

    const inputComp = document.getElementById('duplicarCompetenciaDestino');
    if (inputComp) {
        const compOrigem = despesas[0]?.competencia;
        inputComp.value = compOrigem || new Date().toISOString().slice(0, 7);
    }

    abrirModal('modalDuplicarDespesa');
}

function inicializarDuplicacao() {
    const btnConfirmar = document.getElementById('btnConfirmarDuplicar');
    if (btnConfirmar) {
        btnConfirmar.addEventListener('click', async function () {
            const inputComp = document.getElementById('duplicarCompetenciaDestino');
            const compDestino = inputComp ? inputComp.value : '';
            if (!compDestino) {
                toast('Informe a competência de destino.', true);
                if (inputComp) inputComp.focus();
                return;
            }

            if (!idsParaDuplicar || idsParaDuplicar.length === 0) {
                toast('Nenhuma despesa selecionada para duplicação.', true);
                fecharModal('modalDuplicarDespesa');
                return;
            }

            btnConfirmar.disabled = true;
            try {
                const body = new URLSearchParams();
                idsParaDuplicar.forEach(id => body.append('ids', id));
                body.append('competenciaDestino', compDestino);

                const res = await enviar(cfg().urlDuplicar, 'POST', body);
                if (res.sucesso) {
                    toast(res.mensagem || 'Despesa(s) duplicada(s) com sucesso.');
                    fecharModal('modalDuplicarDespesa');
                    selecionadasMap.clear();
                    atualizarBotoesLote();
                    if (chkTodos) chkTodos.checked = false;
                    await carregar();
                } else {
                    const erroMsg = res.errosNegocio?.ids || res.errosCampos?.competenciaDestino || res.mensagem || 'Erro ao duplicar despesas.';
                    toast(erroMsg, true);
                }
            } catch (err) {
                toast('Erro de comunicação ao duplicar despesas.', true);
            } finally {
                btnConfirmar.disabled = false;
            }
        });
    }

    if (btnDuplicarLote) {
        btnDuplicarLote.addEventListener('click', function () {
            if (selecionadasMap.size === 0) return;
            abrirModalDuplicar(Array.from(selecionadasMap.values()));
        });
    }
}

function inicializarImportacaoFatura() {
    const modalEl = document.getElementById('modalImportarFatura');
    if (modalEl) {
        modalEl.addEventListener('show.bs.modal', async function () {
            const inputComp = document.getElementById('importarCompetencia');
            if (inputComp && !inputComp.value) {
                inputComp.value = new Date().toISOString().slice(0, 7);
            }
            try {
                const [cartoes, categorias] = await Promise.all([
                    getJson(cfg().urlCartoesOpcoes),
                    getJson(cfg().urlCategoriasOpcoes)
                ]);
                const selCartao = document.getElementById('importarCartaoId');
                if (selCartao) {
                    const placeholder = selCartao.options[0]?.text || 'Selecione o cartão...';
                    selCartao.innerHTML = `<option value="">${placeholder}</option>` +
                        cartoes.map(c => `<option value="${c.id}">${c.descricao || c.nome}</option>`).join('');
                }
                const selCat = document.getElementById('importarCategoriaId');
                if (selCat) {
                    const placeholder = selCat.options[0]?.text || 'Sem categoria';
                    selCat.innerHTML = `<option value="">${placeholder}</option>` +
                        categorias.map(c => `<option value="${c.id}">${c.nome}</option>`).join('');
                }
            } catch (err) {
                console.error('Erro ao carregar opções de importação', err);
            }
        });
    }

    const form = document.getElementById('formImportarFatura');
    if (form) {
        form.addEventListener('submit', async function (e) {
            e.preventDefault();
            const btnExec = document.getElementById('btnExecutarImportacao');
            if (btnExec) btnExec.disabled = true;
            try {
                const formData = new FormData(form);
                const res = await enviar(cfg().urlImportar, 'POST', formData);
                if (res.sucesso) {
                    toast(res.mensagem || 'Fatura importada com sucesso.');
                    fecharModal('modalImportarFatura');
                    form.reset();
                    await carregar();
                } else {
                    toast(res.mensagem || 'Erro ao importar fatura.', true);
                }
            } catch (err) {
                toast('Erro de comunicação ao importar fatura.', true);
            } finally {
                if (btnExec) btnExec.disabled = false;
            }
        });
    }
}

function inicializarSelecaoMultipla() {
    if (chkTodos) {
        chkTodos.addEventListener('change', function () {
            const checks = corpo.querySelectorAll('.chk-despesa');
            checks.forEach(chk => {
                chk.checked = chkTodos.checked;
                const id = Number(chk.dataset.id);
                if (chkTodos.checked) {
                    const desp = todas.find(d => d.id === id);
                    if (desp) selecionadasMap.set(id, desp);
                } else {
                    selecionadasMap.delete(id);
                }
            });
            atualizarBotoesLote();
        });
    }

    corpo.addEventListener('change', function (e) {
        if (e.target.classList.contains('chk-despesa')) {
            const id = Number(e.target.dataset.id);
            if (e.target.checked) {
                const desp = todas.find(d => d.id === id);
                if (desp) selecionadasMap.set(id, desp);
            } else {
                selecionadasMap.delete(id);
            }
            atualizarBotoesLote();
        }
    });

    if (btnPagarLote) {
        btnPagarLote.addEventListener('click', function () {
            const paraPagar = Array.from(selecionadasMap.values()).filter(d => d.statusPagamento === 'NAO');
            if (paraPagar.length === 0) return;
            abrirPagamentoLote(paraPagar);
        });
    }
}

function inicializarAcoes() {
    corpo.addEventListener('click', function (e) {
        const btn = e.target.closest('button[data-acao]');
        if (!btn) return;
        const acao = btn.dataset.acao;
        const id = Number(btn.dataset.id);
        const valor = btn.dataset.valor;
        const nome = btn.dataset.nome;
        const parcelada = btn.dataset.parcelada === 'true';
        const nro = Number(btn.dataset.nro);
        const qtd = Number(btn.dataset.qtd);
        const recorrente = btn.dataset.recorrente === 'true';
        const idRecorrentePai = btn.dataset.recorrentePai ? Number(btn.dataset.recorrentePai) : null;

        if (acao === 'editar' || acao === 'ratear') {
            abrirEdicao(id);
        } else if (acao === 'duplicar') {
            const desp = todas.find(item => item.id === id);
            if (desp) abrirModalDuplicar([desp]);
        } else if (acao === 'pagamento') {
            abrirPagamento(id, valor);
        } else if (acao === 'excluir') {
            excluir(id, nome, parcelada, nro, qtd, recorrente, idRecorrentePai);
        }
    });
}

function atualizarCabecalhoOrdenacao() {
    document.querySelectorAll('#tabelaDespesas thead th.sortable').forEach(th => {
        const col = th.dataset.col;
        const iconeExistente = th.querySelector('.icone-ordenacao');
        if (iconeExistente) iconeExistente.remove();

        if (ordenacao.col === col) {
            const icone = document.createElement('i');
            icone.className = `icone-ordenacao ph ${ordenacao.asc ? 'ph-caret-up' : 'ph-caret-down'} ms-1`;
            th.appendChild(icone);
        }
    });
}

function inicializarOrdenacao() {
    document.querySelectorAll('#tabelaDespesas thead th.sortable').forEach(th => {
        th.addEventListener('click', function () {
            const col = this.dataset.col;
            if (ordenacao.col === col) {
                ordenacao.asc = !ordenacao.asc;
            } else {
                ordenacao.col = col;
                ordenacao.asc = true;
            }
            render();
        });
    });
}

document.addEventListener('DOMContentLoaded', function () {
    inicializarForm();
    inicializarPagamento();
    inicializarPagamentoLote();
    inicializarDuplicacao();
    inicializarImportacaoFatura();
    inicializarEdicaoInline();
    inicializarEdicaoInlineCompetencia();
    inicializarFiltro(() => render());
    inicializarOrdenacao();
    inicializarSelecaoMultipla();
    inicializarAcoes();

    const btnFiltrar = document.getElementById('btnFiltrar');
    if (btnFiltrar) {
        btnFiltrar.addEventListener('click', function (e) {
            e.preventDefault();
            abrirModalFiltro();
        });
    }

    document.addEventListener(EVENTO_ALTERADO, carregar);
    document.addEventListener(EVENTO_PAGAMENTO_REGISTRADO, carregar);
    document.addEventListener(EVENTO_PAGAMENTO_LOTE_REGISTRADO, carregar);

    carregar();
});
