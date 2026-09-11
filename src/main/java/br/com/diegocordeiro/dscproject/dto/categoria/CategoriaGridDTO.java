package br.com.diegocordeiro.dscproject.dto.categoria;

import br.com.diegocordeiro.dscproject.enums.AplicaA;
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
public class CategoriaGridDTO {

    private Long id;
    private String codigo;
    private String nome;
    private AplicaA aplicaA;
    private String cor;
    private String icone;
    private boolean sistema;
    private boolean ativo;
    private boolean excluido;
    private long qtdUso;
}
