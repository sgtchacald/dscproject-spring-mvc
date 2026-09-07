package br.com.diegocordeiro.dscproject.dto.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Formulário de cadastro / edição de perfil. {@code permissoes} é a lista completa
 * de códigos de permissão que o perfil deve conceder após salvar.
 */
@Getter
@Setter
@NoArgsConstructor
public class PerfilFormDTO {

    private Long id;

    @NotBlank(message = "{perfil.codigo.obrigatorio}")
    @Size(max = 30, message = "{perfil.codigo.tamanho}")
    private String codigo;

    @NotBlank(message = "{perfil.nome.obrigatorio}")
    @Size(max = 100, message = "{perfil.nome.tamanho}")
    private String nome;

    @Size(max = 255, message = "{perfil.descricao.tamanho}")
    private String descricao;

    private List<String> permissoes = new ArrayList<>();

    public List<String> getPermissoes() {
        return permissoes == null ? new ArrayList<>() : permissoes;
    }
}
