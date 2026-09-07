const formSolicitar = document.getElementById('formSolicitar');
const formConfirmar = document.getElementById('formConfirmar');
const info = document.getElementById('mensagemInfo');
const erro = document.getElementById('mensagemErro');

function mostrar(el, texto, tipo) {
    (tipo === 'erro' ? erro : info).style.display = 'block';
    (tipo === 'erro' ? erro : info).textContent = texto;
    (tipo === 'erro' ? info : erro).style.display = 'none';
}

function limparCampos(form) {
    form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    form.querySelectorAll('.invalid-feedback').forEach(el => el.textContent = '');
}

function aplicarErros(form, erros) {
    Object.entries(erros || {}).forEach(([campo, msg]) => {
        const input = form.querySelector('[name="' + campo + '"]');
        const feedback = document.getElementById('erro-' + campo);
        if (input) input.classList.add('is-invalid');
        if (feedback) feedback.textContent = msg;
    });
}

if (formSolicitar) {
    formSolicitar.addEventListener('submit', async function (e) {
        e.preventDefault();
        limparCampos(this);
        const btn = document.getElementById('btnEnviarLink');
        btn.disabled = true;
        try {
            const resp = await fetch(this.action, {
                method: 'POST',
                body: new URLSearchParams(new FormData(this)),
                headers: { 'Accept': 'application/json' }
            });
            const data = await resp.json();
            if (data.sucesso) {
                mostrar(info, data.mensagem, 'info');
                this.reset();
            } else {
                aplicarErros(this, data.errosCampos);
            }
        } catch (err) {
            mostrar(erro, this.dataset.erroComunicacao, 'erro');
        } finally {
            btn.disabled = false;
        }
    });
}

if (formConfirmar) {
    formConfirmar.addEventListener('submit', async function (e) {
        e.preventDefault();
        limparCampos(this);
        const btn = document.getElementById('btnDefinirSenha');
        btn.disabled = true;
        try {
            const resp = await fetch(this.action, {
                method: 'POST',
                body: new URLSearchParams(new FormData(this)),
                headers: { 'Accept': 'application/json' }
            });
            const data = await resp.json();
            if (data.sucesso) {
                mostrar(info, data.mensagem, 'info');
                setTimeout(() => window.location.href = this.dataset.loginUrl, 1500);
            } else if (data.errosCampos || data.errosNegocio) {
                aplicarErros(this, data.errosCampos);
                aplicarErros(this, data.errosNegocio);
            } else {
                mostrar(erro, data.mensagem, 'erro');
            }
        } catch (err) {
            mostrar(erro, this.dataset.erroComunicacao, 'erro');
        } finally {
            btn.disabled = false;
        }
    });
}
