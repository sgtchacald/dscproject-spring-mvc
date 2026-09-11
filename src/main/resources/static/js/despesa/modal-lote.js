import { enviar } from '../comum/http.js';
import { toast, abrirModal, fecharModal } from '../comum/ui.js';

export const EVENTO_PAGAMENTO_LOTE_REGISTRADO = 'despesa:pagamento-lote-registrado';

const cfg = () => document.getElementById('dadosTelaDespesa').dataset;
const form = () => document.getElementById('formPagamentoLote');

let idsSelecionados = [];

function formatarMoeda(valor) {
    const num = Number(valor != null ? valor : 0);
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(num);
}

function limparErros() {
    const f = form();
    if (!f) return;
    f.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    f.querySelectorAll('.invalid-feedback').forEach(el => (el.textContent = ''));
    const alerta = document.getElementById('alertaFormPagamentoLote');
    if (alerta) {
        alerta.style.display = 'none';
        alerta.textContent = '';
    }
}

export function abrirPagamentoLote(selecionados) {
    limparErros();
    idsSelecionados = selecionados.map(d => d.id);

    const totalValor = selecionados.reduce((acc, curr) => acc + (Number(curr.valor) || 0), 0);
    const textoResumo = `${selecionados.length} despesa(s) selecionada(s) totalizando ${formatarMoeda(totalValor)}.`;
    document.getElementById('pagamentoLoteResumo').textContent = textoResumo;

    document.getElementById('pagamentoLoteData').value = new Date().toISOString().slice(0, 10);
    abrirModal('modalPagamentoLote');
}

export function inicializarPagamentoLote() {
    const f = form();
    if (!f) return;

    f.addEventListener('submit', async function (e) {
        e.preventDefault();
        limparErros();

        const dataPagamento = document.getElementById('pagamentoLoteData').value;
        if (!dataPagamento) {
            const input = document.getElementById('pagamentoLoteData');
            input.classList.add('is-invalid');
            document.getElementById('erroPagamentoLoteData').textContent = 'O campo Data de pagamento é obrigatório.';
            return;
        }

        const body = new URLSearchParams();
        idsSelecionados.forEach(id => body.append('ids', id));
        body.append('dataPagamento', dataPagamento);

        try {
            const resp = await enviar(cfg().urlPagamentoLote, 'POST', body);
            if (resp.sucesso) {
                fecharModal('modalPagamentoLote');
                toast(resp.mensagem || 'Pagamento em lote registrado com sucesso.', false);
                document.dispatchEvent(new CustomEvent(EVENTO_PAGAMENTO_LOTE_REGISTRADO));
            } else {
                if (resp.errosCampos && resp.errosCampos.dataPagamento) {
                    const input = document.getElementById('pagamentoLoteData');
                    input.classList.add('is-invalid');
                    document.getElementById('erroPagamentoLoteData').textContent = resp.errosCampos.dataPagamento;
                } else {
                    const alerta = document.getElementById('alertaFormPagamentoLote');
                    alerta.style.display = 'block';
                    alerta.textContent = resp.mensagem || 'Não foi possível registrar o pagamento em lote.';
                }
            }
        } catch (err) {
            toast('Erro de comunicação ao registrar o pagamento em lote.', true);
        }
    });
}
