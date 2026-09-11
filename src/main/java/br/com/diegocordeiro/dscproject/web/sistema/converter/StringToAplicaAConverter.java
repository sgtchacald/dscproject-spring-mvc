package br.com.diegocordeiro.dscproject.web.sistema.converter;

import br.com.diegocordeiro.dscproject.enums.AplicaA;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToAplicaAConverter implements Converter<String, AplicaA> {

    @Override
    public AplicaA convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        try {
            return AplicaA.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
