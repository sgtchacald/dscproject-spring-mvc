package br.com.diegocordeiro.dscproject.repository;

import br.com.diegocordeiro.dscproject.model.Perfil;
import br.com.diegocordeiro.dscproject.model.PerfilPermissao;
import br.com.diegocordeiro.dscproject.model.Permissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfilPermissaoRepository extends JpaRepository<PerfilPermissao, Long> {

    boolean existsByPerfilAndPermissao(Perfil perfil, Permissao permissao);
}
