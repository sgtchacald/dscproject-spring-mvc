package br.com.diegocordeiro.dscproject.dto.receita;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Entrada de {@code /receitas/marcar-recebida/{id}}: a data em que o dinheiro entrou. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceitaRecebimentoDTO {

    @NotNull(message = "{receita.validacao.dataRecebimento.obrigatoria}")
    private LocalDate dataRecebimento;
}
