package OTO.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findAllByOrderByNome();

    /** Ids dos estabelecimentos do usuário, sem carregar as entidades. */
    @Query("select e.id from Usuario u join u.estabelecimentos e where u.id = :usuarioId")
    List<Long> findEstabelecimentoIds(@Param("usuarioId") Long usuarioId);
}
