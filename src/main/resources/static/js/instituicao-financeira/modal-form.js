import { getJson, enviar } from '../comum/http.js';
import { toast, abrirModal, fecharModal } from '../comum/ui.js';

export const EVENTO_ALTERADO = 'instituicao:alterada';

const cfg = () => document.getElementById('dadosTelaInstituicao').dataset;
const form = () => document.getElementById('formInstituicao');

let instituicaoEmEdicao = null;

function limparErros() {
    const f = form();
    f.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    f.querySelectorAll('.invalid-feedback').forEach(el => (el.textContent = ''));
    const alerta = document.getElementById('alertaFormInstituicao');
    if (alerta) {
        alerta.style.display = 'none';
        alerta.textContent = '';
    }
}

export function abrirNovo() {
    instituicaoEmEdicao = null;
    const f = form();
    f.reset();
    limparErros();

    document.getElementById('instituicaoId').value = '';
    document.getElementById('tituloModalInstituicao').textContent = cfg().labelNovo || 'Nova instituição';
    document.getElementById('instituicaoNome').disabled = false;
    document.getElementById('instituicaoCodigo').disabled = false;
    document.getElementById('grupoAtiva').style.display = 'none';
    document.getElementById('avisoInstituicao').style.display = 'none';

    abrirModal('modalInstituicao');
}

export async function abrirEdicao(id) {
    limparErros();
    try {
        const dados = await getJson(`${cfg().urlBuscar}/${id}`);
        instituicaoEmEdicao = dados;

        document.getElementById('instituicaoId').value = dados.id;
        document.getElementById('tituloModalInstituicao').textContent = cfg().labelEditar || 'Editar instituição';
        document.getElementById('instituicaoNome').value = dados.nome || '';
        document.getElementById('instituicaoCodigo').value = dados.codigo || '';
        document.getElementById('instituicaoTipo').value = dados.tipo || '';

        const chkAtiva = document.getElementById('instituicaoAtiva');
        if (chkAtiva) {
            chkAtiva.checked = !!dados.ativo;
        }
        document.getElementById('grupoAtiva').style.display = 'block';

        const campoNome = document.getElementById('instituicaoNome');
        const campoCodigo = document.getElementById('instituicaoCodigo');
        const aviso = document.getElementById('avisoInstituicao');
        const avisoTxt = document.getElementById('avisoInstituicaoTexto');

        if (dados.sistema) {
            campoNome.disabled = true;
            campoCodigo.disabled = true;
            avisoTxt.textContent = cfg().labelAvisoSistema || 'Instituição da lista-base do sistema: nome e código não podem ser alterados e ela não pode ser excluída.';
            aviso.style.display = 'flex';
        } else {
            campoNome.disabled = false;
            campoCodigo.disabled = false;
            if (dados.qtdUso > 0) {
                const modelo = cfg().labelAvisoEmUso || 'Esta instituição é usada por {0} cadastro(s). Ela não pode ser excluída; você pode desativá-la.';
                avisoTxt.textContent = modelo.replace('{0}', dados.qtdUso);
                aviso.style.display = 'flex';
            } else {
                aviso.style.display = 'none';
            }
        }

        abrirModal('modalInstituicao');
    } catch (err) {
        toast('Erro ao carregar dados da instituição.', true);
    }
}

export async function excluir(id, nome) {
    const template = cfg().msgConfirmaExclusao || 'Confirma a exclusão da instituição financeira "{0}"?';
    const msg = template.replace('{0}', nome);
    if (!confirm(msg)) {
        return;
    }

    try {
        const resp = await enviar(`${cfg().urlExcluir}/${id}`, 'DELETE');
        if (resp.sucesso) {
            toast(resp.mensagem || 'Instituição financeira excluída com sucesso.', false);
            document.dispatchEvent(new CustomEvent(EVENTO_ALTERADO));
        } else {
            const erro = resp.mensagem || 'Não foi possível excluir a instituição.';
            if (resp.errosNegocio && resp.errosNegocio['geral'] && resp.errosNegocio['geral'].includes('em uso')) {
                const oferta = cfg().msgDesativarOferta || 'Deseja desativar esta instituição agora?';
                if (confirm(`${erro}\n\n${oferta}`)) {
                    await desativar(id);
                }
            } else {
                toast(erro, true);
            }
        }
    } catch (e) {
        toast('Erro de comunicação ao excluir.', true);
    }
}

