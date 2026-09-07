import { getJson } from '../comum/http.js';
import { abrirModal } from '../comum/ui.js';

const cfg = () => document.getElementById('dadosTela').dataset;

export async function abrirHistorico(id) {
    const page = await getJson(cfg().urlHistorico + '/' + id);
    const corpo = document.getElementById('corpoHistorico');
    corpo.innerHTML = '';
    (page.content || []).forEach(function (r) {
        const tr = document.createElement('tr');
        tr.innerHTML = '<td>' + new Date(r.data).toLocaleString('pt-BR') + '</td>'
            + '<td>' + (r.autor || '') + '</td><td>' + r.tipo + '</td>'
            + '<td>' + (r.nome || '') + '</td><td>' + (r.login || '') + '</td>'
            + '<td>' + (r.perfilCodigo || '') + '</td>';
        corpo.appendChild(tr);
    });
    abrirModal('modalHistorico');
}
