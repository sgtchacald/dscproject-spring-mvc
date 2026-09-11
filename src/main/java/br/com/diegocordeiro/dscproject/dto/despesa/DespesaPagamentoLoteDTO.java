package br.com.diegocordeiro.dscproject.dto.despesa;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DespesaPagamentoLoteDTO {

    @NotEmpty(message = "{msg.despesa.campo.obrigatorio}")
    private List<Long> ids;

    @NotNull(message = "{msg.despesa.campo.obrigatorio}")
    private LocalDate dataPagamento;
}
