package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.config.SecurityConfig;
import br.com.diegocordeiro.dscproject.dto.cartaocredito.CartaoCreditoGridDTO;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.CartaoCreditoRepository;
import br.com.diegocordeiro.dscproject.repository.ContaRepository;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import br.com.diegocordeiro.dscproject.service.AutorizacaoService;
import br.com.diegocordeiro.dscproject.service.CartaoCreditoService;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CartaoCreditoController.class)
@Import(SecurityConfig.class)
class CartaoCreditoControllerTest {

    private MockMvc mockMvc;

    @MockitoBean
    private CartaoCreditoService cartaoCreditoService;

    @MockitoBean
    private CartaoCreditoRepository cartaoCreditoRepository;

    @MockitoBean
    private ContaRepository contaRepository;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private AutorizacaoService autorizacaoService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setLogin("user_teste");
        usuarioMock.setNome("Usuário Teste");

        when(usuarioRepository.findByLogin("user_teste")).thenReturn(Optional.of(usuarioMock));
    }

    @Test
    @DisplayName("Listar view com autoridade PERM_CARTOES_LISTAR")
    @WithMockUser(username = "user_teste", authorities = "PERM_CARTOES_LISTAR")
    void listar_comPermissao_deveRetornar200EViewListar() throws Exception {
        mockMvc.perform(get("/cartoes/listar"))
                .andExpect(status().isOk())
                .andExpect(view().name("sistema/modulos/cartao/listar"));
    }

    @Test
    @DisplayName("BDD 16.1 / RN01 - Bloquear acesso de perfil sem CARTOES_LISTAR")
    @WithMockUser(username = "user_teste", authorities = "ROLE_USER")
    void listar_semPermissao_deveRetornar403() throws Exception {
        mockMvc.perform(get("/cartoes/listar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BDD 16.1 - Bloquear acesso de perfil sem CARTOES_LISTAR também no endpoint de dados")
    @WithMockUser(username = "user_teste", authorities = "ROLE_USER")
    void listarDados_semPermissao_deveRetornar403() throws Exception {
        mockMvc.perform(get("/cartoes/listar-dados"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BDD 16.0 / BDD 16.2 - Listar dados JSON da grid com isolamento por usuário")
    @WithMockUser(username = "user_teste", authorities = "PERM_CARTOES_LISTAR")
    void listarDados_comPermissao_deveRetornarGridDoUsuario() throws Exception {
        when(cartaoCreditoService.listarParaGrid(1L)).thenReturn(List.of(
                CartaoCreditoGridDTO.builder()
                        .id(10L)
                        .descricao("Nubank Ultravioleta")
                        .bandeira("MASTERCARD")
                        .finalCartao("1234")
                        .limite(new BigDecimal("5000.00"))
                        .diaFechamento(3)
                        .diaVencimento(10)
                        .ativo(true)
                        .build()
        ));

        mockMvc.perform(get("/cartoes/listar-dados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].descricao").value("Nubank Ultravioleta"))
                .andExpect(jsonPath("$[0].bandeira").value("MASTERCARD"));

        verify(cartaoCreditoService).listarParaGrid(1L);
    }
}
