import { getJson } from '../comum/http.js';
import { abrirModal, fecharModal } from '../comum/ui.js';

const cfg = () => document.getElementById('dadosTelaDespesa').dataset;

let categoriasCarregadas = false;

const filtro = {
    busca: '',
    competenciaInicio: '',
    competenciaFim: '',
    status: '',
    forma: '',
    categoriaId: '',
    parcelada: '',
    recorrente: ''
};

export function obterFiltroAtual() {
    return { ...filtro };
}

export function abrirModalFiltro() {
    carregarCategoriasFiltro();
    abrirModal('modalFiltroDespesa');
}

async function carregarCategoriasFiltro() {
    if (categoriasCarregadas) return;
    const select = document.getElementById('filtroCategoria');
    if (!select) return;

    try {
        const categorias = await getJson(cfg().urlCategoriasOpcoes);
        categorias.forEach(cat => {
            const opt = document.createElement('option');
            opt.value = cat.id;
            opt.textContent = cat.nome;
            select.appendChild(opt);
        });
        categoriasCarregadas = true;
    } catch (e) {
        console.error('Erro ao carregar categorias no filtro', e);
    }
}

export function inicializarFiltro(onAplicar) {
    const btnAplicar = document.getElementById('btnAplicarFiltro');
    const btnLimpar = document.getElementById('btnLimparFiltro');

    if (btnAplicar) {
        btnAplicar.addEventListener('click', () => {
            filtro.busca = (document.getElementById('filtroBusca')?.value || '').trim();
            filtro.competenciaInicio = document.getElementById('filtroCompetenciaInicio')?.value || '';
            filtro.competenciaFim = document.getElementById('filtroCompetenciaFim')?.value || '';
            filtro.status = document.getElementById('filtroStatus')?.value || '';
            filtro.forma = document.getElementById('filtroForma')?.value || '';
            filtro.categoriaId = document.getElementById('filtroCategoria')?.value || '';
            filtro.parcelada = document.getElementById('filtroParcelada')?.value || '';
            filtro.recorrente = document.getElementById('filtroRecorrente')?.value || '';

            fecharModal('modalFiltroDespesa');
            if (typeof onAplicar === 'function') onAplicar();
        });
    }

    if (btnLimpar) {
        btnLimpar.addEventListener('click', () => {
            const elBusca = document.getElementById('filtroBusca');
            const elCompIni = document.getElementById('filtroCompetenciaInicio');
            const elCompFim = document.getElementById('filtroCompetenciaFim');
            const elStatus = document.getElementById('filtroStatus');
            const elForma = document.getElementById('filtroForma');
            const elCat = document.getElementById('filtroCategoria');
            const elParc = document.getElementById('filtroParcelada');
            const elRec = document.getElementById('filtroRecorrente');

            if (elBusca) elBusca.value = '';
            if (elCompIni) elCompIni.value = '';
            if (elCompFim) elCompFim.value = '';
            if (elStatus) elStatus.value = '';
            if (elForma) elForma.value = '';
            if (elCat) elCat.value = '';
            if (elParc) elParc.value = '';
            if (elRec) elRec.value = '';

            filtro.busca = '';
            filtro.competenciaInicio = '';
            filtro.competenciaFim = '';
            filtro.status = '';
            filtro.forma = '';
            filtro.categoriaId = '';
            filtro.parcelada = '';
            filtro.recorrente = '';

            fecharModal('modalFiltroDespesa');
            if (typeof onAplicar === 'function') onAplicar();
        });
    }
}
