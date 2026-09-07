const form = document.getElementById('formCadastro');
const alertaErro = document.getElementById('alertaErro');
const alertaErroTexto = document.getElementById('alertaErroTexto');

function limparErros() {
    alertaErro.style.display = 'none';
    form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    form.querySelectorAll('.invalid-feedback').forEach(el => el.textContent = '');
}

function aplicarErros(erros) {
    Object.entries(erros || {}).forEach(([campo, msg]) => {
        const input = document.getElementById(campo) || form.querySelector('[name="' + campo + '"]');
        const feedback = document.getElementById('erro-' + campo);
        if (input) input.classList.add('is-invalid');
        if (feedback) feedback.textContent = msg;
    });
}

form.addEventListener('submit', async function (e) {
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
            mostrarToast('toastSucesso');
            this.reset();
            setTimeout(() => window.location.href = this.dataset.loginUrl, 1500);
        } else {
            aplicarErros(data.errosCampos);
            aplicarErros(data.errosNegocio);
        }
    } catch (err) {
        alertaErroTexto.textContent = form.dataset.erroComunicacao;
        alertaErro.style.display = 'block';
    } finally {
        btn.disabled = false;
    }
});
