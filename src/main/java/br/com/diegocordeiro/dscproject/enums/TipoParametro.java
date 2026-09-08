package br.com.diegocordeiro.dscproject.enums;

/**
 * Domínio de {@code PAGL_TIPO_DADO}. Define como o valor de um parâmetro global
 * é validado antes de ser gravado. Vem do catálogo do código — a tela nunca o edita.
 */
public enum TipoParametro {

    STRING,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    JSON;

    public static TipoParametro toEnum(String nome) {
        if (nome != null && !nome.isBlank()) {
            for (TipoParametro tipo : values()) {
                if (tipo.name().equalsIgnoreCase(nome.trim())) {
                    return tipo;
                }
            }
        }
        throw new IllegalArgumentException("Tipo de parâmetro inválido: " + nome);
    }
}
