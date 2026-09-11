import { enviar } from '../comum/http.js';
import { toast, abrirModal, fecharModal } from '../comum/ui.js';

export const EVENTO_RECEBIMENTO_REGISTRADO = 'receita:recebimento-registrado';

const cfg = () => document.getElementById('dadosTelaReceita').dataset;
const form = () => document.getElementById('formRecebimento');

function formatarMoeda(valor) {
    const num = Number(valor != null ? valor : 0);
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(num);
}

function limparErros() {
    const f = form();
    if (!f) return;
    f.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    f.querySelectorAll('.invalid-feedback').forEach(el => (el.textContent = ''));
    const alerta = document.getElementById('alertaFormRecebimento');
    if (alerta) {
        alerta.style.display = 'none';
        alerta.textContent = '';
    }
}

export function abrirRecebimento(id, valor) {
    limparErros();
    document.getElementById('recebimentoReceitaId').value = id;
    document.getElementById('recebimentoValor').textContent = formatarMoeda(valor);
    document.getElementById('recebimentoDataRecebimento').value = new Date().toISOString().slice(0, 10);
    abrirModal('modalRecebimento');
}

export function inicializarRecebimento() {
    const f = form();
    if (!f) return;

    f.addEventListener('submit', async function (e) {
        e.preventDefault();
        limparErros();

        const id = document.getElementById('recebimentoReceitaId').value;
        const body = new URLSearchParams();
        body.append('dataRecebimento', document.getElementById('recebimentoDataRecebimento').value || '');

        try {
            const resp = await enviar(`${cfg().urlMarcarRecebida}/${id}`, 'PUT', body);
            if (resp.sucesso) {
                fecharModal('modalRecebimento');
                toast(resp.mensagem || 'Recebimento registrado com sucesso.', false);
                document.dispatchEvent(new CustomEvent(EVENTO_RECEBIMENTO_REGISTRADO));
            } else {
                if (resp.errosCampos && resp.errosCampos.dataRecebimento) {
                    const input = document.getElementById('recebimentoDataRecebimento');
                    const divErro = document.getElementById('erroRecebimentoDataRecebimento');
                    input.classList.add('is-invalid');
                    divErro.textContent = resp.errosCampos.dataRecebimento;
                } else {
                    toast(resp.mensagem || 'Não foi possível registrar o recebimento.', true);
                }
            }
        } catch (err) {
            toast('Erro de comunicação ao registrar o recebimento.', true);
        }
    });
}
