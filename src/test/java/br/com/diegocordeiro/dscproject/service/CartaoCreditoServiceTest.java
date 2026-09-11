package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.cartaocredito.CartaoCreditoGridDTO;
import br.com.diegocordeiro.dscproject.model.CartaoCredito;
import br.com.diegocordeiro.dscproject.model.Conta;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.CartaoCreditoRepository;
import br.com.diegocordeiro.dscproject.repository.ContaRepository;
import br.com.diegocordeiro.dscproject.repository.ParametroGlobalRepository;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartaoCreditoServiceTest {

    @Mock
    private CartaoCreditoRepository cartaoCreditoRepository;

    @Mock
    private ContaRepository contaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ParametroGlobalRepository parametroGlobalRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private CartaoCreditoService service;

    @BeforeEach
    void setUp() {
        service = new CartaoCreditoService(cartaoCreditoRepository, contaRepository, usuarioRepository, parametroGlobalRepository, jdbcTemplate);
    }

    private Usuario criarUsuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setLogin("user" + id);
        return u;
    }

    @Test
    @DisplayName("BDD 16.0 / RN02 - Lista apenas os cartões do usuário autenticado, com dados da conta de débito")
    void listarParaGrid_deveRetornarCartoesDoUsuarioComContagemDeVinculos() {
        Conta conta = new Conta();
        conta.setId(7L);
        conta.setDescricao("Conta Corrente");

        CartaoCredito cartao = new CartaoCredito();
        cartao.setId(1L);
        cartao.setDescricao("Nubank Ultravioleta");
        cartao.setBandeira("MASTERCARD");
        cartao.setFinalCartao("1234");
        cartao.setLimite(new BigDecimal("5000.00"));
        cartao.setDiaFechamento(3);
        cartao.setDiaVencimento(10);
        cartao.setConta(conta);
        cartao.setAtivo(true);
        cartao.setUsuario(criarUsuario(1L));

        when(cartaoCreditoRepository.listarPorUsuario(1L)).thenReturn(List.of(cartao));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1L))).thenReturn(0L);

        List<CartaoCreditoGridDTO> grid = service.listarParaGrid(1L);

        assertEquals(1, grid.size());
        CartaoCreditoGridDTO dto = grid.get(0);
        assertEquals("Nubank Ultravioleta", dto.getDescricao());
        assertEquals("MASTERCARD", dto.getBandeira());
        assertEquals("1234", dto.getFinalCartao());
        assertEquals(7L, dto.getContaId());
        assertEquals("Conta Corrente", dto.getContaDescricao());
        assertTrue(dto.isAtivo());
        assertFalse(dto.isExcluido());
        assertEquals(0L, dto.getQtdVinculos());
    }

    @Test
    @DisplayName("Cartão sem conta de débito expõe contaId/contaDescricao nulos")
    void listarParaGrid_semContaDebito_deveExporContaNula() {
        CartaoCredito cartao = new CartaoCredito();
        cartao.setId(2L);
        cartao.setDescricao("Cartão sem conta");
        cartao.setUsuario(criarUsuario(1L));

        when(cartaoCreditoRepository.listarPorUsuario(1L)).thenReturn(List.of(cartao));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(2L))).thenReturn(0L);

        CartaoCreditoGridDTO dto = service.listarParaGrid(1L).get(0);
        assertNull(dto.getContaId());
        assertNull(dto.getContaDescricao());
    }

    @Test
    @DisplayName("RN08/C4 - Contabiliza vínculos de faturas e despesas para exibir na grid")
    void listarParaGrid_comVinculos_deveSomarFaturasEDespesas() {
        CartaoCredito cartao = new CartaoCredito();
        cartao.setId(3L);
        cartao.setDescricao("Nubank");
        cartao.setUsuario(criarUsuario(1L));

        when(cartaoCreditoRepository.listarPorUsuario(1L)).thenReturn(List.of(cartao));
        when(jdbcTemplate.queryForObject(contains("FATURAS_CARTAO"), eq(Long.class), eq(3L))).thenReturn(1L);
        when(jdbcTemplate.queryForObject(contains("DESPESAS"), eq(Long.class), eq(3L))).thenReturn(2L);

        CartaoCreditoGridDTO dto = service.listarParaGrid(1L).get(0);
        assertEquals(3L, dto.getQtdVinculos());
    }
}
