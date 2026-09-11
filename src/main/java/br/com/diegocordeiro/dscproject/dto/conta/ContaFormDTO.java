package br.com.diegocordeiro.dscproject.dto.conta;

import br.com.diegocordeiro.dscproject.enums.TipoConta;
import jakarta.validation.constraints.NotBlank;
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
public class ContaFormDTO {

    private Long id;

    @NotBlank(message = "{conta.validacao.descricao.obrigatoria}")
    @Size(max = 100, message = "{conta.validacao.descricao.tamanho}")
    private String descricao;

    @NotNull(message = "{conta.validacao.tipo.obrigatorio}")
    private TipoConta tipo;

    private Long instituicaoId;

    @Size(max = 30, message = "{conta.validacao.agencia.tamanho}")
    private String agencia;

    @Size(max = 30, message = "{conta.validacao.numero.tamanho}")
    private String numero;

    @NotBlank(message = "{conta.validacao.moeda.obrigatoria}")
    @Size(max = 3, message = "{conta.validacao.moeda.tamanho}")
    private String moeda = "BRL";

    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Size(max = 100, message = "{conta.validacao.nomeGerente.tamanho}")
    private String nomeGerente;

    @Size(max = 20, message = "{conta.validacao.telGerente.tamanho}")
    private String telGerente;

    private boolean consideraSaldo = true;

    private boolean ativo = true;
}
