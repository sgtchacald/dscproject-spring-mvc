package br.com.diegocordeiro.dscproject.dto.recuperacaosenha;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Etapa 1 de recuperação de senha: o usuário informa o e-mail. */
@Getter
@Setter
@NoArgsConstructor
public class RecuperarSenhaSolicitacaoDTO {

    @NotBlank(message = "{usuario.email.obrigatorio}")
    @Email(message = "{usuario.email.invalido}")
    private String email;
}
