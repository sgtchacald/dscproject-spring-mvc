package br.com.diegocordeiro.dscproject.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum TipoInstituicaoFinanceira {

    BANCO("B", "Banco"),
    CORRETORA("C", "Corretora");

    @Getter
    private final String codigo;

    @Getter
    private final String descricao;

    public static TipoInstituicaoFinanceira toEnum(String codigo) {
        if (codigo != null && !codigo.isBlank()) {
            for (TipoInstituicaoFinanceira t : TipoInstituicaoFinanceira.values()) {
                if (t.getCodigo().equalsIgnoreCase(codigo) || t.name().equalsIgnoreCase(codigo)) {
                    return t;
                }
            }
        }
        throw new IllegalArgumentException("Código de tipo de instituição financeira inválido: " + codigo);
    }
}
