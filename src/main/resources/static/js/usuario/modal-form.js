import { getJson, enviar } from '../comum/http.js';
import { toast, abrirModal, fecharModal } from '../comum/ui.js';

// Emitido após inserir, editar ou excluir; listar.js recarrega o grid ao ouvir.
export const EVENTO_ALTERADO = 'usuario:alterado';

const cfg = () => document.getElementById('dadosTela').dataset;
const form = () => document.getElementById('formUsuario');

function limparForm() {
    const f = form();
    f.reset();
    f.querySelectorAll('.is-invalid').forEach((el) => el.classList.remove('is-invalid'));
    f.querySelectorAll('.invalid-feedback').forEach((el) => (el.textContent = ''));
    document.getElementById('alertaFormUsuario').style.display = 'none';
}

function prepararNovo() {
    limparForm();
    document.getElementById('usuId').value = '';
    document.getElementById('modalUsuarioTitulo').textContent = 'Novo usuário';
    document.getElementById('hintSenha').style.display = 'none';
}

export async function abrirEdicao(id) {
    limparForm();
    const u = await getJson(cfg().urlBuscar + '/' + id);
    document.getElementById('usuId').value = u.id;
    document.getElementById('nome').value = u.nome;
    document.getElementById('genero').value = u.genero;
    document.getElementById('nascimento').value = u.nascimento || '';
    document.getElementById('email').value = u.email;
    document.getElementById('login').value = u.login;
    document.getElementById('perfilCodigo').value = u.perfilCodigo;
    document.getElementById('modalUsuarioTitulo').textContent = 'Editar usuário';
    document.getElementById('hintSenha').style.display = 'block';
    abrirModal('modalUsuario');
}

export async function excluir(id, nome) {
    if (!window.confirm(cfg().msgConfirmaExclusao.replace('{0}', nome))) return;
    const data = await enviar(cfg().urlExcluir + '/' + id, 'DELETE');
    if (data.sucesso) {
        toast(cfg().msgExcluido);
        document.dispatchEvent(new CustomEvent(EVENTO_ALTERADO));
    } else {
        toast(data.mensagem || 'Erro', true);
    }
}

function aplicarErros(erros) {
    Object.entries(erros || {}).forEach(([campo, msg]) => {
        const input = form().querySelector('[name="' + campo + '"]');
        const feedback = document.getElementById('erro-' + campo);
        if (input) input.classList.add('is-invalid');
        if (feedback) feedback.textContent = msg;
        if (campo === 'geral') {
            const a = document.getElementById('alertaFormUsuario');
            a.textContent = msg;
            a.style.display = 'block';
        }
    });
}

async function submeter(e) {
    e.preventDefault();
    const f = form();
    f.querySelectorAll('.is-invalid').forEach((el) => el.classList.remove('is-invalid'));
    f.querySelectorAll('.invalid-feedback').forEach((el) => (el.textContent = ''));
    document.getElementById('alertaFormUsuario').style.display = 'none';

    const id = document.getElementById('usuId').value;
    const url = id ? (f.dataset.urlEditar + '/' + id) : f.dataset.urlInserir;
    try {
        const data = await enviar(url, id ? 'PUT' : 'POST', new URLSearchParams(new FormData(f)));
        if (data.sucesso) {
            fecharModal('modalUsuario');
            toast(id ? cfg().msgAtualizado : cfg().msgCadastrado);
            document.dispatchEvent(new CustomEvent(EVENTO_ALTERADO));
        } else {
            aplicarErros(data.errosCampos);
            aplicarErros(data.errosNegocio);
        }
    } catch (err) {
        const a = document.getElementById('alertaFormUsuario');
        a.textContent = f.dataset.erroComunicacao;
        a.style.display = 'block';
    }
}

const btnNovo = document.getElementById('btnNovo');
if (btnNovo) btnNovo.addEventListener('click', prepararNovo);
form().addEventListener('submit', submeter);
