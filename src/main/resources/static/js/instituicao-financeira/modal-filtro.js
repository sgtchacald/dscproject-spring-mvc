import { abrirModal, fecharModal } from '../comum/ui.js';

export const EVENTO_FILTRO_APLICADO = 'instituicao:filtro-aplicado';

const filtro = {
    busca: '',
    tipo: '',
    situacao: 'ATIVA'
};

export function obterFiltroAtual() {
    return { ...filtro };
}

export function abrirModalFiltro() {
    document.getElementById('filtroBusca').value = filtro.busca;
    document.getElementById('filtroTipo').value = filtro.tipo;
    document.getElementById('filtroSituacao').value = filtro.situacao;
    abrirModal('modalFiltroInstituicao');
}

export function inicializarFiltro(onAplicar) {
    const btnAplicar = document.getElementById('btnAplicarFiltro');
    const btnLimpar = document.getElementById('btnLimparFiltro');

    if (btnAplicar) {
        btnAplicar.addEventListener('click', function () {
            filtro.busca = (document.getElementById('filtroBusca').value || '').trim();
            filtro.tipo = document.getElementById('filtroTipo').value || '';
            filtro.situacao = document.getElementById('filtroSituacao').value || 'TODAS';
            fecharModal('modalFiltroInstituicao');
            if (onAplicar) onAplicar(obterFiltroAtual());
        });
    }

    if (btnLimpar) {
        btnLimpar.addEventListener('click', function () {
            filtro.busca = '';
            filtro.tipo = '';
            filtro.situacao = 'ATIVA';
            document.getElementById('filtroBusca').value = '';
            document.getElementById('filtroTipo').value = '';
            document.getElementById('filtroSituacao').value = 'ATIVA';
            fecharModal('modalFiltroInstituicao');
            if (onAplicar) onAplicar(obterFiltroAtual());
        });
    }
}
