import { abrirModal, fecharModal } from '../comum/ui.js';

export const EVENTO_FILTRO_APLICADO = 'cartao:filtro-aplicado';

const filtro = {
    busca: '',
    bandeira: '',
    situacao: 'ATIVO'
};

export function obterFiltroAtual() {
    return { ...filtro };
}

export function abrirModalFiltro() {
    document.getElementById('filtroBusca').value = filtro.busca;
    document.getElementById('filtroBandeira').value = filtro.bandeira;
    document.getElementById('filtroSituacao').value = filtro.situacao;
    abrirModal('modalFiltroCartao');
}

export function inicializarFiltro(onAplicar) {
    const btnAplicar = document.getElementById('btnAplicarFiltro');
    const btnLimpar = document.getElementById('btnLimparFiltro');

    if (btnAplicar) {
        btnAplicar.addEventListener('click', function () {
            filtro.busca = (document.getElementById('filtroBusca').value || '').trim();
            filtro.bandeira = document.getElementById('filtroBandeira').value || '';
            filtro.situacao = document.getElementById('filtroSituacao').value || 'ATIVO';
            fecharModal('modalFiltroCartao');
            if (onAplicar) onAplicar(obterFiltroAtual());
        });
    }

    if (btnLimpar) {
        btnLimpar.addEventListener('click', function () {
            filtro.busca = '';
            filtro.bandeira = '';
            filtro.situacao = 'ATIVO';
            document.getElementById('filtroBusca').value = '';
            document.getElementById('filtroBandeira').value = '';
            document.getElementById('filtroSituacao').value = 'ATIVO';
            fecharModal('modalFiltroCartao');
            if (onAplicar) onAplicar(obterFiltroAtual());
        });
    }
}
