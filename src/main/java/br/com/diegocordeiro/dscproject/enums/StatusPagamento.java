package br.com.diegocordeiro.dscproject.enums;

import lombok.Getter;

@Getter
public enum StatusPagamento {

    SIM("Pago"),
    NAO("Em aberto"),
    NAO_SE_APLICA("Não se aplica");

    private final String descricao;

    StatusPagamento(String descricao) {
        this.descricao = descricao;
    }
}
