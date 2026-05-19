package br.com.diegocordeiro.dscproject.model;

import br.com.diegocordeiro.dscproject.enums.Perfis;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioUserDetailsTest {

    @Test
    void getPassword_deveRetornarSenha() {
        Usuario usuario = new Usuario();
        usuario.setSenha("$2a$10$hash");

        assertThat(usuario.getPassword()).isEqualTo("$2a$10$hash");
    }

    @Test
    void getUsername_deveRetornarLogin() {
        Usuario usuario = new Usuario();
        usuario.setLogin("chacalsgt");

        assertThat(usuario.getUsername()).isEqualTo("chacalsgt");
    }

    @Test
    void getAuthorities_adminDeveRetornarDoisPapeis() {
        Usuario usuario = new Usuario();
        usuario.setPerfil(Perfis.ADMIN);

        Collection<? extends GrantedAuthority> authorities = usuario.getAuthorities();

        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void getAuthorities_userDeveRetornarUmPapel() {
        Usuario usuario = new Usuario();
        usuario.setPerfil(Perfis.USER);

        Collection<? extends GrantedAuthority> authorities = usuario.getAuthorities();

        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");
    }
}
