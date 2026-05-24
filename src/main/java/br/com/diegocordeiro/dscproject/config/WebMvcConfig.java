package br.com.diegocordeiro.dscproject.config;

import br.com.diegocordeiro.dscproject.web.sistema.converter.StringToGeneroConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToGeneroConverter());
    }
}