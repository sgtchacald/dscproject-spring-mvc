package br.com.diegocordeiro.dscproject.repository;

import br.com.diegocordeiro.dscproject.model.Usuario;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Usuario findByLoginOrEmail(String login, String email);

    Optional<Usuario> findById(Long id);

    Usuario findByLogin(String login);

    @Query("SELECT u FROM Usuario u WHERE u.login = ?1 OR u.email = ?1")
    List<Usuario> findByCredenciaisList(String valor);

    Usuario findByEmail(String email);
}
