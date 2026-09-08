// Validação do valor conforme o tipo, no cliente. Espelha o
// ParametroValorValidador do servidor, que revalida a cada gravação.

const cfg = () => document.getElementById('dadosTela').dataset;

export function chaveErro(tipo, valor) {
    const v = (valor ?? '').trim();
    switch (tipo) {
        case 'STRING':
            return v.length === 0 ? 'obrigatorio' : null;
        case 'INTEGER':
            return /^[+-]?\d+$/.test(v) ? null : 'erroInteiro';
        case 'DECIMAL':
            return isNaN(Number(v.replace(',', '.'))) || v.length === 0 ? 'erroDecimal' : null;
        case 'BOOLEAN':
            return /^(true|false)$/i.test(v) ? null : 'erroBooleano';
        case 'JSON':
            try { JSON.parse(v); return null; } catch (e) { return 'erroJson'; }
        default:
            return 'erroInteiro';
    }
}

export function mensagemErro(chave) {
    const d = cfg();
    return { erroInteiro: d.erroInteiro, erroDecimal: d.erroDecimal, erroBooleano: d.erroBooleano, erroJson: d.erroJson }[chave]
        || 'Valor inválido.';
}

// Formata o valor para exibição na célula (não editável).
export function formatarValor(param) {
    if (param.tipoDado === 'BOOLEAN') {
        return String(param.valor).toLowerCase() === 'true' ? cfg().labelSim : cfg().labelNao;
    }
    const texto = param.valor ?? '';
    return texto.length > 60 ? '<span title="' + escaparAttr(texto) + '">' + escapar(texto.slice(0, 60)) + '…</span>'
        : escapar(texto);
}

export function escapar(s) {
    return (s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function escaparAttr(s) {
    return escapar(s).replace(/\n/g, ' ');
}

/**
 * Transforma a célula no editor adequado ao tipo do parâmetro.
 * onConfirmar(novoValor) roda ao pressionar Enter/sair com valor válido e alterado.
 * onCancelar() roda ao Esc, ao abrir outra célula ou ao sair sem confirmar.
 */
export function montarEditor(td, param, onConfirmar, onCancelar) {
    const original = param.valor ?? '';
    td.classList.add('p-1');
    td.innerHTML = '';

    let input;
    if (param.tipoDado === 'BOOLEAN') {
        input = document.createElement('select');
        input.className = 'form-select form-select-sm';
        input.innerHTML = '<option value="true">true</option><option value="false">false</option>';
        input.value = String(original).toLowerCase() === 'true' ? 'true' : 'false';
    } else if (param.tipoDado === 'JSON') {
        input = document.createElement('textarea');
        input.className = 'form-control form-control-sm font-monospace';
        input.rows = 3;
        input.value = original;
    } else {
        input = document.createElement('input');
        input.type = 'text';
        input.className = 'form-control form-control-sm';
        if (param.tipoDado === 'INTEGER') input.inputMode = 'numeric';
        input.value = original;
    }

    const feedback = document.createElement('div');
    feedback.className = 'invalid-feedback d-block';
    feedback.hidden = true;

    td.appendChild(input);
    td.appendChild(feedback);
    input.focus();
    if (input.select) input.select();

    let encerrado = false;

    function validar() {
        const chave = chaveErro(param.tipoDado, input.value);
        if (chave && chave !== 'obrigatorio') {
            input.classList.add('is-invalid');
            feedback.textContent = mensagemErro(chave);
            feedback.hidden = false;
            return false;
        }
        input.classList.remove('is-invalid');
        feedback.hidden = true;
        return chave !== 'obrigatorio';
    }

    function confirmar() {
        if (encerrado) return;
        if (!validar()) return;              // inválido: mantém a célula em edição
        const novo = param.tipoDado === 'JSON' ? input.value : input.value.trim();
        encerrado = true;
        if (novo === original) {
            onCancelar();
        } else {
            onConfirmar(novo);
        }
    }

    function cancelar() {
        if (encerrado) return;
        encerrado = true;
        onCancelar();
    }

    input.addEventListener('input', validar);
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && param.tipoDado !== 'JSON') { e.preventDefault(); confirmar(); }
        if (e.key === 'Escape') { e.preventDefault(); cancelar(); }
    });
    input.addEventListener('blur', confirmar);

    return { cancelar };
}
