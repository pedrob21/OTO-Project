package OTO.historico;

import static OTO.Fixtures.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import OTO.Fixtures;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.historico.HistoricoDtos.ErroLinha;
import OTO.historico.HistoricoDtos.HistoricoRequest;
import OTO.historico.HistoricoDtos.HistoricoResponse;
import OTO.historico.HistoricoDtos.ImportacaoResponse;
import OTO.pesquisa.Modalidade;
import OTO.referencia.FeriadoEvento;
import OTO.referencia.FeriadoEventoRepository;
import OTO.referencia.TipoHospedagem;
import OTO.referencia.TipoHospedagemRepository;

@ExtendWith(MockitoExtension.class)
class HistoricoServiceTest {

    @Mock
    private IndicadorHistoricoRepository historico;
    @Mock
    private FeriadoEventoRepository feriados;
    @Mock
    private TipoHospedagemRepository tipos;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private HistoricoService servico;

    @BeforeEach
    void referencias() {
        lenient().when(feriados.findAll()).thenReturn(List.of(new FeriadoEvento("CARNAVAL", "Carnaval", 1)));
        lenient().when(tipos.findAll()).thenReturn(List.of(new TipoHospedagem("HOTEL", "Hotel", true, 1)));
    }

    private static IndicadorHistorico valor(long id, Indicador indicador, Modalidade modalidade, int ano, Integer mes,
            String feriado, String tipo, String numero) {
        IndicadorHistorico h = new IndicadorHistorico(indicador, modalidade, ano, mes, feriado, tipo);
        h.definirValor(new BigDecimal(numero), null, "Planilha");
        return Fixtures.comId(h, id);
    }

    private static HistoricoRequest requisicao(Indicador indicador, Integer mes, String feriado, String tipo, String numero) {
        return new HistoricoRequest(indicador, Modalidade.CONSOLIDADA, 2024, mes, feriado, tipo, new BigDecimal(numero),
                " obs ", " Planilha 2024 ");
    }

    // ------------------------------------------------------------------ listagem

    @Test
    void listarAplicaOsFiltros() {
        when(historico.findAll(any(Sort.class))).thenReturn(List.of(
                valor(1, Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2024, 1, null, null, "60"),
                valor(2, Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2024, 1, null, "HOTEL", "65"),
                valor(3, Indicador.FLUXO_TURISTAS, Modalidade.EXPECTATIVA, 2023, null, "CARNAVAL", null, "1000")));

        assertThat(servico.listar(null, null, null, null)).hasSize(3);
        assertThat(servico.listar(Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2024, " HOTEL "))
                .extracting(HistoricoResponse::id).containsExactly(2L);
        assertThat(servico.listar(null, Modalidade.EXPECTATIVA, null, " ")).extracting(HistoricoResponse::id)
                .containsExactly(3L);
        assertThat(servico.listar(null, null, 2022, null)).isEmpty();
    }

    // ------------------------------------------------------------------ gravação

    @Test
    void salvarCriaValorNovo() {
        when(historico.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 9L));
        HistoricoRequest r = requisicao(Indicador.TAXA_OCUPACAO, 1, null, " HOTEL ", "63.45");

        HistoricoResponse gravado = servico.salvar(r, ADMIN);

