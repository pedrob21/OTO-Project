package OTO.referencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;

/** Tabelas de referência cujos dados vêm da migration V1. */
class ReferenciaRepositoriesTest extends BaseRepositorioTest {

    @Autowired
    private TipoHospedagemRepository tipos;

    @Autowired
    private FeriadoEventoRepository feriados;

    @Autowired
    private FaixaOcupacaoRepository faixas;

    @Test
    void tiposDeHospedagemVemNaOrdemDeExibicao() {
        assertThat(tipos.findAllByOrderByOrdem()).extracting(TipoHospedagem::getCodigo)
                .containsExactly("HOTEL", "RESORT", "POUSADA", "HOTEL_FAZENDA", "FLAT", "HOSTEL");
    }

    @Test
    void soHotelResortEPousadaCalculamReceita() {
        assertThat(tipos.findAllByOrderByOrdem()).filteredOn(TipoHospedagem::isCalculaReceita)
                .extracting(TipoHospedagem::getCodigo).containsExactly("HOTEL", "RESORT", "POUSADA");
    }

    @Test
    void novoTipoComOrdemMenorPassaAFrenteDosDemais() {
        gravar(new TipoHospedagem("CHALE", "Chalé", false, 0));
        gravarELimpar();

        assertThat(tipos.findAllByOrderByOrdem()).extracting(TipoHospedagem::getCodigo).first().isEqualTo("CHALE");
    }

    @Test
    void feriadosVemNaOrdemDaPlanilhaComNatalEReveillonUnificadoPorUltimo() {
        var codigos = feriados.findAllByOrderByOrdem().stream().map(FeriadoEvento::getCodigo).toList();

        assertThat(codigos).hasSize(16).startsWith("CARNAVAL", "PASCOA", "TIRADENTES").endsWith("REVEILLON",
                "NATAL_REVEILLON");
    }

    @Test
    void faixasVemDoMenorParaOMaiorPercentualMinimo() {
        assertThat(faixas.findAllByOrderByPercentualMinimo()).extracting(FaixaOcupacao::getCodigo)
                .containsExactly("RUIM", "REGULAR", "BOA", "OTIMA", "EXCELENTE");
        assertThat(faixas.findAllByOrderByPercentualMinimo()).extracting(FaixaOcupacao::getPercentualMinimo)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(BigDecimal.ZERO, new BigDecimal("21"), new BigDecimal("41"), new BigDecimal("61"),
                        new BigDecimal("81"));
    }

    @Test
    void percentualMinimoRepetidoViolaAUnicidade() {
        assertThatThrownBy(() -> gravar(new FaixaOcupacao("DUPLICADA", "Duplicada", new BigDecimal("41"))))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("uk_faixa_ocupacao_minimo");
    }

    @Test
    void percentualMinimoAcimaDeCemEhRejeitado() {
        assertThatThrownBy(() -> gravar(new FaixaOcupacao("ALEM", "Além", new BigDecimal("100.01"))))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_faixa_ocupacao_minimo");
    }
}
