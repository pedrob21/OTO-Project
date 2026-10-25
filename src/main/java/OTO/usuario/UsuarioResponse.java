package OTO.usuario;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import OTO.estabelecimento.Estabelecimento;

/**
 * Dados do usuário devolvidos pela API.
 *
 * @param primeiroAcessoPendente o usuário ainda não definiu a senha pelo convite
 */
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Perfil perfil,
        boolean ativo,
        boolean primeiroAcessoPendente,
        Instant ultimoAcessoEm,
        List<EstabelecimentoRef> estabelecimentos) {

    public record EstabelecimentoRef(Long id, String nomeFantasia) {
    }

    /** Converte a entidade; chamar dentro de uma transação (carrega os estabelecimentos). */
    public static UsuarioResponse de(Usuario u) {
        List<EstabelecimentoRef> estabelecimentos = u.getEstabelecimentos().stream()
                .sorted(Comparator.comparing(Estabelecimento::getNomeFantasia))
                .map(e -> new EstabelecimentoRef(e.getId(), e.getNomeFantasia()))
                .toList();
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo(),
                u.getSenhaHash() == null, u.getUltimoAcessoEm(), estabelecimentos);
    }
}
