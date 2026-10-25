package OTO.pesquisa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RespostaOcupacaoRepository extends JpaRepository<RespostaOcupacao, Long> {

    Optional<RespostaOcupacao> findByCampanhaIdAndEstabelecimentoId(Long campanhaId, Long estabelecimentoId);

    boolean existsByCampanhaId(Long campanhaId);

    /** Respostas de uma campanha com os estabelecimentos já carregados. */
    @Query("""
            select r from RespostaOcupacao r join fetch r.estabelecimento
            where r.campanha.id = :campanhaId
            """)
    List<RespostaOcupacao> findByCampanhaIdComEstabelecimento(@Param("campanhaId") Long campanhaId);

    @Query("""
            select r from RespostaOcupacao r join fetch r.estabelecimento
            where r.campanha.id in :campanhaIds
            """)
    List<RespostaOcupacao> findByCampanhaIdsComEstabelecimento(@Param("campanhaIds") Collection<Long> campanhaIds);

    /** Respostas dos estabelecimentos de um parceiro, com campanha e estabelecimento carregados. */
    @Query("""
            select r from RespostaOcupacao r join fetch r.campanha c join fetch r.estabelecimento
            where r.estabelecimento.id in :estabelecimentoIds
            order by c.periodoInicio desc, c.id desc
            """)
    List<RespostaOcupacao> findByEstabelecimentoIdsComCampanha(
            @Param("estabelecimentoIds") Collection<Long> estabelecimentoIds);
}
