import { getJson } from '../comum/http.js';
import { semAcento, dataBr } from '../comum/ui.js';
import { inicializarFiltro, obterFiltroAtual, abrirModalFiltro } from './modal-filtro.js';
import { inicializarForm, EVENTO_ALTERADO } from './modal-form.js';

const cfg = () => document.getElementById('dadosTelaReceita').dataset;

let todas = [];
let ordenacao = { col: 'competencia', asc: false };

const corpo = document.getElementById('corpoTabelaReceitas');
const rodape = document.getElementById('rodapeContagemReceitas');

async function carregar() {
    try {
        todas = await getJson(cfg().urlDados);
        render();
    } catch (e) {
        corpo.innerHTML = '<tr><td colspan="10" class="text-center text-danger">Erro ao carregar receitas.</td></tr>';
    }
}

function situacaoCodigo(r) {
    if (r.excluido) return 'EXCLUIDA';
    return r.recebido ? 'RECEBIDA' : 'PREVISTA';
}

function filtrar(lista) {
    const f = obterFiltroAtual();
    const buscaNorm = semAcento(f.busca);

    return lista.filter(r => {
        if (buscaNorm) {
            const nomeNorm = semAcento(r.nome);
            const descNorm = semAcento(r.descricao);
            if (!nomeNorm.includes(buscaNorm) && !descNorm.includes(buscaNorm)) {
                return false;
            }
        }

        if (f.competenciaInicial && r.competencia < f.competenciaInicial) return false;
        if (f.competenciaFinal && r.competencia > f.competenciaFinal) return false;

        if (f.contaId && String(r.contaId) !== String(f.contaId)) return false;
        if (f.categoriaId && String(r.categoriaId) !== String(f.categoriaId)) return false;

        if (f.situacao && situacaoCodigo(r) !== f.situacao) return false;

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
        } else if (col === 'conta') {
            va = a.contaDescricao || '';
            vb = b.contaDescricao || '';
        } else if (col === 'valor') {
            va = a.valor != null ? Number(a.valor) : 0;
            vb = b.valor != null ? Number(b.valor) : 0;
            return asc ? va - vb : vb - va;
        } else if (col === 'situacao') {
            const ordem = { PREVISTA: 0, RECEBIDA: 1, EXCLUIDA: 2 };
            va = ordem[situacaoCodigo(a)];
            vb = ordem[situacaoCodigo(b)];
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

function badgeSituacao(r) {
    const cod = situacaoCodigo(r);
    if (cod === 'EXCLUIDA') return `<span class="badge bg-red text-red-fg">${cfg().labelExcluida || 'Excluída'}</span>`;
    if (cod === 'RECEBIDA') return `<span class="badge bg-green text-green-fg">${cfg().labelRecebida || 'Recebida'}</span>`;
    return `<span class="badge bg-secondary text-secondary-fg">${cfg().labelPrevista || 'Prevista'}</span>`;
}

function labelOrigem(origem) {
    if (origem === 'OPEN_FINANCE') return cfg().labelOrigemOpenFinance || 'Open Finance';
    if (origem === 'IMPORTACAO') return cfg().labelOrigemImportacao || 'Importação';
    return cfg().labelOrigemManual || 'Manual';
}

function render() {
    const filtradas = filtrar(todas);
    const lista = ordenar(filtradas);
    corpo.innerHTML = '';

    if (lista.length === 0) {
        corpo.innerHTML = `<tr><td colspan="10" class="text-center text-secondary">${cfg().msgFiltroVazio}</td></tr>`;
    } else {
        lista.forEach(r => {
            const tr = document.createElement('tr');

            const categoriaHtml = r.categoriaNome
                ? `<span class="badge bg-blue-lt">${r.categoriaNome}</span>`
                : '<span class="text-muted">—</span>';

            const valorHtml = `<span class="fw-bold">${formatarMoeda(r.valor)}</span>`;

            tr.innerHTML = `
                <td>${formatarCompetencia(r.competencia)}</td>
                <td><strong>${r.nome}</strong></td>
                <td>${categoriaHtml}</td>
                <td>${r.contaDescricao || '<span class="text-muted">—</span>'}</td>
                <td class="text-end">${valorHtml}</td>
                <td>${dataBr(r.dataLancamento)}</td>
                <td>${badgeSituacao(r)}</td>
                <td>${r.dataRecebimento ? dataBr(r.dataRecebimento) : '<span class="text-muted">—</span>'}</td>
                <td>${labelOrigem(r.origem)}</td>
                <td></td>
            `;

            corpo.appendChild(tr);
        });
    }

    rodape.textContent = `Mostrando ${lista.length} de ${todas.length} receitas`;
}

function inicializarOrdenacao() {
    document.querySelectorAll('#tabelaReceitas thead th.sortable').forEach(th => {
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
    inicializarFiltro(() => render());
    inicializarOrdenacao();

    const btnFiltrar = document.getElementById('btnFiltrar');
    if (btnFiltrar) {
        btnFiltrar.addEventListener('click', function (e) {
            e.preventDefault();
            abrirModalFiltro();
        });
    }

    document.addEventListener(EVENTO_ALTERADO, carregar);

    carregar();
});
