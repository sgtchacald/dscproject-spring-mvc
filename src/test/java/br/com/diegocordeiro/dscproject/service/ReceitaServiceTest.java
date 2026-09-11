package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.receita.ReceitaGridDTO;
import br.com.diegocordeiro.dscproject.enums.OrigemLancamento;
import br.com.diegocordeiro.dscproject.model.Categoria;
import br.com.diegocordeiro.dscproject.model.Conta;
import br.com.diegocordeiro.dscproject.model.Receita;
import br.com.diegocordeiro.dscproject.repository.ReceitaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceitaServiceTest {

    @Mock
    private ReceitaRepository receitaRepository;

    private ReceitaService receitaService;

    private Receita criarReceita(Long id, Long contaUsuarioId, boolean recebido, boolean excluida) {
        Conta conta = new Conta();
        conta.setId(10L);
        conta.setDescricao("Nubank Conta Corrente");
        var usuario = new br.com.diegocordeiro.dscproject.model.Usuario();
        usuario.setId(contaUsuarioId);
        conta.setUsuario(usuario);

        Categoria categoria = new Categoria();
        categoria.setId(20L);
        categoria.setNome("Salário");

        Receita r = new Receita();
        r.setId(id);
        r.setNome("Salário");
        r.setDescricao(null);
        r.setValor(new BigDecimal("5000.00"));
        r.setDataLancamento(LocalDate.of(2026, 9, 5));
        r.setCompetencia(YearMonth.of(2026, 9));
        r.setConta(conta);
        r.setCategoria(categoria);
        r.setOrigem(OrigemLancamento.MANUAL);
        r.setRecebido(recebido);
        if (recebido) {
            r.setDataRecebimento(LocalDate.of(2026, 9, 6));
        }
        if (excluida) {
            r.setDataExclusao(java.time.Instant.now());
            r.setExcluidoPor("user1");
        }
        return r;
    }

    @Test
    @DisplayName("RF01 / RF07 / RN02 - Lista as receitas apenas do usuário autenticado, mapeando todos os campos do grid")
    void listarParaGrid_deveConsultarPorUsuarioEMapearCampos() {
        receitaService = new ReceitaService(receitaRepository);
        Receita receita = criarReceita(1L, 1L, false, false);
        when(receitaRepository.listarPorUsuario(1L)).thenReturn(List.of(receita));

        List<ReceitaGridDTO> grid = receitaService.listarParaGrid(1L);

        assertEquals(1, grid.size());
        ReceitaGridDTO dto = grid.get(0);
        assertEquals(1L, dto.getId());
        assertEquals(YearMonth.of(2026, 9), dto.getCompetencia());
        assertEquals("Salário", dto.getNome());
        assertEquals(new BigDecimal("5000.00"), dto.getValor());
        assertEquals(LocalDate.of(2026, 9, 5), dto.getDataLancamento());
        assertFalse(dto.isRecebido());
        assertNull(dto.getDataRecebimento());
        assertEquals(OrigemLancamento.MANUAL, dto.getOrigem());
        assertEquals(10L, dto.getContaId());
        assertEquals("Nubank Conta Corrente", dto.getContaDescricao());
        assertEquals(20L, dto.getCategoriaId());
        assertEquals("Salário", dto.getCategoriaNome());
        assertFalse(dto.isExcluido());

        verify(receitaRepository).listarPorUsuario(1L);
    }

    @Test
    @DisplayName("QUADRO_DESCRITIVO_1/ID13 - Receita recebida aparece com dataRecebimento preenchida")
    void listarParaGrid_receitaRecebida_devePreencherDataRecebimento() {
        receitaService = new ReceitaService(receitaRepository);
        Receita receita = criarReceita(2L, 1L, true, false);
        when(receitaRepository.listarPorUsuario(1L)).thenReturn(List.of(receita));

        ReceitaGridDTO dto = receitaService.listarParaGrid(1L).get(0);

        assertTrue(dto.isRecebido());
        assertEquals(LocalDate.of(2026, 9, 6), dto.getDataRecebimento());
    }

    @Test
    @DisplayName("QUADRO_DESCRITIVO_1/ID13 - Receita excluída continua na listagem, sinalizada como excluída (C1 não filtra soft delete)")
    void listarParaGrid_receitaExcluida_apareceNaListaSinalizada() {
        receitaService = new ReceitaService(receitaRepository);
        Receita receita = criarReceita(3L, 1L, false, true);
        when(receitaRepository.listarPorUsuario(1L)).thenReturn(List.of(receita));

        ReceitaGridDTO dto = receitaService.listarParaGrid(1L).get(0);

        assertTrue(dto.isExcluido());
    }

    @Test
    @DisplayName("Receita sem categoria vem com categoria nula no grid")
    void listarParaGrid_semCategoria_deveVirComCategoriaNula() {
        receitaService = new ReceitaService(receitaRepository);
        Receita receita = criarReceita(4L, 1L, false, false);
        receita.setCategoria(null);
        when(receitaRepository.listarPorUsuario(1L)).thenReturn(List.of(receita));

        ReceitaGridDTO dto = receitaService.listarParaGrid(1L).get(0);

        assertNull(dto.getCategoriaId());
        assertNull(dto.getCategoriaNome());
    }
}
