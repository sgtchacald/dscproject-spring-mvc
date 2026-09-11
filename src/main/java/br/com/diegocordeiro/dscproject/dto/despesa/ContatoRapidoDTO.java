package br.com.diegocordeiro.dscproject.dto.despesa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContatoRapidoDTO {

    @NotBlank(message = "{contato.validacao.nome.obrigatorio}")
    @Size(max = 150)
    private String nome;

    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String telefone;

    @Size(max = 100)
    private String chavePix;
}
