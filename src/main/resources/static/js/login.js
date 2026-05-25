const formCadastro = document.getElementById('formCadastro');
const alertaErro   = document.getElementById('alertaErroModal');
const alertaTexto  = document.getElementById('alertaErroTexto');

function fecharAlertaErro() {
    alertaErro.style.display = 'none';
    alertaTexto.textContent  = '';
}

function limparErros() {
    fecharAlertaErro();
    formCadastro.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    formCadastro.querySelectorAll('.invalid-feedback').forEach(el => el.textContent = '');
}

function aplicarErros(erros) {
    Object.entries(erros).forEach(([campo, msg]) => {
        const input    = document.getElementById(campo) || document.querySelector('[name="' + campo + '"]');
        const feedback = document.getElementById('erro-' + campo);
        if (input)    input.classList.add('is-invalid');
        if (feedback) feedback.textContent = msg;
    });
}

formCadastro.addEventListener('submit', async function (e) {
    e.preventDefault();
    limparErros();

    const btn = document.getElementById('btnCadastrar');
    btn.disabled = true;

    try {
        const resp = await fetch(this.action, {
            method: 'POST',
            body: new URLSearchParams(new FormData(this)),
            headers: { 'Accept': 'application/json' }
        });

        const data = await resp.json();

        if (data.sucesso) {
            window.tabler.Modal.getOrCreateInstance(document.getElementById('modalCadastro')).hide();
            this.reset();
            mostrarToast('toastSucesso');
        } else {
            // Erros de campo (Bean Validation) — somente inline
            if (data.errosCampos) aplicarErros(data.errosCampos);

            // Erros de negócio (duplicidade, senha) — somente inline
            if (data.errosNegocio && Object.keys(data.errosNegocio).length) {
                aplicarErros(data.errosNegocio);
            }
        }
    } catch (err) {
        alertaTexto.textContent = formCadastro.dataset.erroComunicacao;
        alertaErro.style.display = 'block';
    } finally {
        btn.disabled = false;
    }
});

document.getElementById('modalCadastro').addEventListener('hidden.bs.modal', function () {
    formCadastro.reset();
    limparErros();
});

if (document.getElementById('js-auto-logout')) {
    mostrarToast('toastLogout');
}