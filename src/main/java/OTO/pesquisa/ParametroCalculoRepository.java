package OTO.pesquisa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParametroCalculoRepository extends JpaRepository<ParametroCalculo, Long> {

    List<ParametroCalculo> findAllByOrderByVigenteDesdeDesc();

    Optional<ParametroCalculo> findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(LocalDate data);

    boolean existsByVigenteDesde(LocalDate data);
}
