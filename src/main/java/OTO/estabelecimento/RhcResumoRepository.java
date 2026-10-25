package OTO.estabelecimento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RhcResumoRepository extends JpaRepository<RhcResumo, Long> {

    List<RhcResumo> findAllByOrderByVigenteDesdeDesc();

    Optional<RhcResumo> findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(LocalDate data);

    boolean existsByVigenteDesde(LocalDate data);
}
