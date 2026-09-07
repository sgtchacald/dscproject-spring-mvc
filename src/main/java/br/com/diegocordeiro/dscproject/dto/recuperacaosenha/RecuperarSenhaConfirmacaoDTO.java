package br.com.diegocordeiro.dscproject.dto.recuperacaosenha;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Etapa 2 de recuperação de senha (EDP11). */
@Getter
@Setter
@NoArgsConstructor
public class RecuperarSenhaConfirmacaoDTO {

    @NotBlank(message = "{recuperacao.token.obrigatorio}")
    private String token;

    @NotBlank(message = "{usuario.senha.obrigatoria}")
    @Size(min = 6, message = "{usuario.senha.tamanho}")
    private String senha;

    private String confirmacaoSenha;

    public boolean senhasConferem() {
        return senha != null && senha.equals(confirmacaoSenha);
    }
}
