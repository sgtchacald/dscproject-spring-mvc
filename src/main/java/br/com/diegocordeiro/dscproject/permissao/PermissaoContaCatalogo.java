package br.com.diegocordeiro.dscproject.permissao;

/**
 * Catálogo, no código, das permissões do módulo Contas.
 * Convenção domínio-primeiro; cada uma vira a autoridade {@code PERM_{CODIGO}}.
 */
public enum PermissaoContaCatalogo implements PermissaoDefinida {

    CONTAS_LISTAR("Listar minhas contas", "Abrir a tela Minhas Contas, listar e filtrar as próprias contas, e obter a lista de contas ativas para os comboboxes de lançamento. Controla a visibilidade do menu 'Minhas Contas'."),
    CONTAS_MANTER("Manter minhas contas", "Cadastrar, editar, ajustar saldo, desativar/reativar e excluir as próprias contas.");

    public static final String MODULO = "Contas";

    private final String nome;
    private final String descricao;

    PermissaoContaCatalogo(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    @Override
    public String getCodigo() {
        return name();
    }

    @Override
    public String getNome() {
        return nome;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }

    @Override
    public String getModulo() {
        return MODULO;
    }
}
