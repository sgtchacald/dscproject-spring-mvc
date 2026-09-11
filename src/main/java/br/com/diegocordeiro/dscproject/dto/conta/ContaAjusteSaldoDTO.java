package br.com.diegocordeiro.dscproject.dto.conta;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContaAjusteSaldoDTO {

    @NotNull(message = "{conta.validacao.novoSaldo.obrigatorio}")
    private BigDecimal novoSaldo;

    @Size(max = 255, message = "{conta.validacao.observacao.tamanho}")
    private String observacao;
}
