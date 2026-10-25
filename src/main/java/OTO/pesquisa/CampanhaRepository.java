package OTO.pesquisa;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampanhaRepository extends JpaRepository<Campanha, Long> {

    List<Campanha> findByRodadaIdOrderByPeriodoInicio(Long rodadaId);

    List<Campanha> findByRodadaIdIn(Collection<Long> rodadaIds);

    List<Campanha> findByModalidadeAndTipoPeriodoAndPeriodoInicio(Modalidade modalidade, TipoPeriodo tipoPeriodo,
            LocalDate periodoInicio);

    boolean existsByRodadaIdAndPublicadaEmIsNotNull(Long rodadaId);

    /** Campanhas publicadas, com a rodada já carregada, da mais recente para a mais antiga. */
    @Query("""
            select c from Campanha c join fetch c.rodada
            where c.publicadaEm is not null
            order by c.periodoInicio desc, c.id desc
            """)
    List<Campanha> findPublicadas();

    @Query("""
            select c from Campanha c join fetch c.rodada r
            where r.status = :status
            order by c.periodoInicio
            """)
    List<Campanha> findByStatusDaRodada(@Param("status") StatusRodada status);
}
