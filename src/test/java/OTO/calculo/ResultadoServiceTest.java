package OTO.calculo;

import static OTO.Fixtures.ADMIN;
import static OTO.Fixtures.VISUALIZADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import OTO.Fixtures;
import OTO.calculo.BaseCalculo.Participante;
import OTO.calculo.ResultadoDtos.CampanhaPublicadaResponse;
import OTO.calculo.ResultadoDtos.FaixaQuantidade;
import OTO.calculo.ResultadoDtos.FaixaRef;
import OTO.calculo.ResultadoDtos.PreviaResponse;
import OTO.calculo.ResultadoDtos.ResultadoResponse;
import OTO.calculo.ResultadoDtos.ResultadoTipoResponse;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.estabelecimento.Estabelecimento;
import OTO.estabelecimento.RhcResumo;
import OTO.estabelecimento.Situacao;
import OTO.pesquisa.Campanha;
import OTO.pesquisa.CampanhaDiaria;
import OTO.pesquisa.CampanhaRepository;
import OTO.pesquisa.Modalidade;
import OTO.pesquisa.ParametroCalculo;
import OTO.pesquisa.RespostaOcupacao;
import OTO.pesquisa.RodadaPesquisa;
import OTO.pesquisa.StatusRodada;
import OTO.pesquisa.TipoPeriodo;
import OTO.referencia.FaixaOcupacao;
import OTO.referencia.FaixaOcupacaoRepository;
import OTO.referencia.TipoHospedagem;
import OTO.referencia.TipoHospedagemRepository;

/**
 * Mesmo cenário de {@link CalculadoraOcupacaoTest}: dois hotéis responderam (80% e 50%), a pousada não,
 * e há RHC. Taxa geral 60%, leitos respondentes 80 de 117.
 */
@ExtendWith(MockitoExtension.class)
class ResultadoServiceTest {

    private static final YearMonth FEV = YearMonth.of(2026, 2);
    private static final LocalDate INICIO = FEV.atDay(1);

    @Mock
    private CampanhaRepository campanhas;
    @Mock
    private BaseCalculoService bases;
    @Mock
    private ResultadoCampanhaRepository resultados;
    @Mock
    private ResultadoCampanhaTipoRepository resultadosTipo;
    @Mock
    private ResultadoCampanhaFaixaRepository resultadosFaixa;
    @Mock
    private FaixaOcupacaoRepository faixas;
    @Mock
    private TipoHospedagemRepository tipos;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private ResultadoService servico;

    private final List<FaixaOcupacao> listaFaixas = List.of(
            new FaixaOcupacao("RUIM", "Ruim", new BigDecimal("0.00")),
            new FaixaOcupacao("REGULAR", "Regular", new BigDecimal("21.00")),
            new FaixaOcupacao("BOA", "Boa", new BigDecimal("41.00")),
            new FaixaOcupacao("OTIMA", "Ótima", new BigDecimal("61.00")),
            new FaixaOcupacao("EXCELENTE", "Excelente", new BigDecimal("81.00")));
    private final List<TipoHospedagem> listaTipos = List.of(
            new TipoHospedagem("POUSADA", "Pousada", true, 3), new TipoHospedagem("HOTEL", "Hotel", true, 1));

    private final Estabelecimento hotelA = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
    private final Estabelecimento hotelB = Fixtures.estabelecimento(2, "Hotel B", "HOTEL");
    private final Estabelecimento pousada = Fixtures.estabelecimento(3, "Pousada C", "POUSADA");

    @BeforeEach
    void referencias() {
        lenient().when(faixas.findAllByOrderByPercentualMinimo()).thenReturn(listaFaixas);
        lenient().when(tipos.findAll()).thenReturn(listaTipos);
    }

    private Campanha campanha(StatusRodada status) {
        Campanha c = Fixtures.campanhaMensal(10, Fixtures.rodada(1, "005/26", status), FEV);
        when(campanhas.findById(10L)).thenReturn(Optional.of(c));
        return c;
    }

