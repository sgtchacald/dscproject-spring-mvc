package br.com.diegocordeiro.dscproject.dto.conta;

import br.com.diegocordeiro.dscproject.enums.TipoConta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContaGridDTO {

    private Long id;
    private String descricao;
    private TipoConta tipo;
    private Long instituicaoId;
    private String instituicaoNome;
    private String agencia;
    private String numero;
    private String moeda;
    private BigDecimal saldo;
    private Instant saldoSincronizadoEm;
    private boolean consideraSaldo;
    private long qtdUso;
    private boolean ativo;
    private boolean excluido;

    public String getTipoCodigo() {
        return tipo != null ? tipo.getCodigo() : null;
    }

    public String getTipoDescricao() {
        return tipo != null ? tipo.getDescricao() : null;
    }

    public String getAgenciaNumero() {
        boolean temAgencia = agencia != null && !agencia.isBlank();
        boolean temNumero = numero != null && !numero.isBlank();
        if (temAgencia && temNumero) {
            return "Ag. " + agencia + " / C/C " + numero;
        } else if (temAgencia) {
            return "Ag. " + agencia;
        } else if (temNumero) {
            return "C/C " + numero;
        }
        return "-";
    }
}
