package OTO.estabelecimento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;
import OTO.usuario.Usuario;

class CapacidadeHospedagemRepositoryTest extends BaseRepositorioTest {

    private static final LocalDate JAN_2025 = LocalDate.of(2025, 1, 1);
    private static final LocalDate MAR_2026 = LocalDate.of(2026, 3, 1);

    @Autowired
    private CapacidadeHospedagemRepository repository;

    private Usuario autor;
    private Estabelecimento hotel;
    private Estabelecimento pousada;
    private Estabelecimento resort;

    private void cenario() {
        autor = usuario("autor@oto.com");
        hotel = estabelecimento("Hotel", "HOTEL", "11111111000101");
        pousada = estabelecimento("Pousada", "POUSADA", "11111111000102");
        resort = estabelecimento("Resort", "RESORT", "11111111000103");
        capacidade(hotel, JAN_2025, 10);
        capacidade(hotel, MAR_2026, 20);
        capacidade(pousada, LocalDate.of(2026, 6, 1), 5);
        capacidade(resort, LocalDate.of(2026, 1, 1), 100);
        gravarELimpar();
    }

    private CapacidadeHospedagem capacidade(Estabelecimento e, LocalDate desde, int uh) {
        return gravar(new CapacidadeHospedagem(e, desde, uh, 0, uh * 3, 4, autor.getId()));
    }

    @Test
    void vigenteNoDiaExatoDaMudancaJaUsaAnovaCapacidade() {
        cenario();

        assertThat(repository.findVigentesEm(MAR_2026))
                .extracting(c -> c.getEstabelecimento().getNomeFantasia(), CapacidadeHospedagem::getQtdUh)
                .containsExactlyInAnyOrder(tuple("Hotel", 20), tuple("Resort", 100));
    }

    @Test
    void vigenteNoDiaAnteriorAMudancaAindaUsaACapacidadeAntiga() {
        cenario();

        assertThat(repository.findVigentesEm(MAR_2026.minusDays(1)))
                .extracting(c -> c.getEstabelecimento().getNomeFantasia(), CapacidadeHospedagem::getQtdUh)
                .containsExactlyInAnyOrder(tuple("Hotel", 10), tuple("Resort", 100));
    }

    @Test
    void estabelecimentoSoApareceDepoisDeSuaPrimeiraVigencia() {
        cenario();

        assertThat(repository.findVigentesEm(LocalDate.of(2026, 5, 31)))
                .extracting(c -> c.getEstabelecimento().getNomeFantasia())
                .containsExactlyInAnyOrder("Hotel", "Resort");
        assertThat(repository.findVigentesEm(LocalDate.of(2026, 6, 1)))
                .extracting(c -> c.getEstabelecimento().getNomeFantasia())
                .containsExactlyInAnyOrder("Hotel", "Pousada", "Resort");
    }

    @Test
    void antesDeQualquerVigenciaNaoHaCapacidade() {
        cenario();

        assertThat(repository.findVigentesEm(JAN_2025.minusDays(1))).isEmpty();
    }

    @Test
    void vigentesTrazemUmaLinhaPorEstabelecimentoComOEstabelecimentoCarregado() {
        cenario();

        var vigentes = repository.findVigentesEm(LocalDate.of(2030, 1, 1));

        assertThat(vigentes).hasSize(3);
        assertThat(vigentes).allSatisfy(c -> assertThat(org.hibernate.Hibernate.isInitialized(c.getEstabelecimento()))
                .isTrue());
    }

    @Test
    void historicoDoEstabelecimentoVemDaMaisRecenteParaAMaisAntiga() {
        cenario();

        assertThat(repository.findByEstabelecimentoIdOrderByVigenteDesdeDesc(hotel.getId()))
                .extracting(CapacidadeHospedagem::getVigenteDesde).containsExactly(MAR_2026, JAN_2025);
    }

    @Test
    void vigenteAteUmaDataEhAMaisRecenteNaoPosteriorAEla() {
        cenario();

        assertThat(repository.findFirstByEstabelecimentoIdAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                hotel.getId(), MAR_2026).orElseThrow().getQtdUh()).isEqualTo(20);
        assertThat(repository.findFirstByEstabelecimentoIdAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                hotel.getId(), MAR_2026.minusDays(1)).orElseThrow().getQtdUh()).isEqualTo(10);
        assertThat(repository.findFirstByEstabelecimentoIdAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                hotel.getId(), JAN_2025.minusDays(1))).isEmpty();
    }

    @Test
    void existenciaPorEstabelecimentoEDataConsideraOsDois() {
        cenario();

        assertThat(repository.existsByEstabelecimentoIdAndVigenteDesde(hotel.getId(), MAR_2026)).isTrue();
        assertThat(repository.existsByEstabelecimentoIdAndVigenteDesde(hotel.getId(), MAR_2026.plusDays(1))).isFalse();
        assertThat(repository.existsByEstabelecimentoIdAndVigenteDesde(pousada.getId(), MAR_2026)).isFalse();
    }

    @Test
    void mesmaVigenciaParaOMesmoEstabelecimentoViolaAUnicidade() {
        cenario();

        assertThatThrownBy(() -> capacidade(hotel, MAR_2026, 30)).isInstanceOf(PersistenceException.class).hasMessageContaining("uk_capacidade_vigencia");
    }

    @Test
    void mesmaDataEmOutroEstabelecimentoEhPermitida() {
        cenario();

        capacidade(pousada, MAR_2026, 7);
        gravarELimpar();

        assertThat(repository.existsByEstabelecimentoIdAndVigenteDesde(pousada.getId(), MAR_2026)).isTrue();
    }

    @Test
    void uhsPcdNaoPodemPassarDoTotalDeUhs() {
        autor = usuario("autor@oto.com");
        hotel = estabelecimento("Hotel", "HOTEL", "11111111000101");

        assertThatThrownBy(() -> gravar(new CapacidadeHospedagem(hotel, JAN_2025, 10, 11, 30, 4, autor.getId())))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_capacidade_uh_pcd");
    }

    @Test
    void quantidadesZeradasSaoRejeitadas() {
        autor = usuario("autor@oto.com");
        hotel = estabelecimento("Hotel", "HOTEL", "11111111000101");

        assertThatThrownBy(() -> gravar(new CapacidadeHospedagem(hotel, JAN_2025, 0, 0, 30, 4, autor.getId())))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_capacidade_valores");
    }

    @Test
    void leitosUteisSaoCalculadosPeloBancoComAMetodologiaDoOto() {
        autor = usuario("autor@oto.com");
        hotel = estabelecimento("Hotel", "HOTEL", "11111111000101");
        CapacidadeHospedagem c = capacidade(hotel, JAN_2025, 10);
        gravarELimpar();

        Number leitosUteis = (Number) em.getEntityManager()
                .createNativeQuery("select leitos_uteis from capacidade_hospedagem where id = :id")
                .setParameter("id", c.getId()).getSingleResult();

        assertThat(leitosUteis.doubleValue()).isEqualTo(25.0);
    }
}
