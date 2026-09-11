package br.com.diegocordeiro.dscproject.converter;

import br.com.diegocordeiro.dscproject.enums.TipoInstituicaoFinanceira;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persiste {@link TipoInstituicaoFinanceira} como o código de 1 caractere na coluna {@code INFI_TIPO_INSTITUICAO}
 * ({@code CHAR(1)} — B/C).
 */
@Converter(autoApply = true)
public class TipoInstituicaoFinanceiraConverter implements AttributeConverter<TipoInstituicaoFinanceira, String> {

    @Override
    public String convertToDatabaseColumn(TipoInstituicaoFinanceira tipo) {
        return tipo == null ? null : tipo.getCodigo();
    }

    @Override
    public TipoInstituicaoFinanceira convertToEntityAttribute(String codigo) {
        return (codigo == null || codigo.isBlank()) ? null : TipoInstituicaoFinanceira.toEnum(codigo);
    }
}
