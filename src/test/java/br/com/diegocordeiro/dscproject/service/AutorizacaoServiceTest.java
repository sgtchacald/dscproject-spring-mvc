package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizacaoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AutorizacaoService autorizacaoService;

    @Test
    void loadUserByUsername_encontraPorLogin() {
        Usuario usuario = new Usuario();
        usuario.setLogin("chacalsgt");
        usuario.setSenha("$2a$10$hash");
        when(usuarioRepository.findByLoginOrEmail("chacalsgt", "chacalsgt")).thenReturn(usuario);

        UserDetails result = autorizacaoService.loadUserByUsername("chacalsgt");

        assertThat(result.getUsername()).isEqualTo("chacalsgt");
    }

    @Test
    void loadUserByUsername_encontraPorEmail() {
        Usuario usuario = new Usuario();
        usuario.setLogin("chacalsgt");
        usuario.setEmail("sgt.chacal.d@gmail.com");
        usuario.setSenha("$2a$10$hash");
        when(usuarioRepository.findByLoginOrEmail("sgt.chacal.d@gmail.com", "sgt.chacal.d@gmail.com"))
            .thenReturn(usuario);

        UserDetails result = autorizacaoService.loadUserByUsername("sgt.chacal.d@gmail.com");

        assertThat(result.getUsername()).isEqualTo("chacalsgt");
    }

    @Test
    void loadUserByUsername_lancaExcecaoSeNaoEncontrar() {
        when(usuarioRepository.findByLoginOrEmail("naoexiste", "naoexiste")).thenReturn(null);

        assertThatThrownBy(() -> autorizacaoService.loadUserByUsername("naoexiste"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("naoexiste");
    }
}
