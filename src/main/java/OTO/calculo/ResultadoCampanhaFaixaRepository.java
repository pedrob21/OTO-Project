package OTO.calculo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoCampanhaFaixaRepository extends JpaRepository<ResultadoCampanhaFaixa, ResultadoCampanhaFaixaId> {

    List<ResultadoCampanhaFaixa> findByIdCampanhaId(Long campanhaId);

    void deleteByIdCampanhaId(Long campanhaId);
}
