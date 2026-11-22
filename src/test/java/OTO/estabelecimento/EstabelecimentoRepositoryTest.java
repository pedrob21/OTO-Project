package OTO.estabelecimento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import OTO.BaseRepositorioTest;

class EstabelecimentoRepositoryTest extends BaseRepositorioTest {

    @Autowired
    private EstabelecimentoRepository repository;

    @Test
    void listaPorNomeFantasiaEmOrdemAlfabetica() {
        estabelecimento("Zeta", "HOTEL", "11111111000101");
        estabelecimento("Alfa", "POUSADA", "11111111000102");
        estabelecimento("Beta", "RESORT", "11111111000103");
        gravarELimpar();

        assertThat(repository.findAllByOrderByNomeFantasia()).extracting(Estabelecimento::getNomeFantasia)
                .containsExactly("Alfa", "Beta", "Zeta");
    }

    @Test
    void buscaPorIdsSoTrazOsPedidosEmOrdemAlfabetica() {
        Estabelecimento zeta = estabelecimento("Zeta", "HOTEL", "11111111000101");
        Estabelecimento alfa = estabelecimento("Alfa", "POUSADA", "11111111000102");
        estabelecimento("Beta", "RESORT", "11111111000103");
        gravarELimpar();

        assertThat(repository.findByIdInOrderByNomeFantasia(List.of(zeta.getId(), alfa.getId())))
                .extracting(Estabelecimento::getNomeFantasia).containsExactly("Alfa", "Zeta");
    }

    @Test
    void existePorCnpjSoParaOCnpjGravado() {
        estabelecimento("Alfa", "HOTEL", "11111111000101");
        gravarELimpar();

        assertThat(repository.existsByCnpj("11111111000101")).isTrue();
        assertThat(repository.existsByCnpj("11111111000102")).isFalse();
    }

    @Test
    void existePorCnpjIgnoraOProprioRegistroMasVeOsOutros() {
        Estabelecimento alfa = estabelecimento("Alfa", "HOTEL", "11111111000101");
        Estabelecimento beta = estabelecimento("Beta", "HOTEL", "11111111000102");
        gravarELimpar();

        assertThat(repository.existsByCnpjAndIdNot("11111111000101", alfa.getId())).isFalse();
        assertThat(repository.existsByCnpjAndIdNot("11111111000101", beta.getId())).isTrue();
    }

    @Test
    void cnpjRepetidoViolaAUnicidade() {
        estabelecimento("Alfa", "HOTEL", "11111111000101");

        assertThatThrownBy(() -> estabelecimento("Beta", "HOTEL", "11111111000101"))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("uk_estabelecimento_cnpj");
    }

    @Test
    void estabelecimentosSemCnpjPodemSeRepetir() {
        estabelecimento("Alfa", "HOTEL", null);
        estabelecimento("Beta", "HOTEL", null);
        gravarELimpar();

        assertThat(repository.findAllByOrderByNomeFantasia()).hasSize(2);
    }

    @Test
    void cnpjPrecisaTerExatamenteQuatorzeDigitos() {
        assertThatThrownBy(() -> estabelecimento("Alfa", "HOTEL", "1111111100010"))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_estabelecimento_cnpj");
    }

    @Test
    void cnpjComLetraEhRejeitado() {
        assertThatThrownBy(() -> estabelecimento("Alfa", "HOTEL", "1111111100010A"))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("ck_estabelecimento_cnpj");
    }

    @Test
    void tipoDeHospedagemInexistenteViolaAChaveEstrangeira() {
        assertThatThrownBy(() -> estabelecimento("Alfa", "CASTELO", "11111111000101"))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("estabelecimento_tipo_hospedagem_codigo_fkey");
    }

    @Test
    void gravaEReleDadosCadastraisESecretariaSemPerda() {
        Estabelecimento gravado = estabelecimento("Alfa", "POUSADA", "11111111000101", Situacao.IRREGULAR);
        gravarELimpar();

        Estabelecimento lido = repository.findById(gravado.getId()).orElseThrow();

        assertThat(lido.getNomeFantasia()).isEqualTo("Alfa");
        assertThat(lido.getCnpj()).isEqualTo("11111111000101");
        assertThat(lido.getTipoHospedagemCodigo()).isEqualTo("POUSADA");
        assertThat(lido.getSituacao()).isEqualTo(Situacao.IRREGULAR);
        assertThat(lido.isAtivo()).isTrue();
        assertThat(lido.isAceitaPesquisas()).isTrue();
    }
}
