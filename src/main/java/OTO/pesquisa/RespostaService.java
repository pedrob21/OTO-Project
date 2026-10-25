package OTO.pesquisa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.calculo.ResultadoCampanha;
import OTO.calculo.ResultadoCampanhaRepository;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.ControleAcesso;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.estabelecimento.Estabelecimento;
import OTO.estabelecimento.EstabelecimentoRepository;
import OTO.pesquisa.RespostaDtos.HistoricoRespostaResponse;
import OTO.pesquisa.RespostaDtos.MinhaRespostaResponse;
import OTO.pesquisa.RespostaDtos.PendenciaResponse;
import OTO.pesquisa.RespostaDtos.RespostaRequest;
import OTO.pesquisa.RespostaDtos.RespostaResponse;

/**
 * Respostas de ocupação. Vale o último envio: cada correção soma uma versão e todos
 * os envios ficam no histórico. A resposta individual é sigilosa: só o ADMIN e o
 * próprio parceiro a veem; o restante do sistema só mostra os resultados agregados.
 */
@Service
public class RespostaService {

    private final RespostaOcupacaoRepository respostas;
    private final RespostaOcupacaoHistoricoRepository historicos;
    private final CampanhaRepository campanhas;
    private final EstabelecimentoRepository estabelecimentos;
    private final ResultadoCampanhaRepository resultados;
    private final ControleAcesso acesso;

    public RespostaService(RespostaOcupacaoRepository respostas, RespostaOcupacaoHistoricoRepository historicos,
            CampanhaRepository campanhas, EstabelecimentoRepository estabelecimentos,
            ResultadoCampanhaRepository resultados, ControleAcesso acesso) {
        this.respostas = respostas;
        this.historicos = historicos;
        this.campanhas = campanhas;
        this.estabelecimentos = estabelecimentos;
        this.resultados = resultados;
        this.acesso = acesso;
    }

