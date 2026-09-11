package br.com.diegocordeiro.dscproject.dto.categoriaprovedor;

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
public class CategoriaProvedorGridDTO {

    private Long id;
    private String rotuloExterno;
    private Long provedorId;
    private String provedorNome;
    private Long categoriaId;
    private String categoriaNome;
}
