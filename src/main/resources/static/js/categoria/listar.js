import { initModalFiltro } from './modal-filtro.js';
import { initModalForm } from './modal-form.js';

document.addEventListener('DOMContentLoaded', () => {
    initModalFiltro();
    initModalForm();

    const btnNovo = document.getElementById('btnNovaCategoria');
    if (btnNovo) {
        btnNovo.addEventListener('click', () => {
            if (window.abrirModalNovaCategoria) {
                window.abrirModalNovaCategoria();
            }
        });
    }

    document.querySelectorAll('.btnEditarCategoria').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const tr = e.target.closest('tr');
            if (tr && window.abrirModalEditarCategoria) {
                window.abrirModalEditarCategoria(tr);
            }
        });
    });

    document.querySelectorAll('.formExcluirCategoria').forEach(form => {
        form.addEventListener('submit', (e) => {
            const btn = form.querySelector('button[type="submit"]');
            const nome = btn ? btn.getAttribute('data-nome') : '';
            const sistema = btn ? btn.getAttribute('data-sistema') === 'true' : false;

            if (sistema) {
                alert('Categorias de sistema não podem ser excluídas.');
                e.preventDefault();
                return false;
            }

            const msg = `Confirma a exclusão da categoria "${nome}"?`;
            if (!confirm(msg)) {
                e.preventDefault();
                return false;
            }
        });
    });
});
