// Torna a sidebar redimensionável arrastando a alça na borda direita (só em telas
// >= lg, onde o menu é uma coluna fixa — no mobile ele é off-canvas e a alça fica oculta).
// A largura escolhida persiste em localStorage e é restaurada nas próximas visitas.

const CHAVE_LARGURA = 'sidebarWidth';
const LARGURA_PADRAO = 240;
const LARGURA_MINIMA = 180;
const LARGURA_MAXIMA = 520;
const PASSO_TECLADO = 16;

function aplicarLargura(px) {
    const largura = Math.min(LARGURA_MAXIMA, Math.max(LARGURA_MINIMA, px));
    document.documentElement.style.setProperty('--sidebar-width', largura + 'px');
    return largura;
}

function larguraSalva() {
    const valor = parseInt(localStorage.getItem(CHAVE_LARGURA), 10);
    return Number.isFinite(valor) ? valor : LARGURA_PADRAO;
}

document.addEventListener('DOMContentLoaded', () => {
    aplicarLargura(larguraSalva());

    const alca = document.getElementById('sidebarResizeHandle');
    if (!alca) return;

    let larguraInicial = 0;
    let xInicial = 0;

    function mover(e) {
        aplicarLargura(larguraInicial + (e.clientX - xInicial));
    }

    function soltar(e) {
        alca.classList.remove('resizando');
        document.removeEventListener('pointermove', mover);
        document.removeEventListener('pointerup', soltar);
        localStorage.setItem(CHAVE_LARGURA, aplicarLargura(larguraInicial + (e.clientX - xInicial)));
    }

    alca.addEventListener('pointerdown', (e) => {
        larguraInicial = document.querySelector('.navbar-vertical').getBoundingClientRect().width;
        xInicial = e.clientX;
        alca.classList.add('resizando');
        document.addEventListener('pointermove', mover);
        document.addEventListener('pointerup', soltar);
        e.preventDefault();
    });

    alca.addEventListener('dblclick', () => {
        localStorage.removeItem(CHAVE_LARGURA);
        aplicarLargura(LARGURA_PADRAO);
    });

    alca.addEventListener('keydown', (e) => {
        if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
        e.preventDefault();
        const atual = document.querySelector('.navbar-vertical').getBoundingClientRect().width;
        const sinal = e.key === 'ArrowRight' ? 1 : -1;
        localStorage.setItem(CHAVE_LARGURA, aplicarLargura(atual + sinal * PASSO_TECLADO));
    });
});
