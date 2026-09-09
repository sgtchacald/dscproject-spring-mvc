package br.com.diegocordeiro.dscproject.dto.categoria;

import br.com.diegocordeiro.dscproject.enums.AplicaA;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaFiltroDTO {

    private String busca;
    private AplicaA aplicaA;
    private String tipo;
    private String situacao = "ATIVA";

    public Boolean getSistemaBoolean() {
        if ("SISTEMA".equalsIgnoreCase(tipo)) {
            return true;
        } else if ("COMUM".equalsIgnoreCase(tipo)) {
            return false;
        }
        return null;
    }

    public String getSituacaoNormalizada() {
        if (situacao == null || situacao.isBlank() || "TODAS".equalsIgnoreCase(situacao)) {
            return "TODAS";
        }
        return situacao.toUpperCase();
    }
}
