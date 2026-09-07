package br.com.diegocordeiro.dscproject.repository;

import br.com.diegocordeiro.dscproject.model.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    Optional<Perfil> findByCodigo(String codigo);

    List<Perfil> findAllByOrderByNomeAsc();
}
