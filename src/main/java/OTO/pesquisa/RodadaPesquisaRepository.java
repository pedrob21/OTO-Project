package OTO.pesquisa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RodadaPesquisaRepository extends JpaRepository<RodadaPesquisa, Long> {

    List<RodadaPesquisa> findAllByOrderByCriadoEmDesc();

    List<RodadaPesquisa> findByStatusOrderByCriadoEmDesc(StatusRodada status);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
