import { getJson } from '../comum/http.js';
import { semAcento } from '../comum/ui.js';
import { montarEditor, formatarValor, escapar } from './editor-celula.js';
import { abrirConfirmacao, EVENTO_ALTERADO } from './modal-confirmar.js';
import { abrirHistorico } from './modal-historico.js';

const cfg = document.getElementById('dadosTela').dataset;
const podeEditar = !!document.querySelector('[data-perm="editar"]');

let todos = [];
let ordenacao = { col: 'modulo', asc: true };
let editorAberto = null;   // { cancelar }

const corpo = document.getElementById('corpoTabelaParametros');
const rodape = document.getElementById('rodapeContagemParametros');

// Ícones do Lucide (lucide.dev) — traço/tamanho padronizados.
const ICONES = {
    editar: '<path d="M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z"/><path d="m15 5 4 4"/>',
    restaurar: '<path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/>',
    historico: '<path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M12 7v5l4 2"/>'
};

async function carregar() {
    todos = await getJson(cfg.urlDados);
    preencherFiltroModulo();
    render();
}

function preencherFiltroModulo() {
    const select = document.getElementById('filtroModulo');
    const atual = select.value;
    const modulos = [...new Set(todos.map((p) => p.modulo))].sort((a, b) => a.localeCompare(b, 'pt-BR'));
    select.length = 1;   // mantém o "Todos"
    modulos.forEach((m) => {
        const opt = document.createElement('option');
        opt.value = m;
        opt.textContent = m;
        select.appendChild(opt);
    });
    select.value = atual;
}

function filtrados() {
    const busca = semAcento(document.getElementById('filtroBusca').value);
    const modulo = document.getElementById('filtroModulo').value;
    const tipo = document.getElementById('filtroTipo').value;
    const situacao = document.getElementById('filtroSituacao').value;
    return todos.filter((p) => {
        if (busca && !(semAcento(p.codigo).includes(busca) || semAcento(p.nome).includes(busca))) return false;
        if (modulo && p.modulo !== modulo) return false;
        if (tipo && p.tipoDado !== tipo) return false;
        if (situacao === 'ATIVO' && p.orfa) return false;
        if (situacao === 'ORFAO' && !p.orfa) return false;
        return true;
    });
}

function ordenar(lista) {
    const { col, asc } = ordenacao;
    return lista.slice().sort((a, b) => {
        const va = a[col] ?? '';
        const vb = b[col] ?? '';
        if (va === vb) return 0;
        return (va > vb ? 1 : -1) * (asc ? 1 : -1);
    });
}

function situacaoBadge(p) {
    return p.orfa
        ? '<span class="badge bg-yellow-lt">' + escapar(cfg.labelOrfao) + '</span>'
        : '<span class="badge bg-green-lt">' + escapar(cfg.labelAtivo) + '</span>';
}

function ultimaAlteracao(p) {
    if (!p.dataAlteracao) return '<span class="text-secondary">—</span>';
    const data = new Date(p.dataAlteracao).toLocaleString('pt-BR');
    return escapar(p.alteradoPor || '') + '<br><span class="text-secondary">' + data + '</span>';
}

function botaoAcao(acao, rotulo, id, desabilitado) {
    return '<button type="button" class="btn btn-action" data-acao="' + acao + '" data-id="' + id + '"'
        + (desabilitado ? ' disabled' : '')
        + ' title="' + rotulo + '" aria-label="' + rotulo + '">'
        + '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none"'
        + ' stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"'
        + ' class="icon" aria-hidden="true">' + ICONES[acao] + '</svg></button> ';
}

function acoes(p) {
    let html = '';
    if (podeEditar) {
        html += botaoAcao('editar', cfg.labelEditar || 'Editar valor', p.id, p.orfa);
        html += botaoAcao('restaurar', cfg.labelRestaurar || 'Restaurar valor padrão', p.id, p.valor === p.valorDefault || p.orfa);
    }
    html += botaoAcao('historico', cfg.labelHistorico || 'Ver histórico', p.id, false);
    return html;
}

