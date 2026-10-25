package OTO.referencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FaixaOcupacaoRepository extends JpaRepository<FaixaOcupacao, String> {

    List<FaixaOcupacao> findAllByOrderByPercentualMinimo();
}
