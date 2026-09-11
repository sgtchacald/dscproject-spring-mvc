package br.com.diegocordeiro.dscproject.enums;

import lombok.Getter;

/**
 * Origem de um lançamento financeiro (receita, despesa ou transação).
 */
@Getter
public enum OrigemLancamento {

    MANUAL("Manual"),
    OPEN_FINANCE("Open Finance"),
    IMPORTACAO("Importação");

    private final String descricao;

    OrigemLancamento(String descricao) {
        this.descricao = descricao;
    }
}
