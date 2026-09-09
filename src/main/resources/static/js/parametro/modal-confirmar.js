import { getJson, enviar } from '../comum/http.js';
import { toast, abrirModal, fecharModal } from '../comum/ui.js';
import { escapar } from './editor-celula.js';

// Emitido após gravar; listar.js recarrega o grid ao ouvir.
export const EVENTO_ALTERADO = 'parametro:alterado';

const cfg = () => document.getElementById('dadosTela').dataset;
const form = () => document.getElementById('formConfirmar');
const motivo = () => document.getElementById('confirmarMotivo');

let pendente = null;   // { id, url, acao, valorAnterior, valorNovo, onCancelar, onSucesso }

function limpar() {
    motivo().value = '';
    motivo().classList.remove('is-invalid');
    document.getElementById('erro-motivo').textContent = '';
}

/**
 * Abre o modal de confirmação da alteração.
 * @param param  linha do grid (código, nome, órfão, valorDefault, tipoDado)
 * @param acao   'editar' | 'restaurar'
 * @param valorNovo valor confirmado na célula (ignorado no restaurar — usa o default)
 * @param onCancelar reverte a célula
 * @param onSucesso  recarrega o grid
 */
export async function abrirConfirmacao(param, acao, valorNovo, onCancelar, onSucesso) {
    limpar();

    // relê o estado atual para detectar edição concorrente
    let atual = param;
    try {
        atual = await getJson(cfg().urlBuscar + '/' + param.id);
    } catch (e) { /* usa o que veio do grid */ }

    const novo = acao === 'restaurar' ? atual.valorDefault : valorNovo;

    pendente = {
        id: param.id,
        acao,
        url: (acao === 'restaurar' ? cfg().urlRestaurar : cfg().urlEditar) + '/' + param.id,
        valorNovo: novo,
        onCancelar,
        onSucesso
    };

    document.getElementById('confirmarCodigo').textContent = atual.codigo;
    document.getElementById('confirmarNome').textContent = atual.nome ? ' — ' + atual.nome : '';
    document.getElementById('confirmarValorAnterior').innerHTML = escapar(atual.valor);
    document.getElementById('confirmarValorNovo').innerHTML = escapar(novo);
    document.getElementById('confirmarAvisoOrfao').hidden = !atual.orfa;

    abrirModal('modalConfirmar');
    motivo().focus();
}

async function submeter(e) {
    e.preventDefault();
    if (!pendente) return;

    const texto = motivo().value.trim();
    if (texto.length === 0) {
        motivo().classList.add('is-invalid');
        document.getElementById('erro-motivo').textContent = 'O motivo é obrigatório.';
        return;
    }

    const params = new URLSearchParams();
    params.append('motivo', texto);
    if (pendente.acao === 'editar') params.append('valor', pendente.valorNovo);

    try {
        const data = await enviar(pendente.url, 'PUT', params);
        if (data.sucesso) {
            fecharModal('modalConfirmar');
            toast(data.mensagem || cfg().msgAtualizado);
            const ok = pendente.onSucesso;
            pendente = null;
            document.dispatchEvent(new CustomEvent(EVENTO_ALTERADO));
            if (ok) ok();
        } else {
            const msg = (data.errosCampos && data.errosCampos.motivo)
                || (data.errosNegocio && (data.errosNegocio.valor || data.errosNegocio.geral))
                || data.mensagem || 'Erro';
            if (data.errosCampos && data.errosCampos.motivo) {
                motivo().classList.add('is-invalid');
                document.getElementById('erro-motivo').textContent = data.errosCampos.motivo;
            } else {
                toast(msg, true);
            }
        }
    } catch (err) {
        toast(cfg().erroComunicacao, true);
    }
}

function cancelar() {
    if (pendente && pendente.onCancelar) pendente.onCancelar();
    pendente = null;
}

form().addEventListener('submit', submeter);
document.getElementById('btnCancelarConfirmar').addEventListener('click', cancelar);
document.getElementById('modalConfirmar').addEventListener('hidden.bs.modal', () => {
    if (pendente) cancelar();
});
