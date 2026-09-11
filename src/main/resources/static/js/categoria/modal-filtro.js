export function initModalFiltro() {
    const btnLimpar = document.getElementById('btnLimparFiltro');
    if (btnLimpar) {
        btnLimpar.addEventListener('click', () => {
            const inputBusca = document.getElementById('filtroBusca');
            const selectAplicaA = document.getElementById('filtroAplicaA');
            const selectTipo = document.getElementById('filtroTipo');
            const selectSituacao = document.getElementById('filtroSituacao');

            if (inputBusca) inputBusca.value = '';
            if (selectAplicaA) selectAplicaA.value = '';
            if (selectTipo) selectTipo.value = '';
            if (selectSituacao) selectSituacao.value = 'ATIVA';

            const form = document.getElementById('formFiltroCategorias');
            if (form) form.submit();
        });
    }
}
