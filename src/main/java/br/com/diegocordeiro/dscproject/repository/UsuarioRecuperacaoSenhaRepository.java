package br.com.diegocordeiro.dscproject.repository;

import br.com.diegocordeiro.dscproject.model.UsuarioRecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRecuperacaoSenhaRepository extends JpaRepository<UsuarioRecuperacaoSenha, Long> {

    /** C3 — busca o registro pelo hash do token (RN14). */
    Optional<UsuarioRecuperacaoSenha> findByTokenHashAndDataExclusaoIsNull(String tokenHash);

    /** Tokens pendentes do usuário — a invalidar a cada nova solicitação (RN13). */
    List<UsuarioRecuperacaoSenha> findByUsuarioIdAndUtilizadoFalseAndDataExclusaoIsNull(Long usuarioId);

    /** RN16 — quantas solicitações o usuário fez desde {@code limite}. */
    long countByUsuarioIdAndDataCriacaoAfter(Long usuarioId, Instant limite);
}
