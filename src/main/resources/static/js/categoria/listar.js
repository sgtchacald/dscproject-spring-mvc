import { initModalFiltro } from './modal-filtro.js';
import { initModalForm, obterIconeHtml } from './modal-form.js';
import { semAcento } from '../comum/ui.js';

document.addEventListener('DOMContentLoaded', () => {
    initModalFiltro();
    initModalForm();

    // Renderiza os ícones SVG nas linhas da tabela
    document.querySelectorAll('.cat-icone-display').forEach(span => {
        const icone = span.getAttribute('data-icone');
        if (icone) {
            span.innerHTML = obterIconeHtml(icone, 16);
        }
    });

    const btnNovo = document.getElementById('btnNovaCategoria');
    if (btnNovo) {
        btnNovo.addEventListener('click', () => {
            if (window.abrirModalNovaCategoria) {
                window.abrirModalNovaCategoria();
            }
        });
    }

    // Delegação de eventos para Edição
    document.addEventListener('click', (e) => {
        const btnEdit = e.target.closest('.btnEditarCategoria');
        if (btnEdit) {
            const tr = btnEdit.closest('tr');
            if (tr && window.abrirModalEditarCategoria) {
                window.abrirModalEditarCategoria(tr);
            }
        }
    });

    // Delegação de eventos para Exclusão
    document.addEventListener('submit', (e) => {
        const form = e.target.closest('.formExcluirCategoria');
        if (form) {
            const btn = form.querySelector('button[type="submit"]');
            const nome = btn ? btn.getAttribute('data-nome') : '';
            const sistema = btn ? btn.getAttribute('data-sistema') === 'true' : false;

            if (sistema) {
                alert('Categorias de sistema não podem ser excluídas nem ter o código alterado.');
                e.preventDefault();
                return false;
            }

            const msg = `Confirma a exclusão da categoria "${nome}"?`;
            if (!confirm(msg)) {
                e.preventDefault();
                return false;
            }
        }
    });

    // Gerenciador de Grid Client-side (Ordenação, Busca rápida e Paginação)
    initGridClientSide();
});

function initGridClientSide() {
    const tbody = document.getElementById('corpoTabelaCategorias');
    if (!tbody) return;

    const todasLinhas = Array.from(tbody.querySelectorAll('tr[data-id]'));
    if (todasLinhas.length === 0) return;

    const selectQtd = document.getElementById('itensPorPagina');
    const inputBusca = document.getElementById('tabelaBuscaRapida');
    const infoPaginacao = document.getElementById('infoPaginacao');
    const paginacaoControles = document.getElementById('paginacaoControles');
    const ths = document.querySelectorAll('#tabelaCategorias th.sortable');

    let itensPorPagina = parseInt(selectQtd ? selectQtd.value : '10', 10);
    let paginaAtual = 1;
    let ordenacao = { col: 'nome', asc: true };
    let termoBusca = '';

    function obterValorColuna(tr, col) {
        switch (col) {
            case 'codigo': return tr.getAttribute('data-codigo') || '';
            case 'nome': return tr.getAttribute('data-nome') || '';
            case 'aplicaA': return tr.getAttribute('data-aplica-a') || '';
            case 'qtdUso': return parseInt(tr.getAttribute('data-qtd-uso') || '0', 10);
            case 'sistema': return tr.getAttribute('data-sistema') === 'true' ? 1 : 0;
            case 'situacao':
                if (tr.querySelector('.badge.bg-danger-lt')) return 3;
                if (tr.getAttribute('data-ativo') === 'true') return 1;
                return 2;
            default: return tr.textContent.trim();
        }
    }

    function render() {
        // Filtro
        const filtradas = todasLinhas.filter(tr => {
            if (!termoBusca) return true;
            const cod = semAcento(tr.getAttribute('data-codigo') || '');
            const nome = semAcento(tr.getAttribute('data-nome') || '');
            return cod.includes(termoBusca) || nome.includes(termoBusca);
        });

        // Ordenação
        filtradas.sort((a, b) => {
            const va = obterValorColuna(a, ordenacao.col);
            const vb = obterValorColuna(b, ordenacao.col);
            let cmp = 0;
            if (typeof va === 'number' && typeof vb === 'number') {
                cmp = va - vb;
            } else {
                cmp = String(va).localeCompare(String(vb), 'pt-BR', { sensitivity: 'base' });
            }
            return ordenacao.asc ? cmp : -cmp;
        });

        const total = filtradas.length;
        const totalPaginas = Math.max(1, Math.ceil(total / itensPorPagina));
        if (paginaAtual > totalPaginas) paginaAtual = totalPaginas;

        const inicio = (paginaAtual - 1) * itensPorPagina;
        const fim = Math.min(inicio + itensPorPagina, total);

        // Oculta todas
        todasLinhas.forEach(tr => tr.style.display = 'none');

        // Reanexa ordenadas na página visível
        filtradas.slice(inicio, fim).forEach(tr => {
            tr.style.display = '';
            tbody.appendChild(tr);
        });

        // Atualiza rodapé
        if (infoPaginacao) {
            infoPaginacao.textContent = total === 0
                ? 'Nenhuma categoria encontrada'
                : `Mostrando ${inicio + 1} a ${fim} de ${total} categoria${total > 1 ? 's' : ''}`;
        }

        // Atualiza paginação
        if (paginacaoControles) {
            paginacaoControles.innerHTML = '';
            if (totalPaginas > 1) {
                // Anterior
                const liPrev = document.createElement('li');
                liPrev.className = `page-item ${paginaAtual === 1 ? 'disabled' : ''}`;
                liPrev.innerHTML = `<a class="page-link" href="javascript:void(0)">&laquo;</a>`;
                if (paginaAtual > 1) {
                    liPrev.addEventListener('click', () => { paginaAtual--; render(); });
                }
                paginacaoControles.appendChild(liPrev);

                for (let p = 1; p <= totalPaginas; p++) {
                    const li = document.createElement('li');
                    li.className = `page-item ${p === paginaAtual ? 'active' : ''}`;
                    li.innerHTML = `<a class="page-link" href="javascript:void(0)">${p}</a>`;
                    const numPagina = p;
                    li.addEventListener('click', () => { paginaAtual = numPagina; render(); });
                    paginacaoControles.appendChild(li);
                }

                // Próximo
                const liNext = document.createElement('li');
                liNext.className = `page-item ${paginaAtual === totalPaginas ? 'disabled' : ''}`;
                liNext.innerHTML = `<a class="page-link" href="javascript:void(0)">&raquo;</a>`;
                if (paginaAtual < totalPaginas) {
                    liNext.addEventListener('click', () => { paginaAtual++; render(); });
                }
                paginacaoControles.appendChild(liNext);
            }
        }
    }

    if (selectQtd) {
        selectQtd.addEventListener('change', () => {
            itensPorPagina = parseInt(selectQtd.value, 10);
            paginaAtual = 1;
            render();
        });
    }

    if (inputBusca) {
        inputBusca.addEventListener('input', () => {
            termoBusca = semAcento(inputBusca.value.trim());
            paginaAtual = 1;
            render();
        });
    }

    ths.forEach(th => {
        th.addEventListener('click', () => {
            const col = th.getAttribute('data-col');
            if (ordenacao.col === col) {
                ordenacao.asc = !ordenacao.asc;
            } else {
                ordenacao.col = col;
                ordenacao.asc = true;
            }

            ths.forEach(h => {
                h.classList.remove('table-sort-asc', 'table-sort-desc');
            });
            th.classList.add(ordenacao.asc ? 'table-sort-asc' : 'table-sort-desc');
            render();
        });
    });

    render();
}
