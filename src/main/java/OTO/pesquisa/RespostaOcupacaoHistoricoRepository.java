package OTO.pesquisa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RespostaOcupacaoHistoricoRepository extends JpaRepository<RespostaOcupacaoHistorico, Long> {

    List<RespostaOcupacaoHistorico> findByRespostaIdOrderByVersaoDesc(Long respostaId);
}
