package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.despesa.DespesaEdicaoDTO;
import br.com.diegocordeiro.dscproject.dto.despesa.DespesaFormDTO;
import br.com.diegocordeiro.dscproject.dto.despesa.DespesaGridDTO;
import br.com.diegocordeiro.dscproject.dto.despesa.DespesaPagamentoDTO;
import br.com.diegocordeiro.dscproject.dto.despesa.DespesaPagamentoLoteDTO;
import br.com.diegocordeiro.dscproject.dto.despesa.DespesaRateioAcertoDTO;
import br.com.diegocordeiro.dscproject.dto.despesa.UsuarioRateioDTO;
import br.com.diegocordeiro.dscproject.model.Despesa;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.permissao.PermissaoDespesaCatalogo;
import br.com.diegocordeiro.dscproject.repository.CartaoCreditoRepository;
import br.com.diegocordeiro.dscproject.repository.CategoriaRepository;
import br.com.diegocordeiro.dscproject.repository.ContaRepository;
import br.com.diegocordeiro.dscproject.repository.DespesaRepository;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import br.com.diegocordeiro.dscproject.service.DespesaService;
import br.com.diegocordeiro.dscproject.service.exceptions.RegistroNaoEncontradoException;
import br.com.diegocordeiro.dscproject.util.SecurityUtils;
import br.com.diegocordeiro.dscproject.web.sistema.validator.DespesaValidator;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class DespesaController {

    private final DespesaService despesaService;
    private final DespesaRepository despesaRepository;
    private final ContaRepository contaRepository;
    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MessageSource messageSource;
    private final SmartValidator smartValidator;

    public DespesaController(
            DespesaService despesaService,
            DespesaRepository despesaRepository,
            ContaRepository contaRepository,
            CartaoCreditoRepository cartaoCreditoRepository,
            CategoriaRepository categoriaRepository,
            UsuarioRepository usuarioRepository,
            MessageSource messageSource,
            SmartValidator smartValidator) {
        this.despesaService = despesaService;
        this.despesaRepository = despesaRepository;
        this.contaRepository = contaRepository;
        this.cartaoCreditoRepository = cartaoCreditoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.messageSource = messageSource;
        this.smartValidator = smartValidator;
    }

    @InitBinder
    public void binderComum(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/despesas/listar")
    public String listar(Model model, Principal principal) {
        boolean podeRatear = usuarioPodeRatear(principal);
        model.addAttribute("podeRatear", podeRatear);
        return "sistema/modulos/despesa/listar";
    }

    @GetMapping("/despesas/listar-dados")
    @ResponseBody
    public List<DespesaGridDTO> listarDados(Principal principal) {
        Usuario usuario = obterUsuarioAutenticado(principal);
        return despesaService.listarParaGrid(usuario.getId());
    }

    @PostMapping("/despesas/inserir")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> inserir(@ModelAttribute DespesaFormDTO dto, Principal principal, Locale locale) {
        Usuario usuario = obterUsuarioAutenticado(principal);
        dto.setId(null);
        boolean podeRatear = usuarioPodeRatear(principal);

        BindingResult resultado = validar(dto, usuario.getId(), podeRatear, locale);
        if (resultado.hasErrors()) {
            return respostaErros(resultado);
        }

        Despesa salva = despesaService.inserir(dto, usuario.getId(), usuario.getLogin(), podeRatear);
        String mensagemRetorno;
        if (salva.isParcelada()) {
            mensagemRetorno = messageSource.getMessage("msg.despesa.parcelada.sucesso", new Object[]{salva.getQtdParcelas()}, locale);
        } else {
            mensagemRetorno = mensagem("msg.despesa.cadastrada", locale);
        }
        return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", mensagemRetorno));
    }

    @GetMapping("/despesas/buscar/{id}")
    @ResponseBody
    public DespesaEdicaoDTO buscar(@PathVariable Long id, Principal principal) {
        Usuario usuario = obterUsuarioAutenticado(principal);
        return despesaService.buscarParaEdicao(id, usuario.getId());
    }

    @PutMapping("/despesas/editar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> editar(@PathVariable Long id, @ModelAttribute DespesaFormDTO dto, Principal principal, Locale locale) {
        Usuario usuario = obterUsuarioAutenticado(principal);
        despesaService.buscarPorIdEUsuario(id, usuario.getId());
        dto.setId(id);
        boolean podeRatear = usuarioPodeRatear(principal);

        BindingResult resultado = validar(dto, usuario.getId(), podeRatear, locale);
        if (resultado.hasErrors()) {
            return respostaErros(resultado);
        }

        despesaService.editar(id, dto, usuario.getId(), usuario.getLogin(), podeRatear);
        return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", mensagem("msg.despesa.atualizada", locale)));
    }

    @DeleteMapping("/despesas/excluir/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> excluir(@PathVariable Long id, Principal principal, Locale locale) {
        Usuario usuario = obterUsuarioAutenticado(principal);
        despesaService.excluir(id, usuario.getId(), usuario.getLogin());
        return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", mensagem("msg.despesa.excluida", locale)));
    }

    @PutMapping("/despesas/registrar-pagamento/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarPagamento(@PathVariable Long id, @ModelAttribute DespesaPagamentoDTO dto, Principal principal, Locale locale) {
        Usuario usuario = obterUsuarioAutenticado(principal);

        BindingResult resultado = new BeanPropertyBindingResult(dto, "despesaPagamentoDTO");
        smartValidator.validate(dto, resultado);
        if (resultado.hasErrors()) {
            return respostaErros(resultado);
        }

        despesaService.registrarPagamento(id, dto.getDataPagamento(), usuario.getId(), usuario.getLogin());
        return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", mensagem("msg.despesa.pagamento.registrado", locale)));
    }

    @PostMapping("/despesas/registrar-pagamento-lote")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarPagamentoLote(@ModelAttribute DespesaPagamentoLoteDTO dto, Principal principal, Locale locale) {
        Usuario usuario = obterUsuarioAutenticado(principal);

        BindingResult resultado = new BeanPropertyBindingResult(dto, "despesaPagamentoLoteDTO");
        smartValidator.validate(dto, resultado);
        if (resultado.hasErrors()) {
            return respostaErros(resultado);
        }

        int alteradas = despesaService.registrarPagamentoLote(dto.getIds(), dto.getDataPagamento(), usuario.getId(), usuario.getLogin());
        String msg = messageSource.getMessage("msg.despesa.pagamento-lote.registrado", new Object[]{alteradas}, locale);
        return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", msg));
    }

    @PutMapping("/despesas/{id}/rateio-acerto")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarAcertoRateio(@PathVariable Long id, @ModelAttribute DespesaRateioAcertoDTO dto, Principal principal, Locale locale) {
        Usuario usuario = obterUsuarioAutenticado(principal);

        BindingResult resultado = new BeanPropertyBindingResult(dto, "despesaRateioAcertoDTO");
        smartValidator.validate(dto, resultado);
        if (resultado.hasErrors()) {
            return respostaErros(resultado);
        }

        despesaService.registrarAcertoRateio(id, dto.getUsuarioId(), dto.isAcertado(), dto.getDataAcerto(), usuario.getId(), usuario.getLogin());
        return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", mensagem("msg.despesa.rateio.acerto-registrado", locale)));
    }

    @GetMapping("/despesas/usuarios-rateio")
    @ResponseBody
    public List<UsuarioRateioDTO> buscarUsuariosRateio(@RequestParam(required = false, defaultValue = "") String termo, Principal principal) {
        Usuario usuario = obterUsuarioAutenticado(principal);
        return despesaService.buscarUsuariosParaRateio(termo, usuario.getId());
    }

    private boolean usuarioPodeRatear(Principal principal) {
        if (principal instanceof Authentication auth) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> PermissaoDespesaCatalogo.DESPESA_RATEAR_MULTIUSUARIO.getAuthority().equals(a.getAuthority()));
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream()
                    .anyMatch(a -> PermissaoDespesaCatalogo.DESPESA_RATEAR_MULTIUSUARIO.getAuthority().equals(a.getAuthority()));
        }
        return false;
    }

    private Usuario obterUsuarioAutenticado(Principal principal) {
        String login = principal != null ? principal.getName() : SecurityUtils.loginAtual();
        return usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new RegistroNaoEncontradoException("usuario.nao.encontrado"));
    }

    private BindingResult validar(DespesaFormDTO dto, Long usuarioId, boolean podeRatear, Locale locale) {
        BindingResult resultado = new BeanPropertyBindingResult(dto, "despesaFormDTO");
        smartValidator.validate(dto, resultado);
        new DespesaValidator(
                despesaRepository,
                contaRepository,
                cartaoCreditoRepository,
                categoriaRepository,
                usuarioRepository,
                messageSource,
                locale,
                usuarioId,
                podeRatear)
                .validate(dto, resultado);
        return resultado;
    }

    private ResponseEntity<Map<String, Object>> respostaErros(BindingResult bindingResult) {
        Map<String, String> errosCampos = new LinkedHashMap<>();
        Map<String, String> errosNegocio = new LinkedHashMap<>();
        bindingResult.getFieldErrors().forEach(fe -> {
            String code = fe.getCode();
            if (code != null && (code.startsWith("Differ.") || code.startsWith("Duplicate.") || code.startsWith("Immutable."))) {
                errosNegocio.putIfAbsent(fe.getField(), fe.getDefaultMessage());
            } else {
                errosCampos.putIfAbsent(fe.getField(), fe.getDefaultMessage());
            }
        });
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sucesso", false);
        body.put("errosCampos", errosCampos);
        body.put("errosNegocio", errosNegocio);
        return ResponseEntity.unprocessableEntity().body(body);
    }

    private String mensagem(String chave, Locale locale) {
        return messageSource.getMessage(chave, null, locale);
    }
}
