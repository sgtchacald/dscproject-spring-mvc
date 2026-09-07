package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.usuario.RevisaoUsuarioDTO;
import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioEdicaoDTO;
import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioListaDTO;
import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.service.UsuarioService;
import br.com.diegocordeiro.dscproject.web.sistema.validator.UsuarioValidator;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.beans.PropertyEditorSupport;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final MessageSource messageSource;
    private final SmartValidator smartValidator;

    public UsuarioController(UsuarioService usuarioService,
                             MessageSource messageSource,
                             SmartValidator smartValidator) {
        this.usuarioService = usuarioService;
        this.messageSource = messageSource;
        this.smartValidator = smartValidator;
    }

    @InitBinder("usuarioDTO")
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Genero.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue((text == null || text.isBlank()) ? null : Genero.toEnum(text));
            }
        });
    }

    // ---------- Área administrativa ----------

    @GetMapping("/listar")
    public String listar() {
        return "sistema/modulos/usuario/listar";
    }

    @GetMapping("/listar-dados")
    @ResponseBody
    public List<UsuarioListaDTO> listarDados() {
        return usuarioService.listarParaGrid();
    }

    @GetMapping("/buscar/{id}")
    @ResponseBody
    public UsuarioEdicaoDTO buscar(@PathVariable Long id) {
        return usuarioService.buscarParaEdicao(id);
    }

    @PostMapping("/inserir")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> inserir(@ModelAttribute UsuarioDTO dto, Locale locale) {
        dto.setId(null);
        BindingResult resultado = validar(dto, locale);
        if (resultado.hasErrors()) {
            return respostaErros(resultado);
        }
        usuarioService.inserir(dto);
        return ResponseEntity.ok(Map.of("sucesso", true));
    }

    @PutMapping("/editar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> editar(@PathVariable Long id,
                                                      @ModelAttribute UsuarioDTO dto,
                                                      Locale locale) {
        dto.setId(id);
        BindingResult resultado = validar(dto, locale);
        if (resultado.hasErrors()) {
            return respostaErros(resultado);
        }
        usuarioService.editar(id, dto);
        return ResponseEntity.ok(Map.of("sucesso", true));
    }

    @DeleteMapping("/excluir/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> excluir(@PathVariable Long id, Authentication authentication) {
        usuarioService.excluir(id, authentication != null ? authentication.getName() : null);
        return ResponseEntity.ok(Map.of("sucesso", true));
    }

    @GetMapping("/historico/{id}")
    @ResponseBody
    public Page<RevisaoUsuarioDTO> historico(@PathVariable Long id,
                                             @RequestParam(defaultValue = "0") int pagina,
                                             @RequestParam(defaultValue = "20") int tamanho) {
        return usuarioService.buscarHistorico(id, PageRequest.of(pagina, tamanho));
    }

    // ---------- Públicos ----------

    @GetMapping("/existe")
    @ResponseBody
    public boolean existe(@RequestParam String valor,
                          @RequestParam(required = false) Long idAtual) {
        return usuarioService.verificarSeExiste(valor, idAtual);
    }

    @GetMapping("/cadastrar-site")
    public String paginaAutoCadastro(Model model) {
        model.addAttribute("generos", Genero.values());
        return "sistema/publico/auto-cadastro";
    }

    @PostMapping("/cadastrar-site")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cadastrarSite(@ModelAttribute UsuarioDTO dto, Locale locale) {
        dto.setId(null);
        dto.setPerfilCodigo(null);              // RN08 — perfil enviado é ignorado
        BindingResult resultado = validar(dto, locale);
        if (resultado.hasErrors()) {
            return respostaErros(resultado);
        }
        usuarioService.autoCadastrar(dto);
        return ResponseEntity.ok(Map.of("sucesso", true));
    }

    // ---------- infra ----------

    private BindingResult validar(UsuarioDTO dto, Locale locale) {
        BindingResult resultado = new BeanPropertyBindingResult(dto, "usuarioDTO");
        smartValidator.validate(dto, resultado);
        new UsuarioValidator(usuarioService, messageSource, locale).validate(dto, resultado);
        return resultado;
    }

    private ResponseEntity<Map<String, Object>> respostaErros(BindingResult bindingResult) {
        Map<String, String> errosCampos = new LinkedHashMap<>();
        Map<String, String> errosNegocio = new LinkedHashMap<>();
        bindingResult.getFieldErrors().forEach(fe -> {
            String code = fe.getCode();
            if (code != null && (code.startsWith("Differ.") || code.startsWith("Duplicate."))) {
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
}
