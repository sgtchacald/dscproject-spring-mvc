package br.com.diegocordeiro.dscproject.dto.despesa;

import br.com.diegocordeiro.dscproject.enums.MeioPagamento;
import br.com.diegocordeiro.dscproject.enums.StatusPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DespesaFormDTO {

    private Long id;

    @NotBlank(message = "{msg.despesa.campo.obrigatorio}")
    @Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "{msg.despesa.competencia.invalida}")
    private String competencia;

    @NotBlank(message = "{msg.despesa.campo.obrigatorio}")
    @Size(max = 100, message = "{msg.despesa.nome.tamanho}")
    private String nome;

    @Size(max = 512, message = "{msg.despesa.descricao.tamanho}")
    private String descricao;

    @NotNull(message = "{msg.despesa.campo.obrigatorio}")
    @DecimalMin(value = "0.01", message = "{msg.despesa.valor.positivo}")
    private BigDecimal valor;

    private BigDecimal valorTotalCompra;

    @NotNull(message = "{msg.despesa.campo.obrigatorio}")
    private LocalDate dataLancamento;

    private LocalDate dataVencimento;

    private LocalDate dataPagamento;

    @NotBlank(message = "{msg.despesa.campo.obrigatorio}")
    private String formaPagamento;

    private Long contaId;

    private Long cartaoId;

    private MeioPagamento meioPagamento;

    private StatusPagamento statusPagamento;

    private Long categoriaId;

    private boolean parcelada = false;

    private Integer qtdParcelas;

    private List<DespesaRateioDTO> rateio = new ArrayList<>();
}
