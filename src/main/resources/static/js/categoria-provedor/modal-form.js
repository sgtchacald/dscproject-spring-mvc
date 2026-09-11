import { abrirModal } from '../comum/ui.js';

export function initModalForm() {
    const modalEl = document.getElementById('modalVinculoForm');
    if (!modalEl) return;

    const form = document.getElementById('formVinculo');
    const inputHttp = document.getElementById('vinculoHttpMethod');
    const inputId = document.getElementById('vinculoId');
    const selectProvedor = document.getElementById('vinculoProvedorId');
    const inputRotulo = document.getElementById('vinculoRotuloExterno');
    const selectCategoria = document.getElementById('vinculoCategoriaId');
    const tituloModal = document.getElementById('modalVinculoTitulo');

    window.abrirModalNovoVinculo = function() {
        if (!form) return;
        form.action = '/categorias-provedor/inserir';
        if (inputHttp) inputHttp.value = 'POST';
        if (inputId) inputId.value = '';
        if (selectProvedor) selectProvedor.value = '';
        if (inputRotulo) inputRotulo.value = '';
        if (selectCategoria) selectCategoria.value = '';
        if (tituloModal) tituloModal.textContent = 'Novo vínculo';
        abrirModal('modalVinculoForm');
    };

    window.abrirModalEditarVinculo = function(tr) {
        if (!form || !tr) return;
        const id = tr.getAttribute('data-id');
        const provedorId = tr.getAttribute('data-provedor-id');
        const rotulo = tr.getAttribute('data-rotulo-externo');
        const categoriaId = tr.getAttribute('data-categoria-id');

        form.action = '/categorias-provedor/editar/' + id;
        if (inputHttp) inputHttp.value = 'PUT';
        if (inputId) inputId.value = id;
        if (selectProvedor) selectProvedor.value = provedorId;
        if (inputRotulo) inputRotulo.value = rotulo;
        if (selectCategoria) selectCategoria.value = categoriaId;
        if (tituloModal) tituloModal.textContent = 'Editar vínculo';
        abrirModal('modalVinculoForm');
    };

    // Reabre o modal automaticamente se houve erro de validação no servidor
    const dadosTela = document.getElementById('dadosTela');
    if (dadosTela && dadosTela.dataset.abrirModalForm === 'true') {
        abrirModal('modalVinculoForm');
    }
}
