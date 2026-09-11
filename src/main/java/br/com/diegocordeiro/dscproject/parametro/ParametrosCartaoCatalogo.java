package br.com.diegocordeiro.dscproject.parametro;

import br.com.diegocordeiro.dscproject.enums.TipoParametro;

/**
 * Catálogo, no código, dos parâmetros do módulo Cartões de Crédito.
 * A carga inicial reflete estas entradas na tabela {@code PARAMETROS_GLOBAIS}.
 */
public enum ParametrosCartaoCatalogo implements ParametroDefinido {

    CARTAO_EXCLUSAO_BLOQUEIA_EM_USO(
        "Exclusão de cartão bloqueia em uso",
        "Se verdadeiro, impede excluir um cartão referenciado por despesas (só desativar). Se falso, a exclusão é permitida e as despesas afetadas ficam com CACR_ID nulo. O bloqueio por faturas é sempre aplicado, independentemente deste parâmetro.",
        TipoParametro.BOOLEAN, "true");

    public static final String MODULO = "Cartões de Crédito";

    private final String nome;
    private final String descricao;
    private final TipoParametro tipo;
    private final String valorDefault;

    ParametrosCartaoCatalogo(String nome, String descricao, TipoParametro tipo, String valorDefault) {
        this.nome = nome;
        this.descricao = descricao;
        this.tipo = tipo;
        this.valorDefault = valorDefault;
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
    public TipoParametro getTipo() {
        return tipo;
    }

    @Override
    public String getValorDefault() {
        return valorDefault;
    }
}
