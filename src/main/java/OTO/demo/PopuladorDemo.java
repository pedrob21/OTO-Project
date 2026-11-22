package OTO.demo;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import OTO.calculo.ResultadoService;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.demo.DadosDemo.Estabelecimento;
import OTO.demo.DadosDemo.Feriado;
import OTO.demo.DadosDemo.Historico;
import OTO.demo.DadosDemo.Periodo;
import OTO.usuario.Perfil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Popula o banco com dados fictícios para demonstrar o painel, os indicadores e os relatórios.
 *
 * <p>Só roda com {@code OTO_DEMO_POPULAR=true} (propriedade {@code oto.demo.popular}), depois que a
 * aplicação sobe, e só uma vez: se já houver estabelecimento da demonstração, não faz nada. Precisa
 * de um ADMIN no banco (o do ADMIN_EMAIL serve), que fica como autor das pesquisas e publicações.
 *
 * <p>Os resultados não são inventados: as respostas entram no banco e cada campanha é publicada pelo
 * {@link ResultadoService}, com o mesmo cálculo da tela de publicação. Depois, as datas de
 * publicação são levadas para o passado, para a lista de relatórios parecer real.
 *
 * <p>O mês passado fica de fora de propósito: é o que a equipe ainda estaria pesquisando, e serve
 * para testar à mão a criação da pesquisa, os lançamentos, a prorrogação e a publicação.
 */
@Component
@ConditionalOnProperty(prefix = "oto.demo", name = "popular", havingValue = "true")
public class PopuladorDemo {

    private static final Logger log = LoggerFactory.getLogger(PopuladorDemo.class);

    /** Meses publicados no sistema: do 13º ao 2º antes do atual. Antes disso, série histórica. */
    private static final int MESES_NO_SISTEMA = 12;
    private static final int ANOS_DE_HISTORICO = 3;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;
    private final ResultadoService resultados;
    private final Clock relogio;

    @PersistenceContext
    private EntityManager entityManager;

    public PopuladorDemo(JdbcTemplate jdbc, PlatformTransactionManager transacoes, ResultadoService resultados,
            Clock relogio) {
        this.jdbc = jdbc;
        this.transacao = new TransactionTemplate(transacoes);
        this.resultados = resultados;
        this.relogio = relogio;
    }

    /** Campanha criada aqui e o dia em que ela "foi" publicada. */
    private record Publicacao(long campanhaId, LocalDateTime em) {
    }

    @EventListener(ApplicationReadyEvent.class)
    public void popular() {
        try {
            Integer existentes = jdbc.queryForObject("SELECT count(*) FROM estabelecimento WHERE cnpj LIKE ?",
                    Integer.class, DadosDemo.PREFIXO_CNPJ + "%");
            if (existentes != null && existentes > 0) {
                log.info("Demonstração: os dados fictícios já estão no banco. Nada a fazer.");
                return;
            }
            List<Long> admins = jdbc.queryForList(
                    "SELECT id FROM usuario WHERE perfil = 'ADMIN' AND ativo ORDER BY id LIMIT 1", Long.class);
            if (admins.isEmpty()) {
                log.warn("Demonstração: nenhum ADMIN ativo no banco. Defina ADMIN_EMAIL e ADMIN_SENHA e suba de novo.");
                return;
            }
            Resumo resumo = transacao.execute(status -> popular(admins.get(0)));
            log.info("Demonstração: {} estabelecimentos, {} campanhas publicadas, {} respostas e {} valores da série "
                    + "histórica. Desligue OTO_DEMO_POPULAR.", resumo.estabelecimentos, resumo.publicacoes,
                    resumo.respostas, resumo.historico);
        } catch (RuntimeException e) {
            // A aplicação continua no ar: a demonstração é opcional, e a transação desfaz tudo.
            log.error("Demonstração: não foi possível popular o banco. Nada foi gravado.", e);
        }
    }

    private static final class Resumo {
        int estabelecimentos;
        int publicacoes;
        int respostas;
        int historico;
    }

