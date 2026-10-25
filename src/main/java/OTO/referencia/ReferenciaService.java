package OTO.referencia;

import java.math.BigDecimal;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OTO.comum.Codigos;
import OTO.comum.auditoria.AuditoriaService;
import OTO.comum.excecao.RecursoNaoEncontradoException;
import OTO.comum.excecao.RegraNegocioException;
import OTO.comum.seguranca.UsuarioLogado;
import OTO.referencia.ReferenciaDtos.FaixaRef;
import OTO.referencia.ReferenciaDtos.FaixaRequest;
import OTO.referencia.ReferenciaDtos.FeriadoRef;
import OTO.referencia.ReferenciaDtos.FeriadoRequest;
import OTO.referencia.ReferenciaDtos.Referencias;
import OTO.referencia.ReferenciaDtos.TipoRef;
import OTO.referencia.ReferenciaDtos.TipoRequest;

/**
 * Tabelas de referência mantidas pela equipe do OTO. O código é a chave usada no resto
 * do banco, então não muda; a exclusão só vale para o que nunca foi usado.
 */
@Service
public class ReferenciaService {

    private final TipoHospedagemRepository tipos;
    private final FeriadoEventoRepository feriados;
    private final FaixaOcupacaoRepository faixas;
    private final AuditoriaService auditoria;

    public ReferenciaService(TipoHospedagemRepository tipos, FeriadoEventoRepository feriados,
            FaixaOcupacaoRepository faixas, AuditoriaService auditoria) {
        this.tipos = tipos;
        this.feriados = feriados;
        this.faixas = faixas;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public Referencias listar() {
        return new Referencias(
                tipos.findAllByOrderByOrdem().stream().map(TipoRef::de).toList(),
                feriados.findAllByOrderByOrdem().stream().map(FeriadoRef::de).toList(),
                faixas.findAllByOrderByPercentualMinimo().stream().map(FaixaRef::de).toList());
    }

    // ------------------------------------------------------------------ tipos de hospedagem

    @Transactional
    public TipoRef criarTipo(TipoRequest r, UsuarioLogado admin) {
        String nome = r.nome().trim();
        String codigo = codigoNovo(r.codigo(), nome, 20, tipos::existsById, "um tipo de hospedagem");
        int ordem = r.ordem() != null ? r.ordem() : proxima(tipos.findAll().stream().map(TipoHospedagem::getOrdem));
        TipoRef criado = TipoRef.de(tipos.save(new TipoHospedagem(codigo, nome, r.calculaReceita(), ordem)));
        auditoria.registrar(admin.id(), "tipo_hospedagem", codigo, "CRIAR", null, criado);
        return criado;
    }

    @Transactional
    public TipoRef alterarTipo(String codigo, TipoRequest r, UsuarioLogado admin) {
        TipoHospedagem tipo = tipos.findById(codigo)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Tipo de hospedagem", codigo));
        exigirMesmoCodigo(codigo, r.codigo());
        TipoRef antes = TipoRef.de(tipo);
        tipo.alterar(r.nome().trim(), r.calculaReceita(), r.ordem() != null ? r.ordem() : tipo.getOrdem());
        TipoRef depois = TipoRef.de(tipo);
        auditoria.registrar(admin.id(), "tipo_hospedagem", codigo, "ATUALIZAR", antes, depois);
        return depois;
    }

    @Transactional
    public void excluirTipo(String codigo, UsuarioLogado admin) {
        TipoHospedagem tipo = tipos.findById(codigo)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Tipo de hospedagem", codigo));
        TipoRef antes = TipoRef.de(tipo);
        try {
            tipos.delete(tipo);
            tipos.flush();
        }
        catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("O tipo " + tipo.getNome() + " já é usado (estabelecimentos, resultados, "
                    + "diárias ou série histórica) e não pode ser excluído.");
        }
        auditoria.registrar(admin.id(), "tipo_hospedagem", codigo, "EXCLUIR", antes, null);
    }

    // ------------------------------------------------------------------ feriados e eventos

    @Transactional
    public FeriadoRef criarFeriado(FeriadoRequest r, UsuarioLogado admin) {
        String nome = r.nome().trim();
        String codigo = codigoNovo(r.codigo(), nome, 40, feriados::existsById, "um feriado ou evento");
        int ordem = r.ordem() != null ? r.ordem() : proxima(feriados.findAll().stream().map(FeriadoEvento::getOrdem));
        FeriadoRef criado = FeriadoRef.de(feriados.save(new FeriadoEvento(codigo, nome, ordem)));
        auditoria.registrar(admin.id(), "feriado_evento", codigo, "CRIAR", null, criado);
        return criado;
    }

    @Transactional
    public FeriadoRef alterarFeriado(String codigo, FeriadoRequest r, UsuarioLogado admin) {
        FeriadoEvento feriado = feriados.findById(codigo)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Feriado ou evento", codigo));
        exigirMesmoCodigo(codigo, r.codigo());
        FeriadoRef antes = FeriadoRef.de(feriado);
        feriado.alterar(r.nome().trim(), r.ordem() != null ? r.ordem() : feriado.getOrdem());
        FeriadoRef depois = FeriadoRef.de(feriado);
        auditoria.registrar(admin.id(), "feriado_evento", codigo, "ATUALIZAR", antes, depois);
        return depois;
    }

