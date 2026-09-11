package br.com.diegocordeiro.dscproject.repository;

import br.com.diegocordeiro.dscproject.model.Receita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceitaRepository extends JpaRepository<Receita, Long> {

    /** RN02 / C2 — usada antes de editar, registrar o recebimento ou excluir. */
    Optional<Receita> findByIdAndContaUsuarioIdAndDataExclusaoIsNull(Long id, Long usuarioId);

    /**
     * C1 — listagem para o grid. Não filtra {@code dataExclusao}: a receita
     * excluída continua aparecendo na grid, com a situação "Excluída".
     */
    @Query("""
        SELECT r FROM Receita r
        JOIN FETCH r.conta c
        LEFT JOIN FETCH r.categoria cat
        WHERE c.usuario.id = :usuarioId
        ORDER BY r.competencia DESC, r.dataLancamento DESC
        """)
    List<Receita> listarPorUsuario(@Param("usuarioId") Long usuarioId);
}
