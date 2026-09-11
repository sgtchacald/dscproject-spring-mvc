import { abrirModal, fecharModal } from '../comum/ui.js';
import { getJson } from '../comum/http.js';

export const EVENTO_FILTRO_APLICADO = 'conta:filtro-aplicado';

const cfg = () => document.getElementById('dadosTelaConta').dataset;

const filtro = {
    busca: '',
    tipo: '',
    instituicaoId: '',
    situacao: 'ATIVA'
};

export function obterFiltroAtual() {
    return { ...filtro };
}

export async function carregarInstituicoesFiltro() {
    const select = document.getElementById('filtroInstituicao');
    if (!select || select.options.length > 1) return;

    try {
        const opcoes = await getJson(cfg().urlInstituicoesOpcoes);
        opcoes.forEach(inst => {
            const opt = document.createElement('option');
            opt.value = inst.id;
            opt.textContent = inst.nome;
            select.appendChild(opt);
        });
    } catch (e) {
        console.error('Erro ao carregar opções de instituições para o filtro', e);
    }
}

export function abrirModalFiltro() {
    document.getElementById('filtroBusca').value = filtro.busca;
    document.getElementById('filtroTipo').value = filtro.tipo;
    document.getElementById('filtroInstituicao').value = filtro.instituicaoId;
    document.getElementById('filtroSituacao').value = filtro.situacao;
    carregarInstituicoesFiltro();
    abrirModal('modalFiltroConta');
}

export function inicializarFiltro(onAplicar) {
    const btnAplicar = document.getElementById('btnAplicarFiltro');
    const btnLimpar = document.getElementById('btnLimparFiltro');

    if (btnAplicar) {
        btnAplicar.addEventListener('click', function () {
            filtro.busca = (document.getElementById('filtroBusca').value || '').trim();
            filtro.tipo = document.getElementById('filtroTipo').value || '';
            filtro.instituicaoId = document.getElementById('filtroInstituicao').value || '';
            filtro.situacao = document.getElementById('filtroSituacao').value || 'TODAS';
            fecharModal('modalFiltroConta');
            if (onAplicar) onAplicar(obterFiltroAtual());
        });
    }

    if (btnLimpar) {
        btnLimpar.addEventListener('click', function () {
            filtro.busca = '';
            filtro.tipo = '';
            filtro.instituicaoId = '';
            filtro.situacao = 'ATIVA';
            document.getElementById('filtroBusca').value = '';
            document.getElementById('filtroTipo').value = '';
            document.getElementById('filtroInstituicao').value = '';
            document.getElementById('filtroSituacao').value = 'ATIVA';
            fecharModal('modalFiltroConta');
            if (onAplicar) onAplicar(obterFiltroAtual());
        });
    }
}
