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

    private Long contatoId;

    private boolean acertado;

    private LocalDate dataAcerto;

    public Long getUsuarioId() {
        return contatoId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.contatoId = usuarioId;
    }
}
