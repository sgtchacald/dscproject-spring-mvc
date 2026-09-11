package br.com.diegocordeiro.dscproject.dto.receita;

import br.com.diegocordeiro.dscproject.enums.OrigemLancamento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/** Saída de {@code /receitas/buscar/{id}} — dados para preencher o modal de edição (EDP03). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceitaEdicaoDTO {

    private Long id;
    private YearMonth competencia;
    private String nome;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dataLancamento;
    private LocalDate dataRecebimento;
    private boolean recebido;
    private OrigemLancamento origem;
    private Long contaId;
    private Long categoriaId;
}
