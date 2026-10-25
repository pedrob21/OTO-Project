package OTO.referencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeriadoEventoRepository extends JpaRepository<FeriadoEvento, String> {

    List<FeriadoEvento> findAllByOrderByOrdem();
}
