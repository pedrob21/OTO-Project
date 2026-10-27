package OTO.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import OTO.comum.excecao.RegraNegocioException;

@ExtendWith(MockitoExtension.class)
class TokenAcessoServiceTest {

    @Mock
    private TokenUsuarioRepository tokens;

    @InjectMocks
    private TokenAcessoService servico;

    @Test
    void conviteValeSeteDiasEGuardaSoOHash() {
        when(tokens.findByUsuarioIdAndUsadoEmIsNull(5L)).thenReturn(List.of());

        LinkAcesso link = servico.gerar(5L, TipoToken.CONVITE);

        ArgumentCaptor<TokenUsuario> salvo = ArgumentCaptor.forClass(TokenUsuario.class);
        verify(tokens).save(salvo.capture());
        assertThat(link.tipo()).isEqualTo(TipoToken.CONVITE);
        assertThat(link.token()).matches("[A-Za-z0-9_-]{43}");
        assertThat(link.expiraEm()).isCloseTo(Instant.now().plus(Duration.ofDays(7)), within(1, ChronoUnit.MINUTES));
        assertThat(salvo.getValue().getUsuarioId()).isEqualTo(5L);
        assertThat(salvo.getValue().getExpiraEm()).isEqualTo(link.expiraEm());
        assertThat(salvo.getValue().getUsadoEm()).isNull();
    }

    @Test
    void redefinicaoValeUmDia() {
        LinkAcesso link = servico.gerar(5L, TipoToken.REDEFINICAO_SENHA);
        assertThat(link.expiraEm()).isCloseTo(Instant.now().plus(Duration.ofHours(24)), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void novoLinkInvalidaOsAnterioresNaoUsados() {
        TokenUsuario antigo1 = new TokenUsuario(5L, TipoToken.CONVITE, "h1", Instant.now().plusSeconds(60));
        TokenUsuario antigo2 = new TokenUsuario(5L, TipoToken.CONVITE, "h2", Instant.now().plusSeconds(60));
        when(tokens.findByUsuarioIdAndUsadoEmIsNull(5L)).thenReturn(List.of(antigo1, antigo2));

        servico.gerar(5L, TipoToken.REDEFINICAO_SENHA);

        assertThat(antigo1.getUsadoEm()).isNotNull();
        assertThat(antigo2.getUsadoEm()).isNotNull();
    }

    @Test
    void tokensGeradosSaoDiferentes() {
        assertThat(servico.gerar(5L, TipoToken.CONVITE).token()).isNotEqualTo(servico.gerar(5L, TipoToken.CONVITE).token());
    }

    @Test
    void validarProcuraPeloHash() {
        TokenUsuario token = new TokenUsuario(5L, TipoToken.CONVITE, "h", Instant.now().plusSeconds(60));
        when(tokens.findByTokenHash(TokenAcessoService.hash("abc"))).thenReturn(Optional.of(token));
        assertThat(servico.validar("abc")).isSameAs(token);
    }

    @Test
    void tokenInexistenteEhInvalido() {
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.validar("abc")).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Link inválido");
    }

    @Test
    void tokenVencidoOuUsadoEhRecusado() {
        TokenUsuario vencido = new TokenUsuario(5L, TipoToken.CONVITE, "h", Instant.now().minusSeconds(1));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(vencido));
        assertThatThrownBy(() -> servico.validar("abc")).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("expirou ou já foi usado");

        TokenUsuario usado = new TokenUsuario(5L, TipoToken.CONVITE, "h", Instant.now().plusSeconds(60));
        usado.marcarUsado();
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(usado));
        assertThatThrownBy(() -> servico.validar("abc")).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void hashEhSha256EmHexadecimal() {
        assertThat(TokenAcessoService.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
