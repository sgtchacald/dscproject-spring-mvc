package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.model.Perfil;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.PerfilRepository;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import br.com.diegocordeiro.dscproject.service.exceptions.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static br.com.diegocordeiro.dscproject.support.TestFixtures.perfil;
import static br.com.diegocordeiro.dscproject.support.TestFixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PerfilRepository perfilRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private final Perfil admin = perfil("ADMIN", "USUARIOS_LISTAR");
    private final Perfil user = perfil("USER");

    @BeforeEach
    void stubComum() {
        lenient().when(perfilRepository.findByCodigo("ADMIN")).thenReturn(Optional.of(admin));
        lenient().when(perfilRepository.findByCodigo("USER")).thenReturn(Optional.of(user));
        lenient().when(passwordEncoder.encode(any())).thenReturn("$2a$10$novoHash");
        lenient().when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private UsuarioDTO dtoValido() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNome("Diego Cordeiro");
        dto.setGenero(Genero.MASCULINO);
        dto.setNascimento(LocalDate.of(1986, 5, 20));
        dto.setEmail("diego@test.com");
        dto.setLogin("diego");
        dto.setSenha("senha123");
        dto.setConfirmacaoSenha("senha123");
        dto.setPerfilCodigo("USER");
        return dto;
    }

    // ---------- RN02 ----------

    @Test
    void inserir_cifraSenhaComBCryptAntesDePersistir() {
        Usuario salvo = usuarioService.inserir(dtoValido());

        verify(passwordEncoder).encode(eq("senha123"));
        assertThat(salvo.getSenha()).isEqualTo("$2a$10$novoHash");
    }

    // ---------- RN08 ----------

    @Test
    void autoCadastrar_ignoraPerfilEnviadoEForcaUser() {
        UsuarioDTO dto = dtoValido();
        dto.setPerfilCodigo("ADMIN");

        Usuario salvo = usuarioService.autoCadastrar(dto);

        assertThat(salvo.getPerfil().getCodigo()).isEqualTo("USER");
    }

    @Test
    void inserir_usaOPerfilInformadoNoDto() {
        UsuarioDTO dto = dtoValido();
        dto.setPerfilCodigo("ADMIN");

        Usuario salvo = usuarioService.inserir(dto);

        assertThat(salvo.getPerfil().getCodigo()).isEqualTo("ADMIN");
    }

    // ---------- RN07 ----------

    @Test
    void editar_senhaVazia_mantemSenhaAtual() {
        Usuario existente = usuario(7L, "diego", user);
        existente.setSenha("$2a$10$hashAntigo");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(existente));

        UsuarioDTO dto = dtoValido();
        dto.setSenha(null);
        dto.setConfirmacaoSenha(null);

        Usuario salvo = usuarioService.editar(7L, dto);

        assertThat(salvo.getSenha()).isEqualTo("$2a$10$hashAntigo");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void editar_senhaPreenchida_reencodaSenha() {
        Usuario existente = usuario(7L, "diego", user);
        existente.setSenha("$2a$10$hashAntigo");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(existente));

        UsuarioDTO dto = dtoValido();
        dto.setSenha("novaSenha");
        dto.setConfirmacaoSenha("novaSenha");

        Usuario salvo = usuarioService.editar(7L, dto);

        verify(passwordEncoder).encode("novaSenha");
        assertThat(salvo.getSenha()).isEqualTo("$2a$10$novoHash");
    }

    // ---------- RN10 ----------

    @Test
    void excluir_proprioUsuario_lancaRegraNegocio() {
        Usuario alvo = usuario(5L, "diego", admin);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(alvo));

        assertThatThrownBy(() -> usuarioService.excluir(5L, "diego"))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessage("usuario.exclusao.proprio");

        verify(usuarioRepository, never()).save(any());
    }

    // ---------- RN11 ----------

    @Test
    void excluir_ultimoAdminAtivo_lancaRegraNegocio() {
        Usuario alvo = usuario(9L, "outroAdmin", admin);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(alvo));
        when(usuarioRepository.contarAdminsAtivos()).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.excluir(9L, "quemEstaLogado"))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessage("usuario.ultimo.admin");
    }

    @Test
    void excluir_admin_comOutrosAdmins_efetuaExclusaoLogica() {
        Usuario alvo = usuario(9L, "outroAdmin", admin);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(alvo));
        when(usuarioRepository.contarAdminsAtivos()).thenReturn(2L);

        usuarioService.excluir(9L, "logado");

        assertThat(alvo.getDataExclusao()).isNotNull();
        assertThat(alvo.getExcluidoPor()).isNotBlank();
        verify(usuarioRepository).save(alvo);
    }

    @Test
    void editar_rebaixaUltimoAdminParaUser_lancaRegraNegocio() {
        Usuario existente = usuario(3L, "adminUnico", admin);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.contarAdminsAtivos()).thenReturn(1L);

        UsuarioDTO dto = dtoValido();
        dto.setPerfilCodigo("USER");

        assertThatThrownBy(() -> usuarioService.editar(3L, dto))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessage("usuario.ultimo.admin");
    }

    // ---------- RN04 / C2 ----------

    @Test
    void verificarSeExiste_delegaAoRepositorioComIdAtual() {
        when(usuarioRepository.contarPorLoginOuEmail("diego", 4L)).thenReturn(1L);

        assertThat(usuarioService.verificarSeExiste("diego", 4L)).isTrue();
    }

    @Test
    void verificarSeExiste_valorVazio_retornaFalseSemConsultar() {
        assertThat(usuarioService.verificarSeExiste("  ", null)).isFalse();
        verify(usuarioRepository, never()).contarPorLoginOuEmail(any(), any());
    }
}
