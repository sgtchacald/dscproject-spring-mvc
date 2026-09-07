package br.com.diegocordeiro.dscproject.converter;

import br.com.diegocordeiro.dscproject.enums.Genero;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persiste {@link Genero} como o código de 1 caractere na coluna {@code USU_GENERO}
 * ({@code CHAR(1)} — F/M/O), conforme o Documento 0 (§7.3).
 */
@Converter(autoApply = true)
public class GeneroConverter implements AttributeConverter<Genero, String> {

    @Override
    public String convertToDatabaseColumn(Genero genero) {
        return genero == null ? null : genero.getCodigo();
    }

    @Override
    public Genero convertToEntityAttribute(String codigo) {
        return (codigo == null || codigo.isBlank()) ? null : Genero.toEnum(codigo);
    }
}
