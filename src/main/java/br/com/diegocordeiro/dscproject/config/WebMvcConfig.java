package br.com.diegocordeiro.dscproject.config;

import br.com.diegocordeiro.dscproject.web.sistema.converter.StringToGeneroConverter;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasenames("classpath:messages", "classpath:labels");
        ms.setDefaultEncoding("UTF-8");
        return ms;
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setValidationMessageSource(messageSource());
        return factory;
    }

    @Bean
    public org.springframework.web.filter.HiddenHttpMethodFilter hiddenHttpMethodFilter() {
        return new org.springframework.web.filter.HiddenHttpMethodFilter();
    }

    @Override
    public Validator getValidator() {
        return validator();
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToGeneroConverter());
        registry.addConverter(new br.com.diegocordeiro.dscproject.web.sistema.converter.StringToAplicaAConverter());
        registry.addConverter(new br.com.diegocordeiro.dscproject.web.sistema.converter.StringToTipoInstituicaoFinanceiraConverter());
        registry.addConverter(new br.com.diegocordeiro.dscproject.web.sistema.converter.StringToTipoContaConverter());
    }
}