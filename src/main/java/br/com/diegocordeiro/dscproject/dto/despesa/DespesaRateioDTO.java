package br.com.diegocordeiro.dscproject.dto.despesa;

import br.com.diegocordeiro.dscproject.enums.StatusPagamento;
import br.com.diegocordeiro.dscproject.enums.TipoContato;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DespesaRateioDTO {

    private Long contatoId;
    private String contatoNome;
    private String contatoEmail;
    private TipoContato contatoTipo;
    private String contatoChavePix;
    private BigDecimal valor;
    private StatusPagamento statusPagamento;
    private LocalDate dataAcerto;

    public DespesaRateioDTO(Long contatoId, String contatoNome, BigDecimal valor, StatusPagamento statusPagamento, LocalDate dataAcerto) {
        this.contatoId = contatoId;
        this.contatoNome = contatoNome;
        this.valor = valor;
        this.statusPagamento = statusPagamento;
        this.dataAcerto = dataAcerto;
    }

    public Long getUsuarioId() {
        return contatoId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.contatoId = usuarioId;
    }

    public String getUsuarioNome() {
        return contatoNome;
    }

    public void setUsuarioNome(String usuarioNome) {
        this.contatoNome = usuarioNome;
    }
}
