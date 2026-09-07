import { getJson, enviar } from '../comum/http.js';
import { toast } from '../comum/ui.js';
import { abrirNovo, abrirEdicao, EVENTO_ALTERADO } from './modal-form.js';
import './modal-catalogo.js';

const cfg = document.getElementById('dadosTelaPerfil').dataset;

const pode = {
    manter: !!document.querySelector('[data-perm="manter"]')
};

let todos = [];
let ordenacao = { col: 'nome', asc: true };

const corpo = document.getElementById('corpoTabelaPerfis');
const rodape = document.getElementById('rodapeContagemPerfis');

async function carregar() {
    todos = await getJson(cfg.urlDados);
    render();
}

function ordenar(lista) {
    const { col, asc } = ordenacao;
    return lista.slice().sort(function (a, b) {
        const va = a[col], vb = b[col];
        if (va === vb) return 0;
        return (va > vb ? 1 : -1) * (asc ? 1 : -1);
    });
}

// Ícones do Tabler (tabler.io/icons).
const ICONES = {
    editar: '<path d="M7 7h-1a2 2 0 0 0 -2 2v9a2 2 0 0 0 2 2h9a2 2 0 0 0 2 -2v-1" />'
        + '<path d="M20.385 6.585a2.1 2.1 0 0 0 -2.97 -2.97l-8.415 8.385v3h3l8.385 -8.415z" />'
        + '<path d="M16 5l3 3" />',
    excluir: '<path d="M4 7l16 0" /><path d="M10 11l0 6" /><path d="M14 11l0 6" />'
        + '<path d="M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2 -2l1 -12" />'
        + '<path d="M9 7v-3a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v3" />'
};

function botaoAcao(acao, rotulo, p, classeCor) {
    return '<button type="button" class="btn btn-action' + classeCor + '"'
        + ' data-acao="' + acao + '" data-id="' + p.id + '" data-nome="' + p.nome + '"'
        + ' title="' + rotulo + '" aria-label="' + rotulo + '">'
        + '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none"'
        + ' stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"'
        + ' class="icon" aria-hidden="true">' + ICONES[acao] + '</svg></button> ';
}

function acoes(p) {
    if (!pode.manter) return '';
    let html = botaoAcao('editar', cfg.labelEditar || 'Editar', p, '');
    if (!p.sistema && p.qtdUsuarios === 0) {
        html += botaoAcao('excluir', 'Excluir', p, ' text-danger');
    }
    return html;
}

function situacao(p) {
    const badges = [];
    if (p.sistema) {
        badges.push('<span class="badge bg-purple-lt">' + (cfg.labelBadgeSistema || 'Sistema') + '</span>');
    }
    badges.push('<span class="badge bg-green-lt">' + (cfg.labelBadgeAtivo || 'Ativo') + '</span>');
    return badges.join(' ');
}

function render() {
    const lista = ordenar(todos);
    corpo.innerHTML = '';
    if (lista.length === 0) {
        corpo.innerHTML = '<tr><td colspan="6" class="text-center text-secondary">'
            + (cfg.labelListaVazia || '') + '</td></tr>';
    } else {
        lista.forEach(function (p) {
            const tr = document.createElement('tr');
            tr.innerHTML = '<td>' + p.codigo + '</td><td>' + p.nome + '</td>'
                + '<td>' + p.qtdPermissoes + '</td><td>' + p.qtdUsuarios + '</td>'
                + '<td>' + situacao(p) + '</td>'
                + '<td class="text-nowrap">' + acoes(p) + '</td>';
            corpo.appendChild(tr);
        });
    }
    rodape.textContent = lista.length + ' perfil(is)';
}

document.querySelectorAll('#tabelaPerfis th.sortable').forEach(function (th) {
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
});

async function excluir(id, nome) {
    if (!window.confirm((cfg.msgConfirmaExclusao || 'Excluir "{0}"?').replace('{0}', nome))) return;
    try {
        const data = await enviar(cfg.urlExcluir + '/' + id, 'DELETE');
        if (data.sucesso) {
            toast(data.mensagem || 'OK');
            document.dispatchEvent(new CustomEvent(EVENTO_ALTERADO));
        } else {
            toast(data.mensagem || 'Erro', true);
        }
    } catch (err) {
        toast(cfg.erroComunicacao, true);
    }
}

const btnNovo = document.getElementById('btnNovoPerfil');
if (btnNovo) btnNovo.addEventListener('click', abrirNovo);

document.addEventListener(EVENTO_ALTERADO, carregar);

carregar();
