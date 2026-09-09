import { getJson, enviar } from '../comum/http.js';
import { toast } from '../comum/ui.js';
import { EVENTO_ALTERADO } from './modal-form.js';

const cfg = () => document.getElementById('dadosTelaPerfil').dataset;
const corpo = () => document.getElementById('corpoCatalogo');

async function carregarCatalogo() {
    const itens = await getJson(cfg().urlPermissoes);
    const alvo = corpo();
    alvo.innerHTML = '';
    let moduloAtual = null;
    itens.forEach((p) => {
        if (p.modulo !== moduloAtual) {
            moduloAtual = p.modulo;
            const cab = document.createElement('tr');
            cab.innerHTML = '<td colspan="5" class="fw-bold bg-light">' + moduloAtual + '</td>';
            alvo.appendChild(cab);
        }
        const tr = document.createElement('tr');
        if (p.orfa) tr.className = 'text-warning';
        const situacao = p.orfa ? (cfg().labelCatOrfa || 'Órfã') : (cfg().labelCatAtiva || 'Ativa');
        const porPlano = p.concedivelPorPlano ? (cfg().labelCatSim || 'Sim') : (cfg().labelCatNao || 'Não');
        tr.innerHTML = '<td>' + p.codigo + '</td><td>' + p.nome + '</td><td>' + p.modulo + '</td>'
            + '<td>' + porPlano + '</td><td>' + situacao + '</td>';
        alvo.appendChild(tr);
    });
}

const modal = document.getElementById('modalCatalogo');
if (modal) {
    modal.addEventListener('show.bs.modal', carregarCatalogo);
}

const btnSincronizar = document.getElementById('btnSincronizarCatalogo');
if (btnSincronizar) {
    btnSincronizar.addEventListener('click', async function () {
        btnSincronizar.disabled = true;
        try {
            const data = await enviar(cfg().urlSincronizar, 'POST');
            if (data.sucesso) {
                toast(data.mensagem);
                await carregarCatalogo();
                document.dispatchEvent(new CustomEvent(EVENTO_ALTERADO));
            } else {
                toast(data.mensagem || 'Erro', true);
            }
        } catch (err) {
            toast(cfg().erroComunicacao, true);
        } finally {
            btnSincronizar.disabled = false;
        }
    });
}
