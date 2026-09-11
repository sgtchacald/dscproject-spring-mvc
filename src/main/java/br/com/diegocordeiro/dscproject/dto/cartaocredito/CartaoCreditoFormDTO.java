package br.com.diegocordeiro.dscproject.dto.cartaocredito;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class CartaoCreditoFormDTO {

    private Long id;

    @NotBlank(message = "{cartao.validacao.descricao.obrigatoria}")
    @Size(max = 100, message = "{cartao.validacao.descricao.tamanho}")
    private String descricao;

    private String bandeira;

    @Pattern(regexp = "\\d{4}", message = "{cartao.validacao.finalCartao.invalido}")
    private String finalCartao;

    private BigDecimal limite;

    @Min(value = 1, message = "{cartao.validacao.dia.invalido}")
    @Max(value = 31, message = "{cartao.validacao.dia.invalido}")
    private Integer diaFechamento;

    @Min(value = 1, message = "{cartao.validacao.dia.invalido}")
    @Max(value = 31, message = "{cartao.validacao.dia.invalido}")
    private Integer diaVencimento;

    private Long contaId;

    private boolean ativo = true;
}
