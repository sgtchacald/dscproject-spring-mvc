// Chamadas HTTP com CSRF e Accept JSON, compartilhadas pelas telas do sistema.

const meta = (nome) => document.querySelector('meta[name="' + nome + '"]')?.content || '';

export const csrf = { token: meta('_csrf'), header: meta('_csrf_header') };

export function headers(json) {
    const h = { [csrf.header]: csrf.token, 'Accept': 'application/json' };
    if (json) h['Content-Type'] = 'application/json';
    return h;
}

export async function getJson(url) {
    const resp = await fetch(url, { headers: { 'Accept': 'application/json' } });
    return resp.json();
}

// body: URLSearchParams | string | undefined. O Content-Type fica a cargo do fetch.
export async function enviar(url, metodo, body) {
    const resp = await fetch(url, { method: metodo, body, headers: headers(false) });
    return resp.json();
}
