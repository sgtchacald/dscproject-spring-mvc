import { getJson } from '../comum/http.js';
import { semAcento, dataBr } from '../comum/ui.js';
import { inicializarFiltro, obterFiltroAtual, abrirModalFiltro } from './modal-filtro.js';
import { inicializarForm, abrirEdicao, excluir, EVENTO_ALTERADO } from './modal-form.js';
import { inicializarPagamento, abrirPagamento, EVENTO_PAGAMENTO_REGISTRADO } from './modal-pagamento.js';
import { inicializarPagamentoLote, abrirPagamentoLote, EVENTO_PAGAMENTO_LOTE_REGISTRADO } from './modal-lote.js';

const cfg = () => document.getElementById('dadosTelaDespesa').dataset;

let todas = [];
let ordenacao = { col: 'competencia', asc: false };
let selecionadasMap = new Map();

const corpo = document.getElementById('corpoTabelaDespesas');
const rodape = document.getElementById('rodapeContagemDespesas');
const chkTodos = document.getElementById('chkTodos');
const btnPagarLote = document.getElementById('btnPagarLote');

const podeManter = () => !!document.querySelector('[data-perm="manter"]');
const podeRatear = () => !!document.querySelector('[data-perm="ratear"]') || cfg().podeRatear === 'true';

async function carregar() {
    try {
        todas = await getJson(cfg().urlDados);
        selecionadasMap.clear();
        atualizarBotaoLote();
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

        return true;
    });
}

function ordenar(lista) {
    const { col, asc } = ordenacao;
    return lista.slice().sort((a, b) => {
        let va = a[col];
        let vb = b[col];

        if (col === 'categoria') {
            va = a.categoriaNome || '';
            vb = b.categoriaNome || '';
        } else if (col === 'forma') {
            va = a.formaPagamento || '';
            vb = b.formaPagamento || '';
        } else if (col === 'valor') {
            va = a.valor != null ? Number(a.valor) : 0;
            vb = b.valor != null ? Number(b.valor) : 0;
            return asc ? va - vb : vb - va;
        } else if (col === 'status') {
            va = situacaoCodigo(a);
            vb = situacaoCodigo(b);
        }

        if (va == null) va = '';
        if (vb == null) vb = '';

        if (typeof va === 'string') {
            const res = va.localeCompare(vb, 'pt-BR');
            return asc ? res : -res;
        }

        if (va === vb) return 0;
        return (va > vb ? 1 : -1) * (asc ? 1 : -1);
    });
}

function formatarMoeda(valor) {
    const num = Number(valor != null ? valor : 0);
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(num);
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

function atualizarBotaoLote() {
    if (!btnPagarLote) return;
    const qtd = selecionadasMap.size;
    btnPagarLote.disabled = qtd === 0;
}

function render() {
    const filtradas = filtrar(todas);
    const lista = ordenar(filtradas);
    corpo.innerHTML = '';

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

            const descHtml = d.descricao ? `<br><small class="text-muted">${d.descricao}</small>` : '';

            // Apenas despesas com status NAO podem ser baixadas em lote
            const podeSelecionar = !d.excluido && d.statusPagamento === 'NAO';
            const checkHtml = podeSelecionar
                ? `<input class="form-check-input chk-despesa" type="checkbox" data-id="${d.id}" ${selecionadasMap.has(d.id) ? 'checked' : ''}>`
                : '';

            let acaoHtml = '';
            if (podeManter() && !d.excluido) {
                // Registrar Pagamento (somente se status for NAO)
                if (d.statusPagamento === 'NAO') {
                    acaoHtml += `<button type="button" class="btn btn-action text-success" data-acao="pagamento"
                        data-id="${d.id}" data-valor="${d.valor}" title="${cfg().acaoPagamento || 'Registrar pagamento'}" aria-label="Registrar pagamento">
                        <i class="ph ph-currency-dollar" aria-hidden="true"></i>
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
                acaoHtml += `<button type="button" class="btn btn-action" data-acao="editar"
                    data-id="${d.id}" title="${cfg().acaoEditar || 'Editar despesa'}" aria-label="Editar despesa">
                    <i class="ph ph-pencil-simple" aria-hidden="true"></i>
                </button> `;

                // Excluir (somente MANUAL)
                if (d.origem === 'MANUAL') {
                    acaoHtml += `<button type="button" class="btn btn-action text-danger" data-acao="excluir"
                        data-id="${d.id}" data-nome="${d.nome}" data-parcelada="${d.parcelada}"
                        data-nro="${d.nroParcela}" data-qtd="${d.qtdParcelas}"
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

            tr.innerHTML = `
                <td>${checkHtml}</td>
                <td>${formatarCompetencia(d.competencia)}</td>
                <td><strong>${d.nome}</strong>${parcelaBadge}${descHtml}</td>
                <td>${categoriaHtml}</td>
                <td>${badgeForma(d)}</td>
                <td class="text-end fw-bold">${formatarMoeda(d.valor)}</td>
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
            atualizarBotaoLote();
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
            atualizarBotaoLote();
        }
    });

    if (btnPagarLote) {
        btnPagarLote.addEventListener('click', function () {
            if (selecionadasMap.size === 0) return;
            abrirPagamentoLote(Array.from(selecionadasMap.values()));
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

        if (acao === 'editar' || acao === 'ratear') {
            abrirEdicao(id);
        } else if (acao === 'pagamento') {
            abrirPagamento(id, valor);
        } else if (acao === 'excluir') {
            excluir(id, nome, parcelada, nro, qtd);
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
