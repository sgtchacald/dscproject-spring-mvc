package br.com.diegocordeiro.dscproject.dto.parametro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Entrada da alteração inline do valor. Só {@code valor} e {@code motivo} são
 * graváveis pela tela — código, nome, descrição, módulo e tipo vêm do catálogo
 * do código e não entram no binding.
 */
@Getter
@Setter
public class ParametroValorFormDTO {

    @NotBlank(message = "{parametro.valor.obrigatorio}")
    private String valor;

    @NotBlank(message = "{parametro.motivo.obrigatorio}")
    @Size(max = 255, message = "{parametro.motivo.tamanho}")
    private String motivo;
}
