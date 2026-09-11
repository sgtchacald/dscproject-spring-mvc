import { initModalForm } from './modal-form.js';
import { semAcento } from '../comum/ui.js';

document.addEventListener('DOMContentLoaded', () => {
    initModalForm();

    const btnNovo = document.getElementById('btnNovoVinculo');
    if (btnNovo) {
        btnNovo.addEventListener('click', () => {
            if (window.abrirModalNovoVinculo) {
                window.abrirModalNovoVinculo();
            }
        });
    }

    // Delegação de eventos para Edição
    document.addEventListener('click', (e) => {
        const btnEdit = e.target.closest('.btnEditarVinculo');
        if (btnEdit) {
            const tr = btnEdit.closest('tr');
            if (tr && window.abrirModalEditarVinculo) {
                window.abrirModalEditarVinculo(tr);
            }
        }
    });

    // Delegação de eventos para Exclusão
    document.addEventListener('submit', (e) => {
        const form = e.target.closest('.formExcluirVinculo');
        if (form) {
            const btn = form.querySelector('button[type="submit"]');
            const rotulo = btn ? btn.getAttribute('data-rotulo') : '';
            const categoria = btn ? btn.getAttribute('data-categoria') : '';

            const msg = `Confirma a exclusão do vínculo "${rotulo}" → "${categoria}"?`;
            if (!confirm(msg)) {
                e.preventDefault();
                return false;
            }
        }
    });

    // Gerenciador de Grid Client-side (Ordenação, Busca rápida e Paginação)
    initGridVinculosClientSide();
});

function initGridVinculosClientSide() {
    const tbody = document.getElementById('corpoTabelaVinculos');
    if (!tbody) return;

    const todasLinhas = Array.from(tbody.querySelectorAll('tr[data-id]'));
    if (todasLinhas.length === 0) return;

    const selectQtd = document.getElementById('itensPorPagina');
    const inputBusca = document.getElementById('tabelaBuscaRapida');
    const infoPaginacao = document.getElementById('infoPaginacao');
    const paginacaoControles = document.getElementById('paginacaoControles');
    const ths = document.querySelectorAll('#tabelaVinculos th.sortable');

    let itensPorPagina = parseInt(selectQtd ? selectQtd.value : '10', 10);
    let paginaAtual = 1;
    let ordenacao = { col: 'provedor', asc: true };
    let termoBusca = '';

    function obterValorColuna(tr, col) {
        switch (col) {
            case 'provedor': return tr.getAttribute('data-provedor-nome') || '';
            case 'rotulo': return tr.getAttribute('data-rotulo-externo') || '';
            case 'categoria': return tr.getAttribute('data-categoria-nome') || '';
            default: return tr.textContent.trim();
        }
    }

    function render() {
        // Filtro
        const filtradas = todasLinhas.filter(tr => {
            if (!termoBusca) return true;
            const prov = semAcento(tr.getAttribute('data-provedor-nome') || '');
            const rot = semAcento(tr.getAttribute('data-rotulo-externo') || '');
            const cat = semAcento(tr.getAttribute('data-categoria-nome') || '');
            return prov.includes(termoBusca) || rot.includes(termoBusca) || cat.includes(termoBusca);
        });

        // Ordenação
        filtradas.sort((a, b) => {
            const va = obterValorColuna(a, ordenacao.col);
            const vb = obterValorColuna(b, ordenacao.col);
            let cmp = String(va).localeCompare(String(vb), 'pt-BR', { sensitivity: 'base' });
            if (cmp === 0 && ordenacao.col === 'provedor') {
                const ra = obterValorColuna(a, 'rotulo');
                const rb = obterValorColuna(b, 'rotulo');
                cmp = String(ra).localeCompare(String(rb), 'pt-BR', { sensitivity: 'base' });
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
                ? 'Nenhum vínculo encontrado'
                : `Mostrando ${inicio + 1} a ${fim} de ${total} vínculo${total > 1 ? 's' : ''}`;
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
