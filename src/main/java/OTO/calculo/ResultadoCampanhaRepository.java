package OTO.calculo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoCampanhaRepository extends JpaRepository<ResultadoCampanha, Long> {

    List<ResultadoCampanha> findByCampanhaIdIn(Collection<Long> campanhaIds);

    boolean existsByParametroCalculoId(Long parametroCalculoId);
}
