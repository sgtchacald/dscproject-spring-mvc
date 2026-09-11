package br.com.diegocordeiro.dscproject.web.sistema.converter;

import br.com.diegocordeiro.dscproject.enums.TipoConta;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToTipoContaConverter implements Converter<String, TipoConta> {

    @Override
    public TipoConta convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return TipoConta.porCodigo(source);
    }
}
