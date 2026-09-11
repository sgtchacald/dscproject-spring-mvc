package br.com.diegocordeiro.dscproject.enums;

import lombok.Getter;

@Getter
public enum StatusContato {

    ATIVO("Ativo"),
    PENDENTE_CONVITE("Pendente"),
    RECUSADO("Recusado"),
    BLOQUEADO("Bloqueado");

    private final String descricao;

    StatusContato(String descricao) {
        this.descricao = descricao;
    }
}
