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
public class InstituicaoProvedorEdicaoDTO {

    private Long id;
    private String idExterno;
    private Long provedorId;
    private Long instituicaoId;
}
