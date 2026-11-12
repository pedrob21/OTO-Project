package OTO.usuario;

import static OTO.Fixtures.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import OTO.Fixtures;
import OTO.auth.LinkAcesso;
import OTO.auth.TipoToken;
import OTO.auth.TokenAcessoService;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.estabelecimento.EstabelecimentoRepository;
import OTO.usuario.UsuarioDtos.TrocaSenhaRequest;
import OTO.usuario.UsuarioDtos.UsuarioAlteracaoRequest;
import OTO.usuario.UsuarioDtos.UsuarioCriadoResponse;
import OTO.usuario.UsuarioDtos.UsuarioRequest;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarios;
    @Mock
    private EstabelecimentoRepository estabelecimentos;
    @Mock
    private TokenAcessoService tokensAcesso;
    @Mock
    private PasswordEncoder senhas;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private UsuarioService servico;

    private static final LinkAcesso LINK = new LinkAcesso("tk", TipoToken.CONVITE, Instant.now());

    @Test
    void listarConverteOsUsuarios() {
        when(usuarios.findAllByOrderByNome()).thenReturn(List.of(
                Fixtures.usuario(1, "Ana", "ana@oto.com", Perfil.ADMIN),
                Fixtures.usuario(2, "Bia", "bia@oto.com", Perfil.VISUALIZADOR)));
        assertThat(servico.listar()).extracting(UsuarioResponse::nome).containsExactly("Ana", "Bia");
    }

    @Test
    void buscarUsuarioInexistente() {
        when(usuarios.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.buscar(9L)).isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Usuário 9 não encontrado(a).");
    }

    @Test
    void criarGeraOConviteEAudita() {
        when(usuarios.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 10L));
        when(tokensAcesso.gerar(10L, TipoToken.CONVITE)).thenReturn(LINK);

        UsuarioCriadoResponse criado = servico.criar(
                new UsuarioRequest(" Ana ", " Ana@OTO.com ", Perfil.ADMIN, null), ADMIN);

        assertThat(criado.usuario().id()).isEqualTo(10L);
        assertThat(criado.usuario().nome()).isEqualTo("Ana");
        assertThat(criado.usuario().email()).isEqualTo("ana@oto.com");
        assertThat(criado.usuario().primeiroAcessoPendente()).isTrue();
        assertThat(criado.convite()).isEqualTo(LINK);
        verify(auditoria).registrar(eq(1L), eq("usuario"), eq(10L), eq("CRIAR"), isNull(), any());
    }

    @Test
    void emailRepetidoNaoCria() {
        when(usuarios.existsByEmail("ana@oto.com")).thenReturn(true);
        assertThatThrownBy(() -> servico.criar(new UsuarioRequest("Ana", "ana@oto.com", Perfil.ADMIN, null), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("e-mail");
        verify(usuarios, never()).save(any());
    }

    @Test
    void parceiroPrecisaDeEstabelecimentos() {
        assertThatThrownBy(() -> servico.criar(new UsuarioRequest("P", "p@x.com", Perfil.PARCEIRO, List.of()), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ao menos um estabelecimento");
    }

    @Test
    void parceiroComEstabelecimentosRepetidosEhVinculadoUmaVez() {
        when(estabelecimentos.findAllById(Set.of(7L)))
                .thenReturn(List.of(Fixtures.estabelecimento(7, "Hotel A", "HOTEL")));
        when(usuarios.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 10L));

        UsuarioCriadoResponse criado = servico.criar(
                new UsuarioRequest("P", "p@x.com", Perfil.PARCEIRO, List.of(7L, 7L)), ADMIN);

        assertThat(criado.usuario().estabelecimentos())
                .containsExactly(new UsuarioResponse.EstabelecimentoRef(7L, "Hotel A"));
    }

    @Test
    void estabelecimentoInexistenteNaoVincula() {
        when(estabelecimentos.findAllById(Set.of(7L, 8L)))
                .thenReturn(List.of(Fixtures.estabelecimento(7, "Hotel A", "HOTEL")));
        assertThatThrownBy(() -> servico.criar(new UsuarioRequest("P", "p@x.com", Perfil.PARCEIRO, List.of(7L, 8L)), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não existe");
    }

    @Test
    void soParceiroTemEstabelecimentos() {
        assertThatThrownBy(() -> servico.criar(new UsuarioRequest("V", "v@x.com", Perfil.VISUALIZADOR, List.of(7L)), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Só usuários PARCEIRO");
    }

    @Test
    void alterarAtualizaEAudita() {
        Usuario usuario = Fixtures.usuario(4, "Bia", "bia@oto.com", Perfil.VISUALIZADOR);
        when(usuarios.findById(4L)).thenReturn(Optional.of(usuario));

        UsuarioResponse resposta = servico.alterar(4L, new UsuarioAlteracaoRequest(" Beatriz ", Perfil.ADMIN, null), ADMIN);

        assertThat(resposta.nome()).isEqualTo("Beatriz");
        assertThat(resposta.perfil()).isEqualTo(Perfil.ADMIN);
        verify(auditoria).registrar(1L, "usuario", 4L, "ATUALIZAR",
                Map.of("nome", "Bia", "email", "bia@oto.com", "perfil", "VISUALIZADOR", "estabelecimentos", List.of()),
                Map.of("nome", "Beatriz", "email", "bia@oto.com", "perfil", "ADMIN", "estabelecimentos", List.of()));
    }

    @Test
    void adminNaoTiraOProprioPerfil() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(Fixtures.usuario(1, "Ana", "ana@oto.com", Perfil.ADMIN)));
        assertThatThrownBy(() -> servico.alterar(1L, new UsuarioAlteracaoRequest("Ana", Perfil.VISUALIZADOR, null), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("próprio perfil");
    }

    @Test
    void adminNaoSeInativa() {
        assertThatThrownBy(() -> servico.definirAtivo(1L, false, ADMIN)).isInstanceOf(RegraNegocioException.class);
        verify(usuarios, never()).findById(anyLong());
    }

    @Test
    void inativarEReativarOutroUsuario() {
        Usuario usuario = Fixtures.usuario(4, "Bia", "bia@oto.com", Perfil.VISUALIZADOR);
        when(usuarios.findById(4L)).thenReturn(Optional.of(usuario));

        servico.definirAtivo(4L, false, ADMIN);
        assertThat(usuario.isAtivo()).isFalse();
        verify(auditoria).registrar(1L, "usuario", 4L, "INATIVAR", null, null);

        servico.definirAtivo(4L, true, ADMIN);
        assertThat(usuario.isAtivo()).isTrue();
        verify(auditoria).registrar(1L, "usuario", 4L, "REATIVAR", null, null);
    }

    @Test
    void linkDeSenhaEhConviteParaQuemNuncaDefiniuSenha() {
        when(usuarios.findById(4L)).thenReturn(Optional.of(Fixtures.usuario(4, "Bia", "bia@oto.com", Perfil.PARCEIRO)));
        when(tokensAcesso.gerar(4L, TipoToken.CONVITE)).thenReturn(LINK);

        assertThat(servico.gerarLinkDeSenha(4L, ADMIN)).isEqualTo(LINK);
        verify(auditoria).registrar(1L, "usuario", 4L, "GERAR_LINK_CONVITE", null, null);
    }

    @Test
    void linkDeSenhaEhRedefinicaoParaQuemJaTemSenha() {
        Usuario usuario = Fixtures.usuario(4, "Bia", "bia@oto.com", Perfil.PARCEIRO);
        usuario.definirSenha("hash");
        LinkAcesso redefinicao = new LinkAcesso("tk", TipoToken.REDEFINICAO_SENHA, Instant.now());
        when(usuarios.findById(4L)).thenReturn(Optional.of(usuario));
        when(tokensAcesso.gerar(4L, TipoToken.REDEFINICAO_SENHA)).thenReturn(redefinicao);

        assertThat(servico.gerarLinkDeSenha(4L, ADMIN)).isEqualTo(redefinicao);
        verify(auditoria).registrar(1L, "usuario", 4L, "GERAR_LINK_REDEFINICAO_SENHA", null, null);
    }

    @Test
    void usuarioInativoNaoRecebeLink() {
        Usuario usuario = Fixtures.usuario(4, "Bia", "bia@oto.com", Perfil.PARCEIRO);
        usuario.definirAtivo(false);
        when(usuarios.findById(4L)).thenReturn(Optional.of(usuario));
        assertThatThrownBy(() -> servico.gerarLinkDeSenha(4L, ADMIN)).isInstanceOf(RegraNegocioException.class);
        verify(tokensAcesso, never()).gerar(anyLong(), any());
    }

    @Test
    void meusDadosSaoDoUsuarioLogado() {
        when(usuarios.findById(2L)).thenReturn(Optional.of(Fixtures.usuario(2, "Pedro", "p@x.com", Perfil.PARCEIRO)));
        assertThat(servico.meusDados(new UsuarioLogado(2L, Perfil.PARCEIRO)).email()).isEqualTo("p@x.com");
    }

    @Test
    void trocarSenhaConfereASenhaAtual() {
        Usuario usuario = Fixtures.usuario(2, "Pedro", "p@x.com", Perfil.PARCEIRO);
        usuario.definirSenha("hashAntigo");
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario));
        when(senhas.matches("antiga123", "hashAntigo")).thenReturn(true);
        when(senhas.encode("nova12345")).thenReturn("hashNovo");

        servico.trocarSenha(new UsuarioLogado(2L, Perfil.PARCEIRO), new TrocaSenhaRequest("antiga123", "nova12345"));

        assertThat(usuario.getSenhaHash()).isEqualTo("hashNovo");
        verify(auditoria).registrar(2L, "usuario", 2L, "TROCAR_SENHA", null, null);
    }

    @Test
    void senhaAtualErradaOuAusenteNaoTroca() {
        Usuario usuario = Fixtures.usuario(2, "Pedro", "p@x.com", Perfil.PARCEIRO);
        when(usuarios.findById(2L)).thenReturn(Optional.of(usuario));
        UsuarioLogado logado = new UsuarioLogado(2L, Perfil.PARCEIRO);

        assertThatThrownBy(() -> servico.trocarSenha(logado, new TrocaSenhaRequest("x", "nova12345")))
                .isInstanceOf(RegraNegocioException.class);

        usuario.definirSenha("hash");
        when(senhas.matches("errada", "hash")).thenReturn(false);
        assertThatThrownBy(() -> servico.trocarSenha(logado, new TrocaSenhaRequest("errada", "nova12345")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A senha atual está incorreta.");
        verify(senhas, never()).encode(any());
    }
}
