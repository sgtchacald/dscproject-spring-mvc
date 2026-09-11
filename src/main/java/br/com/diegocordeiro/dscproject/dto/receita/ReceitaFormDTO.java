package br.com.diegocordeiro.dscproject.dto.receita;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entrada do cadastro/edição de receita. {@code competencia} chega como texto
 * ({@code yyyy-MM}, validado por {@link Pattern}) e é convertida para
 * {@link java.time.YearMonth} no Service — evita um conversor de formulário só
 * para este campo. Não tem campo {@code origem}: quem decide a origem do
 * lançamento é sempre o Service, nunca o cliente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceitaFormDTO {

    private Long id;

    @NotBlank(message = "{receita.validacao.nome.obrigatorio}")
    @Size(max = 100, message = "{receita.validacao.nome.tamanho}")
    private String nome;

    @Size(max = 512, message = "{receita.validacao.descricao.tamanho}")
    private String descricao;

    @NotNull(message = "{receita.validacao.valor.obrigatorio}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{receita.validacao.valor.positivo}")
    private BigDecimal valor;

    @NotNull(message = "{receita.validacao.dataLancamento.obrigatoria}")
    private LocalDate dataLancamento;

    @NotBlank(message = "{receita.validacao.competencia.obrigatoria}")
    @Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "{receita.validacao.competencia.formato}")
    private String competencia;

    @NotNull(message = "{receita.validacao.contaId.obrigatoria}")
    private Long contaId;

    private Long categoriaId;

    private boolean recebido = false;

    private LocalDate dataRecebimento;
}
