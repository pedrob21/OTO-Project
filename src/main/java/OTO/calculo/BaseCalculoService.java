package OTO.calculo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import OTO.comum.excecao.RegraNegocioException;
import OTO.estabelecimento.CapacidadeHospedagem;
import OTO.estabelecimento.CapacidadeHospedagemRepository;
import OTO.estabelecimento.Estabelecimento;
import OTO.estabelecimento.EstabelecimentoRepository;
import OTO.estabelecimento.RhcResumo;
import OTO.estabelecimento.RhcResumoRepository;
import OTO.pesquisa.Campanha;
import OTO.pesquisa.CampanhaDiariaRepository;
import OTO.pesquisa.ParametroCalculo;
import OTO.pesquisa.ParametroCalculoRepository;
import OTO.pesquisa.RespostaOcupacao;
import OTO.pesquisa.RespostaOcupacaoRepository;
import OTO.referencia.TipoHospedagemRepository;

/** lê do banco a {@link BaseCalculo} de uma campanha. Chamar dentro de uma transação. */
@Service
public class BaseCalculoService {

    private final EstabelecimentoRepository estabelecimentos;
    private final CapacidadeHospedagemRepository capacidades;
    private final RespostaOcupacaoRepository respostas;
    private final RhcResumoRepository rhc;
    private final ParametroCalculoRepository parametros;
    private final TipoHospedagemRepository tipos;
    private final CampanhaDiariaRepository diarias;

    public BaseCalculoService(EstabelecimentoRepository estabelecimentos, CapacidadeHospedagemRepository capacidades,
            RespostaOcupacaoRepository respostas, RhcResumoRepository rhc, ParametroCalculoRepository parametros,
            TipoHospedagemRepository tipos, CampanhaDiariaRepository diarias) {
        this.estabelecimentos = estabelecimentos;
        this.capacidades = capacidades;
        this.respostas = respostas;
        this.rhc = rhc;
        this.parametros = parametros;
        this.tipos = tipos;
        this.diarias = diarias;
    }

    public BaseCalculo montar(Campanha campanha) {
        LocalDate data = campanha.getPeriodoFim();

        Map<Long, CapacidadeHospedagem> capacidadePorEstabelecimento = new HashMap<>();
        for (CapacidadeHospedagem c : capacidades.findVigentesEm(data)) {
            capacidadePorEstabelecimento.put(c.getEstabelecimento().getId(), c);
        }
        Map<Long, RespostaOcupacao> respostaPorEstabelecimento = new HashMap<>();
        for (RespostaOcupacao r : respostas.findByCampanhaIdComEstabelecimento(campanha.getId())) {
            respostaPorEstabelecimento.put(r.getEstabelecimento().getId(), r);
        }

        List<BaseCalculo.Participante> participantes = new ArrayList<>();
        List<Estabelecimento> semCapacidade = new ArrayList<>();
        for (Estabelecimento e : estabelecimentos.findAllByOrderByNomeFantasia()) {
            if (!e.isConsideradoNoCalculo()) {
                continue;
            }
            CapacidadeHospedagem capacidade = capacidadePorEstabelecimento.get(e.getId());
            if (capacidade == null) {
                semCapacidade.add(e);
                continue;
            }
            participantes.add(new BaseCalculo.Participante(e, capacidade, respostaPorEstabelecimento.remove(e.getId())));
        }
        List<RespostaOcupacao> desconsideradas = respostaPorEstabelecimento.values().stream()
                .sorted(Comparator.comparing(r -> r.getEstabelecimento().getNomeFantasia()))
                .toList();

        RhcResumo caseira = rhc.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(data).orElse(null);
        ParametroCalculo parametro = parametros.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(data)
                .orElseThrow(() -> new RegraNegocioException(
                        "Não há parâmetros de cálculo vigentes em " + data + ". Cadastre em Parâmetros de cálculo."));

        return new BaseCalculo(campanha, data, List.copyOf(participantes), List.copyOf(semCapacidade), desconsideradas,
                caseira, parametro, tipos.findAllByOrderByOrdem(), diarias.findByCampanhaId(campanha.getId()));
    }
}
