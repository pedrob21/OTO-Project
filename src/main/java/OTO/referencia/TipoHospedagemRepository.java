package OTO.referencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoHospedagemRepository extends JpaRepository<TipoHospedagem, String> {

    List<TipoHospedagem> findAllByOrderByOrdem();
}