    private BaseCalculo base(Campanha c, boolean comRespostas, ParametroCalculo parametro, RhcResumo rhc,
            List<Estabelecimento> semCapacidade, List<RespostaOcupacao> desconsideradas, List<CampanhaDiaria> diarias) {
        List<Participante> participantes = new ArrayList<>();
        participantes.add(new Participante(hotelA, Fixtures.capacidade(1, hotelA, INICIO, 10, 30, 4),
                comRespostas ? Fixtures.resposta(100, c, hotelA, "80", "500") : null));
        participantes.add(new Participante(hotelB, Fixtures.capacidade(2, hotelB, INICIO, 20, 50, 3),
                comRespostas ? Fixtures.resposta(101, c, hotelB, "50", "400") : null));
        participantes.add(new Participante(pousada, Fixtures.capacidade(3, pousada, INICIO, 10, 25, 4), null));
        return new BaseCalculo(c, FEV.atEndOfMonth(), participantes, semCapacidade, desconsideradas, rhc, parametro,
                listaTipos, diarias);
    }

    private BaseCalculo baseCompleta(Campanha c) {
        BaseCalculo base = base(c, true, Fixtures.parametro(1, INICIO), Fixtures.rhc(1, INICIO, 4, 4, 12, 3), List.of(),
                List.of(), List.of());
        when(bases.montar(c)).thenReturn(base);
        return base;
    }

    // ------------------------------------------------------------------ prévia

    @Test
    void previaComTudoProntoPodePublicar() {
        Campanha c = campanha(StatusRodada.ENCERRADA);
        baseCompleta(c);

        PreviaResponse previa = servico.previa(10L);

        assertThat(previa.podePublicar()).isTrue();
        assertThat(previa.impedimentos()).isEmpty();
        assertThat(previa.qtdMh()).isEqualTo(3);
        assertThat(previa.qtdRespondentes()).isEqualTo(2);
        assertThat(previa.leitosTotal()).isEqualTo(117);
        assertThat(previa.leitosRespondentes()).isEqualTo(80);
        assertThat(previa.percentualLeitosRespondentes()).isEqualByComparingTo("68.38");
        assertThat(previa.avisos()).containsExactly(
                "Tipo HOTEL com 2 respondente(s): na divulgação, os valores do tipo ficam ocultos.");

        ResultadoResponse r = previa.resultado();
        assertThat(r.publicado()).isFalse();
        assertThat(r.taxaOcupacao()).isEqualByComparingTo("60.00");
        assertThat(r.faixa()).isEqualTo(new FaixaRef("BOA", "Boa"));
        assertThat(r.diariaMedia()).isEqualByComparingTo("437.50");
        assertThat(r.percentualMhRespondentes()).isEqualByComparingTo("66.67");
        assertThat(r.porTipo()).extracting(ResultadoTipoResponse::tipoHospedagemCodigo).containsExactly("HOTEL", "POUSADA");
        assertThat(r.porTipo().get(0).nome()).isEqualTo("Hotel");
        assertThat(r.porTipo().get(0).omitido()).isFalse();
        assertThat(r.porTipo().get(0).taxaOcupacao()).isEqualByComparingTo("60.00");
        assertThat(r.distribuicaoFaixas()).extracting(FaixaQuantidade::codigo)
                .containsExactly("EXCELENTE", "OTIMA", "BOA", "REGULAR", "RUIM");
        assertThat(r.distribuicaoFaixas()).extracting(FaixaQuantidade::qtdMh).containsExactly(0, 1, 1, 0, 0);
        assertThat(r.distribuicaoFaixas().get(1).percentual()).isEqualByComparingTo("50.00");
    }

    @Test
    void previaListaOQueImpedeAPublicacao() {
        Campanha c = campanha(StatusRodada.ABERTA);
        c.publicar(1L);
        when(bases.montar(c)).thenReturn(base(c, false, Fixtures.parametro(1, INICIO), null, List.of(), List.of(), List.of()));

        PreviaResponse previa = servico.previa(10L);

        assertThat(previa.podePublicar()).isFalse();
        assertThat(previa.resultado()).isNull();
        assertThat(previa.impedimentos()).containsExactly(
                "A campanha já foi publicada.",
                "A pesquisa 005/26 precisa ser encerrada antes da publicação.",
                "Leitos respondentes: 0,00% (mínimo para publicar: 30,00%).",
                "Nenhum meio de hospedagem respondeu.");
    }

