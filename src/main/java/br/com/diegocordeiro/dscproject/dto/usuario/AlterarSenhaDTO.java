package br.com.diegocordeiro.dscproject.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nova senha e sua confirmação para a ação administrativa dedicada do grid.
 * Não carrega nenhum outro dado do usuário — a ação não altera mais nada.
 */
@Getter
@Setter
@NoArgsConstructor
public class AlterarSenhaDTO {

    @NotBlank(message = "{usuario.senha.obrigatoria}")
    @Size(min = 6, message = "{usuario.senha.tamanho}")
    private String senha;

    private String confirmacaoSenha;

    public boolean senhasConferem() {
        return senha != null && senha.equals(confirmacaoSenha);
    }
}
