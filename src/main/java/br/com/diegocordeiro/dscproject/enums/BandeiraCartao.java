package br.com.diegocordeiro.dscproject.enums;

import lombok.Getter;

@Getter
public enum BandeiraCartao {

    VISA("VISA", "Visa"),
    MASTERCARD("MASTERCARD", "Mastercard"),
    ELO("ELO", "Elo"),
    AMEX("AMEX", "Amex");

    private final String codigo;
    private final String descricao;

    BandeiraCartao(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public static BandeiraCartao porCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        for (BandeiraCartao bandeira : values()) {
            if (bandeira.name().equalsIgnoreCase(codigo) || bandeira.codigo.equalsIgnoreCase(codigo)) {
                return bandeira;
            }
        }
        return null;
    }
}
