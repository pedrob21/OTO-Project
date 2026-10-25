package OTO.pesquisa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CampanhaDiariaRepository extends JpaRepository<CampanhaDiaria, Long> {

    List<CampanhaDiaria> findByCampanhaId(Long campanhaId);
}
