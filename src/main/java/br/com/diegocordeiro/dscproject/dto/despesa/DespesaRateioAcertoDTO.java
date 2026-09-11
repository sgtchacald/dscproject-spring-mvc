package br.com.diegocordeiro.dscproject.dto.despesa;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DespesaRateioAcertoDTO {

    @NotNull(message = "{msg.despesa.campo.obrigatorio}")
    private Long usuarioId;

    private boolean acertado;

    private LocalDate dataAcerto;
}
