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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceitaGridDTO {

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
    private String contaDescricao;
    private Long categoriaId;
    private String categoriaNome;
    private boolean excluido;
}
