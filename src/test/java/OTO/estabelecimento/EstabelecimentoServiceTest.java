package OTO.estabelecimento;

import static OTO.Fixtures.ADMIN;
import static OTO.Fixtures.PARCEIRO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import OTO.Fixtures;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.ControleAcesso;
import OTO.estabelecimento.EstabelecimentoDtos.AtualizacaoRequest;
import OTO.estabelecimento.EstabelecimentoDtos.CapacidadeRequest;
import OTO.estabelecimento.EstabelecimentoDtos.CapacidadeResponse;
import OTO.estabelecimento.EstabelecimentoDtos.CriacaoRequest;
import OTO.estabelecimento.EstabelecimentoDtos.EstabelecimentoResponse;
import OTO.estabelecimento.EstabelecimentoDtos.EstabelecimentoResumo;
import OTO.referencia.TipoHospedagemRepository;

@ExtendWith(MockitoExtension.class)
class EstabelecimentoServiceTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 1);

    @Mock
    private EstabelecimentoRepository estabelecimentos;
    @Mock
    private CapacidadeHospedagemRepository capacidades;
    @Mock
    private TipoHospedagemRepository tipos;
    @Mock
    private ControleAcesso acesso;
    @Mock
    private AuditoriaService auditoria;

    @InjectMocks
    private EstabelecimentoService servico;

    private static CapacidadeRequest capacidadeRequest(LocalDate desde, int uh, int pcd) {
        return new CapacidadeRequest(desde, uh, pcd, uh * 2, 4);
    }

    // ------------------------------------------------------------------ listagem e consulta

    @Test
    void adminListaTodosComACapacidadeVigente() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        Estabelecimento pousada = Fixtures.estabelecimento(2, "Pousada B", "POUSADA");
        pousada.definirAtivo(false);
        when(estabelecimentos.findAllByOrderByNomeFantasia()).thenReturn(List.of(hotel, pousada));
        when(capacidades.findVigentesEm(LocalDate.now()))
                .thenReturn(List.of(Fixtures.capacidade(5, hotel, JAN, 10, 30, 4)));

        List<EstabelecimentoResumo> lista = servico.listar(ADMIN, null, null);

        assertThat(lista).extracting(EstabelecimentoResumo::nomeFantasia).containsExactly("Hotel A", "Pousada B");
        assertThat(lista.get(0).qtdUh()).isEqualTo(10);
        assertThat(lista.get(0).qtdLeitos()).isEqualTo(30);
        assertThat(lista.get(1).qtdUh()).isNull();
    }

    @Test
    void listarFiltraPorTipoEAtivo() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        Estabelecimento pousada = Fixtures.estabelecimento(2, "Pousada B", "POUSADA");
        pousada.definirAtivo(false);
        when(estabelecimentos.findAllByOrderByNomeFantasia()).thenReturn(List.of(hotel, pousada));

        assertThat(servico.listar(ADMIN, "HOTEL", null)).extracting(EstabelecimentoResumo::id).containsExactly(1L);
        assertThat(servico.listar(ADMIN, " ", false)).extracting(EstabelecimentoResumo::id).containsExactly(2L);
        assertThat(servico.listar(ADMIN, "POUSADA", true)).isEmpty();
    }

    @Test
    void parceiroListaSoOsSeus() {
        when(acesso.estabelecimentosDo(PARCEIRO)).thenReturn(List.of(1L));
        when(estabelecimentos.findByIdInOrderByNomeFantasia(List.of(1L)))
                .thenReturn(List.of(Fixtures.estabelecimento(1, "Hotel A", "HOTEL")));

        assertThat(servico.listar(PARCEIRO, null, null)).extracting(EstabelecimentoResumo::id).containsExactly(1L);
        verify(estabelecimentos, never()).findAllByOrderByNomeFantasia();
    }

    @Test
    void capacidadesVigentesFicaComUmaPorEstabelecimento() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        CapacidadeHospedagem primeira = Fixtures.capacidade(5, hotel, JAN, 10, 30, 4);
        when(capacidades.findVigentesEm(JAN)).thenReturn(List.of(primeira, Fixtures.capacidade(6, hotel, JAN, 12, 30, 4)));

        assertThat(servico.capacidadesVigentes(JAN)).containsExactly(Map.entry(1L, primeira));
    }

    @Test
    void buscarConfereOAcessoEDevolveACapacidadeVigente() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        when(estabelecimentos.findById(1L)).thenReturn(Optional.of(hotel));
        when(capacidades.findFirstByEstabelecimentoIdAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(1L, LocalDate.now()))
                .thenReturn(Optional.of(Fixtures.capacidade(5, hotel, JAN, 10, 30, 4)));

        EstabelecimentoResponse resposta = servico.buscar(1L, PARCEIRO);

        verify(acesso).exigirLeitura(PARCEIRO, 1L);
        assertThat(resposta.dados().nomeFantasia()).isEqualTo("Hotel A");
        assertThat(resposta.capacidadeVigente().leitosUteis()).isEqualTo(25.0);
    }

    @Test
    void buscarSemAcessoNaoCarrega() {
        doThrow(new AccessDeniedException("não")).when(acesso).exigirLeitura(PARCEIRO, 1L);
        assertThatThrownBy(() -> servico.buscar(1L, PARCEIRO)).isInstanceOf(AccessDeniedException.class);
        verify(estabelecimentos, never()).findById(anyLong());
    }

    @Test
    void buscarInexistente() {
        when(estabelecimentos.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.buscar(9L, ADMIN)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ------------------------------------------------------------------ criação e alteração

    @Test
    void criarGravaEstabelecimentoECapacidadeInicial() {
        when(tipos.existsById("HOTEL")).thenReturn(true);
        when(estabelecimentos.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 10L));
        when(capacidades.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 20L));
        CriacaoRequest requisicao = new CriacaoRequest(Fixtures.dados("Hotel A", "12345678000199"),
                Fixtures.secretaria("HOTEL"), capacidadeRequest(JAN, 10, 1));

        EstabelecimentoResponse criado = servico.criar(requisicao, ADMIN);

        assertThat(criado.id()).isEqualTo(10L);
        assertThat(criado.ativo()).isTrue();
        assertThat(criado.inventarioAtualizadoEm()).isNotNull();
        assertThat(criado.capacidadeVigente().id()).isEqualTo(20L);
        assertThat(criado.capacidadeVigente().qtdUh()).isEqualTo(10);
        verify(auditoria).registrar(1L, "estabelecimento", 10L, "CRIAR", null, requisicao);
    }

    @Test
    void criarRecusaTipoInvalidoCnpjRepetidoEPcdAcimaDoTotal() {
        CriacaoRequest tipoErrado = new CriacaoRequest(Fixtures.dados("A"), Fixtures.secretaria("XYZ"),
                capacidadeRequest(JAN, 10, 0));
        assertThatThrownBy(() -> servico.criar(tipoErrado, ADMIN)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Tipo de hospedagem inválido: XYZ");

        when(tipos.existsById("HOTEL")).thenReturn(true);
        when(estabelecimentos.existsByCnpj("12345678000199")).thenReturn(true);
        CriacaoRequest cnpjRepetido = new CriacaoRequest(Fixtures.dados("A", "12345678000199"),
                Fixtures.secretaria("HOTEL"), capacidadeRequest(JAN, 10, 0));
        assertThatThrownBy(() -> servico.criar(cnpjRepetido, ADMIN)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("CNPJ");

        CriacaoRequest pcdDemais = new CriacaoRequest(Fixtures.dados("A"), Fixtures.secretaria("HOTEL"),
                capacidadeRequest(JAN, 10, 11));
        assertThatThrownBy(() -> servico.criar(pcdDemais, ADMIN)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("PCD");

        verify(estabelecimentos, never()).save(any());
    }

    @Test
    void parceiroAlteraSoOsDadosCadastrais() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        when(estabelecimentos.findById(1L)).thenReturn(Optional.of(hotel));

        EstabelecimentoResponse resposta = servico.atualizar(1L,
                new AtualizacaoRequest(Fixtures.dados("Hotel A Novo"), Fixtures.secretaria("POUSADA")), PARCEIRO);

        verify(acesso).exigirEscrita(PARCEIRO, 1L);
        verify(tipos, never()).existsById(anyString());
        assertThat(resposta.dados().nomeFantasia()).isEqualTo("Hotel A Novo");
        assertThat(resposta.secretaria().tipoHospedagemCodigo()).isEqualTo("HOTEL");
        assertThat(hotel.getInventarioAtualizadoEm()).isNotNull();
        verify(auditoria).registrar(eq(2L), eq("estabelecimento"), eq(1L), eq("ATUALIZAR"), any(), any());
    }

    @Test
    void adminAlteraTambemOsDadosDaSecretaria() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        when(estabelecimentos.findById(1L)).thenReturn(Optional.of(hotel));
        when(tipos.existsById("POUSADA")).thenReturn(true);

        servico.atualizar(1L, new AtualizacaoRequest(Fixtures.dados("Hotel A"),
                Fixtures.secretaria("POUSADA", Situacao.IRREGULAR)), ADMIN);

        assertThat(hotel.getTipoHospedagemCodigo()).isEqualTo("POUSADA");
        assertThat(hotel.getSituacao()).isEqualTo(Situacao.IRREGULAR);
    }

    @Test
    void atualizarRecusaCnpjDeOutroEstabelecimento() {
        when(estabelecimentos.findById(1L)).thenReturn(Optional.of(Fixtures.estabelecimento(1, "Hotel A", "HOTEL")));
        when(estabelecimentos.existsByCnpjAndIdNot("12345678000199", 1L)).thenReturn(true);

        assertThatThrownBy(() -> servico.atualizar(1L,
                new AtualizacaoRequest(Fixtures.dados("Hotel A", "12345678000199"), null), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("outro estabelecimento");
    }

    @Test
    void inativarEReativar() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        when(estabelecimentos.findById(1L)).thenReturn(Optional.of(hotel));

        servico.definirAtivo(1L, false, ADMIN);
        assertThat(hotel.isAtivo()).isFalse();
        verify(auditoria).registrar(1L, "estabelecimento", 1L, "INATIVAR", null, null);

        servico.definirAtivo(1L, true, ADMIN);
        assertThat(hotel.isAtivo()).isTrue();
        verify(auditoria).registrar(1L, "estabelecimento", 1L, "REATIVAR", null, null);
    }

    // ------------------------------------------------------------------ capacidade

    @Test
    void historicoDeCapacidades() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        when(estabelecimentos.findById(1L)).thenReturn(Optional.of(hotel));
        when(capacidades.findByEstabelecimentoIdOrderByVigenteDesdeDesc(1L)).thenReturn(List.of(
                Fixtures.capacidade(6, hotel, JAN.plusMonths(3), 12, 36, 4), Fixtures.capacidade(5, hotel, JAN, 10, 30, 4)));

        assertThat(servico.capacidades(1L, PARCEIRO)).extracting(CapacidadeResponse::id).containsExactly(6L, 5L);
        verify(acesso).exigirLeitura(PARCEIRO, 1L);
    }

    @Test
    void registrarCapacidadeCriaNovaVigencia() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        when(estabelecimentos.findById(1L)).thenReturn(Optional.of(hotel));
        when(capacidades.save(any())).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 30L));
        CapacidadeRequest requisicao = capacidadeRequest(JAN.plusMonths(2), 12, 1);

        CapacidadeResponse resposta = servico.registrarCapacidade(1L, requisicao, PARCEIRO);

        verify(acesso).exigirEscrita(PARCEIRO, 1L);
        assertThat(resposta.id()).isEqualTo(30L);
        assertThat(resposta.qtdUh()).isEqualTo(12);
        assertThat(hotel.getInventarioAtualizadoEm()).isNotNull();
        verify(auditoria).registrar(2L, "capacidade_hospedagem", 30L, "CRIAR", null, requisicao);
    }

    @Test
    void registrarCapacidadeNaMesmaDataNaoPode() {
        when(estabelecimentos.findById(1L)).thenReturn(Optional.of(Fixtures.estabelecimento(1, "Hotel A", "HOTEL")));
        when(capacidades.existsByEstabelecimentoIdAndVigenteDesde(1L, JAN)).thenReturn(true);

        assertThatThrownBy(() -> servico.registrarCapacidade(1L, capacidadeRequest(JAN, 10, 0), ADMIN))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Use outra data");
        verify(capacidades, never()).save(any());
    }

    @Test
    void corrigirCapacidadeAlteraOLancamento() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        CapacidadeHospedagem capacidade = Fixtures.capacidade(5, hotel, JAN, 10, 30, 4);
        when(capacidades.findById(5L)).thenReturn(Optional.of(capacidade));
        when(estabelecimentos.findById(1L)).thenReturn(Optional.of(hotel));

        CapacidadeResponse depois = servico.corrigirCapacidade(1L, 5L, capacidadeRequest(JAN, 11, 0), ADMIN);

        assertThat(depois.qtdUh()).isEqualTo(11);
        assertThat(capacidade.getQtdLeitos()).isEqualTo(22);
        verify(capacidades, never()).existsByEstabelecimentoIdAndVigenteDesde(anyLong(), any());
        verify(auditoria).registrar(eq(1L), eq("capacidade_hospedagem"), eq(5L), eq("CORRIGIR"), any(), eq(depois));
    }

    @Test
    void corrigirCapacidadeParaUmaDataJaUsadaNaoPode() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        when(capacidades.findById(5L)).thenReturn(Optional.of(Fixtures.capacidade(5, hotel, JAN, 10, 30, 4)));
        when(capacidades.existsByEstabelecimentoIdAndVigenteDesde(1L, JAN.plusMonths(1))).thenReturn(true);

        assertThatThrownBy(() -> servico.corrigirCapacidade(1L, 5L, capacidadeRequest(JAN.plusMonths(1), 10, 0), ADMIN))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void capacidadeDeOutroEstabelecimentoNaoEhEncontrada() {
        Estabelecimento outro = Fixtures.estabelecimento(2, "Outro", "HOTEL");
        when(capacidades.findById(5L)).thenReturn(Optional.of(Fixtures.capacidade(5, outro, JAN, 10, 30, 4)));

        assertThatThrownBy(() -> servico.corrigirCapacidade(1L, 5L, capacidadeRequest(JAN, 10, 0), ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.excluirCapacidade(1L, 5L, ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void excluirCapacidade() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        CapacidadeHospedagem capacidade = Fixtures.capacidade(5, hotel, JAN, 10, 30, 4);
        when(capacidades.findById(5L)).thenReturn(Optional.of(capacidade));
        when(capacidades.findByEstabelecimentoIdOrderByVigenteDesdeDesc(1L))
                .thenReturn(List.of(Fixtures.capacidade(6, hotel, JAN.plusMonths(1), 10, 30, 4), capacidade));

        servico.excluirCapacidade(1L, 5L, ADMIN);

        verify(capacidades).delete(capacidade);
        verify(auditoria).registrar(eq(1L), eq("capacidade_hospedagem"), eq(5L), eq("EXCLUIR"), any(), isNull());
    }

    @Test
    void aUnicaCapacidadeNaoPodeSerExcluida() {
        Estabelecimento hotel = Fixtures.estabelecimento(1, "Hotel A", "HOTEL");
        CapacidadeHospedagem capacidade = Fixtures.capacidade(5, hotel, JAN, 10, 30, 4);
        when(capacidades.findById(5L)).thenReturn(Optional.of(capacidade));
        when(capacidades.findByEstabelecimentoIdOrderByVigenteDesdeDesc(1L)).thenReturn(List.of(capacidade));

        assertThatThrownBy(() -> servico.excluirCapacidade(1L, 5L, ADMIN)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("única capacidade");
        verify(capacidades, never()).delete(any());
    }
}
