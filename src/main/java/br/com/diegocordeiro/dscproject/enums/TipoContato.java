package br.com.diegocordeiro.dscproject.enums;

import lombok.Getter;

@Getter
public enum TipoContato {

    EXTERNO("Contato Externo"),
    SISTEMA("Usuário do Sistema");

    private final String descricao;

    TipoContato(String descricao) {
        this.descricao = descricao;
    }
}
