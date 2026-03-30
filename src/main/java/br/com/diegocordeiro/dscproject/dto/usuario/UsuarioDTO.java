package br.com.diegocordeiro.dscproject.dto.usuario;

import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.enums.Perfis;
import br.com.diegocordeiro.dscproject.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UsuarioDTO {
    private Long id;

    private String nome;

    private Genero genero;

    private String email;

    private String login;

    private String senha;

    private Perfis perfil;

    public UsuarioDTO(Usuario usuario) {
        this.id     = usuario.getId();
        this.nome   = usuario.getNome();
        this.genero = usuario.getGenero();
        this.email  = usuario.getEmail();
        this.login  = usuario.getLogin();
        this.senha  = usuario.getSenha();
        this.perfil = usuario.getPerfil();
    }
}
