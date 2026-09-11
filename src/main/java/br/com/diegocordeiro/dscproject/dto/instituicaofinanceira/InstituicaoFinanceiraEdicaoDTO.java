package br.com.diegocordeiro.dscproject.dto.instituicaofinanceira;

import br.com.diegocordeiro.dscproject.enums.TipoInstituicaoFinanceira;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstituicaoFinanceiraEdicaoDTO {

    private Long id;
    private String nome;
    private String codigo;
    private TipoInstituicaoFinanceira tipo;
    private boolean ativo;
    private boolean sistema;
    private long qtdUso;

    public String getTipoCodigo() {
        return tipo != null ? tipo.getCodigo() : null;
    }
}
