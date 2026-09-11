import { getJson, enviar } from '../comum/http.js';
import { toast, abrirModal, fecharModal } from '../comum/ui.js';

export const EVENTO_ALTERADO = 'instituicaoprovedor:alterado';

const cfg = () => document.getElementById('dadosTelaVinculo').dataset;
const form = () => document.getElementById('formVinculo');

let provedoresCache = null;
let instituicoesCache = null;

function limparErros() {
    const f = form();
    f.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    f.querySelectorAll('.invalid-feedback').forEach(el => (el.textContent = ''));
    const alerta = document.getElementById('alertaFormVinculo');
    if (alerta) {
        alerta.style.display = 'none';
        alerta.textContent = '';
    }
}

async function carregarOpcoes() {
    const selProv = document.getElementById('vinculoProvedor');
    const selInst = document.getElementById('vinculoInstituicao');

    if (!provedoresCache) {
        provedoresCache = await getJson(cfg().urlProvedores);
    }
    if (!instituicoesCache) {
        instituicoesCache = await getJson(cfg().urlInstituicoes);
    }

    selProv.innerHTML = '<option value="">Selecione…</option>';
    provedoresCache.filter(p => p.ativo).forEach(p => {
        const opt = document.createElement('option');
        opt.value = p.id;
        opt.textContent = p.nome;
        selProv.appendChild(opt);
    });

    selInst.innerHTML = '<option value="">Selecione…</option>';
    instituicoesCache.forEach(i => {
        const opt = document.createElement('option');
        opt.value = i.id;
        opt.textContent = i.nome;
        selInst.appendChild(opt);
    });
}

export async function abrirNovo() {
    limparErros();
    await carregarOpcoes();

    form().reset();
    document.getElementById('vinculoId').value = '';
    document.getElementById('tituloModalVinculo').textContent = cfg().labelNovo || 'Novo vínculo';

    abrirModal('modalVinculo');
}

export async function abrirEdicao(id) {
    limparErros();
    await carregarOpcoes();

    try {
        const dados = await getJson(`${cfg().urlBuscar}/${id}`);
        document.getElementById('vinculoId').value = dados.id;
        document.getElementById('tituloModalVinculo').textContent = cfg().labelEditar || 'Editar vínculo';
        document.getElementById('vinculoProvedor').value = dados.provedorId || '';
        document.getElementById('vinculoInstituicao').value = dados.instituicaoId || '';
        document.getElementById('vinculoIdExterno').value = dados.idExterno || '';

        abrirModal('modalVinculo');
    } catch (e) {
        toast('Erro ao carregar vínculo.', true);
    }
}

export async function excluir(id, instituicaoNome, provedorNome) {
    const template = cfg().msgConfirmaExclusao || 'Confirma a exclusão do vínculo de "{0}" no provedor "{1}"?';
    const msg = template.replace('{0}', instituicaoNome).replace('{1}', provedorNome);
    if (!confirm(msg)) {
        return;
    }

    try {
        const resp = await enviar(`${cfg().urlExcluir}/${id}`, 'DELETE');
        if (resp.sucesso) {
            toast(resp.mensagem || 'Vínculo excluído com sucesso.', false);
            document.dispatchEvent(new CustomEvent(EVENTO_ALTERADO));
        } else {
            toast(resp.mensagem || 'Erro ao excluir vínculo.', true);
        }
    } catch (e) {
        toast('Erro de comunicação ao excluir.', true);
    }
}

export function inicializarForm() {
    const f = form();
    if (!f) return;

    const btnNovo = document.getElementById('btnNovoVinculo');
    if (btnNovo) {
        btnNovo.addEventListener('click', function (e) {
            e.preventDefault();
            abrirNovo();
        });
    }

    f.addEventListener('submit', async function (e) {
        e.preventDefault();
        limparErros();

        const id = document.getElementById('vinculoId').value;
        const ehEdicao = !!id;
        const url = ehEdicao ? `${cfg().urlEditar}/${id}` : cfg().urlInserir;
        const metodo = ehEdicao ? 'PUT' : 'POST';

        const body = new URLSearchParams();
        if (ehEdicao) {
            body.append('id', id);
        }
        body.append('provedorId', document.getElementById('vinculoProvedor').value);
        body.append('instituicaoId', document.getElementById('vinculoInstituicao').value);
        body.append('idExterno', document.getElementById('vinculoIdExterno').value);

        try {
            const resp = await enviar(url, metodo, body);
            if (resp.sucesso) {
                fecharModal('modalVinculo');
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
            const input = document.getElementById(`vinculo${campo.charAt(0).toUpperCase() + campo.slice(1)}`);
            const divErro = document.getElementById(`erro${campo.charAt(0).toUpperCase() + campo.slice(1)}`);
            if (input) input.classList.add('is-invalid');
            if (divErro) divErro.textContent = msg;
        }
    }
    if (resp.errosNegocio) {
        for (const [campo, msg] of Object.entries(resp.errosNegocio)) {
            if (campo === 'geral') {
                const alerta = document.getElementById('alertaFormVinculo');
                if (alerta) {
                    alerta.textContent = msg;
                    alerta.style.display = 'block';
                }
            } else {
                const input = document.getElementById(`vinculo${campo.charAt(0).toUpperCase() + campo.slice(1)}`);
                const divErro = document.getElementById(`erro${campo.charAt(0).toUpperCase() + campo.slice(1)}`);
                if (input) input.classList.add('is-invalid');
                if (divErro) divErro.textContent = msg;
            }
        }
    }
}
