package br.com.diegocordeiro.dscproject.dto.despesa;

import br.com.diegocordeiro.dscproject.enums.StatusPagamento;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DespesaRateioDTO {

    private Long usuarioId;
    private String usuarioNome;
    private BigDecimal valor;
    private StatusPagamento statusPagamento;
    private LocalDate dataAcerto;
}
