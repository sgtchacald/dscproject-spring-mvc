package br.com.diegocordeiro.dscproject.web.sistema.converter;

import br.com.diegocordeiro.dscproject.enums.Genero;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToGeneroConverter implements Converter<String, Genero> {

    @Override
    public Genero convert(String source) {
        return Genero.toEnum(source);
    }
}