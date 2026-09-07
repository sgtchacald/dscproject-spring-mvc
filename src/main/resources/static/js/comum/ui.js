// Utilidades de interface compartilhadas: texto, data e toast.

export function semAcento(s) {
    return (s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

export function dataBr(iso) {
    if (!iso) return '';
    const p = iso.split('-');
    return p.length === 3 ? p[2] + '/' + p[1] + '/' + p[0] : iso;
}

// O bundle do Tabler 1.x expõe o Bootstrap em window.tabler, não em window.bootstrap.
const bootstrap = () => window.bootstrap || window.tabler;

export function abrirModal(id) {
    bootstrap().Modal.getOrCreateInstance(document.getElementById(id)).show();
}

export function fecharModal(id) {
    bootstrap().Modal.getOrCreateInstance(document.getElementById(id)).hide();
}

export function toast(texto, erro) {
    const div = document.createElement('div');
    div.className = 'toast align-items-center text-bg-' + (erro ? 'danger' : 'success') + ' border-0 show mb-2';
    div.innerHTML = '<div class="d-flex"><div class="toast-body">' + texto + '</div>'
        + '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>';
    document.getElementById('toastArea').appendChild(div);
    setTimeout(() => div.remove(), 5000);
}
