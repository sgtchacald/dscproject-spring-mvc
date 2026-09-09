package br.com.diegocordeiro.dscproject.dto.parametro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Entrada da ação "restaurar padrão" — só o motivo obrigatório. */
@Getter
@Setter
public class RestaurarPadraoFormDTO {

    @NotBlank(message = "{parametro.motivo.obrigatorio}")
    @Size(max = 255, message = "{parametro.motivo.tamanho}")
    private String motivo;
}
