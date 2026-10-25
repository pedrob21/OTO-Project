package OTO.comum.seguranca;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import OTO.usuario.UsuarioRepository;

/**
 * regras de acesso por estabelecimento: ADMIN e VISUALIZADOR leem tudo;
 * PARCEIRO só os estabelecimentos vinculados a ele; VISUALIZADOR nunca altera.
 */
@Component
public class ControleAcesso {

    private final UsuarioRepository usuarios;

    public ControleAcesso(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public List<Long> estabelecimentosDo(UsuarioLogado usuario) {
        return usuarios.findEstabelecimentoIds(usuario.id());
    }

    public void exigirLeitura(UsuarioLogado usuario, Long estabelecimentoId) {
        if (usuario.isParceiro() && !estabelecimentosDo(usuario).contains(estabelecimentoId)) {
            throw new AccessDeniedException("Estabelecimento não vinculado ao usuário.");
        }
    }

    public void exigirEscrita(UsuarioLogado usuario, Long estabelecimentoId) {
        if (usuario.isVisualizador()) {
            throw new AccessDeniedException("Perfil somente leitura.");
        }
        exigirLeitura(usuario, estabelecimentoId);
    }
}