export async function desativar(id) {
    try {
        const resp = await enviar(`${cfg().urlDesativar}/${id}`, 'PUT');
        if (resp.sucesso) {
            toast(resp.mensagem || 'Instituição desativada com sucesso.', false);
            document.dispatchEvent(new CustomEvent(EVENTO_ALTERADO));
        } else {
            toast(resp.mensagem || 'Erro ao desativar instituição.', true);
        }
    } catch (e) {
        toast('Erro de comunicação ao desativar.', true);
    }
}

export function inicializarForm() {
    const f = form();
    if (!f) return;

    const campoCodigo = document.getElementById('instituicaoCodigo');
    if (campoCodigo) {
        campoCodigo.addEventListener('input', function () {
            this.value = this.value.replace(/\D/g, '');
        });
    }

    const btnNovo = document.getElementById('btnNovaInstituicao');
    if (btnNovo) {
        btnNovo.addEventListener('click', function (e) {
            e.preventDefault();
            abrirNovo();
        });
    }

    f.addEventListener('submit', async function (e) {
        e.preventDefault();
        limparErros();

        const id = document.getElementById('instituicaoId').value;
        const ehEdicao = !!id;
        const url = ehEdicao ? `${cfg().urlEditar}/${id}` : cfg().urlInserir;
        const metodo = ehEdicao ? 'PUT' : 'POST';

        const body = new URLSearchParams();
        if (ehEdicao) {
            body.append('id', id);
        }
        body.append('nome', document.getElementById('instituicaoNome').value);
        body.append('codigo', document.getElementById('instituicaoCodigo').value);
        body.append('tipo', document.getElementById('instituicaoTipo').value);

        if (ehEdicao) {
            const chk = document.getElementById('instituicaoAtiva');
            body.append('ativo', chk ? chk.checked : true);
        }

        try {
            const resp = await enviar(url, metodo, body);
            if (resp.sucesso) {
                fecharModal('modalInstituicao');
                toast(resp.mensagem || 'Operação realizada com sucesso.', false);
                document.dispatchEvent(new CustomEvent(EVENTO_ALTERADO));
            } else {
                aplicarErros(resp);
            }
        } catch (err) {
            toast('Erro de comunicação ao salvar.', true);
        }
    });
}

function aplicarErros(resp) {
    if (resp.errosCampos) {
        for (const [campo, msg] of Object.entries(resp.errosCampos)) {
            const input = document.getElementById(`instituicao${campo.charAt(0).toUpperCase() + campo.slice(1)}`);
            const divErro = document.getElementById(`erro${campo.charAt(0).toUpperCase() + campo.slice(1)}`);
            if (input) input.classList.add('is-invalid');
            if (divErro) divErro.textContent = msg;
        }
    }
    if (resp.errosNegocio) {
        for (const [campo, msg] of Object.entries(resp.errosNegocio)) {
            if (campo === 'geral') {
                const alerta = document.getElementById('alertaFormInstituicao');
                if (alerta) {
                    alerta.textContent = msg;
                    alerta.style.display = 'block';
                }
            } else {
                const input = document.getElementById(`instituicao${campo.charAt(0).toUpperCase() + campo.slice(1)}`);
                const divErro = document.getElementById(`erro${campo.charAt(0).toUpperCase() + campo.slice(1)}`);
                if (input) input.classList.add('is-invalid');
                if (divErro) divErro.textContent = msg;
            }
        }
    }
}
