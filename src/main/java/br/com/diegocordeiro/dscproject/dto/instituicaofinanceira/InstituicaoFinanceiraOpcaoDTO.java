package br.com.diegocordeiro.dscproject.dto.instituicaofinanceira;

import br.com.diegocordeiro.dscproject.enums.TipoInstituicaoFinanceira;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstituicaoFinanceiraOpcaoDTO {

    private Long id;
    private String nome;
    private String codigo;
    private TipoInstituicaoFinanceira tipo;

    public String getTipoCodigo() {
        return tipo != null ? tipo.getCodigo() : null;
    }

    public String getTipoDescricao() {
        return tipo != null ? tipo.getDescricao() : null;
    }
}
