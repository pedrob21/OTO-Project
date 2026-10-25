package OTO.dashboard;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import OTO.dashboard.DashboardDtos.FeriadoSerie;
import OTO.dashboard.DashboardDtos.ResumoResponse;
import OTO.dashboard.DashboardDtos.SerieMensalResponse;
import OTO.historico.Indicador;
import OTO.pesquisa.Modalidade;

/**
 * dados dos gráficos da tela inicial (qualquer perfil). Só valores gerais da cidade.
 * Sem anos informados, o intervalo é dos últimos três anos.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService servico;

    public DashboardController(DashboardService servico) {
        this.servico = servico;
    }

    /** Último mês e último feriado publicados, com o comparativo do ano anterior. */
    @GetMapping("/resumo")
    public ResumoResponse resumo(@RequestParam(defaultValue = "CONSOLIDADA") Modalidade modalidade) {
        return servico.resumo(modalidade);
    }

    @GetMapping("/serie-mensal")
    public SerieMensalResponse serieMensal(@RequestParam(defaultValue = "TAXA_OCUPACAO") Indicador indicador,
            @RequestParam(defaultValue = "CONSOLIDADA") Modalidade modalidade,
            @RequestParam(required = false) Integer anoInicio, @RequestParam(required = false) Integer anoFim) {
        int fim = anoFim == null ? LocalDate.now().getYear() : anoFim;
        int inicio = anoInicio == null ? fim - 2 : anoInicio;
        return servico.serieMensal(indicador, modalidade, inicio, fim);
    }

    @GetMapping("/feriados")
    public List<FeriadoSerie> feriados(@RequestParam(defaultValue = "CONSOLIDADA") Modalidade modalidade,
            @RequestParam(required = false) String feriado, @RequestParam(required = false) Integer anoInicio,
            @RequestParam(required = false) Integer anoFim) {
        int fim = anoFim == null ? LocalDate.now().getYear() : anoFim;
        int inicio = anoInicio == null ? fim - 2 : anoInicio;
        return servico.feriados(modalidade, feriado, inicio, fim);
    }
}