    @Test
    void previaAvisaOQueOsResponsaveisDevemConferir() {
        Campanha c = campanha(StatusRodada.ENCERRADA);
        Estabelecimento semCapacidade = Fixtures.estabelecimento(4, "Flat D", "FLAT");
        Estabelecimento inativo = Fixtures.estabelecimento(5, "Hostel E", "HOSTEL", Situacao.IRREGULAR);
        List<RespostaOcupacao> desconsideradas = List.of(Fixtures.resposta(102, c, semCapacidade, "40", null),
                Fixtures.resposta(103, c, inativo, "40", null));
        List<CampanhaDiaria> diarias = List.of(new CampanhaDiaria(10L, "HOTEL", new BigDecimal("450"), null, 1L),
                new CampanhaDiaria(10L, null, new BigDecimal("420"), null, 1L));
        when(bases.montar(c)).thenReturn(base(c, true, Fixtures.parametro(1, INICIO), null, List.of(semCapacidade),
                desconsideradas, diarias));

        List<String> avisos = servico.previa(10L).avisos();

        assertThat(avisos).containsExactly(
                "Flat D: sem UH/leitos cadastrados para 2026-02-28; fica fora do cálculo.",
                "Hostel E: respondeu, mas está inativo ou irregular; a resposta não entra no cálculo.",
                "Não há totais de hospedagem caseira (RHC) cadastrados: o fluxo do RHC fica zerado.",
                "Diária média geral digitada pela equipe (R$ 420,00): substitui a média das respostas.",
                "Diária de HOTEL digitada pela equipe (R$ 450,00): substitui a média das respostas.",
                "Tipo HOTEL com 2 respondente(s): na divulgação, os valores do tipo ficam ocultos.");
    }

