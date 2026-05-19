package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.enums.Perfis;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceInserirTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private UsuarioDTO dtoBase() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNome("Diego Cordeiro");
        dto.setGenero(Genero.MASCULINO);
        dto.setEmail("diego@test.com");
        dto.setSenha("senha123");
        return dto;
    }

    @Test
    void inserir_loginDscordeiro86_atribuiAdmin() {
        UsuarioDTO dto = dtoBase();
        dto.setLogin("dscordeiro86");
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.inserir(dto);

        assertThat(result.getPerfil()).isEqualTo(Perfis.ADMIN);
    }

    @Test
    void inserir_loginComum_atribuiUser() {
        UsuarioDTO dto = dtoBase();
        dto.setLogin("joao");
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.inserir(dto);

        assertThat(result.getPerfil()).isEqualTo(Perfis.USER);
    }

    @Test
    void inserir_codificaSenhaComEncoder() {
        UsuarioDTO dto = dtoBase();
        dto.setLogin("qualquer");
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.inserir(dto);

        verify(passwordEncoder).encode(eq("senha123"));
        assertThat(result.getSenha()).isEqualTo("$2a$hash");
    }

    @Test
    void inserir_persisteNoBancoChamandoSave() {
        UsuarioDTO dto = dtoBase();
        dto.setLogin("qualquer");
        when(passwordEncoder.encode(any())).thenReturn("$2a$hash");
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        usuarioService.inserir(dto);

        verify(usuarioRepository).save(any(Usuario.class));
    }
}
