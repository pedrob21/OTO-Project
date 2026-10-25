package OTO.usuario;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.auth.LinkAcesso;
import OTO.auth.TipoToken;
import OTO.auth.TokenAcessoService;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.estabelecimento.Estabelecimento;
import OTO.estabelecimento.EstabelecimentoRepository;
import OTO.usuario.UsuarioDtos.TrocaSenhaRequest;
import OTO.usuario.UsuarioDtos.UsuarioAlteracaoRequest;
import OTO.usuario.UsuarioDtos.UsuarioCriadoResponse;
import OTO.usuario.UsuarioDtos.UsuarioRequest;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final EstabelecimentoRepository estabelecimentos;
    private final TokenAcessoService tokensAcesso;
    private final PasswordEncoder senhas;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarios, EstabelecimentoRepository estabelecimentos,
            TokenAcessoService tokensAcesso, PasswordEncoder senhas, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.estabelecimentos = estabelecimentos;
        this.tokensAcesso = tokensAcesso;
        this.senhas = senhas;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findAllByOrderByNome().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.de(carregar(id));
    }

    /** Cria o usuário sem senha e devolve o convite de primeiro acesso. */
    @Transactional
    public UsuarioCriadoResponse criar(UsuarioRequest requisicao, UsuarioLogado admin) {
        String email = requisicao.email().trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmail(email)) {
            throw new RegraNegocioException("Já existe um usuário com este e-mail.");
        }
        Usuario usuario = new Usuario(requisicao.nome().trim(), email, requisicao.perfil());
        usuario.substituirEstabelecimentos(estabelecimentosDoPerfil(requisicao.perfil(), requisicao.estabelecimentoIds()));
        usuarios.save(usuario);
        LinkAcesso convite = tokensAcesso.gerar(usuario.getId(), TipoToken.CONVITE);
        auditoria.registrar(admin.id(), "usuario", usuario.getId(), "CRIAR", null, resumo(usuario));
        return new UsuarioCriadoResponse(UsuarioResponse.de(usuario), convite);
    }

    @Transactional
    public UsuarioResponse alterar(Long id, UsuarioAlteracaoRequest requisicao, UsuarioLogado admin) {
        Usuario usuario = carregar(id);
        if (usuario.getId().equals(admin.id()) && requisicao.perfil() != Perfil.ADMIN) {
            throw new RegraNegocioException("Você não pode tirar o seu próprio perfil de administrador.");
        }
        Map<String, Object> antes = resumo(usuario);
        usuario.alterarDados(requisicao.nome().trim(), requisicao.perfil());
        usuario.substituirEstabelecimentos(estabelecimentosDoPerfil(requisicao.perfil(), requisicao.estabelecimentoIds()));
        auditoria.registrar(admin.id(), "usuario", id, "ATUALIZAR", antes, resumo(usuario));
        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public void definirAtivo(Long id, boolean ativo, UsuarioLogado admin) {
        if (id.equals(admin.id()) && !ativo) {
            throw new RegraNegocioException("Você não pode inativar o seu próprio usuário.");
        }
        Usuario usuario = carregar(id);
        usuario.definirAtivo(ativo);
        auditoria.registrar(admin.id(), "usuario", id, ativo ? "REATIVAR" : "INATIVAR", null, null);
    }

    /** Novo link para o usuário definir a senha (convite expirado ou senha esquecida). */
    @Transactional
    public LinkAcesso gerarLinkDeSenha(Long id, UsuarioLogado admin) {
        Usuario usuario = carregar(id);
        if (!usuario.isAtivo()) {
            throw new RegraNegocioException("Reative o usuário antes de gerar um link de acesso.");
        }
        TipoToken tipo = usuario.getSenhaHash() == null ? TipoToken.CONVITE : TipoToken.REDEFINICAO_SENHA;
        LinkAcesso link = tokensAcesso.gerar(id, tipo);
        auditoria.registrar(admin.id(), "usuario", id, "GERAR_LINK_" + tipo.name(), null, null);
        return link;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse meusDados(UsuarioLogado logado) {
        return UsuarioResponse.de(carregar(logado.id()));
    }

    @Transactional
    public void trocarSenha(UsuarioLogado logado, TrocaSenhaRequest requisicao) {
        Usuario usuario = carregar(logado.id());
        if (usuario.getSenhaHash() == null || !senhas.matches(requisicao.senhaAtual(), usuario.getSenhaHash())) {
            throw new RegraNegocioException("A senha atual está incorreta.");
        }
        usuario.definirSenha(senhas.encode(requisicao.novaSenha()));
        auditoria.registrar(logado.id(), "usuario", logado.id(), "TROCAR_SENHA", null, null);
    }

    private Usuario carregar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> RecursoNaoEncontradoException.de("Usuário", id));
    }

    private Set<Estabelecimento> estabelecimentosDoPerfil(Perfil perfil, List<Long> ids) {
        boolean semIds = ids == null || ids.isEmpty();
        if (perfil != Perfil.PARCEIRO) {
            if (!semIds) {
                throw new RegraNegocioException("Só usuários PARCEIRO são vinculados a estabelecimentos.");
            }
            return Set.of();
        }
        if (semIds) {
            throw new RegraNegocioException("Informe ao menos um estabelecimento para o parceiro.");
        }
        Set<Long> unicos = new HashSet<>(ids);
        List<Estabelecimento> encontrados = estabelecimentos.findAllById(unicos);
        if (encontrados.size() != unicos.size()) {
            throw new RegraNegocioException("Algum dos estabelecimentos informados não existe.");
        }
        return new HashSet<>(encontrados);
    }

    private static Map<String, Object> resumo(Usuario u) {
        return Map.of(
                "nome", u.getNome(),
                "email", u.getEmail(),
                "perfil", u.getPerfil().name(),
                "estabelecimentos", u.getEstabelecimentos().stream().map(Estabelecimento::getId).sorted().toList());
    }
}
