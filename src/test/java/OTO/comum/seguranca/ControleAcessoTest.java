package OTO.comum.seguranca;

import static OTO.Fixtures.ADMIN;
import static OTO.Fixtures.PARCEIRO;
import static OTO.Fixtures.VISUALIZADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import OTO.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ControleAcessoTest {

    @Mock
    private UsuarioRepository usuarios;

    @InjectMocks
    private ControleAcesso acesso;

    @Test
    void estabelecimentosDoUsuarioVemDoRepositorio() {
        when(usuarios.findEstabelecimentoIds(2L)).thenReturn(List.of(10L, 11L));
        assertThat(acesso.estabelecimentosDo(PARCEIRO)).containsExactly(10L, 11L);
    }

    @Test
    void adminEVisualizadorLeemQualquerEstabelecimento() {
        assertThatCode(() -> acesso.exigirLeitura(ADMIN, 99L)).doesNotThrowAnyException();
        assertThatCode(() -> acesso.exigirLeitura(VISUALIZADOR, 99L)).doesNotThrowAnyException();
        verify(usuarios, never()).findEstabelecimentoIds(anyLong());
    }

    @Test
    void parceiroSoLeOsSeus() {
        when(usuarios.findEstabelecimentoIds(2L)).thenReturn(List.of(10L));
        assertThatCode(() -> acesso.exigirLeitura(PARCEIRO, 10L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> acesso.exigirLeitura(PARCEIRO, 11L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void visualizadorNuncaAltera() {
        assertThatThrownBy(() -> acesso.exigirEscrita(VISUALIZADOR, 10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Perfil somente leitura.");
    }

    @Test
    void parceiroAlteraSoOsSeusEAdminAlteraTudo() {
        when(usuarios.findEstabelecimentoIds(2L)).thenReturn(List.of(10L));
        assertThatCode(() -> acesso.exigirEscrita(PARCEIRO, 10L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> acesso.exigirEscrita(PARCEIRO, 11L)).isInstanceOf(AccessDeniedException.class);
        assertThatCode(() -> acesso.exigirEscrita(ADMIN, 11L)).doesNotThrowAnyException();
    }
}
