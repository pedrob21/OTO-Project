package OTO.estabelecimento;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, Long> {

    List<Estabelecimento> findAllByOrderByNomeFantasia();

    List<Estabelecimento> findByIdInOrderByNomeFantasia(Collection<Long> ids);

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);
}