    private Resumo popular(long adminId) {
        Resumo resumo = new Resumo();
        YearMonth mesAtual = YearMonth.now(relogio);
        YearMonth primeiroMes = mesAtual.minusMonths(MESES_NO_SISTEMA + 1L);
        YearMonth ultimoMes = mesAtual.minusMonths(2);
        LocalDate inicioDoSistema = primeiroMes.atDay(1);
        LocalDate fimDoSistema = ultimoMes.atEndOfMonth();

        Map<Estabelecimento, Long> ids = inserirEstabelecimentos(adminId, mesAtual.minusMonths(6).atDay(1));
        resumo.estabelecimentos = ids.size();
        jdbc.update("""
                INSERT INTO rhc_resumo (vigente_desde, qtd_registros, qtd_uh, qtd_leitos, max_leitos_por_uh, registrado_por)
                VALUES (DATE '2022-01-01', 420, 610, 2300, 5, ?) ON CONFLICT (vigente_desde) DO NOTHING""", adminId);

        List<Publicacao> publicacoes = new ArrayList<>();

        // Pesquisas mensais: abertas no dia 2 do mês seguinte, com o mês e os feriados que começaram nele.
        for (YearMonth mes = primeiroMes; !mes.isAfter(ultimoMes); mes = mes.plusMonths(1)) {
            YearMonth seguinte = mes.plusMonths(1);
            Month mesDoAno = mes.getMonth();
            long rodada = inserirRodada(adminId, String.format("%sM%02d%02d", DadosDemo.PREFIXO_RODADA,
                    mes.getMonthValue(), mes.getYear() % 100), "Pesquisa de " + nomeDoMes(mes),
                    seguinte.atDay(2), seguinte.atDay(12));
            LocalDateTime publicadaEm = seguinte.atDay(15).atTime(10, 0);

            long mensal = inserirCampanha(rodada, nomeDoMes(mes), "CONSOLIDADA", null,
                    new Periodo(mes.atDay(1), mes.atEndOfMonth()), mes.lengthOfMonth());
            resumo.respostas += inserirRespostas(mensal, ids, adminId, seguinte.atDay(3),
                    (e, sorteio) -> DadosDemo.taxaMensal(e, mesDoAno, sorteio),
                    DadosDemo.fatorDaDiaria(mesDoAno));
            publicacoes.add(new Publicacao(mensal, publicadaEm));

            for (Feriado feriado : Feriado.values()) {
                Periodo periodo = feriado.periodo(mes.getYear());
                if (!YearMonth.from(periodo.inicio()).equals(mes) || periodo.fim().isAfter(fimDoSistema)) {
                    continue;
                }
                // A expectativa tem pesquisa própria, aberta 12 dias antes e publicada 2 dias antes do feriado.
                // Entra antes da consolidada: no empate de período, a lista mostra primeiro a de id maior.
                LocalDate inicio = periodo.inicio();
                long rodadaExpectativa = inserirRodada(adminId, String.format("%sX%02d%02d%s",
                        DadosDemo.PREFIXO_RODADA, mes.getMonthValue(), mes.getYear() % 100, feriado.ordinal()),
                        "Expectativa: " + feriado.nome(mes.getYear()), inicio.minusDays(12), inicio.minusDays(4));
                long expectativa = inserirCampanha(rodadaExpectativa, feriado.nome(mes.getYear()), "EXPECTATIVA",
                        feriado, periodo, periodo.diarias());
                resumo.respostas += inserirRespostas(expectativa, ids, adminId, inicio.minusDays(11),
                        (e, sorteio) -> DadosDemo.taxaFeriado(e, feriado, true, sorteio),
                        DadosDemo.fatorDaDiaria(feriado));
                publicacoes.add(new Publicacao(expectativa, inicio.minusDays(2).atTime(10, 0)));
                long consolidada = inserirCampanha(rodada, feriado.nome(mes.getYear()), "CONSOLIDADA", feriado,
                        periodo, periodo.diarias());
                resumo.respostas += inserirRespostas(consolidada, ids, adminId, seguinte.atDay(3),
                        (e, sorteio) -> DadosDemo.taxaFeriado(e, feriado, false, sorteio),
                        DadosDemo.fatorDaDiaria(feriado));
                publicacoes.add(new Publicacao(consolidada, publicadaEm));
            }
        }

        UsuarioLogado admin = new UsuarioLogado(adminId, Perfil.ADMIN);
        for (Publicacao publicacao : publicacoes) {
            resultados.publicar(publicacao.campanhaId(), admin);
        }
        // grava as publicações antes de mudar as datas, senão o Hibernate as sobrescreve no commit
        entityManager.flush();
        for (Publicacao publicacao : publicacoes) {
            Timestamp em = instante(publicacao.em());
            jdbc.update("UPDATE campanha SET publicada_em = ? WHERE id = ?", em, publicacao.campanhaId());
            jdbc.update("UPDATE resultado_campanha SET calculado_em = ? WHERE campanha_id = ?", em,
                    publicacao.campanhaId());
        }
        resumo.publicacoes = publicacoes.size();
        resumo.historico = inserirHistorico(inicioDoSistema);
        return resumo;
    }

