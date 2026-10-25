package OTO.auth;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenUsuarioRepository extends JpaRepository<TokenUsuario, Long> {

    Optional<TokenUsuario> findByTokenHash(String tokenHash);

    List<TokenUsuario> findByUsuarioIdAndUsadoEmIsNull(Long usuarioId);
}
