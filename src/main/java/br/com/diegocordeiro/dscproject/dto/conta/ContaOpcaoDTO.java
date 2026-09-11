package br.com.diegocordeiro.dscproject.dto.conta;

import br.com.diegocordeiro.dscproject.enums.TipoConta;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContaOpcaoDTO {

    private Long id;
    private String descricao;
    private TipoConta tipo;
    private String moeda;
    private String instituicaoNome;
    private BigDecimal saldo;

    public String getTipoDescricao() {
        return tipo != null ? tipo.getDescricao() : null;
    }
}
