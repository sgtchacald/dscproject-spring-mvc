(function () {
    const cfg = document.getElementById('dadosTela').dataset;
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
    const loginAtual = cfg.loginAtual || '';

    const pode = {
        editar: !!document.querySelector('[data-perm="editar"]'),
        excluir: !!document.querySelector('[data-perm="excluir"]'),
        historico: !!document.querySelector('[data-perm="historico"]')
    };

    let todos = [];
    let ordenacao = { col: 'nome', asc: true };

    const corpo = document.getElementById('corpoTabela');
    const rodape = document.getElementById('rodapeContagem');
    const form = document.getElementById('formUsuario');

    function headers(json) {
        const h = { [csrfHeader]: csrfToken };
        if (json) h['Content-Type'] = 'application/json';
        h['Accept'] = 'application/json';
        return h;
    }

    function semAcento(s) {
        return (s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    }

    async function carregar() {
        const resp = await fetch(cfg.urlDados, { headers: { 'Accept': 'application/json' } });
        todos = await resp.json();
        aplicarFiltro();
    }

    function filtrados() {
        const busca = semAcento(document.getElementById('filtroBusca').value);
        const perfil = document.getElementById('filtroPerfil').value;
        const situacao = document.getElementById('filtroSituacao').value;
        return todos.filter(function (u) {
            if (busca && !(semAcento(u.nome).includes(busca)
                || semAcento(u.login).includes(busca)
                || semAcento(u.email).includes(busca))) return false;
            if (perfil && u.perfilCodigo !== perfil) return false;
            if (situacao === 'ATIVO' && u.excluido) return false;
            if (situacao === 'EXCLUIDO' && !u.excluido) return false;
            return true;
        });
    }

    function ordenar(lista) {
        const { col, asc } = ordenacao;
        return lista.slice().sort(function (a, b) {
            const va = a[col], vb = b[col];
            if (va === vb) return 0;
            return (va > vb ? 1 : -1) * (asc ? 1 : -1);
        });
    }

    function dataBr(iso) {
        if (!iso) return '';
        const p = iso.split('-');
        return p.length === 3 ? p[2] + '/' + p[1] + '/' + p[0] : iso;
    }

    function acoes(u) {
        let html = '';
        if (pode.editar && !u.excluido) {
            html += '<button class="btn btn-icon btn-sm" data-acao="editar" data-id="' + u.id + '" title="Editar">✎</button> ';
        }
        if (pode.excluir && !u.excluido && u.login !== loginAtual) {
            html += '<button class="btn btn-icon btn-sm text-danger" data-acao="excluir" data-id="' + u.id + '" data-nome="' + u.nome + '" title="Excluir">🗑</button> ';
        }
        if (pode.historico) {
            html += '<button class="btn btn-icon btn-sm" data-acao="historico" data-id="' + u.id + '" title="Histórico">⟲</button>';
        }
        return html;
    }

    function render() {
        const lista = ordenar(filtrados());
        corpo.innerHTML = '';
        if (lista.length === 0) {
            corpo.innerHTML = '<tr><td colspan="8" class="text-center text-secondary">' + cfg.msgFiltroVazio + '</td></tr>';
        } else {
            lista.forEach(function (u) {
                const tr = document.createElement('tr');
                const situacao = u.excluido
                    ? '<span class="badge bg-secondary">Excluído</span>'
                    : '<span class="badge bg-success">Ativo</span>';
                tr.innerHTML = '<td>' + u.nome + '</td><td>' + u.login + '</td><td>' + u.email + '</td>'
                    + '<td><span class="badge bg-blue-lt">' + (u.perfilNome || '') + '</span></td>'
                    + '<td>' + (u.generoDescricao || '') + '</td>'
                    + '<td>' + dataBr(u.criadoEm) + '</td><td>' + situacao + '</td>'
                    + '<td class="text-nowrap">' + acoes(u) + '</td>';
                corpo.appendChild(tr);
            });
        }
        rodape.textContent = lista.length + ' de ' + todos.length + ' usuário(s)';
    }

    function aplicarFiltro() { render(); }

    // ---------- eventos de filtro / ordenação ----------

    document.getElementById('btnAplicarFiltro').addEventListener('click', aplicarFiltro);
    document.getElementById('btnLimparFiltro').addEventListener('click', function () {
        document.getElementById('filtroBusca').value = '';
        document.getElementById('filtroPerfil').value = '';
        document.getElementById('filtroSituacao').value = 'ATIVO';
        aplicarFiltro();
    });
    document.querySelectorAll('#tabelaUsuarios th.sortable').forEach(function (th) {
        th.addEventListener('click', function () {
            const col = th.dataset.col;
            ordenacao = { col: col, asc: ordenacao.col === col ? !ordenacao.asc : true };
            render();
        });
    });

    // ---------- ações da linha ----------

    corpo.addEventListener('click', function (e) {
        const btn = e.target.closest('button[data-acao]');
        if (!btn) return;
        const id = btn.dataset.id;
        if (btn.dataset.acao === 'editar') abrirEdicao(id);
        if (btn.dataset.acao === 'excluir') excluir(id, btn.dataset.nome);
        if (btn.dataset.acao === 'historico') abrirHistorico(id);
    });

    // ---------- cadastro / edição ----------

    function limparForm() {
        form.reset();
        form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
        form.querySelectorAll('.invalid-feedback').forEach(el => el.textContent = '');
        document.getElementById('alertaFormUsuario').style.display = 'none';
    }

    document.getElementById('btnNovo') && document.getElementById('btnNovo').addEventListener('click', function () {
        limparForm();
        document.getElementById('usuId').value = '';
        document.getElementById('modalUsuarioTitulo').textContent = 'Novo usuário';
        document.getElementById('hintSenha').style.display = 'none';
    });

    async function abrirEdicao(id) {
        limparForm();
        const resp = await fetch(cfg.urlBuscar + '/' + id, { headers: { 'Accept': 'application/json' } });
        const u = await resp.json();
        document.getElementById('usuId').value = u.id;
        document.getElementById('nome').value = u.nome;
        document.getElementById('genero').value = u.genero;
        document.getElementById('nascimento').value = u.nascimento || '';
        document.getElementById('email').value = u.email;
        document.getElementById('login').value = u.login;
        document.getElementById('perfilCodigo').value = u.perfilCodigo;
        document.getElementById('modalUsuarioTitulo').textContent = 'Editar usuário';
        document.getElementById('hintSenha').style.display = 'block';
        window.bootstrap.Modal.getOrCreateInstance(document.getElementById('modalUsuario')).show();
    }

    form.addEventListener('submit', async function (e) {
        e.preventDefault();
        form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
        form.querySelectorAll('.invalid-feedback').forEach(el => el.textContent = '');
        document.getElementById('alertaFormUsuario').style.display = 'none';
        const id = document.getElementById('usuId').value;
        const url = id ? (form.dataset.urlEditar + '/' + id) : form.dataset.urlInserir;
        const metodo = id ? 'PUT' : 'POST';
        try {
            const resp = await fetch(url, {
                method: metodo,
                body: new URLSearchParams(new FormData(form)),
                headers: headers(false)
            });
            const data = await resp.json();
            if (data.sucesso) {
                window.bootstrap.Modal.getOrCreateInstance(document.getElementById('modalUsuario')).hide();
                notificar(id ? cfg.msgAtualizado : cfg.msgCadastrado);
                carregar();
            } else {
                aplicarErrosForm(data.errosCampos);
                aplicarErrosForm(data.errosNegocio);
            }
        } catch (err) {
            const a = document.getElementById('alertaFormUsuario');
            a.textContent = form.dataset.erroComunicacao;
            a.style.display = 'block';
        }
    });

    function aplicarErrosForm(erros) {
        Object.entries(erros || {}).forEach(([campo, msg]) => {
            const input = form.querySelector('[name="' + campo + '"]');
            const feedback = document.getElementById('erro-' + campo);
            if (input) input.classList.add('is-invalid');
            if (feedback) feedback.textContent = msg;
            if (campo === 'geral') {
                const a = document.getElementById('alertaFormUsuario');
                a.textContent = msg;
                a.style.display = 'block';
            }
        });
    }

    // ---------- exclusão ----------

    async function excluir(id, nome) {
        if (!window.confirm(cfg.msgConfirmaExclusao.replace('{0}', nome))) return;
        const resp = await fetch(cfg.urlExcluir + '/' + id, { method: 'DELETE', headers: headers(false) });
        const data = await resp.json();
        if (data.sucesso) {
            notificar(cfg.msgExcluido);
            carregar();
        } else {
            notificar(data.mensagem || 'Erro', true);
        }
    }

    // ---------- histórico ----------

    async function abrirHistorico(id) {
        const resp = await fetch(cfg.urlHistorico + '/' + id, { headers: { 'Accept': 'application/json' } });
        const page = await resp.json();
        const corpoH = document.getElementById('corpoHistorico');
        corpoH.innerHTML = '';
        (page.content || []).forEach(function (r) {
            const tr = document.createElement('tr');
            tr.innerHTML = '<td>' + new Date(r.data).toLocaleString('pt-BR') + '</td>'
                + '<td>' + (r.autor || '') + '</td><td>' + r.tipo + '</td>'
                + '<td>' + (r.nome || '') + '</td><td>' + (r.login || '') + '</td>'
                + '<td>' + (r.perfilCodigo || '') + '</td>';
            corpoH.appendChild(tr);
        });
        window.bootstrap.Modal.getOrCreateInstance(document.getElementById('modalHistorico')).show();
    }

    // ---------- toast ----------

    function notificar(texto, erro) {
        const div = document.createElement('div');
        div.className = 'toast align-items-center text-bg-' + (erro ? 'danger' : 'success') + ' border-0 show mb-2';
        div.innerHTML = '<div class="d-flex"><div class="toast-body">' + texto + '</div>'
            + '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>';
        document.getElementById('toastArea').appendChild(div);
        setTimeout(() => div.remove(), 5000);
    }

    carregar();
})();
