package br.com.diegocordeiro.dscproject.dto.usuario;

import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.enums.Perfis;
import br.com.diegocordeiro.dscproject.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UsuarioDTO {

    private Long id;

    @NotBlank(message = "{usuario.nome.obrigatorio}")
    @Size(max = 100, message = "{usuario.nome.tamanho}")
    private String nome;

    @NotNull(message = "{usuario.genero.obrigatorio}")
    private Genero genero;

    @NotNull(message = "{usuario.nascimento.obrigatoria}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date nascimento;

    @NotBlank(message = "{usuario.email.obrigatorio}")
    @Email(message = "{usuario.email.invalido}")
    private String email;

    @NotBlank(message = "{usuario.login.obrigatorio}")
    @Size(max = 40, message = "{usuario.login.tamanho}")
    private String login;

    @NotBlank(message = "{usuario.senha.obrigatoria}")
    @Size(min = 6, message = "{usuario.senha.tamanho}")
    private String senha;

    @NotBlank(message = "{usuario.confirmacaoSenha.obrigatoria}")
    private String confirmacaoSenha;

    private Perfis perfil;

    //Teste gitflow

    public UsuarioDTO(Usuario usuario) {
        this.id      = usuario.getId();
        this.nome    = usuario.getNome();
        this.genero  = usuario.getGenero();
        this.email   = usuario.getEmail();
        this.login   = usuario.getLogin();
        this.senha   = usuario.getSenha();
        this.perfil  = usuario.getPerfil();
    }
}
