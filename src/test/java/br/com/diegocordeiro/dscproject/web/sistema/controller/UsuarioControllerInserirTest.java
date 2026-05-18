package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.service.AutorizacaoService;
import br.com.diegocordeiro.dscproject.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerInserirTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private AutorizacaoService autorizacaoService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void inserir_comDadosValidos_redirecionaComSucesso() throws Exception {
        when(usuarioService.verificarSeExisteUsuario(any())).thenReturn(false);
        when(usuarioService.inserir(any())).thenReturn(new Usuario());

        mockMvc.perform(post("/usuarios/inserir")
                .with(csrf())
                .param("nome", "João Silva")
                .param("genero", "MASCULINO")
                .param("email", "joao@test.com")
                .param("login", "joaosilva")
                .param("senha", "senha123")
                .param("confirmacaoSenha", "senha123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?cadastroSucesso=true"));
    }

    @Test
    void inserir_senhasDivergentes_redirecionaComErro() throws Exception {
        mockMvc.perform(post("/usuarios/inserir")
                .with(csrf())
                .param("nome", "João Silva")
                .param("genero", "MASCULINO")
                .param("email", "joao@test.com")
                .param("login", "joaosilva")
                .param("senha", "senha123")
                .param("confirmacaoSenha", "outrasenha"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void inserir_loginJaCadastrado_redirecionaComErro() throws Exception {
        when(usuarioService.verificarSeExisteUsuario("joaosilva")).thenReturn(true);
        when(usuarioService.verificarSeExisteUsuario("joao@test.com")).thenReturn(false);

        mockMvc.perform(post("/usuarios/inserir")
                .with(csrf())
                .param("nome", "João Silva")
                .param("genero", "MASCULINO")
                .param("email", "joao@test.com")
                .param("login", "joaosilva")
                .param("senha", "senha123")
                .param("confirmacaoSenha", "senha123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void inserir_nomeFaltando_redirecionaComErro() throws Exception {
        mockMvc.perform(post("/usuarios/inserir")
                .with(csrf())
                .param("nome", "")
                .param("genero", "MASCULINO")
                .param("email", "joao@test.com")
                .param("login", "joaosilva")
                .param("senha", "senha123")
                .param("confirmacaoSenha", "senha123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }
}
