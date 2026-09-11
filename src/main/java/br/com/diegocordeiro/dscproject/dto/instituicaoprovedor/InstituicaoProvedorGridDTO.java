package br.com.diegocordeiro.dscproject.dto.instituicaoprovedor;

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
public class InstituicaoProvedorGridDTO {

    private Long id;
    private String idExterno;
    private Long provedorId;
    private String provedorNome;
    private Long instituicaoId;
    private String instituicaoNome;
}
