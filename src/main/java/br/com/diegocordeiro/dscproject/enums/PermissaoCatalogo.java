package br.com.diegocordeiro.dscproject.enums;

/**
 * Catálogo, no código, das permissões do módulo Usuários (Documento 0, Obs. 23).
 * A carga inicial reflete estas entradas na tabela {@code PERMISSOES}.
 * Cada uma vira a autoridade {@code PERM_{CODIGO}}.
 */
public enum PermissaoCatalogo {

    USUARIOS_LISTAR("Listar usuários", "Abrir a tela de Usuários, listar e filtrar. Controla a visibilidade do menu 'Usuários'."),
    USUARIOS_INSERIR("Inserir usuário", "Cadastrar novo usuário, inclusive escolher o perfil."),
    USUARIOS_EDITAR("Editar usuário", "Editar usuário existente, inclusive trocar o perfil e a senha."),
    USUARIOS_EXCLUIR("Excluir usuário", "Exclusão lógica de usuário, respeitadas as travas."),
    USUARIOS_VER_HISTORICO("Ver histórico de usuário", "Ver o histórico de alterações de um usuário.");

    public static final String MODULO = "Usuários";
    public static final String PREFIXO_AUTHORITY = "PERM_";

    private final String nome;
    private final String descricao;

    PermissaoCatalogo(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public String getCodigo() {
        return name();
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getModulo() {
        return MODULO;
    }

    public String getAuthority() {
        return PREFIXO_AUTHORITY + name();
    }
}