    private Map<Estabelecimento, Long> inserirEstabelecimentos(long adminId, LocalDate dataDaAmpliacao) {
        Map<Estabelecimento, Long> ids = new HashMap<>();
        int sequencia = 1;
        for (Estabelecimento e : DadosDemo.ESTABELECIMENTOS) {
            String cnpj = String.format("%s%010d%02d", DadosDemo.PREFIXO_CNPJ, sequencia, 1);
            String dominio = e.nome().toLowerCase(PT_BR).replaceAll("[^a-z0-9]+", "") + ".example.com";
            Long id = jdbc.queryForObject("""
                    INSERT INTO estabelecimento (tipo_hospedagem_codigo, situacao, nome_fantasia, razao_social, cnpj,
                        data_abertura, endereco, bairro, regiao, telefone_comercial, email_comercial, site,
                        cadastur_exigencia, possui_cadastur, funcionamento_24h, horario_check_in, horario_check_out,
                        contato_nome, aceita_pesquisas, email_pesquisas, ativo)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Olímpia', ?, ?, ?, 'OBRIGATORIO', TRUE, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id""", Long.class,
                    e.tipo(), e.situacao(), e.nome(), e.nome() + " Ltda. (fictícia)", cnpj,
                    Date.valueOf(LocalDate.of(2000 + sequencia % 20, 1 + sequencia % 12, 1 + sequencia % 27)),
                    "Rua Fictícia, " + (100 + sequencia * 37), e.bairro(),
                    String.format("(17) 3280-%04d", 1000 + sequencia), "contato@" + dominio, "https://" + dominio,
                    "RESORT".equals(e.tipo()) || "HOTEL".equals(e.tipo()), LocalTime.of(14, 0), LocalTime.of(12, 0),
                    "Gerência do " + e.nome(), e.resposta() > 0, "pesquisas@" + dominio, e.ativo());
            ids.put(e, id);
            jdbc.update("""
                    INSERT INTO capacidade_hospedagem (estabelecimento_id, vigente_desde, qtd_uh, qtd_uh_pcd, qtd_leitos,
                        max_leitos_por_uh, registrado_por) VALUES (?, DATE '2022-01-01', ?, ?, ?, ?, ?)""",
                    id, e.uh(), e.uhPcd(), e.leitos(), e.maxLeitosPorUh(), adminId);
            if (e.nome().equals(DadosDemo.ESTABELECIMENTO_AMPLIADO)) {
                jdbc.update("""
                        INSERT INTO capacidade_hospedagem (estabelecimento_id, vigente_desde, qtd_uh, qtd_uh_pcd,
                            qtd_leitos, max_leitos_por_uh, registrado_por) VALUES (?, ?, ?, ?, ?, ?, ?)""",
                        id, Date.valueOf(dataDaAmpliacao), DadosDemo.UH_DEPOIS_DA_AMPLIACAO, e.uhPcd(),
                        DadosDemo.LEITOS_DEPOIS_DA_AMPLIACAO, e.maxLeitosPorUh(), adminId);
            }
            sequencia++;
        }
        return ids;
    }

    /** Pesquisa já encerrada no dia seguinte ao prazo. */
    private long inserirRodada(long adminId, String codigo, String titulo, LocalDate abertura, LocalDate prazo) {
        return jdbc.queryForObject("""
                INSERT INTO rodada_pesquisa (codigo, titulo, status, data_abertura, prazo_resposta, encerrada_em,
                    criado_por, criado_em) VALUES (?, ?, 'ENCERRADA', ?, ?, ?, ?, ?) RETURNING id""", Long.class,
                codigo, titulo, Date.valueOf(abertura), Date.valueOf(prazo),
                instante(prazo.plusDays(1).atTime(18, 0)), adminId,
                instante(abertura.minusDays(3).atTime(9, 0)));
    }

