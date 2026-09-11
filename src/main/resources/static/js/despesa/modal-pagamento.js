import { enviar } from '../comum/http.js';
import { toast, abrirModal, fecharModal } from '../comum/ui.js';

export const EVENTO_PAGAMENTO_REGISTRADO = 'despesa:pagamento-registrado';

const cfg = () => document.getElementById('dadosTelaDespesa').dataset;
const form = () => document.getElementById('formPagamento');

function formatarMoeda(valor) {
    const num = Number(valor != null ? valor : 0);
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(num);
}

function limparErros() {
    const f = form();
    if (!f) return;
    f.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    f.querySelectorAll('.invalid-feedback').forEach(el => (el.textContent = ''));
    const alerta = document.getElementById('alertaFormPagamento');
    if (alerta) {
        alerta.style.display = 'none';
        alerta.textContent = '';
    }
}

export function abrirPagamento(id, valor) {
    limparErros();
    document.getElementById('pagamentoDespesaId').value = id;
    document.getElementById('pagamentoValorExibicao').textContent = formatarMoeda(valor);
    document.getElementById('pagamentoData').value = new Date().toISOString().slice(0, 10);
    abrirModal('modalPagamento');
}

export function inicializarPagamento() {
    const f = form();
    if (!f) return;

    f.addEventListener('submit', async function (e) {
        e.preventDefault();
        limparErros();

        const id = document.getElementById('pagamentoDespesaId').value;
        const dataPagamento = document.getElementById('pagamentoData').value;

        if (!dataPagamento) {
            const input = document.getElementById('pagamentoData');
            input.classList.add('is-invalid');
            document.getElementById('erroPagamentoData').textContent = 'O campo Data de pagamento é obrigatório.';
            return;
        }

        const body = new URLSearchParams();
        body.append('dataPagamento', dataPagamento);

        try {
            const resp = await enviar(`${cfg().urlPagamento}/${id}`, 'PUT', body);
            if (resp.sucesso) {
                fecharModal('modalPagamento');
                toast(resp.mensagem || 'Pagamento registrado com sucesso.', false);
                document.dispatchEvent(new CustomEvent(EVENTO_PAGAMENTO_REGISTRADO));
            } else {
                if (resp.errosCampos && resp.errosCampos.dataPagamento) {
                    const input = document.getElementById('pagamentoData');
                    input.classList.add('is-invalid');
                    document.getElementById('erroPagamentoData').textContent = resp.errosCampos.dataPagamento;
                } else {
                    const alerta = document.getElementById('alertaFormPagamento');
                    alerta.style.display = 'block';
                    alerta.textContent = resp.mensagem || (resp.errosNegocio && resp.errosNegocio.geral) || 'Não foi possível registrar o pagamento.';
                }
            }
        } catch (err) {
            toast('Erro de comunicação ao registrar o pagamento.', true);
        }
    });
}