    @Test
    void campanhaInexistente() {
        when(campanhas.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.previa(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ------------------------------------------------------------------ publicação

    @Test
    void publicarGravaOResultadoEMarcaACampanha() {
        Campanha c = campanha(StatusRodada.ENCERRADA);
        baseCompleta(c);
        when(resultados.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResultadoResponse r = servico.publicar(10L, ADMIN);

        assertThat(c.isPublicada()).isTrue();
        assertThat(c.getPublicadaPor()).isEqualTo(1L);
        assertThat(r.publicado()).isTrue();
        assertThat(r.parametroCalculoId()).isEqualTo(1L);
        ArgumentCaptor<ResultadoCampanha> salvo = ArgumentCaptor.forClass(ResultadoCampanha.class);
        verify(resultados).save(salvo.capture());
        assertThat(salvo.getValue().getCampanhaId()).isEqualTo(10L);
        assertThat(salvo.getValue().getTaxaOcupacao()).isEqualByComparingTo("60.00");
        verify(resultadosTipo).saveAll(anyList());
        verify(resultadosFaixa).saveAll(anyList());
        verify(auditoria).registrar(1L, "campanha", 10L, "PUBLICAR", null, Map.of(
                "taxaOcupacao", salvo.getValue().getTaxaOcupacao(),
                "fluxoTotal", salvo.getValue().getFluxoTotal(),
                "percentualLeitosRespondentes", new BigDecimal("68.38"),
                "parametroCalculoId", 1L));
    }

    @Test
    void publicarExigePesquisaEncerradaENaoPublicada() {
        campanha(StatusRodada.ABERTA);
        assertThatThrownBy(() -> servico.publicar(10L, ADMIN)).isInstanceOf(RegraNegocioException.class)
                .hasMessage("Encerre a pesquisa 005/26 antes de publicar.");

        campanha(StatusRodada.ENCERRADA).publicar(1L);
        assertThatThrownBy(() -> servico.publicar(10L, ADMIN)).hasMessage("A campanha já foi publicada.");
        verify(bases, never()).montar(any());
    }

    @Test
    void publicarExigeRespostasEOMinimoDeLeitos() {
        Campanha c = campanha(StatusRodada.ENCERRADA);
        when(bases.montar(c)).thenReturn(base(c, false, Fixtures.parametro(1, INICIO), null, List.of(), List.of(), List.of()));
        assertThatThrownBy(() -> servico.publicar(10L, ADMIN)).hasMessage("Nenhum meio de hospedagem respondeu à campanha.");

        ParametroCalculo exigente = Fixtures.parametro(1, INICIO);
        exigente.alterar(INICIO, new BigDecimal("15.15"), 4, 2, BigDecimal.TEN, BigDecimal.TEN, new BigDecimal("90"), "x", 1L);
        when(bases.montar(c)).thenReturn(base(c, true, exigente, null, List.of(), List.of(), List.of()));
        assertThatThrownBy(() -> servico.publicar(10L, ADMIN))
                .hasMessage("Leitos respondentes: 76,19%. O mínimo para publicar é 90,00%.");
        verify(resultados, never()).save(any());
        assertThat(c.isPublicada()).isFalse();
    }

    @Test
    void erroDaCalculadoraViraRegraDeNegocio() {
        Campanha c = campanha(StatusRodada.ENCERRADA);
        Estabelecimento semUh = Fixtures.estabelecimento(1, "Sem UH", "HOTEL");
        when(bases.montar(c)).thenReturn(new BaseCalculo(c, FEV.atEndOfMonth(),
                List.of(new Participante(semUh, Fixtures.capacidade(1, semUh, INICIO, 0, 0, 1),
                        Fixtures.resposta(100, c, semUh, "50", null))),
                List.of(), List.of(), null, Fixtures.parametro(1, INICIO), listaTipos, List.of()));

        assertThatThrownBy(() -> servico.publicar(10L, ADMIN)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("UH");
    }

    @Test
    void cancelarPublicacaoApagaOsResultados() {
        Campanha c = campanha(StatusRodada.ENCERRADA);
        c.publicar(1L);

        servico.cancelarPublicacao(10L, ADMIN);

        InOrder ordem = inOrder(resultadosFaixa, resultadosTipo, resultados);
        ordem.verify(resultadosFaixa).deleteByIdCampanhaId(10L);
        ordem.verify(resultadosTipo).deleteByIdCampanhaId(10L);
        ordem.verify(resultados).deleteById(10L);
        assertThat(c.isPublicada()).isFalse();
        verify(auditoria).registrar(1L, "campanha", 10L, "CANCELAR_PUBLICACAO", null, null);
    }

    @Test
    void naoCancelaOQueNaoFoiPublicado() {
        campanha(StatusRodada.ENCERRADA);
        assertThatThrownBy(() -> servico.cancelarPublicacao(10L, ADMIN)).hasMessage("A campanha não está publicada.");
        verify(resultados, never()).deleteById(any());
    }

    // ------------------------------------------------------------------ consulta

    private ResultadoCampanha publicado(Campanha c) {
        BaseCalculo base = base(c, true, Fixtures.parametro(1, INICIO), Fixtures.rhc(1, INICIO, 4, 4, 12, 3), List.of(),
                List.of(), List.of());
        CalculadoraOcupacao.Resultado r = CalculadoraOcupacao.calcular(base.paraEntrada());
        ResultadoCampanha resultado = ResultadoCampanha.de(c.getId(), 1L, r);
        lenient().when(resultadosTipo.findByIdCampanhaId(c.getId()))
                .thenReturn(r.porTipo().stream().map(t -> ResultadoCampanhaTipo.de(c.getId(), t)).toList());
        lenient().when(resultadosFaixa.findByIdCampanhaId(c.getId())).thenReturn(List.of(
                new ResultadoCampanhaFaixa(c.getId(), "OTIMA", 1), new ResultadoCampanhaFaixa(c.getId(), "BOA", 1)));
        return resultado;
    }

    @Test
    void resultadoParaQuemNaoEhAdminOcultaTiposComPoucosRespondentes() {
        Campanha c = campanha(StatusRodada.ENCERRADA);
        c.publicar(1L);
        ResultadoCampanha resultado = publicado(c);
        when(resultados.findById(10L)).thenReturn(Optional.of(resultado));

        ResultadoResponse r = servico.resultado(10L, VISUALIZADOR);

        assertThat(r.publicado()).isTrue();
        assertThat(r.taxaOcupacao()).isEqualByComparingTo("60.00");
        ResultadoTipoResponse hotel = r.porTipo().get(0);
        assertThat(hotel.omitido()).isTrue();
        assertThat(hotel.taxaOcupacao()).isNull();
        assertThat(hotel.receitaEstimada()).isNull();
        assertThat(hotel.qtdMh()).isEqualTo(2);
    }

    @Test
    void resultadoParaAdminMostraTudo() {
        Campanha c = campanha(StatusRodada.ENCERRADA);
        c.publicar(1L);
        ResultadoCampanha resultado = publicado(c);
        when(resultados.findById(10L)).thenReturn(Optional.of(resultado));

        ResultadoTipoResponse hotel = servico.resultado(10L, ADMIN).porTipo().get(0);

        assertThat(hotel.omitido()).isFalse();
        assertThat(hotel.taxaOcupacao()).isEqualByComparingTo("60.00");
    }

    @Test
    void resultadoSoDeCampanhaPublicada() {
        Campanha c = campanha(StatusRodada.ENCERRADA);
        ResultadoCampanha resultado = publicado(c);
        when(resultados.findById(10L)).thenReturn(Optional.of(resultado));
        assertThatThrownBy(() -> servico.resultado(10L, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("A campanha 10 ainda não tem resultado publicado.");

        when(resultados.findById(10L)).thenReturn(Optional.empty());
        c.publicar(1L);
        assertThatThrownBy(() -> servico.resultado(10L, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void csvDoResultado() {
        Campanha c = campanha(StatusRodada.ENCERRADA);
        c.publicar(1L);
        ResultadoCampanha resultado = publicado(c);
        when(resultados.findById(10L)).thenReturn(Optional.of(resultado));

        String csv = new String(servico.csv(10L, ADMIN), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("﻿Campanha;Campanha 2026-02\r\n");
        assertThat(csv).contains("Taxa de ocupação (%);60,00\r\n");
    }

    @Test
    void publicadasComFiltros() {
        RodadaPesquisa encerrada = Fixtures.rodada(1, "X", StatusRodada.ENCERRADA);
        Campanha fev = Fixtures.campanhaMensal(10, encerrada, FEV);
        Campanha carnaval = Fixtures.campanhaFeriado(11, encerrada, "CARNAVAL", LocalDate.of(2025, 3, 1),
                LocalDate.of(2025, 3, 5), Modalidade.EXPECTATIVA);
        Campanha semResultado = Fixtures.campanhaMensal(12, encerrada, FEV.plusMonths(1));
        when(campanhas.findPublicadas()).thenReturn(List.of(semResultado, fev, carnaval));
        List<ResultadoCampanha> lista = List.of(publicado(fev), publicado(carnaval));
        when(resultados.findByCampanhaIdIn(anyList())).thenReturn(lista);

        List<CampanhaPublicadaResponse> consolidadas = servico.publicadas(Modalidade.CONSOLIDADA, null, null);
        assertThat(consolidadas).extracting(CampanhaPublicadaResponse::campanhaId).containsExactly(10L);
        assertThat(consolidadas.get(0).faixa()).isEqualTo(new FaixaRef("BOA", "Boa"));
        assertThat(consolidadas.get(0).taxaOcupacao()).isEqualByComparingTo("60.00");

        assertThat(servico.publicadas(null, TipoPeriodo.FERIADO, 2025))
                .extracting(CampanhaPublicadaResponse::feriadoCodigo).containsExactly("CARNAVAL");
    }

    @Test
    void semPublicadasNoFiltroNaoConsultaResultados() {
        when(campanhas.findPublicadas()).thenReturn(List.of(
                Fixtures.campanhaMensal(10, Fixtures.rodada(1, "X", StatusRodada.ENCERRADA), FEV)));
        assertThat(servico.publicadas(null, null, 1999)).isEmpty();
        verify(resultados, never()).findByCampanhaIdIn(any());
    }

    @Test
    void faixaDeUmaTaxa() {
        assertThat(ResultadoService.faixaDe(null, listaFaixas)).isNull();
        assertThat(ResultadoService.faixaDe(new BigDecimal("61.00"), listaFaixas)).isEqualTo(new FaixaRef("OTIMA", "Ótima"));
        assertThat(ResultadoService.faixaDe(new BigDecimal("60.99"), listaFaixas)).isEqualTo(new FaixaRef("BOA", "Boa"));
        assertThat(ResultadoService.faixaDe(new BigDecimal("100"), listaFaixas).codigo()).isEqualTo("EXCELENTE");
        assertThat(ResultadoService.faixaDe(new BigDecimal("5"), listaFaixas.subList(1, 5))).isNull();
    }

    @Test
    void tipoForaDaTabelaDeReferenciaVaiParaOFimComOProprioCodigo() {
        Campanha c = campanha(StatusRodada.ENCERRADA);
        c.publicar(1L);
        ResultadoCampanha resultado = publicado(c);
        when(resultados.findById(10L)).thenReturn(Optional.of(resultado));
        when(tipos.findAll()).thenReturn(List.of(new TipoHospedagem("POUSADA", "Pousada", true, 3)));

        List<ResultadoTipoResponse> porTipo = servico.resultado(10L, ADMIN).porTipo();

        assertThat(porTipo).extracting(ResultadoTipoResponse::nome).containsExactly("Pousada", "HOTEL");
    }
}
