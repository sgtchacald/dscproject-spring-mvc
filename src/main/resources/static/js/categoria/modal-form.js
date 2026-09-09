export function initModalForm() {
    const modalEl = document.getElementById('modalCategoriaForm');
    if (!modalEl) return;

    const modal = window.bootstrap ? new bootstrap.Modal(modalEl) : null;
    const form = document.getElementById('formCategoria');
    const inputHttp = document.getElementById('categoriaHttpMethod');
    const inputId = document.getElementById('categoriaId');
    const inputSistema = document.getElementById('categoriaSistema');
    const inputCodigo = document.getElementById('categoriaCodigo');
    const inputNome = document.getElementById('categoriaNome');
    const selectAplicaA = document.getElementById('categoriaAplicaA');
    const inputCor = document.getElementById('categoriaCor');
    const inputIcone = document.getElementById('categoriaIcone');
    const checkAtivo = document.getElementById('categoriaAtivo');
    const blocoAtivo = document.getElementById('blocoAtivo');
    const tituloModal = document.getElementById('modalCategoriaTitulo');
    const hintCodigoSistema = document.getElementById('hintCodigoSistema');
    const avisoUso = document.getElementById('avisoCategoriaEmUso');
    const textoAvisoUso = document.getElementById('textoAvisoCategoriaEmUso');
    const previewBox = document.getElementById('previewIconeCor');

    // Normalização em tempo real do campo Código
    if (inputCodigo) {
        inputCodigo.addEventListener('input', () => {
            const original = inputCodigo.value;
            const norm = original.normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '')
                .replace(/[\s-]+/g, '_')
                .replace(/[^a-zA-Z0-9_]/g, '')
                .toUpperCase();
            if (original !== norm) {
                inputCodigo.value = norm;
            }
            atualizarPreview();
        });
    }

    function atualizarPreview() {
        if (!previewBox) return;
        const cor = inputCor ? inputCor.value : '';
        const icone = inputIcone ? inputIcone.value.trim() : '';

        if (cor || icone) {
            previewBox.style.display = 'inline-flex';
            if (cor) {
                previewBox.style.backgroundColor = cor;
            }
        } else {
            previewBox.style.display = 'none';
        }
    }

    if (inputCor) inputCor.addEventListener('input', atualizarPreview);
    if (inputIcone) inputIcone.addEventListener('input', atualizarPreview);

    window.abrirModalNovaCategoria = function() {
        if (!form) return;
        form.action = '/categorias/inserir';
        if (inputHttp) inputHttp.value = 'POST';
        if (inputId) inputId.value = '';
        if (inputSistema) inputSistema.value = 'false';
        if (inputCodigo) {
            inputCodigo.value = '';
            inputCodigo.readOnly = false;
        }
        if (inputNome) inputNome.value = '';
        if (selectAplicaA) selectAplicaA.value = '';
        if (inputCor) inputCor.value = '#206bc4';
        if (inputIcone) inputIcone.value = '';
        if (checkAtivo) checkAtivo.checked = true;
        if (blocoAtivo) blocoAtivo.style.display = 'none';
        if (hintCodigoSistema) hintCodigoSistema.style.display = 'none';
        if (avisoUso) avisoUso.style.display = 'none';
        if (tituloModal) tituloModal.textContent = 'Nova categoria';
        atualizarPreview();
        if (modal) modal.show();
    };

    window.abrirModalEditarCategoria = function(tr) {
        if (!form || !tr) return;
        const id = tr.getAttribute('data-id');
        const codigo = tr.getAttribute('data-codigo');
        const nome = tr.getAttribute('data-nome');
        const aplicaA = tr.getAttribute('data-aplica-a');
        const cor = tr.getAttribute('data-cor') || '#206bc4';
        const icone = tr.getAttribute('data-icone') || '';
        const sistema = tr.getAttribute('data-sistema') === 'true';
        const ativo = tr.getAttribute('data-ativo') === 'true';
        const qtdUso = parseInt(tr.getAttribute('data-qtd-uso') || '0', 10);

        form.action = '/categorias/editar/' + id;
        if (inputHttp) inputHttp.value = 'PUT';
        if (inputId) inputId.value = id;
        if (inputSistema) inputSistema.value = sistema ? 'true' : 'false';
        if (inputCodigo) {
            inputCodigo.value = codigo;
            inputCodigo.readOnly = sistema;
        }
        if (inputNome) inputNome.value = nome;
        if (selectAplicaA) selectAplicaA.value = aplicaA;
        if (inputCor) inputCor.value = cor;
        if (inputIcone) inputIcone.value = icone;
        if (checkAtivo) checkAtivo.checked = ativo;
        if (blocoAtivo) blocoAtivo.style.display = 'block';

        if (hintCodigoSistema) {
            hintCodigoSistema.style.display = sistema ? 'block' : 'none';
        }

        if (avisoUso && textoAvisoUso) {
            if (qtdUso > 0) {
                textoAvisoUso.textContent = `Esta categoria é usada por ${qtdUso} lançamento(s). Ela não pode ser excluída; você pode desativá-la.`;
                avisoUso.style.display = 'block';
            } else {
                avisoUso.style.display = 'none';
            }
        }

        if (tituloModal) tituloModal.textContent = 'Editar categoria';
        atualizarPreview();
        if (modal) modal.show();
    };
}
