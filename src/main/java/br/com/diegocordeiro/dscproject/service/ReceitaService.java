package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.receita.ReceitaFormDTO;
import br.com.diegocordeiro.dscproject.dto.receita.ReceitaGridDTO;
import br.com.diegocordeiro.dscproject.enums.AplicaA;
import br.com.diegocordeiro.dscproject.enums.OrigemLancamento;
import br.com.diegocordeiro.dscproject.model.Categoria;
import br.com.diegocordeiro.dscproject.model.Conta;
import br.com.diegocordeiro.dscproject.model.Receita;
import br.com.diegocordeiro.dscproject.repository.CategoriaRepository;
import br.com.diegocordeiro.dscproject.repository.ContaRepository;
import br.com.diegocordeiro.dscproject.repository.ReceitaRepository;
import br.com.diegocordeiro.dscproject.service.exceptions.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class ReceitaService {

    private final ReceitaRepository receitaRepository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;

    public ReceitaService(ReceitaRepository receitaRepository, ContaRepository contaRepository, CategoriaRepository categoriaRepository) {
        this.receitaRepository = receitaRepository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public Receita inserir(ReceitaFormDTO dto, Long usuarioId, String usuarioAuditoria) {
        Conta conta = validarConta(dto.getContaId(), usuarioId);
        Categoria categoria = validarCategoriaSeInformada(dto.getCategoriaId());

        Receita receita = new Receita();
        receita.setNome(normalizarTexto(dto.getNome()));
        receita.setDescricao(normalizarTexto(dto.getDescricao()));
        receita.setValor(dto.getValor());
        receita.setDataLancamento(dto.getDataLancamento());
        receita.setCompetencia(YearMonth.parse(dto.getCompetencia()));
        receita.setConta(conta);
        receita.setCategoria(categoria);
        receita.setOrigem(OrigemLancamento.MANUAL);

        aplicarRecebimento(receita, dto.isRecebido(), dto.getDataRecebimento());

        return receitaRepository.save(receita);
    }

    private void aplicarRecebimento(Receita receita, boolean recebido, LocalDate dataRecebimento) {
        receita.setRecebido(recebido);
        receita.setDataRecebimento(recebido ? dataRecebimento : null);
    }

    private Conta validarConta(Long contaId, Long usuarioId) {
        return contaRepository.findByIdAndUsuarioIdAndDataExclusaoIsNull(contaId, usuarioId)
            .filter(Conta::isAtivo)
            .orElseThrow(() -> new RegraNegocioException("contaId", "msg.receita.conta.invalida"));
    }

    private Categoria validarCategoriaSeInformada(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return categoriaRepository.findByIdAndDataExclusaoIsNull(categoriaId)
            .filter(Categoria::isAtivo)
            .filter(c -> c.getAplicaA() == AplicaA.RECEITA || c.getAplicaA() == AplicaA.AMBOS)
            .orElseThrow(() -> new RegraNegocioException("categoriaId", "msg.receita.categoria.invalida"));
    }

    private String normalizarTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
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
