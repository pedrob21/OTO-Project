package OTO.estabelecimento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CapacidadeHospedagemRepository extends JpaRepository<CapacidadeHospedagem, Long> {

    List<CapacidadeHospedagem> findByEstabelecimentoIdOrderByVigenteDesdeDesc(Long estabelecimentoId);

    Optional<CapacidadeHospedagem> findFirstByEstabelecimentoIdAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
            Long estabelecimentoId, LocalDate data);

    boolean existsByEstabelecimentoIdAndVigenteDesde(Long estabelecimentoId, LocalDate data);

    /** Capacidade vigente de cada estabelecimento em uma data (a de maior vigente_desde até ela). */
    @Query("""
            select c from CapacidadeHospedagem c
            join fetch c.estabelecimento e
            where c.vigenteDesde = (
                select max(c2.vigenteDesde) from CapacidadeHospedagem c2
                where c2.estabelecimento = c.estabelecimento and c2.vigenteDesde <= :data)
            """)
    List<CapacidadeHospedagem> findVigentesEm(@Param("data") LocalDate data);
}
