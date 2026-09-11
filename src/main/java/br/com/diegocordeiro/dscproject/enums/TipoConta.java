package br.com.diegocordeiro.dscproject.enums;

import lombok.Getter;

@Getter
public enum TipoConta {

    CORRENTE("CORRENTE", "Corrente"),
    POUPANCA("POUPANCA", "Poupança"),
    INVESTIMENTO("INVESTIMENTO", "Investimento"),
    CARTEIRA("CARTEIRA", "Carteira");

    private final String codigo;
    private final String descricao;

    TipoConta(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public static TipoConta porCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        for (TipoConta tipo : values()) {
            if (tipo.name().equalsIgnoreCase(codigo) || tipo.codigo.equalsIgnoreCase(codigo)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Tipo de conta inválido: " + codigo);
    }
}
