package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.despesa.DespesaEdicaoDTO;
import br.com.diegocordeiro.dscproject.dto.despesa.DespesaFormDTO;
import br.com.diegocordeiro.dscproject.dto.despesa.DespesaGridDTO;
import br.com.diegocordeiro.dscproject.dto.despesa.DespesaRateioDTO;
import br.com.diegocordeiro.dscproject.dto.despesa.UsuarioRateioDTO;
import br.com.diegocordeiro.dscproject.enums.MeioPagamento;
import br.com.diegocordeiro.dscproject.enums.OrigemLancamento;
import br.com.diegocordeiro.dscproject.enums.StatusPagamento;
import br.com.diegocordeiro.dscproject.model.CartaoCredito;
import br.com.diegocordeiro.dscproject.model.Categoria;
import br.com.diegocordeiro.dscproject.model.Conta;
import br.com.diegocordeiro.dscproject.model.Despesa;
import br.com.diegocordeiro.dscproject.model.DespesaUsuario;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.CartaoCreditoRepository;
import br.com.diegocordeiro.dscproject.repository.CategoriaRepository;
import br.com.diegocordeiro.dscproject.repository.ContaRepository;
import br.com.diegocordeiro.dscproject.repository.DespesaRepository;
import br.com.diegocordeiro.dscproject.repository.DespesaUsuarioRepository;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import br.com.diegocordeiro.dscproject.service.exceptions.RegistroNaoEncontradoException;
import br.com.diegocordeiro.dscproject.service.exceptions.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final DespesaUsuarioRepository despesaUsuarioRepository;
    private final ContaRepository contaRepository;
    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public DespesaService(
            DespesaRepository despesaRepository,
            DespesaUsuarioRepository despesaUsuarioRepository,
            ContaRepository contaRepository,
            CartaoCreditoRepository cartaoCreditoRepository,
            CategoriaRepository categoriaRepository,
            UsuarioRepository usuarioRepository) {
        this.despesaRepository = despesaRepository;
        this.despesaUsuarioRepository = despesaUsuarioRepository;
        this.contaRepository = contaRepository;
        this.cartaoCreditoRepository = cartaoCreditoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<DespesaGridDTO> listarParaGrid(Long usuarioId) {
        return despesaRepository.listarPorUsuario(usuarioId).stream()
                .map(DespesaGridDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public DespesaEdicaoDTO buscarParaEdicao(Long id, Long usuarioId) {
        Despesa d = despesaRepository.buscarPorIdEUsuario(id, usuarioId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("msg.despesa.nao-encontrada"));
        return new DespesaEdicaoDTO(d);
    }

    @Transactional(readOnly = true)
    public Despesa buscarPorIdEUsuario(Long id, Long usuarioId) {
        return despesaRepository.buscarPorIdEUsuario(id, usuarioId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("msg.despesa.nao-encontrada"));
    }

    @Transactional
    public Despesa inserir(DespesaFormDTO dto, Long usuarioId, String loginAutor, boolean podeRatear) {
        Conta conta = null;
        CartaoCredito cartao = null;
        MeioPagamento meioPagamento;
        StatusPagamento statusPagamento;
        LocalDate dataPagamento = null;

        String forma = dto.getFormaPagamento() != null ? dto.getFormaPagamento().toUpperCase() : "";
        if ("CARTAO".equals(forma)) {
            cartao = cartaoCreditoRepository.findByIdAndUsuarioIdAndDataExclusaoIsNull(dto.getCartaoId(), usuarioId)
                    .orElse(null);
            meioPagamento = MeioPagamento.CREDITO;
            statusPagamento = StatusPagamento.NAO_SE_APLICA;
        } else if ("DINHEIRO".equals(forma)) {
            conta = contaRepository.findByIdAndUsuarioIdAndDataExclusaoIsNull(dto.getContaId(), usuarioId)
                    .orElse(null);
            meioPagamento = MeioPagamento.DINHEIRO;
            statusPagamento = dto.getStatusPagamento() != null ? dto.getStatusPagamento() : StatusPagamento.NAO;
            if (statusPagamento == StatusPagamento.SIM) {
                dataPagamento = dto.getDataPagamento();
            }
        } else {
            conta = contaRepository.findByIdAndUsuarioIdAndDataExclusaoIsNull(dto.getContaId(), usuarioId)
                    .orElse(null);
            meioPagamento = dto.getMeioPagamento() != null ? dto.getMeioPagamento() : MeioPagamento.DEBITO;
            statusPagamento = dto.getStatusPagamento() != null ? dto.getStatusPagamento() : StatusPagamento.NAO;
            if (statusPagamento == StatusPagamento.SIM) {
                dataPagamento = dto.getDataPagamento();
            }
        }

        Categoria categoria = null;
        if (dto.getCategoriaId() != null) {
            categoria = categoriaRepository.findByIdAndDataExclusaoIsNull(dto.getCategoriaId()).orElse(null);
        }

        YearMonth competencia = YearMonth.parse(dto.getCompetencia());

        if (dto.isParcelada()) {
            int n = dto.getQtdParcelas() != null ? dto.getQtdParcelas() : 2;
            BigDecimal total = dto.getValorTotalCompra() != null ? dto.getValorTotalCompra() : dto.getValor();
            BigDecimal parcelaBase = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);

            Despesa mae = new Despesa();
            mae.setNome(dto.getNome());
            mae.setDescricao(dto.getDescricao());
            mae.setDataLancamento(dto.getDataLancamento());
            mae.setDataVencimento(dto.getDataVencimento());
            mae.setCompetencia(competencia);
            mae.setValor(parcelaBase);
            mae.setValorTotalCompra(total);
            mae.setParcelada(true);
            mae.setNroParcela(1);
            mae.setQtdParcelas(n);
            mae.setParcelaPai(null);
            mae.setConta(conta);
            mae.setCartao(cartao);
            mae.setMeioPagamento(meioPagamento);
            mae.setStatusPagamento(statusPagamento);
            mae.setDataPagamento(dataPagamento);
            mae.setCategoria(categoria);
            mae.setOrigem(OrigemLancamento.MANUAL);
            mae.setCriadoPor(loginAutor);
            mae.setAlteradoPor(loginAutor);

            mae = despesaRepository.save(mae);
            salvarRateios(mae, dto.getRateio(), 1, n, podeRatear, loginAutor);

            for (int i = 2; i <= n; i++) {
                Despesa filha = new Despesa();
                filha.setNome(dto.getNome());
                filha.setDescricao(dto.getDescricao());
                filha.setDataLancamento(dto.getDataLancamento());
                if (dto.getDataVencimento() != null) {
                    filha.setDataVencimento(dto.getDataVencimento().plusMonths(i - 1));
                }
                filha.setCompetencia(competencia.plusMonths(i - 1));
                if (i < n) {
                    filha.setValor(parcelaBase);
                } else {
                    BigDecimal residuo = total.subtract(parcelaBase.multiply(BigDecimal.valueOf(n - 1)));
                    filha.setValor(residuo);
                }
                filha.setValorTotalCompra(total);
                filha.setParcelada(true);
                filha.setNroParcela(i);
                filha.setQtdParcelas(n);
                filha.setParcelaPai(mae);
                filha.setConta(conta);
                filha.setCartao(cartao);
                filha.setMeioPagamento(meioPagamento);
                filha.setStatusPagamento(statusPagamento);
                filha.setDataPagamento(dataPagamento);
                filha.setCategoria(categoria);
                filha.setOrigem(OrigemLancamento.MANUAL);
                filha.setCriadoPor(loginAutor);
                filha.setAlteradoPor(loginAutor);

                filha = despesaRepository.save(filha);
                salvarRateios(filha, dto.getRateio(), i, n, podeRatear, loginAutor);
            }

            return mae;
        } else {
            Despesa d = new Despesa();
            d.setNome(dto.getNome());
            d.setDescricao(dto.getDescricao());
            d.setDataLancamento(dto.getDataLancamento());
            d.setDataVencimento(dto.getDataVencimento());
            d.setCompetencia(competencia);
            d.setValor(dto.getValor());
            d.setValorTotalCompra(null);
            d.setParcelada(false);
            d.setNroParcela(null);
            d.setQtdParcelas(null);
            d.setParcelaPai(null);
            d.setConta(conta);
            d.setCartao(cartao);
            d.setMeioPagamento(meioPagamento);
            d.setStatusPagamento(statusPagamento);
            d.setDataPagamento(dataPagamento);
            d.setCategoria(categoria);
            d.setOrigem(OrigemLancamento.MANUAL);
            d.setCriadoPor(loginAutor);
            d.setAlteradoPor(loginAutor);

            d = despesaRepository.save(d);
            salvarRateios(d, dto.getRateio(), 1, 1, podeRatear, loginAutor);
            return d;
        }
    }

    private void salvarRateios(
            Despesa d,
            List<DespesaRateioDTO> rateiosDTO,
            int parcelaAtual,
            int totalParcelas,
            boolean podeRatear,
            String loginAutor) {
        if (!podeRatear || rateiosDTO == null || rateiosDTO.isEmpty()) {
            return;
        }

        for (DespesaRateioDTO item : rateiosDTO) {
            if (item.getUsuarioId() == null || item.getValor() == null) {
                continue;
            }

            Usuario usuario = usuarioRepository.findById(item.getUsuarioId()).orElse(null);
            if (usuario == null) {
                continue;
            }

            BigDecimal valorFatia;
            if (totalParcelas > 1) {
                BigDecimal fatiaBase = item.getValor().divide(BigDecimal.valueOf(totalParcelas), 2, RoundingMode.DOWN);
                if (parcelaAtual < totalParcelas) {
                    valorFatia = fatiaBase;
                } else {
                    valorFatia = item.getValor().subtract(fatiaBase.multiply(BigDecimal.valueOf(totalParcelas - 1)));
                }
            } else {
                valorFatia = item.getValor();
            }

            DespesaUsuario du = new DespesaUsuario();
            du.setDespesa(d);
            du.setUsuario(usuario);
            du.setValor(valorFatia);
            du.setStatusPagamento(item.getStatusPagamento() != null ? item.getStatusPagamento() : StatusPagamento.NAO);
            du.setDataAcerto(item.getDataAcerto());
            du.setCriadoPor(loginAutor);
            du.setAlteradoPor(loginAutor);

            despesaUsuarioRepository.save(du);
            d.getRateios().add(du);
        }
    }

    @Transactional
    public Despesa editar(Long id, DespesaFormDTO dto, Long usuarioId, String loginAutor, boolean podeRatear) {
        Despesa d = despesaRepository.buscarPorIdEUsuario(id, usuarioId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("msg.despesa.nao-encontrada"));

        d.setNome(dto.getNome());
        d.setDescricao(dto.getDescricao());
        d.setCompetencia(YearMonth.parse(dto.getCompetencia()));
        d.setValor(dto.getValor());
        d.setDataLancamento(dto.getDataLancamento());
        d.setDataVencimento(dto.getDataVencimento());

        if (dto.getCategoriaId() != null) {
            d.setCategoria(categoriaRepository.findByIdAndDataExclusaoIsNull(dto.getCategoriaId()).orElse(null));
        } else {
            d.setCategoria(null);
        }

        if (d.getOrigem() == OrigemLancamento.MANUAL) {
            String forma = dto.getFormaPagamento() != null ? dto.getFormaPagamento().toUpperCase() : "";
            if ("CARTAO".equals(forma)) {
                CartaoCredito cc = cartaoCreditoRepository.findByIdAndUsuarioIdAndDataExclusaoIsNull(dto.getCartaoId(), usuarioId).orElse(null);
                d.setCartao(cc);
                d.setConta(null);
                d.setMeioPagamento(MeioPagamento.CREDITO);
                d.setStatusPagamento(StatusPagamento.NAO_SE_APLICA);
                d.setDataPagamento(null);
            } else if ("DINHEIRO".equals(forma)) {
                Conta c = contaRepository.findByIdAndUsuarioIdAndDataExclusaoIsNull(dto.getContaId(), usuarioId).orElse(null);
                d.setConta(c);
                d.setCartao(null);
                d.setMeioPagamento(MeioPagamento.DINHEIRO);
                atualizarStatusPagamento(d, dto);
            } else {
                Conta c = contaRepository.findByIdAndUsuarioIdAndDataExclusaoIsNull(dto.getContaId(), usuarioId).orElse(null);
                d.setConta(c);
                d.setCartao(null);
                d.setMeioPagamento(dto.getMeioPagamento() != null ? dto.getMeioPagamento() : MeioPagamento.DEBITO);
                atualizarStatusPagamento(d, dto);
            }
        } else {
            if (d.getCartao() == null) {
                atualizarStatusPagamento(d, dto);
            }
        }

        d.setAlteradoPor(loginAutor);

        if (podeRatear && dto.getRateio() != null) {
            sincronizarRateio(d, dto.getRateio(), loginAutor);
        }

        return despesaRepository.save(d);
    }

    private void atualizarStatusPagamento(Despesa d, DespesaFormDTO dto) {
        if (dto.getStatusPagamento() == StatusPagamento.SIM) {
            d.setStatusPagamento(StatusPagamento.SIM);
            d.setDataPagamento(dto.getDataPagamento());
        } else {
            d.setStatusPagamento(StatusPagamento.NAO);
            d.setDataPagamento(null);
        }
    }

    private void sincronizarRateio(Despesa d, List<DespesaRateioDTO> novosRateios, String loginAutor) {
        List<DespesaUsuario> existentes = new ArrayList<>(d.getRateios());
        for (DespesaUsuario du : existentes) {
            boolean manter = novosRateios.stream().anyMatch(nr -> nr.getUsuarioId().equals(du.getUsuario().getId()));
            if (!manter) {
                du.setDataExclusao(Instant.now());
                du.setExcluidoPor(loginAutor);
                despesaUsuarioRepository.save(du);
            }
        }

        for (DespesaRateioDTO item : novosRateios) {
            DespesaUsuario du = existentes.stream()
                    .filter(e -> e.getDataExclusao() == null && e.getUsuario().getId().equals(item.getUsuarioId()))
                    .findFirst()
                    .orElse(null);

            if (du != null) {
                du.setValor(item.getValor());
                if (item.getStatusPagamento() != null) {
                    du.setStatusPagamento(item.getStatusPagamento());
                }
                du.setDataAcerto(item.getDataAcerto());
                du.setAlteradoPor(loginAutor);
                despesaUsuarioRepository.save(du);
            } else {
                Usuario u = usuarioRepository.findById(item.getUsuarioId()).orElse(null);
                if (u != null) {
                    DespesaUsuario novo = new DespesaUsuario();
                    novo.setDespesa(d);
                    novo.setUsuario(u);
                    novo.setValor(item.getValor());
                    novo.setStatusPagamento(item.getStatusPagamento() != null ? item.getStatusPagamento() : StatusPagamento.NAO);
                    novo.setDataAcerto(item.getDataAcerto());
                    novo.setCriadoPor(loginAutor);
                    novo.setAlteradoPor(loginAutor);
                    despesaUsuarioRepository.save(novo);
                    d.getRateios().add(novo);
                }
            }
        }
    }

    @Transactional
    public void excluir(Long id, Long usuarioId, String loginAutor) {
        Despesa d = despesaRepository.buscarPorIdEUsuario(id, usuarioId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("msg.despesa.nao-encontrada"));

        if (d.getOrigem() != OrigemLancamento.MANUAL) {
            throw new RegraNegocioException("msg.despesa.importada.nao-excluivel");
        }

        Instant agora = Instant.now();

        if (d.isParcelada() && d.getParcelaPai() == null) {
            List<Despesa> serie = despesaRepository.buscarParcelasDaSerie(d.getId());
            for (Despesa p : serie) {
                p.setDataExclusao(agora);
                p.setExcluidoPor(loginAutor);
                for (DespesaUsuario du : p.getRateios()) {
                    du.setDataExclusao(agora);
                    du.setExcluidoPor(loginAutor);
                    despesaUsuarioRepository.save(du);
                }
                despesaRepository.save(p);
            }
        } else {
            d.setDataExclusao(agora);
            d.setExcluidoPor(loginAutor);
            for (DespesaUsuario du : d.getRateios()) {
                du.setDataExclusao(agora);
                du.setExcluidoPor(loginAutor);
                despesaUsuarioRepository.save(du);
            }
            despesaRepository.save(d);
        }
    }

    @Transactional
    public void registrarPagamento(Long id, LocalDate dataPagamento, Long usuarioId, String loginAutor) {
        Despesa d = despesaRepository.buscarPorIdEUsuario(id, usuarioId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("msg.despesa.nao-encontrada"));

        if (d.getStatusPagamento() == StatusPagamento.NAO_SE_APLICA) {
            throw new RegraNegocioException("msg.despesa.pagamento.cartao-invalido");
        }

        d.setStatusPagamento(StatusPagamento.SIM);
        d.setDataPagamento(dataPagamento);
        d.setAlteradoPor(loginAutor);
        despesaRepository.save(d);
    }

    @Transactional
    public int registrarPagamentoLote(List<Long> ids, LocalDate dataPagamento, Long usuarioId, String loginAutor) {
        long totalEncontrado = despesaRepository.countPorIdsEUsuario(ids, usuarioId);
        if (totalEncontrado != ids.size()) {
            throw new RegistroNaoEncontradoException("msg.despesa.nao-encontrada");
        }

        List<Despesa> lista = despesaRepository.buscarPorIdsEUsuario(ids, usuarioId);
        int alteradas = 0;
        for (Despesa d : lista) {
            if (d.getStatusPagamento() != StatusPagamento.NAO_SE_APLICA) {
                d.setStatusPagamento(StatusPagamento.SIM);
                d.setDataPagamento(dataPagamento);
                d.setAlteradoPor(loginAutor);
                despesaRepository.save(d);
                alteradas++;
            }
        }
        return alteradas;
    }

    @Transactional
    public void registrarAcertoRateio(
            Long despId,
            Long coParticipanteId,
            boolean acertado,
            LocalDate dataAcerto,
            Long usuarioId,
            String loginAutor) {
        despesaRepository.buscarPorIdEUsuario(despId, usuarioId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("msg.despesa.nao-encontrada"));

        DespesaUsuario du = despesaUsuarioRepository.findByDespesaIdAndUsuarioIdAndDataExclusaoIsNull(despId, coParticipanteId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("msg.despesa.rateio.usuario-invalido"));

        du.setStatusPagamento(acertado ? StatusPagamento.SIM : StatusPagamento.NAO);
        du.setDataAcerto(acertado ? dataAcerto : null);
        du.setAlteradoPor(loginAutor);
        despesaUsuarioRepository.save(du);
    }

    @Transactional(readOnly = true)
    public List<UsuarioRateioDTO> buscarUsuariosParaRateio(String termo, Long usuarioIdLogado) {
        if (termo == null || termo.trim().length() < 3) {
            return List.of();
        }
        return usuarioRepository.buscarAtivosParaRateio(termo.trim(), usuarioIdLogado).stream()
                .limit(20)
                .map(UsuarioRateioDTO::new)
                .toList();
    }
}
