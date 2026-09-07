package br.com.diegocordeiro.dscproject.config;

import br.com.diegocordeiro.dscproject.util.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    /**
     * Autor das colunas {@code audit_criado_por} / {@code audit_alterado_por}:
     * o login autenticado, ou {@code "sistema"} quando não há autenticação
     * (carga inicial, jobs, fluxos públicos).
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(SecurityUtils.loginAtual());
    }
}