    @Transactional
    public void excluirFeriado(String codigo, UsuarioLogado admin) {
        FeriadoEvento feriado = feriados.findById(codigo)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Feriado ou evento", codigo));
        FeriadoRef antes = FeriadoRef.de(feriado);
        try {
            feriados.delete(feriado);
            feriados.flush();
        }
        catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("O feriado " + feriado.getNome() + " já é usado (campanhas ou série "
                    + "histórica) e não pode ser excluído.");
        }
        auditoria.registrar(admin.id(), "feriado_evento", codigo, "EXCLUIR", antes, null);
    }

    // ------------------------------------------------------------------ faixas de ocupação

    @Transactional
    public FaixaRef criarFaixa(FaixaRequest r, UsuarioLogado admin) {
        String nome = r.nome().trim();
        String codigo = codigoNovo(r.codigo(), nome, 20, faixas::existsById, "uma faixa");
        BigDecimal minimo = r.percentualMinimo().setScale(2);
        exigirMinimoLivre(minimo, null);
        FaixaRef criada = FaixaRef.de(faixas.save(new FaixaOcupacao(codigo, nome, minimo)));
        auditoria.registrar(admin.id(), "faixa_ocupacao", codigo, "CRIAR", null, criada);
        return criada;
    }

    @Transactional
    public FaixaRef alterarFaixa(String codigo, FaixaRequest r, UsuarioLogado admin) {
        FaixaOcupacao faixa = faixas.findById(codigo)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Faixa de ocupação", codigo));
        exigirMesmoCodigo(codigo, r.codigo());
        BigDecimal minimo = r.percentualMinimo().setScale(2);
        if (faixa.getPercentualMinimo().signum() == 0 && minimo.signum() != 0) {
            throw new RegraNegocioException("A primeira faixa começa em 0% (toda taxa precisa de uma faixa). "
                    + "Nela, só o nome pode mudar.");
        }
        exigirMinimoLivre(minimo, codigo);
        FaixaRef antes = FaixaRef.de(faixa);
        faixa.alterar(r.nome().trim(), minimo);
        FaixaRef depois = FaixaRef.de(faixa);
        auditoria.registrar(admin.id(), "faixa_ocupacao", codigo, "ATUALIZAR", antes, depois);
        return depois;
    }

    @Transactional
    public void excluirFaixa(String codigo, UsuarioLogado admin) {
        FaixaOcupacao faixa = faixas.findById(codigo)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Faixa de ocupação", codigo));
        if (faixa.getPercentualMinimo().signum() == 0) {
            throw new RegraNegocioException("A faixa que começa em 0% não pode ser excluída: toda taxa precisa de uma faixa.");
        }
        FaixaRef antes = FaixaRef.de(faixa);
        try {
            faixas.delete(faixa);
            faixas.flush();
        }
        catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("A faixa " + faixa.getNome() + " já foi usada em resultados publicados "
                    + "e não pode ser excluída.");
        }
        auditoria.registrar(admin.id(), "faixa_ocupacao", codigo, "EXCLUIR", antes, null);
    }

    // ------------------------------------------------------------------ apoio

    /** Código informado (conferido) ou gerado a partir do nome. */
    private static String codigoNovo(String informado, String nome, int tamanho, Predicate<String> existe,
            String rotulo) {
        if (Codigos.vazio(informado)) {
            return Codigos.livre(nome, tamanho, existe);
        }
        String codigo = Codigos.informado(informado, tamanho);
        if (existe.test(codigo)) {
            throw new RegraNegocioException("Já existe " + rotulo + " com o código " + codigo + ".");
        }
        return codigo;
    }

    private static void exigirMesmoCodigo(String codigo, String informado) {
        if (!Codigos.vazio(informado) && !informado.trim().equalsIgnoreCase(codigo)) {
            throw new RegraNegocioException("O código não pode ser alterado, porque é a referência usada no resto do "
                    + "sistema. Cadastre um novo e exclua o antigo, se ele ainda não foi usado.");
        }
    }

    private void exigirMinimoLivre(BigDecimal minimo, String codigoAtual) {
        for (FaixaOcupacao f : faixas.findAll()) {
            if (f.getPercentualMinimo().compareTo(minimo) == 0 && !f.getCodigo().equals(codigoAtual)) {
                throw new RegraNegocioException("A faixa " + f.getNome() + " já começa em "
                        + minimo.stripTrailingZeros().toPlainString().replace('.', ',') + "%.");
            }
        }
    }

    private static int proxima(Stream<Short> ordens) {
        int maior = ordens.mapToInt(Short::intValue).max().orElse(0);
        return Math.min(maior + 1, Short.MAX_VALUE);
    }
}
