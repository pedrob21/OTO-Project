package OTO.estabelecimento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Dados do inventário que o próprio parceiro pode manter. */
public record DadosCadastrais(
        @NotBlank @Size(max = 200) String nomeFantasia,
        @Size(max = 200) String razaoSocial,
        @Pattern(regexp = "\\d{14}", message = "deve ter 14 dígitos, sem pontuação") String cnpj,
        LocalDate dataAbertura,
        @Size(max = 300) String proprietarios,
        @Size(max = 20) String cnae,
        @Size(max = 300) String endereco,
        @Size(max = 120) String bairro,
        @Size(max = 120) String regiao,
        @Size(max = 300) String localizacao,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @Size(max = 30) String telefoneComercial,
        @Email @Size(max = 254) String emailComercial,
        @Size(max = 300) String site,
        @Size(max = 300) String facebook,
        @Size(max = 300) String instagram,
        CadasturExigencia cadasturExigencia,
        boolean possuiCadastur,
        @Size(max = 30) String cadasturNumero,
        LocalDate cadasturValidade,
        @PositiveOrZero Integer funcionariosFixos,
        @PositiveOrZero Integer funcionariosTemporarios,
        Boolean funcionamento24h,
        LocalTime horarioCheckIn,
        LocalTime horarioCheckOut,
        @Size(max = 300) String formasReserva,
        @Size(max = 300) String formasPagamento) {
}
