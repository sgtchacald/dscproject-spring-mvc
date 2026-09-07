import { getJson } from '../comum/http.js';
import { semAcento, dataBr } from '../comum/ui.js';
import { abrirEdicao, excluir, EVENTO_ALTERADO } from './modal-form.js';
import { abrirHistorico } from './modal-historico.js';

const cfg = document.getElementById('dadosTela').dataset;
const loginAtual = cfg.loginAtual || '';

const pode = {
    editar: !!document.querySelector('[data-perm="editar"]'),
    excluir: !!document.querySelector('[data-perm="excluir"]'),
    historico: !!document.querySelector('[data-perm="historico"]')
};

let todos = [];
let ordenacao = { col: 'nome', asc: true };

const corpo = document.getElementById('corpoTabela');
const rodape = document.getElementById('rodapeContagem');

async function carregar() {
    todos = await getJson(cfg.urlDados);
    render();
}

function filtrados() {
    const busca = semAcento(document.getElementById('filtroBusca').value);
    const perfil = document.getElementById('filtroPerfil').value;
    const situacao = document.getElementById('filtroSituacao').value;
    return todos.filter(function (u) {
        if (busca && !(semAcento(u.nome).includes(busca)
            || semAcento(u.login).includes(busca)
            || semAcento(u.email).includes(busca))) return false;
        if (perfil && u.perfilCodigo !== perfil) return false;
        if (situacao === 'ATIVO' && u.excluido) return false;
        if (situacao === 'EXCLUIDO' && !u.excluido) return false;
        return true;
    });
}

function ordenar(lista) {
    const { col, asc } = ordenacao;
    return lista.slice().sort(function (a, b) {
        const va = a[col], vb = b[col];
        if (va === vb) return 0;
        return (va > vb ? 1 : -1) * (asc ? 1 : -1);
    });
}

// Ícones do Tabler (tabler.io/icons) — mesmo traço/tamanho do resto da UI.
const ICONES = {
    editar: '<path d="M7 7h-1a2 2 0 0 0 -2 2v9a2 2 0 0 0 2 2h9a2 2 0 0 0 2 -2v-1" />'
        + '<path d="M20.385 6.585a2.1 2.1 0 0 0 -2.97 -2.97l-8.415 8.385v3h3l8.385 -8.415z" />'
        + '<path d="M16 5l3 3" />',
    excluir: '<path d="M4 7l16 0" /><path d="M10 11l0 6" /><path d="M14 11l0 6" />'
        + '<path d="M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2 -2l1 -12" />'
        + '<path d="M9 7v-3a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v3" />',
    historico: '<path d="M12 8l0 4l2 2" /><path d="M3.05 11a9 9 0 1 1 .5 4m-.5 5v-5h5" />'
};

function botaoAcao(acao, rotulo, u, classeCor) {
    return '<button type="button" class="btn btn-icon' + classeCor + '"'
        + ' data-acao="' + acao + '" data-id="' + u.id + '"'
        + (acao === 'excluir' ? ' data-nome="' + u.nome + '"' : '')
        + ' title="' + rotulo + '" aria-label="' + rotulo + '">'
        + '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none"'
        + ' stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"'
        + ' class="icon" aria-hidden="true">' + ICONES[acao] + '</svg></button> ';
}

function acoes(u) {
    let html = '';
    if (pode.editar && !u.excluido) {
        html += botaoAcao('editar', 'Editar', u, '');
    }
    if (pode.excluir && !u.excluido && u.login !== loginAtual) {
        html += botaoAcao('excluir', 'Excluir', u, ' text-danger');
    }
    if (pode.historico) {
        html += botaoAcao('historico', 'Ver histórico', u, '');
    }
    return html;
}

function render() {
    const lista = ordenar(filtrados());
    corpo.innerHTML = '';
    if (lista.length === 0) {
        corpo.innerHTML = '<tr><td colspan="8" class="text-center text-secondary">' + cfg.msgFiltroVazio + '</td></tr>';
    } else {
        lista.forEach(function (u) {
            const tr = document.createElement('tr');
            const situacao = u.excluido
                ? '<span class="badge bg-red text-red-fg">Excluído</span>'
                : '<span class="badge bg-green text-green-fg">Ativo</span>';
            tr.innerHTML = '<td>' + u.nome + '</td><td>' + u.login + '</td><td>' + u.email + '</td>'
                + '<td><span class="badge bg-blue-lt">' + (u.perfilNome || '') + '</span></td>'
                + '<td>' + (u.generoDescricao || '') + '</td>'
                + '<td>' + dataBr(u.criadoEm) + '</td><td>' + situacao + '</td>'
                + '<td class="text-nowrap">' + acoes(u) + '</td>';
            corpo.appendChild(tr);
        });
    }
    rodape.textContent = lista.length + ' de ' + todos.length + ' usuário(s)';
}

document.getElementById('btnAplicarFiltro').addEventListener('click', render);
document.getElementById('btnLimparFiltro').addEventListener('click', function () {
    document.getElementById('filtroBusca').value = '';
    document.getElementById('filtroPerfil').value = '';
    document.getElementById('filtroSituacao').value = 'ATIVO';
    render();
});
document.querySelectorAll('#tabelaUsuarios th.sortable').forEach(function (th) {
    th.addEventListener('click', function () {
        const col = th.dataset.col;
        ordenacao = { col: col, asc: ordenacao.col === col ? !ordenacao.asc : true };
        render();
    });
});

corpo.addEventListener('click', function (e) {
    const btn = e.target.closest('button[data-acao]');
    if (!btn) return;
    const { acao, id, nome } = btn.dataset;
    if (acao === 'editar') abrirEdicao(id);
    if (acao === 'excluir') excluir(id, nome);
    if (acao === 'historico') abrirHistorico(id);
});

document.addEventListener(EVENTO_ALTERADO, carregar);

carregar();
