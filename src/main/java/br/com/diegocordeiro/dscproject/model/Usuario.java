package br.com.diegocordeiro.dscproject.model;

import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.enums.Perfis;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

@Data @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
@Entity
@Table(name="USUARIOS")
@EntityListeners(AuditingEntityListener.class)
public class Usuario implements UserDetails, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USU_ID", nullable = false)
    private Long id;

    @Column(name = "USU_NOME", length = 100, nullable = false)
    private String nome;

    @Column(name = "USU_GENERO", length = 1, nullable = false)
    private Genero genero;

    @Column(name = "USU_DT_NASCIMENTO", nullable = true)
    private Date nascimento;

    @Column(name = "USU_EMAIL", length = 512, nullable = false, unique = true)
    private String email;

    @Column(name = "USU_LOGIN", length = 40, nullable = false, unique = true)
    private String login;

    @Column(name = "USU_SENHA", length = 1024, nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "USU_PERFIL", nullable = false)
    private Perfis perfil;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.perfil == Perfis.ADMIN) {
            return List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        return this.login;
    }
}
