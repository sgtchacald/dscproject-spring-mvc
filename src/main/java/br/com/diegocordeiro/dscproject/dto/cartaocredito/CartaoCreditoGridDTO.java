package br.com.diegocordeiro.dscproject.dto.cartaocredito;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartaoCreditoGridDTO {

    private Long id;
    private String descricao;
    private String bandeira;
    private String finalCartao;
    private BigDecimal limite;
    private Integer diaFechamento;
    private Integer diaVencimento;
    private Long contaId;
    private String contaDescricao;
    private long qtdVinculos;
    private boolean ativo;
    private boolean excluido;
}