function render() {
    fecharEditor();
    const lista = ordenar(filtrados());
    corpo.innerHTML = '';
    if (lista.length === 0) {
        corpo.innerHTML = '<tr><td colspan="8" class="text-center text-secondary">' + escapar(cfg.msgFiltroVazio) + '</td></tr>';
    } else {
        lista.forEach((p) => {
            const tr = document.createElement('tr');
            tr.dataset.id = p.id;
            const editavel = podeEditar && !p.orfa;
            tr.innerHTML =
                '<td><span class="badge bg-blue-lt">' + escapar(p.modulo) + '</span></td>'
                + '<td class="font-monospace">' + escapar(p.codigo) + '</td>'
                + '<td><span title="' + escapar(p.descricao || '') + '">' + escapar(p.nome) + '</span></td>'
                + '<td><span class="badge bg-secondary-lt">' + escapar(p.tipoDado) + '</span></td>'
                + '<td class="celula-valor' + (editavel ? ' cursor-pointer' : '') + '" data-id="' + p.id + '"'
                + (editavel ? ' title="' + escapar(cfg.labelCliqueEditar || 'Clique para editar o valor') + '"' : '') + '>'
                + formatarValor(p) + '</td>'
                + '<td>' + situacaoBadge(p) + '</td>'
                + '<td>' + ultimaAlteracao(p) + '</td>'
                + '<td class="text-nowrap">' + acoes(p) + '</td>';
            corpo.appendChild(tr);
        });
    }
    rodape.textContent = lista.length + ' de ' + todos.length + ' parâmetro(s)';
}

function fecharEditor() {
    if (editorAberto) {
        const e = editorAberto;
        editorAberto = null;
        e.cancelar();
    }
}

function paramPorId(id) {
    return todos.find((p) => String(p.id) === String(id));
}

function iniciarEdicao(celula, p) {
    if (!podeEditar || p.orfa) return;
    if (editorAberto) {
        if (editorAberto.celula === celula) return;
        fecharEditor();
    }
    editorAberto = montarEditor(celula, p,
        (novoValor) => {
            editorAberto = null;
            abrirConfirmacao(p, 'editar', novoValor,
                () => render(),          // cancelar reverte a célula
                () => carregar());
        },
        () => {
            editorAberto = null;
            render();
        });
}

// Clicar na célula Valor ou no botão Editar a transforma no editor do tipo da linha.
corpo.addEventListener('click', (e) => {
    const btn = e.target.closest('button[data-acao]');
    if (btn) {
        if (btn.hasAttribute('disabled')) return;
        const p = paramPorId(btn.dataset.id);
        if (!p) return;

        if (btn.dataset.acao === 'editar') {
            const tr = btn.closest('tr');
            const celula = tr ? tr.querySelector('td.celula-valor') : null;
            if (celula) iniciarEdicao(celula, p);
            return;
        }

        fecharEditor();

        if (btn.dataset.acao === 'historico') {
            abrirHistorico(p);
        } else if (btn.dataset.acao === 'restaurar') {
            const msg = (cfg.msgConfirmaRestaurar || 'Restaurar "{0}" para "{1}"?')
                .replace('{0}', p.nome).replace('{1}', p.valorDefault);
            if (window.confirm(msg)) {
                abrirConfirmacao(p, 'restaurar', p.valorDefault, () => {}, () => carregar());
            }
        }
        return;
    }

    const celula = e.target.closest('td.celula-valor');
    if (celula && podeEditar) {
        const p = paramPorId(celula.dataset.id);
        if (!p || p.orfa) return;
        if (editorAberto && editorAberto.celula === celula) return;
        iniciarEdicao(celula, p);
    }
});

document.getElementById('btnAplicarFiltro').addEventListener('click', render);
document.getElementById('btnLimparFiltro').addEventListener('click', () => {
    document.getElementById('filtroBusca').value = '';
    document.getElementById('filtroModulo').value = '';
    document.getElementById('filtroTipo').value = '';
    document.getElementById('filtroSituacao').value = '';
    render();
});
document.querySelectorAll('#tabelaParametros th.sortable').forEach((th) => {
    th.addEventListener('click', () => {
        const col = th.dataset.col;
        ordenacao = { col, asc: ordenacao.col === col ? !ordenacao.asc : true };
        render();
    });
});

document.addEventListener(EVENTO_ALTERADO, carregar);

carregar();
