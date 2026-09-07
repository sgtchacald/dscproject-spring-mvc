import { enviar } from '../comum/http.js';
import { toast, abrirModal, fecharModal } from '../comum/ui.js';

const form = () => document.getElementById('formAlterarSenha');

function limpar() {
    const f = form();
    f.querySelectorAll('.is-invalid').forEach((el) => el.classList.remove('is-invalid'));
    f.querySelectorAll('.invalid-feedback').forEach((el) => (el.textContent = ''));
}

function aplicarErros(erros) {
    Object.entries(erros || {}).forEach(([campo, msg]) => {
        const input = form().querySelector('[name="' + campo + '"]');
        const feedback = document.getElementById('erro-' + campo);
        if (input) input.classList.add('is-invalid');
        if (feedback) feedback.textContent = msg;
    });
}

export function abrirAlterarSenha(id, nome) {
    limpar();
    const f = form();
    f.reset();
    document.getElementById('alterarSenhaId').value = id;
    document.getElementById('modalAlterarSenhaTitulo').textContent =
        f.dataset.tituloTpl.replace('{0}', nome);
    abrirModal('modalAlterarSenha');
}

async function submeter(e) {
    e.preventDefault();
    limpar();
    const f = form();
    const id = document.getElementById('alterarSenhaId').value;
    try {
        const data = await enviar(f.dataset.urlBase + '/' + id + '/senha', 'PUT',
            new URLSearchParams(new FormData(f)));
        if (data.sucesso) {
            fecharModal('modalAlterarSenha');
            toast(data.mensagem || f.dataset.msgSucesso);
        } else {
            aplicarErros(data.errosCampos);
            aplicarErros(data.errosNegocio);
        }
    } catch (err) {
        toast(f.dataset.erroComunicacao, true);
    }
}

form().addEventListener('submit', submeter);
