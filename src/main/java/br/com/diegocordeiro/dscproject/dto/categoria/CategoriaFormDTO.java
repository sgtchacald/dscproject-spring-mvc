package br.com.diegocordeiro.dscproject.dto.categoria;

import br.com.diegocordeiro.dscproject.enums.AplicaA;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaFormDTO {

    private Long id;

    @NotBlank(message = "{categoria.validacao.codigo.obrigatorio}")
    @Size(max = 40, message = "{categoria.validacao.codigo.tamanho}")
    private String codigo;

    @NotBlank(message = "{categoria.validacao.nome.obrigatorio}")
    @Size(max = 100, message = "{categoria.validacao.nome.tamanho}")
    private String nome;

    @NotNull(message = "{categoria.validacao.aplicaA.obrigatorio}")
    private AplicaA aplicaA;

    @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "{categoria.validacao.cor.invalida}")
    private String cor;

    @Size(max = 40, message = "{categoria.validacao.icone.tamanho}")
    private String icone;

    private boolean ativo = true;

    private boolean sistema = false;
}
