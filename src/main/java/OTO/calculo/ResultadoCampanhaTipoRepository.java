package OTO.calculo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoCampanhaTipoRepository extends JpaRepository<ResultadoCampanhaTipo, ResultadoCampanhaTipoId> {

    List<ResultadoCampanhaTipo> findByIdCampanhaId(Long campanhaId);

    void deleteByIdCampanhaId(Long campanhaId);
}
