package br.com.diegocordeiro.dscproject.config;

import br.com.diegocordeiro.dscproject.model.Perfil;
import br.com.diegocordeiro.dscproject.model.Permissao;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.PerfilPermissaoRepository;
import br.com.diegocordeiro.dscproject.repository.PerfilRepository;
import br.com.diegocordeiro.dscproject.repository.PermissaoRepository;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CargaInicialRunnerTest {

    @Mock private PerfilRepository perfilRepository;
    @Mock private PermissaoRepository permissaoRepository;
    @Mock private PerfilPermissaoRepository perfilPermissaoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @BeforeEach
    void catalogoJaPopulado() {
        when(perfilRepository.findByCodigo(anyString()))
            .thenReturn(Optional.of(new Perfil("ADMIN", "ADMIN", null, true)));
        when(permissaoRepository.findByCodigo(anyString()))
            .thenReturn(Optional.of(new Permissao("X", "X", null, "Usuários")));
        when(perfilPermissaoRepository.existsByPerfilAndPermissao(any(), any())).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
    }

    private CargaInicialRunner runner(String login, String email, String senha, boolean obrigatorio) {
        return new CargaInicialRunner(perfilRepository, permissaoRepository, perfilPermissaoRepository,
            usuarioRepository, passwordEncoder, login, email, senha, "Administrador", obrigatorio);
    }

    @Test
    void semAdminAtivoSemCredencialEObrigatorio_falhaNoBoot() {
        when(usuarioRepository.contarAdminsAtivos()).thenReturn(0L);

        assertThatThrownBy(() -> runner("", "", "", true).run(null))
            .isInstanceOf(IllegalStateException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void semAdminAtivoSemCredencialENaoObrigatorio_apenasAvisa() {
        when(usuarioRepository.contarAdminsAtivos()).thenReturn(0L);

        runner("", "", "", false).run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void semAdminAtivoComCredencial_criaAdmin() {
        when(usuarioRepository.contarAdminsAtivos()).thenReturn(0L);
        when(usuarioRepository.findByLoginOrEmail(any(), any())).thenReturn(null);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        runner("admin", "admin@dsc.com", "s3nha", true).run(null);

        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void jaExisteAdminAtivo_naoRecria() {
        when(usuarioRepository.contarAdminsAtivos()).thenReturn(1L);

        runner("admin", "admin@dsc.com", "s3nha", true).run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void loginDoAdminJaEmUsoPorOutroUsuario_naoCriaEFalhaSeObrigatorio() {
        when(usuarioRepository.contarAdminsAtivos()).thenReturn(0L);
        when(usuarioRepository.findByLoginOrEmail(any(), any()))
            .thenReturn(new Usuario());

        assertThatThrownBy(() -> runner("admin", "admin@dsc.com", "s3nha", true).run(null))
            .isInstanceOf(IllegalStateException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void encodaASenhaDoAdmin() {
        when(usuarioRepository.contarAdminsAtivos()).thenReturn(0L);
        when(usuarioRepository.findByLoginOrEmail(any(), any())).thenReturn(null);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        runner("admin", "admin@dsc.com", "s3nha", false).run(null);

        verify(passwordEncoder).encode("s3nha");
    }
}
