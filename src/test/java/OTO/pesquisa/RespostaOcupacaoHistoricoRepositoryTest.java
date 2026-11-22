package OTO.pesquisa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.YearMonth;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.estabelecimento.Estabelecimento;
import OTO.usuario.Usuario;

class RespostaOcupacaoHistoricoRepositoryTest extends BaseRepositorioTest {

    @Autowired
    private RespostaOcupacaoHistoricoRepository repository;

    private Usuario autor;
    private RespostaOcupacao resposta;
    private RespostaOcupacao outra;

    private RespostaOcupacaoHistorico versao(RespostaOcupacao r, String taxa) {
        RespostaOcupacaoHistorico h = gravar(RespostaOcupacaoHistorico.de(r));
        r.corrigir(new BigDecimal(taxa), null, autor.getId());
        gravar(r);
        return h;
    }

    private void cenario() {
        autor = usuario("autor@oto.com");
        Estabelecimento alfa = estabelecimento("Alfa", "HOTEL", "11111111000101");
        Estabelecimento beta = estabelecimento("Beta", "HOTEL", "11111111000102");
        RodadaPesquisa rodada = rodada("001/26", StatusRodada.ABERTA, autor);
        Campanha campanha = campanhaMensal(rodada, YearMonth.of(2026, 1), Modalidade.CONSOLIDADA);
        resposta = resposta(campanha, alfa, "50.00", autor);
        outra = resposta(campanha, beta, "20.00", autor);
    }

    @Test
    void historicoVemDaVersaoMaisNovaParaAMaisAntigaSoDaResposta() {
        cenario();
        versao(resposta, "55.00");
        versao(resposta, "60.00");
        versao(outra, "25.00");
        gravarELimpar();

        assertThat(repository.findByRespostaIdOrderByVersaoDesc(resposta.getId()))
                .extracting(RespostaOcupacaoHistorico::getVersao, h -> h.getTaxaOcupacao().intValue())
                .containsExactly(tuple(2, 55), tuple(1, 50));
    }

    @Test
    void respostaSemCorrecoesNaoTemHistorico() {
        cenario();
        gravarELimpar();

        assertThat(repository.findByRespostaIdOrderByVersaoDesc(resposta.getId())).isEmpty();
    }

    @Test
    void mesmaVersaoDaMesmaRespostaViolaAUnicidade() {
        cenario();
        gravar(RespostaOcupacaoHistorico.de(resposta));

        assertThatThrownBy(() -> gravar(RespostaOcupacaoHistorico.de(resposta)))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("uk_resposta_historico_versao");
    }
}
