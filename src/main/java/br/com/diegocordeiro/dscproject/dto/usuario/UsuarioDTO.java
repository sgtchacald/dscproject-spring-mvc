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

    @NotBlank(message = "Nome é obrigatório.")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
    private String nome;

    @NotNull(message = "Gênero é obrigatório.")
    private Genero genero;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date nascimento;

    @NotBlank(message = "E-mail é obrigatório.")
    @Email(message = "E-mail inválido.")
    private String email;

    @NotBlank(message = "Login é obrigatório.")
    @Size(max = 40, message = "Login deve ter no máximo 40 caracteres.")
    private String login;

    @NotBlank(message = "Senha é obrigatória.")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres.")
    private String senha;

    private String confirmacaoSenha;

    private Perfis perfil;

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
