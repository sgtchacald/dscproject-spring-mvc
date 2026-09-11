package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.receita.ReceitaGridDTO;
import br.com.diegocordeiro.dscproject.model.Receita;
import br.com.diegocordeiro.dscproject.repository.ReceitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReceitaService {

    private final ReceitaRepository receitaRepository;

    public ReceitaService(ReceitaRepository receitaRepository) {
        this.receitaRepository = receitaRepository;
    }

    @Transactional(readOnly = true)
    public List<ReceitaGridDTO> listarParaGrid(Long usuarioId) {
        List<Receita> receitas = receitaRepository.listarPorUsuario(usuarioId);
        return receitas.stream().map(this::paraGridDTO).toList();
    }

    private ReceitaGridDTO paraGridDTO(Receita r) {
        return ReceitaGridDTO.builder()
            .id(r.getId())
            .competencia(r.getCompetencia())
            .nome(r.getNome())
            .descricao(r.getDescricao())
            .valor(r.getValor())
            .dataLancamento(r.getDataLancamento())
            .dataRecebimento(r.getDataRecebimento())
            .recebido(r.isRecebido())
            .origem(r.getOrigem())
            .contaId(r.getConta() != null ? r.getConta().getId() : null)
            .contaDescricao(r.getConta() != null ? r.getConta().getDescricao() : null)
            .categoriaId(r.getCategoria() != null ? r.getCategoria().getId() : null)
            .categoriaNome(r.getCategoria() != null ? r.getCategoria().getNome() : null)
            .excluido(r.isExcluido())
            .build();
    }
}
