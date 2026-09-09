import { initModalForm } from './modal-form.js';

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

    document.querySelectorAll('.btnEditarVinculo').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const tr = e.target.closest('tr');
            if (tr && window.abrirModalEditarVinculo) {
                window.abrirModalEditarVinculo(tr);
            }
        });
    });

    document.querySelectorAll('.formExcluirVinculo').forEach(form => {
        form.addEventListener('submit', (e) => {
            const btn = form.querySelector('button[type="submit"]');
            const rotulo = btn ? btn.getAttribute('data-rotulo') : '';
            const categoria = btn ? btn.getAttribute('data-categoria') : '';

            const msg = `Confirma a exclusão do vínculo "${rotulo}" → "${categoria}"?`;
            if (!confirm(msg)) {
                e.preventDefault();
                return false;
            }
        });
    });
});