    private long inserirCampanha(long rodada, String nome, String modalidade, Feriado feriado, Periodo periodo,
            int diarias) {
        return jdbc.queryForObject("""
                INSERT INTO campanha (rodada_id, nome, modalidade, tipo_periodo, feriado_codigo, periodo_inicio,
                    periodo_fim, qtd_diarias) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
                rodada, nome, modalidade, feriado == null ? "MENSAL" : "FERIADO",
                feriado == null ? null : feriado.name(), Date.valueOf(periodo.inicio()), Date.valueOf(periodo.fim()),
                diarias);
    }

    @FunctionalInterface
    private interface Taxa {
        BigDecimal sortear(Estabelecimento estabelecimento, Random sorteio);
    }

    /**
     * Respostas dos estabelecimentos que entram no cálculo, cada um com a sua chance de responder.
     * Uma em cada dez foi corrigida depois (versão 2), para o histórico da resposta ter o que mostrar.
     */
    private int inserirRespostas(long campanha, Map<Estabelecimento, Long> ids, long adminId, LocalDate primeiroDia,
            Taxa taxa, double fatorDaDiaria) {
        Random sorteio = new Random(campanha * 7919L);
        int quantidade = 0;
        for (Estabelecimento e : DadosDemo.ESTABELECIMENTOS) {
            if (sorteio.nextDouble() >= e.resposta()) {
                continue;
            }
            BigDecimal valor = taxa.sortear(e, sorteio);
            BigDecimal diaria = DadosDemo.diaria(e, fatorDaDiaria, sorteio);
            LocalDateTime enviadaEm = primeiroDia.plusDays(sorteio.nextInt(8)).atTime(9 + sorteio.nextInt(9), 0);
            boolean corrigida = sorteio.nextDouble() < 0.1;
            long resposta = jdbc.queryForObject("""
                    INSERT INTO resposta_ocupacao (campanha_id, estabelecimento_id, taxa_ocupacao, diaria_media, versao,
                        enviado_por, enviado_em) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
                    campanha, ids.get(e), valor, diaria, corrigida ? 2 : 1, adminId, instante(enviadaEm));
            String historico = """
                    INSERT INTO resposta_ocupacao_historico (resposta_id, versao, taxa_ocupacao, diaria_media,
                        enviado_por, enviado_em) VALUES (?, ?, ?, ?, ?, ?)""";
            if (corrigida) {
                jdbc.update(historico, resposta, 1, valor.subtract(BigDecimal.TEN).max(BigDecimal.ONE), diaria,
                        adminId, instante(enviadaEm.minusDays(1)));
            }
            jdbc.update(historico, resposta, corrigida ? 2 : 1, valor, diaria, adminId, instante(enviadaEm));
            quantidade++;
        }
        return quantidade;
    }

    /** Série histórica da cidade nos três anos antes do sistema: meses e feriados. */
    private int inserirHistorico(LocalDate inicioDoSistema) {
        int anoInicial = inicioDoSistema.getYear() - ANOS_DE_HISTORICO;
        Random sorteio = new Random(anoInicial);
        String sql = """
                INSERT INTO indicador_historico (indicador, modalidade, ano, mes, feriado_codigo, valor, fonte)
                VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING""";
        int quantidade = 0;
        for (YearMonth mes = YearMonth.of(anoInicial, 1); mes.atDay(1).isBefore(inicioDoSistema);
                mes = mes.plusMonths(1)) {
            int anosAntes = inicioDoSistema.getYear() - mes.getYear();
            Historico h = DadosDemo.historicoMensal(mes.getYear(), mes.getMonth(), anosAntes, sorteio);
            quantidade += inserirIndicadores(sql, "CONSOLIDADA", mes.getYear(), mes.getMonthValue(), null, h);
        }
        for (int ano = anoInicial; ano <= inicioDoSistema.getYear(); ano++) {
            for (Feriado feriado : Feriado.values()) {
                if (!feriado.periodo(ano).inicio().isBefore(inicioDoSistema)) {
                    continue;
                }
                int anosAntes = inicioDoSistema.getYear() - ano;
                quantidade += inserirIndicadores(sql, "CONSOLIDADA", ano, null, feriado.name(),
                        DadosDemo.historicoFeriado(feriado, ano, false, anosAntes, sorteio));
                quantidade += inserirIndicadores(sql, "EXPECTATIVA", ano, null, feriado.name(),
                        DadosDemo.historicoFeriado(feriado, ano, true, anosAntes, sorteio));
            }
        }
        return quantidade;
    }

    private int inserirIndicadores(String sql, String modalidade, int ano, Integer mes, String feriado, Historico h) {
        return jdbc.update(sql, "TAXA_OCUPACAO", modalidade, ano, mes, feriado, h.taxa(), DadosDemo.FONTE)
                + jdbc.update(sql, "FLUXO_TURISTAS", modalidade, ano, mes, feriado, h.fluxo(), DadosDemo.FONTE)
                + jdbc.update(sql, "DIARIA_MEDIA", modalidade, ano, mes, feriado, h.diaria(), DadosDemo.FONTE);
    }

    /** "Outubro de 2025" */
    private static String nomeDoMes(YearMonth mes) {
        String nome = mes.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1) + " de " + mes.getYear();
    }

    /** Hora no fuso do OTO, e não no do servidor (que costuma ser UTC). */
    private Timestamp instante(LocalDateTime dataHora) {
        return Timestamp.from(dataHora.atZone(relogio.getZone()).toInstant());
    }
}
