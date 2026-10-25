package OTO.historico;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import OTO.pesquisa.Modalidade;

public interface IndicadorHistoricoRepository extends JpaRepository<IndicadorHistorico, Long> {

    List<IndicadorHistorico> findByIndicadorAndModalidadeOrderByAnoAscMesAsc(Indicador indicador, Modalidade modalidade);

    /** Valores gerais (todos os tipos) de um indicador, por mês ou por feriado. */
    @Query("""
            select h from IndicadorHistorico h
            where h.indicador = :indicador and h.modalidade = :modalidade
              and h.tipoHospedagemCodigo is null
              and h.ano between :anoInicio and :anoFim
            order by h.ano, h.mes
            """)
    List<IndicadorHistorico> findGerais(@Param("indicador") Indicador indicador,
            @Param("modalidade") Modalidade modalidade,
            @Param("anoInicio") short anoInicio,
            @Param("anoFim") short anoFim);

    List<IndicadorHistorico> findByIndicadorAndModalidadeAndAno(Indicador indicador, Modalidade modalidade, Short ano);
}
