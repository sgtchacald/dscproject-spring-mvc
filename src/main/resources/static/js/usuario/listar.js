import { getJson } from '../comum/http.js';
import { semAcento, dataBr } from '../comum/ui.js';
import { abrirEdicao, excluir, EVENTO_ALTERADO } from './modal-form.js';
import { abrirHistorico } from './modal-historico.js';
import { abrirAlterarSenha } from './modal-alterar-senha.js';

const cfg = document.getElementById('dadosTela').dataset;
const loginAtual = cfg.loginAtual || '';

const pode = {
    editar: !!document.querySelector('[data-perm="editar"]'),
    excluir: !!document.querySelector('[data-perm="excluir"]'),
    historico: !!document.querySelector('[data-perm="historico"]'),
    alterarSenha: !!document.querySelector('[data-perm="alterar-senha"]')
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

// Ícones do Lucide (lucide.dev) — traço/tamanho padronizados.
const ICONES = {
    editar: '<path d="M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z"/><path d="m15 5 4 4"/>',
    excluir: '<path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" x2="10" y1="11" y2="17"/><line x1="14" x2="14" y1="11" y2="17"/>',
    historico: '<path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M12 7v5l4 2"/>',
    'alterar-senha': '<path d="M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z"/><circle cx="16.5" cy="7.5" r=".5" fill="currentColor"/>'
};

function botaoAcao(acao, rotulo, u, classeCor) {
    return '<button type="button" class="btn btn-action' + classeCor + '"'
        + ' data-acao="' + acao + '" data-id="' + u.id + '"'
        + (acao === 'excluir' || acao === 'alterar-senha' ? ' data-nome="' + u.nome + '"' : '')
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
    if (pode.alterarSenha && !u.excluido) {
        html += botaoAcao('alterar-senha', 'Alterar senha', u, '');
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
    if (acao === 'alterar-senha') abrirAlterarSenha(id, nome);
    if (acao === 'historico') abrirHistorico(id);
});

document.addEventListener(EVENTO_ALTERADO, carregar);

carregar();
