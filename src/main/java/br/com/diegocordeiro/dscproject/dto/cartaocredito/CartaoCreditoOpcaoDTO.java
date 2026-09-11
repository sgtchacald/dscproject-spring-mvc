package br.com.diegocordeiro.dscproject.dto.cartaocredito;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartaoCreditoOpcaoDTO {

    private Long id;
    private String descricao;
    private String bandeira;
    private String finalCartao;
    private Integer diaFechamento;
    private Integer diaVencimento;
}
