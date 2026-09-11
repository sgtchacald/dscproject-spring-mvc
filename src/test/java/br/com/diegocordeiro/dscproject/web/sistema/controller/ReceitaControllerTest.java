package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.config.SecurityConfig;
import br.com.diegocordeiro.dscproject.dto.receita.ReceitaGridDTO;
import br.com.diegocordeiro.dscproject.enums.OrigemLancamento;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import br.com.diegocordeiro.dscproject.service.AutorizacaoService;
import br.com.diegocordeiro.dscproject.service.ReceitaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReceitaController.class)
@Import(SecurityConfig.class)
class ReceitaControllerTest {

    private MockMvc mockMvc;

    @MockitoBean
    private ReceitaService receitaService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private AutorizacaoService autorizacaoService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        Usuario usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setLogin("user_teste");
        usuarioMock.setNome("Usuário Teste");

        when(usuarioRepository.findByLogin("user_teste")).thenReturn(Optional.of(usuarioMock));
    }

    @Test
    @DisplayName("BDD 16.0 - Listar view com autoridade PERM_RECEITAS_LISTAR")
    @WithMockUser(username = "user_teste", authorities = "PERM_RECEITAS_LISTAR")
    void listar_comPermissao_deveRetornar200EViewListar() throws Exception {
        mockMvc.perform(get("/receitas/listar"))
                .andExpect(status().isOk())
                .andExpect(view().name("sistema/modulos/receita/listar"));
    }

    @Test
    @DisplayName("BDD 16.1 - Bloquear acesso de usuário sem PERM_RECEITAS_LISTAR à tela e à listagem")
    @WithMockUser(username = "user_teste", authorities = "ROLE_USER")
    void listar_semPermissao_deveRetornar403() throws Exception {
        mockMvc.perform(get("/receitas/listar"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/receitas/listar-dados"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BDD 16.0 / 16.2 / 16.18 - Listar dados JSON da grid restrito ao usuário autenticado")
    @WithMockUser(username = "user_teste", authorities = "PERM_RECEITAS_LISTAR")
    void listarDados_comPermissao_deveRetornarJsonGridDoUsuario() throws Exception {
        when(receitaService.listarParaGrid(1L)).thenReturn(List.of(
                ReceitaGridDTO.builder()
                        .id(10L)
                        .competencia(YearMonth.of(2026, 9))
                        .nome("Salário")
                        .valor(new BigDecimal("5000.00"))
                        .dataLancamento(LocalDate.of(2026, 9, 5))
                        .recebido(false)
                        .origem(OrigemLancamento.MANUAL)
                        .contaId(1L)
                        .contaDescricao("Nubank Conta Corrente")
                        .excluido(false)
                        .build()
        ));

        mockMvc.perform(get("/receitas/listar-dados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].nome").value("Salário"))
                .andExpect(jsonPath("$[0].competencia").value("2026-09"))
                .andExpect(jsonPath("$[0].valor").value(5000.00))
                .andExpect(jsonPath("$[0].recebido").value(false))
                .andExpect(jsonPath("$[0].origem").value("MANUAL"));

        verify(receitaService).listarParaGrid(1L);
    }
}
