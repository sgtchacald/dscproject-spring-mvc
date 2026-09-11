package br.com.diegocordeiro.dscproject.dto.instituicaoprovedor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstituicaoProvedorFormDTO {

    private Long id;

    @NotBlank(message = "{instituicaoprovedor.validacao.idExterno.obrigatorio}")
    @Size(max = 80, message = "{instituicaoprovedor.validacao.idExterno.tamanho}")
    private String idExterno;

    @NotNull(message = "{instituicaoprovedor.validacao.provedor.obrigatorio}")
    private Long provedorId;

    @NotNull(message = "{instituicaoprovedor.validacao.instituicao.obrigatorio}")
    private Long instituicaoId;
}