        assertThat(gravado.id()).isEqualTo(9L);
        assertThat(gravado.tipoHospedagemCodigo()).isEqualTo("HOTEL");
        assertThat(gravado.observacao()).isEqualTo("obs");
        assertThat(gravado.fonte()).isEqualTo("Planilha 2024");
        verify(auditoria).registrar(1L, "indicador_historico", 9L, "GRAVAR", null, r);
    }

    @Test
    void salvarSubstituiOValorDaMesmaCombinacao() {
        IndicadorHistorico existente = valor(4, Indicador.FLUXO_TURISTAS, Modalidade.CONSOLIDADA, 2024, null, "CARNAVAL",
                null, "1000");
        when(historico.findAll()).thenReturn(List.of(existente));

        HistoricoResponse gravado = servico.salvar(requisicao(Indicador.FLUXO_TURISTAS, null, "CARNAVAL", null, "1500"), ADMIN);

        assertThat(gravado.id()).isEqualTo(4L);
        assertThat(existente.getValor()).isEqualByComparingTo("1500");
        verify(historico, never()).save(any());
    }

    @Test
    void salvarJuntaOsErrosNumaMensagem() {
        assertThatThrownBy(() -> servico.salvar(requisicao(Indicador.TAXA_OCUPACAO, 1, "NATAL", "FLAT", "101"), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Informe o mês ou o feriado (um dos dois); feriado inválido: NATAL (veja /api/referencias); "
                        + "tipo de hospedagem inválido: FLAT (veja /api/referencias); a taxa de ocupação vai de 0 a 100.");
        assertThatThrownBy(() -> servico.salvar(requisicao(Indicador.TAXA_OCUPACAO, null, " ", null, "50"), ADMIN))
                .hasMessage("Informe o mês ou o feriado (um dos dois).");
        assertThatThrownBy(() -> servico.salvar(new HistoricoRequest(Indicador.FLUXO_TURISTAS, Modalidade.CONSOLIDADA,
                1999, 13, null, null, new BigDecimal("-1"), null, "x"), ADMIN))
                .hasMessage("Ano fora do intervalo de 2000 a 2100; mês deve ser de 1 a 12; o valor não pode ser negativo.");
        verify(historico, never()).save(any());
    }

    @Test
    void alterarCorrigeACombinacaoEOValor() {
        IndicadorHistorico h = valor(4, Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2024, 1, null, null, "60");
        when(historico.findById(4L)).thenReturn(Optional.of(h));
        when(historico.findAll()).thenReturn(List.of(h));

        HistoricoResponse depois = servico.alterar(4L, requisicao(Indicador.TAXA_OCUPACAO, 2, null, null, "62"), ADMIN);

        assertThat(depois.mes()).isEqualTo(2);
        assertThat(h.getValor()).isEqualByComparingTo("62");
        assertThat(h.getFonte()).isEqualTo("Planilha 2024");
        verify(auditoria).registrar(eq(1L), eq("indicador_historico"), eq(4L), eq("ATUALIZAR"), any(), eq(depois));
    }

    @Test
    void alterarParaACombinacaoDeOutroValorNaoPode() {
        IndicadorHistorico h = valor(4, Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2024, 1, null, null, "60");
        IndicadorHistorico outro = valor(5, Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2024, 2, null, null, "61");
        when(historico.findById(4L)).thenReturn(Optional.of(h));
        when(historico.findAll()).thenReturn(List.of(h, outro));

        assertThatThrownBy(() -> servico.alterar(4L, requisicao(Indicador.TAXA_OCUPACAO, 2, null, null, "62"), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("(id 5)");
        assertThat(h.getMes()).isEqualTo(1);
    }

    @Test
    void alterarComErroOuInexistente() {
        when(historico.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.alterar(9L, requisicao(Indicador.TAXA_OCUPACAO, 1, null, null, "1"), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        when(historico.findById(4L)).thenReturn(Optional.of(
                valor(4, Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2024, 1, null, null, "60")));
        assertThatThrownBy(() -> servico.alterar(4L, requisicao(Indicador.TAXA_OCUPACAO, 1, null, null, "150"), ADMIN))
                .hasMessage("A taxa de ocupação vai de 0 a 100.");
    }

    @Test
    void excluir() {
        IndicadorHistorico h = valor(4, Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2024, 1, null, null, "60");
        when(historico.findById(4L)).thenReturn(Optional.of(h));

        servico.excluir(4L, ADMIN);

        verify(historico).delete(h);
        verify(auditoria).registrar(eq(1L), eq("indicador_historico"), eq(4L), eq("EXCLUIR"), any(), isNull());
    }

    // ------------------------------------------------------------------ importação

    private ImportacaoResponse importar(String csv, String fonte) {
        return servico.importar(csv.getBytes(StandardCharsets.UTF_8), fonte, ADMIN);
    }

    @Test
    void importarGravaTudoEContaInseridosEAtualizados() {
        IndicadorHistorico existente = valor(4, Indicador.TAXA_OCUPACAO, Modalidade.CONSOLIDADA, 2024, 1, null, null, "60");
        when(historico.findAll()).thenReturn(List.of(existente));
        when(historico.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportacaoResponse resposta = importar("""
                indicador;modalidade;ano;mes;feriado;tipo;valor;observacao
                TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;63,456;
                FLUXO_TURISTAS;CONSOLIDADA;2024;;CARNAVAL;;98.512;
                DIARIA_MEDIA;CONSOLIDADA;2024;7;;HOTEL;412,90;
                """, " Planilha 2017-2025 ");

        assertThat(resposta).isEqualTo(new ImportacaoResponse(3, 2, 1, List.of()));
        assertThat(existente.getValor()).isEqualTo(new BigDecimal("63.46"));
        assertThat(existente.getFonte()).isEqualTo("Planilha 2017-2025");
        verify(historico, times(2)).save(any());
        verify(auditoria).registrar(1L, "indicador_historico", "importacao", "IMPORTAR", null,
                Map.of("fonte", "Planilha 2017-2025", "inseridos", 2, "atualizados", 1));
    }

    @Test
    void comQualquerErroNadaEhGravado() {
        ImportacaoResponse resposta = importar("""
                TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;150;
                TAXA;CONSOLIDADA;2024;1;;;1;
                FLUXO_TURISTAS;CONSOLIDADA;2024;2;;;10;
                FLUXO_TURISTAS;CONSOLIDADA;2024;2;;;11;
                """, "Planilha");

        assertThat(resposta.linhasLidas()).isEqualTo(4);
        assertThat(resposta.inseridos()).isZero();
        assertThat(resposta.erros()).extracting(ErroLinha::linha).containsExactly(1, 2, 4);
        assertThat(resposta.erros().get(0).mensagem()).isEqualTo("a taxa de ocupação vai de 0 a 100");
        assertThat(resposta.erros().get(2).mensagem()).contains("repetida no arquivo");
        verify(historico, never()).save(any());
        verify(auditoria, never()).registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void arquivoSemDadosEhErro() {
        ImportacaoResponse resposta = importar("indicador;modalidade;ano;mes;feriado;tipo;valor;observacao\n", "Planilha");
        assertThat(resposta.erros()).containsExactly(new ErroLinha(0, "o arquivo não tem linhas de dados"));
    }

    @Test
    void listaNoMaximoCemErros() {
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            csv.append("TAXA;CONSOLIDADA;2024;1;;;1;\n");
        }
        ImportacaoResponse resposta = importar(csv.toString(), "Planilha");
        assertThat(resposta.linhasLidas()).isEqualTo(150);
        assertThat(resposta.erros()).hasSize(100);
    }

    @Test
    void importarExigeAFonte() {
        assertThatThrownBy(() -> importar("x", " ")).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Informe a fonte");
        assertThatThrownBy(() -> importar("x", null)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> importar("x", "a".repeat(201))).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void entidadeGuardaMesNuloParaFeriado() {
        IndicadorHistorico h = new IndicadorHistorico(Indicador.FLUXO_TURISTAS, Modalidade.CONSOLIDADA, 2024, null,
                "CARNAVAL", null);
        assertThat(h.getMes()).isNull();
        h.corrigirCombinacao(Indicador.FLUXO_TURISTAS, Modalidade.CONSOLIDADA, 2025, 3, null, null);
        assertThat(h.getMes()).isEqualTo(3);
        assertThat(h.getAno()).isEqualTo(2025);
    }

    @Test
    void novoValorDaImportacaoTemOsCamposDoArquivo() {
        when(historico.save(any())).thenAnswer(inv -> inv.getArgument(0));

        importar("DIARIA_MEDIA;EXPECTATIVA;2024;;CARNAVAL;HOTEL;412,90;Booking\n", "Planilha");

        ArgumentCaptor<IndicadorHistorico> salvo = ArgumentCaptor.forClass(IndicadorHistorico.class);
        verify(historico).save(salvo.capture());
        assertThat(salvo.getValue().getModalidade()).isEqualTo(Modalidade.EXPECTATIVA);
        assertThat(salvo.getValue().getFeriadoCodigo()).isEqualTo("CARNAVAL");
        assertThat(salvo.getValue().getTipoHospedagemCodigo()).isEqualTo("HOTEL");
        assertThat(salvo.getValue().getObservacao()).isEqualTo("Booking");
    }
}
