package br.com.diegocordeiro.dscproject.web.sistema.converter;

import br.com.diegocordeiro.dscproject.enums.TipoInstituicaoFinanceira;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToTipoInstituicaoFinanceiraConverter implements Converter<String, TipoInstituicaoFinanceira> {

    @Override
    public TipoInstituicaoFinanceira convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        try {
            return TipoInstituicaoFinanceira.toEnum(source.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
