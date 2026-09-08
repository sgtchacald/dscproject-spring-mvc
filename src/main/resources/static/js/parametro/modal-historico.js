import { getJson } from '../comum/http.js';
import { abrirModal } from '../comum/ui.js';
import { escapar } from './editor-celula.js';

const cfg = () => document.getElementById('dadosTela').dataset;

// Abre a visão de histórico do parâmetro, revisões em ordem decrescente de data.
export async function abrirHistorico(param) {
    document.getElementById('historicoTitulo').textContent = 'Histórico — ' + param.codigo;
    document.getElementById('historicoSubtitulo').textContent =
        [param.nome, param.modulo].filter(Boolean).join(' · ');

    const corpo = document.getElementById('corpoHistorico');
    corpo.innerHTML = '';

    const page = await getJson(cfg().urlHistorico + '/' + param.id);
    const revisoes = page.content || [];

    if (revisoes.length === 0) {
        corpo.innerHTML = '<tr><td colspan="4" class="text-center text-secondary">' + cfg().histVazio + '</td></tr>';
    } else {
        revisoes.forEach((r) => {
            const tr = document.createElement('tr');
            tr.innerHTML = '<td>' + new Date(r.dataHora).toLocaleString('pt-BR') + '</td>'
                + '<td>' + escapar(r.autor || '') + '</td>'
                + '<td>' + escapar(r.valor || '') + '</td>'
                + '<td>' + escapar(r.motivo || '') + '</td>';
            corpo.appendChild(tr);
        });
    }
    abrirModal('modalHistorico');
}
