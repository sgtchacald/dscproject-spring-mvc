package br.com.diegocordeiro.dscproject.enums;

import lombok.Getter;

@Getter
public enum MeioPagamento {

    DINHEIRO("Dinheiro"),
    DEBITO("Débito"),
    CREDITO("Crédito"),
    PIX("PIX"),
    BOLETO("Boleto"),
    TRANSFERENCIA("Transferência");

    private final String descricao;

    MeioPagamento(String descricao) {
        this.descricao = descricao;
    }
}
