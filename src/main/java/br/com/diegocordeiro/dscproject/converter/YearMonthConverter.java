package br.com.diegocordeiro.dscproject.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Converte a competência (mês/ano de um lançamento financeiro) entre
 * {@link YearMonth} e o formato de coluna {@code yyyy-MM}.
 */
@Converter(autoApply = true)
public class YearMonthConverter implements AttributeConverter<YearMonth, String> {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public String convertToDatabaseColumn(YearMonth atributo) {
        return atributo != null ? atributo.format(FORMATO) : null;
    }

    @Override
    public YearMonth convertToEntityAttribute(String coluna) {
        return coluna != null ? YearMonth.parse(coluna, FORMATO) : null;
    }
}
