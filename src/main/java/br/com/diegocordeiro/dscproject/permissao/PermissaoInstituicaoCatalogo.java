package br.com.diegocordeiro.dscproject.permissao;

/**
 * Catálogo, no código, das permissões do módulo Instituições Financeiras.
 * Convenção domínio-primeiro; cada uma vira a autoridade {@code PERM_{CODIGO}}.
 */
public enum PermissaoInstituicaoCatalogo implements PermissaoDefinida {

    INSTITUICOES_LISTAR("Listar instituições financeiras", "Abrir a tela de Instituições Financeiras, listar e filtrar. Controla a visibilidade do menu 'Instituições Financeiras'."),
    INSTITUICOES_MANTER("Manter instituições financeiras", "Cadastrar, editar, desativar/reativar e excluir instituição.");

    public static final String MODULO = "Instituições Financeiras";

    private final String nome;
    private final String descricao;

    PermissaoInstituicaoCatalogo(String nome, String descricao) {
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
