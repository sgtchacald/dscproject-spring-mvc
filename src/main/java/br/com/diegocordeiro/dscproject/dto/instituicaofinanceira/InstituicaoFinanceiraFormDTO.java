package br.com.diegocordeiro.dscproject.dto.instituicaofinanceira;

import br.com.diegocordeiro.dscproject.enums.TipoInstituicaoFinanceira;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstituicaoFinanceiraFormDTO {

    private Long id;

    @NotBlank(message = "{instituicao.validacao.nome.obrigatorio}")
    @Size(max = 100, message = "{instituicao.validacao.nome.tamanho}")
    private String nome;

    @Size(max = 100, message = "{instituicao.validacao.codigo.tamanho}")
    @Pattern(regexp = "^\\d*$", message = "{instituicao.validacao.codigo.digitos}")
    private String codigo;

    @NotNull(message = "{instituicao.validacao.tipo.obrigatorio}")
    private TipoInstituicaoFinanceira tipo;

    private boolean ativo = true;

    private boolean sistema = false;
}
