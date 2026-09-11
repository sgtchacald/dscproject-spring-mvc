package br.com.diegocordeiro.dscproject.dto.categoria;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaOpcaoDTO {

    private Long id;
    private String codigo;
    private String nome;
    private String cor;
    private String icone;
}
