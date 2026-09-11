package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.cartaocredito.CartaoCreditoGridDTO;
import br.com.diegocordeiro.dscproject.model.CartaoCredito;
import br.com.diegocordeiro.dscproject.repository.CartaoCreditoRepository;
import br.com.diegocordeiro.dscproject.repository.ContaRepository;
import br.com.diegocordeiro.dscproject.repository.ParametroGlobalRepository;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartaoCreditoService {

    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final ContaRepository contaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ParametroGlobalRepository parametroGlobalRepository;
    private final JdbcTemplate jdbcTemplate;

    public CartaoCreditoService(CartaoCreditoRepository cartaoCreditoRepository, ContaRepository contaRepository, UsuarioRepository usuarioRepository, ParametroGlobalRepository parametroGlobalRepository, JdbcTemplate jdbcTemplate) {
        this.cartaoCreditoRepository = cartaoCreditoRepository;
        this.contaRepository = contaRepository;
        this.usuarioRepository = usuarioRepository;
        this.parametroGlobalRepository = parametroGlobalRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<CartaoCreditoGridDTO> listarParaGrid(Long usuarioId) {
        List<CartaoCredito> cartoes = cartaoCreditoRepository.listarPorUsuario(usuarioId);
        return cartoes.stream()
            .map(c -> CartaoCreditoGridDTO.builder()
                .id(c.getId())
                .descricao(c.getDescricao())
                .bandeira(c.getBandeira())
                .finalCartao(c.getFinalCartao())
                .limite(c.getLimite())
                .diaFechamento(c.getDiaFechamento())
                .diaVencimento(c.getDiaVencimento())
                .contaId(c.getConta() != null ? c.getConta().getId() : null)
                .contaDescricao(c.getConta() != null ? c.getConta().getDescricao() : null)
                .qtdVinculos(contarVinculos(c.getId()))
                .ativo(c.isAtivo())
                .excluido(c.isExcluido())
                .build())
            .toList();
    }

    /** Soma faturas e despesas não excluídas vinculadas ao cartão (C4/RN08). */
    private long contarVinculos(Long cartaoId) {
        if (cartaoId == null) {
            return 0L;
        }
        return contarEmTabela("FATURAS_CARTAO", cartaoId) + contarEmTabela("DESPESAS", cartaoId);
    }

    private long contarEmTabela(String tabela, Long cartaoId) {
        try {
            String sql = "SELECT COUNT(*) FROM " + tabela + " WHERE CACR_ID = ? AND audit_data_exclusao IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, cartaoId);
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}
