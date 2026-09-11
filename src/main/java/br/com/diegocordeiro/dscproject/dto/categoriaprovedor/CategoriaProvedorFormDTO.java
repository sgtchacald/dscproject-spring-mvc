package br.com.diegocordeiro.dscproject.dto.categoriaprovedor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaProvedorFormDTO {

    private Long id;

    @NotBlank(message = "{categoriaprovedor.validacao.rotulo.obrigatorio}")
    @Size(max = 120, message = "{categoriaprovedor.validacao.rotulo.tamanho}")
    private String rotuloExterno;

    @NotNull(message = "{categoriaprovedor.validacao.provedor.obrigatorio}")
    private Long provedorId;

    @NotNull(message = "{categoriaprovedor.validacao.categoria.obrigatorio}")
    private Long categoriaId;
}
