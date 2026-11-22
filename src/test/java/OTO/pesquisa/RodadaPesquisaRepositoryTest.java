package OTO.pesquisa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.usuario.Usuario;

class RodadaPesquisaRepositoryTest extends BaseRepositorioTest {

    private static final Instant BASE = Instant.parse("2026-01-01T12:00:00Z");

    @Autowired
    private RodadaPesquisaRepository repository;

    private Usuario admin;

    private RodadaPesquisa rodada(String codigo, StatusRodada status, long diasDepois) {
        RodadaPesquisa r = new RodadaPesquisa(codigo, "Pesquisa " + codigo, null, null, admin.getId());
        if (status != StatusRodada.RASCUNHO) {
            r.abrir(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10));
        }
        if (status == StatusRodada.ENCERRADA) {
            r.encerrar();
        }
        definirCampo(r, "criadoEm", BASE.plus(diasDepois, ChronoUnit.DAYS));
        return gravar(r);
    }

    private void cenario() {
        admin = usuario("admin@oto.com");
        rodada("001/26", StatusRodada.ENCERRADA, 0);
        rodada("002/26", StatusRodada.ABERTA, 2);
        rodada("003/26", StatusRodada.RASCUNHO, 1);
        rodada("004/26", StatusRodada.ABERTA, 3);
        gravarELimpar();
    }

    @Test
    void listaDaMaisRecenteParaAMaisAntiga() {
        cenario();

        assertThat(repository.findAllByOrderByCriadoEmDesc()).extracting(RodadaPesquisa::getCodigo)
                .containsExactly("004/26", "002/26", "003/26", "001/26");
    }

    @Test
    void listaPorStatusFiltraEOrdenaPelaCriacao() {
        cenario();

        assertThat(repository.findByStatusOrderByCriadoEmDesc(StatusRodada.ABERTA))
                .extracting(RodadaPesquisa::getCodigo).containsExactly("004/26", "002/26");
        assertThat(repository.findByStatusOrderByCriadoEmDesc(StatusRodada.RASCUNHO))
                .extracting(RodadaPesquisa::getCodigo).containsExactly("003/26");
    }

    @Test
    void existePorCodigoEIgnoraOProprioIdNaEdicao() {
        cenario();
        RodadaPesquisa dois = repository.findAllByOrderByCriadoEmDesc().stream()
                .filter(r -> r.getCodigo().equals("002/26")).findFirst().orElseThrow();

        assertThat(repository.existsByCodigo("002/26")).isTrue();
        assertThat(repository.existsByCodigo("999/26")).isFalse();
        assertThat(repository.existsByCodigoAndIdNot("002/26", dois.getId())).isFalse();
        assertThat(repository.existsByCodigoAndIdNot("001/26", dois.getId())).isTrue();
    }

    @Test
    void codigoRepetidoViolaAUnicidade() {
        cenario();

        assertThatThrownBy(() -> rodada("001/26", StatusRodada.RASCUNHO, 9))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("uk_rodada_pesquisa_codigo");
    }

    @Test
    void prazoNaoPodeSerAnteriorAAbertura() {
        admin = usuario("admin@oto.com");
        RodadaPesquisa r = new RodadaPesquisa("009/26", "Invertida", null, null, admin.getId());
        r.abrir(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 9));

        assertThatThrownBy(() -> gravar(r)).isInstanceOf(PersistenceException.class).hasMessageContaining("ck_rodada_pesquisa_prazo");
    }

    @Test
    void prazoNoMesmoDiaDaAberturaEhValido() {
        admin = usuario("admin@oto.com");
        RodadaPesquisa r = new RodadaPesquisa("009/26", "Um dia", null, null, admin.getId());
        r.abrir(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 10));
        gravar(r);
        gravarELimpar();

        assertThat(repository.existsByCodigo("009/26")).isTrue();
    }

    @Test
    void rodadaAbertaSemDatasEhRejeitada() {
        admin = usuario("admin@oto.com");
        RodadaPesquisa r = new RodadaPesquisa("009/26", "Sem datas", null, null, admin.getId());
        definirCampo(r, "status", StatusRodada.ABERTA);

        assertThatThrownBy(() -> gravar(r)).isInstanceOf(PersistenceException.class).hasMessageContaining("ck_rodada_pesquisa_datas");
    }
}
