package br.com.diegocordeiro.dscproject.dto.conta;

import br.com.diegocordeiro.dscproject.enums.TipoConta;
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
public class ContaEdicaoDTO {

    private Long id;
    private String descricao;
    private TipoConta tipo;
    private Long instituicaoId;
    private String instituicaoNome;
    private String agencia;
    private String numero;
    private String moeda;
    private BigDecimal saldo;
    private String nomeGerente;
    private String telGerente;
    private boolean consideraSaldo;
    private boolean ativo;
    private long qtdUso;
}
