package br.com.diegocordeiro.dscproject.dto.perfil;

import lombok.Getter;

/**
 * Linha do grid de perfis: código, nome e as contagens de permissões ativas e de
 * usuários ativos vinculados. Populada por consulta JPQL (construtor abaixo).
 */
@Getter
public class PerfilResumoDTO {

    private final Long id;
    private final String codigo;
    private final String nome;
    private final boolean sistema;
    private final long qtdPermissoes;
    private final long qtdUsuarios;

    public PerfilResumoDTO(Long id,
                           String codigo,
                           String nome,
                           Boolean sistema,
                           long qtdPermissoes,
                           long qtdUsuarios) {
        this.id = id;
        this.codigo = codigo;
        this.nome = nome;
        this.sistema = Boolean.TRUE.equals(sistema);
        this.qtdPermissoes = qtdPermissoes;
        this.qtdUsuarios = qtdUsuarios;
    }
}
