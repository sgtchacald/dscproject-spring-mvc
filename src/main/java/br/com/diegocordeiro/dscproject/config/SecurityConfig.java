package br.com.diegocordeiro.dscproject.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Rotas públicas — auto-cadastro, recuperação de senha, checagem em tempo real, estáticos. */
    private static final String[] PUBLICO_GET = {
        "/login",
        "/usuarios/existe",
        "/usuarios/cadastrar-site",
        "/usuarios/recuperar-senha",
        "/webjars/**", "/css/**", "/js/**", "/image/**"
    };
    private static final String[] PUBLICO_POST = {
        "/usuarios/cadastrar-site",
        "/usuarios/recuperar-senha/solicitar",
        "/usuarios/recuperar-senha/confirmar"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, @Value("${app.remember-me.key}") String rememberMeKey) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, PUBLICO_GET).permitAll()
                .requestMatchers(HttpMethod.POST, PUBLICO_POST).permitAll()

                // CRUD administrativo — cada operação exige a sua permissão
                .requestMatchers(HttpMethod.GET, "/usuarios/listar", "/usuarios/listar-dados")
                    .hasAuthority("PERM_USUARIOS_LISTAR")
                .requestMatchers(HttpMethod.GET, "/usuarios/buscar/**")
                    .hasAuthority("PERM_USUARIOS_EDITAR")
                .requestMatchers(HttpMethod.POST, "/usuarios/inserir")
                    .hasAuthority("PERM_USUARIOS_INSERIR")
                .requestMatchers(HttpMethod.PUT, "/usuarios/*/senha")
                    .hasAuthority("PERM_USUARIOS_EDITAR")
                .requestMatchers(HttpMethod.PUT, "/usuarios/editar/**")
                    .hasAuthority("PERM_USUARIOS_EDITAR")
                .requestMatchers(HttpMethod.DELETE, "/usuarios/excluir/**")
                    .hasAuthority("PERM_USUARIOS_EXCLUIR")
                .requestMatchers(HttpMethod.GET, "/usuarios/historico/**")
                    .hasAuthority("PERM_USUARIOS_VER_HISTORICO")

                // Perfis e Permissões — cada operação exige a sua permissão
                .requestMatchers(HttpMethod.GET, "/perfis/listar", "/perfis/listar-dados", "/permissoes/listar-dados")
                    .hasAuthority("PERM_PERFIS_LISTAR")
                .requestMatchers(HttpMethod.GET, "/perfis/buscar/**")
                    .hasAuthority("PERM_PERFIS_MANTER")
                .requestMatchers(HttpMethod.POST, "/perfis/inserir")
                    .hasAuthority("PERM_PERFIS_MANTER")
                .requestMatchers(HttpMethod.PUT, "/perfis/editar/**")
                    .hasAuthority("PERM_PERFIS_MANTER")
                .requestMatchers(HttpMethod.DELETE, "/perfis/excluir/**")
                    .hasAuthority("PERM_PERFIS_MANTER")
                .requestMatchers(HttpMethod.POST, "/permissoes/sincronizar-catalogo")
                    .hasAuthority("PERM_PERFIS_SINCRONIZAR_CATALOGO")

                // Parâmetros Globais — cada operação exige a sua permissão
                .requestMatchers(HttpMethod.GET, "/parametros/listar", "/parametros/listar-dados", "/parametros/historico/**")
                    .hasAuthority("PERM_PARAMETROS_LISTAR")
                .requestMatchers(HttpMethod.GET, "/parametros/buscar/**")
                    .hasAuthority("PERM_PARAMETROS_EDITAR")
                .requestMatchers(HttpMethod.PUT, "/parametros/editar/**", "/parametros/restaurar-padrao/**")
                    .hasAuthority("PERM_PARAMETROS_EDITAR")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .rememberMe(rm -> rm
                .key(rememberMeKey)
                .rememberMeParameter("lembrar")
                .tokenValiditySeconds(14 * 24 * 60 * 60)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
