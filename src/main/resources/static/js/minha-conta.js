import { enviar } from './comum/http.js';
import { toast } from './comum/ui.js';

const form = document.getElementById('formMinhaConta');
const alerta = document.getElementById('alertaMinhaConta');

function limpar() {
    alerta.style.display = 'none';
    form.querySelectorAll('.is-invalid').forEach((el) => el.classList.remove('is-invalid'));
    form.querySelectorAll('.invalid-feedback').forEach((el) => (el.textContent = ''));
}

function aplicarErros(erros) {
    Object.entries(erros || {}).forEach(([campo, msg]) => {
        const input = form.querySelector('[name="' + campo + '"]');
        const feedback = document.getElementById('erro-' + campo);
        if (input) input.classList.add('is-invalid');
        if (feedback) feedback.textContent = msg;
    });
}

form.addEventListener('submit', async function (e) {
    e.preventDefault();
    limpar();
    const btn = document.getElementById('btnSalvarMinhaConta');
    btn.disabled = true;
    try {
        const data = await enviar(form.dataset.url, 'PUT', new URLSearchParams(new FormData(form)));
        if (data.sucesso) {
            toast(data.mensagem || form.dataset.msgSucesso);
            document.getElementById('senha').value = '';
            document.getElementById('confirmacaoSenha').value = '';
        } else {
            aplicarErros(data.errosCampos);
            aplicarErros(data.errosNegocio);
        }
    } catch (err) {
        alerta.textContent = form.dataset.erroComunicacao;
        alerta.style.display = 'block';
    } finally {
        btn.disabled = false;
    }
});
