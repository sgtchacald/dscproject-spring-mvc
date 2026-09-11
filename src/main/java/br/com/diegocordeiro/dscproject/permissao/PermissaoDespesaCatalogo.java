package br.com.diegocordeiro.dscproject.permissao;

public enum PermissaoDespesaCatalogo implements PermissaoDefinida {

    DESPESAS_LISTAR("Listar minhas despesas", "Abrir a tela Despesas, listar e filtrar as próprias despesas. Controla a visibilidade do menu 'Despesas'."),
    DESPESAS_MANTER("Manter minhas despesas", "Cadastrar, editar, parcelar, registrar o pagamento (individual e em lote) e excluir as próprias despesas."),
    DESPESA_RATEAR_MULTIUSUARIO("Ratear despesas entre usuários", "Dividir despesas entre múltiplos usuários com gestão de fatias e acerto.", true);

    public static final String MODULO = "Despesas";

    private final String nome;
    private final String descricao;
    private final boolean concedivelPorPlano;

    PermissaoDespesaCatalogo(String nome, String descricao) {
        this(nome, descricao, false);
    }

    PermissaoDespesaCatalogo(String nome, String descricao, boolean concedivelPorPlano) {
        this.nome = nome;
        this.descricao = descricao;
        this.concedivelPorPlano = concedivelPorPlano;
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

    @Override
    public boolean isConcedivelPorPlano() {
        return concedivelPorPlano;
    }
}