    /**
     * Envia ou corrige a resposta de um estabelecimento. O parceiro responde enquanto a
     * pesquisa está aberta; a equipe do OTO (ADMIN) também lança respostas recebidas por
     * telefone ou WhatsApp depois do encerramento, enquanto a campanha não é publicada.
     */
    @Transactional
    public RespostaResponse responder(Long campanhaId, Long estabelecimentoId, RespostaRequest requisicao,
            UsuarioLogado usuario) {
        acesso.exigirEscrita(usuario, estabelecimentoId);
        Campanha campanha = carregarCampanha(campanhaId);
        RodadaPesquisa rodada = campanha.getRodada();
        boolean podeResponder = rodada.isAberta() || (usuario.isAdmin() && rodada.isEncerrada());
        if (!podeResponder) {
            throw new RegraNegocioException("A pesquisa " + rodada.getCodigo() + " não está aberta para respostas.");
        }
        if (campanha.isPublicada()) {
            throw new RegraNegocioException("A campanha já foi publicada. Cancele a publicação para mudar as respostas.");
        }
        Estabelecimento estabelecimento = estabelecimentos.findById(estabelecimentoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Estabelecimento", estabelecimentoId));
        if (!estabelecimento.isConsideradoNoCalculo()) {
            throw new RegraNegocioException("Este estabelecimento está inativo ou irregular e não participa das pesquisas.");
        }
        RespostaOcupacao resposta = respostas.findByCampanhaIdAndEstabelecimentoId(campanhaId, estabelecimentoId)
                .orElse(null);
        if (resposta == null) {
            resposta = respostas.save(new RespostaOcupacao(campanha, estabelecimento, requisicao.taxaOcupacao(),
                    requisicao.diariaMedia(), usuario.id()));
        }
        else {
            resposta.corrigir(requisicao.taxaOcupacao(), requisicao.diariaMedia(), usuario.id());
        }
        historicos.save(RespostaOcupacaoHistorico.de(resposta));
        return RespostaResponse.de(resposta);
    }

    @Transactional(readOnly = true)
    public List<RespostaResponse> listarDaCampanha(Long campanhaId) {
        carregarCampanha(campanhaId);
        return respostas.findByCampanhaIdComEstabelecimento(campanhaId).stream()
                .sorted(Comparator.comparing(r -> r.getEstabelecimento().getNomeFantasia()))
                .map(RespostaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoRespostaResponse> historico(Long respostaId, UsuarioLogado usuario) {
        RespostaOcupacao resposta = respostas.findById(respostaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Resposta", respostaId));
        acesso.exigirLeitura(usuario, resposta.getEstabelecimento().getId());
        return historicos.findByRespostaIdOrderByVersaoDesc(respostaId).stream()
                .map(HistoricoRespostaResponse::de)
                .toList();
    }

    /** Campanhas abertas para cada estabelecimento do parceiro, já respondidas ou não. */
    @Transactional(readOnly = true)
    public List<PendenciaResponse> pendencias(UsuarioLogado parceiro) {
        List<Long> meus = acesso.estabelecimentosDo(parceiro);
        if (meus.isEmpty()) {
            return List.of();
        }
        List<Estabelecimento> participantes = estabelecimentos.findByIdInOrderByNomeFantasia(meus).stream()
                .filter(Estabelecimento::isConsideradoNoCalculo)
                .toList();
        List<Campanha> abertas = campanhas.findByStatusDaRodada(StatusRodada.ABERTA);
        if (participantes.isEmpty() || abertas.isEmpty()) {
            return List.of();
        }
        Map<String, RespostaOcupacao> enviadas = new HashMap<>();
        for (RespostaOcupacao r : respostas.findByCampanhaIdsComEstabelecimento(abertas.stream().map(Campanha::getId).toList())) {
            enviadas.put(chave(r.getCampanha().getId(), r.getEstabelecimento().getId()), r);
        }
        List<PendenciaResponse> lista = new ArrayList<>();
        for (Campanha campanha : abertas) {
            for (Estabelecimento e : participantes) {
                lista.add(PendenciaResponse.de(campanha, e, enviadas.get(chave(campanha.getId(), e.getId()))));
            }
        }
        return lista;
    }

    /** Respostas do parceiro em todas as campanhas, com a taxa geral publicada para comparação. */
    @Transactional(readOnly = true)
    public List<MinhaRespostaResponse> minhas(UsuarioLogado parceiro) {
        List<Long> meus = acesso.estabelecimentosDo(parceiro);
        if (meus.isEmpty()) {
            return List.of();
        }
        List<RespostaOcupacao> lista = respostas.findByEstabelecimentoIdsComCampanha(meus);
        Map<Long, BigDecimal> taxaCidade = new HashMap<>();
        if (!lista.isEmpty()) {
            List<Long> campanhaIds = lista.stream().map(resposta -> resposta.getCampanha().getId()).distinct().toList();
            for (ResultadoCampanha publicado : resultados.findByCampanhaIdIn(campanhaIds)) {
                taxaCidade.put(publicado.getCampanhaId(), publicado.getTaxaOcupacao());
            }
        }
        return lista.stream().map(r -> {
            Campanha c = r.getCampanha();
            Estabelecimento e = r.getEstabelecimento();
            return new MinhaRespostaResponse(r.getId(), c.getId(), c.getNome(), c.getModalidade(), c.getTipoPeriodo(),
                    c.getFeriadoCodigo(), c.getPeriodoInicio(), c.getPeriodoFim(), e.getId(), e.getNomeFantasia(),
                    r.getTaxaOcupacao(), r.getDiariaMedia(), taxaCidade.get(c.getId()));
        }).toList();
    }

    private Campanha carregarCampanha(Long campanhaId) {
        return campanhas.findById(campanhaId).orElseThrow(() -> RecursoNaoEncontradoException.de("Campanha", campanhaId));
    }

    private static String chave(Long campanhaId, Long estabelecimentoId) {
        return campanhaId + ":" + estabelecimentoId;
    }
}
