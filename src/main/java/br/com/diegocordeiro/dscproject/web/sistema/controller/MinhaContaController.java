package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.minhaconta.MinhaContaDTO;
import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.service.UsuarioService;
import br.com.diegocordeiro.dscproject.util.SecurityUtils;
import br.com.diegocordeiro.dscproject.web.sistema.validator.MinhaContaValidator;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.beans.PropertyEditorSupport;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Tela self-service Configurações da Conta: o usuário autenticado gere o próprio registro. */
@Controller
@RequestMapping("/minha-conta")
public class MinhaContaController {

    private final UsuarioService usuarioService;
    private final MessageSource messageSource;
    private final SmartValidator smartValidator;

    public MinhaContaController(UsuarioService usuarioService, MessageSource messageSource, SmartValidator smartValidator) {
        this.usuarioService = usuarioService;
        this.messageSource = messageSource;
        this.smartValidator = smartValidator;
    }

    @InitBinder("minhaContaDTO")
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Genero.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue((text == null || text.isBlank()) ? null : Genero.toEnum(text));
            }
        });
    }

    @GetMapping
    public String pagina(Model model) {
        Usuario usuario = usuarioService.buscarPorLogin(SecurityUtils.loginAtual());
        model.addAttribute("minhaConta", new MinhaContaDTO(usuario));
        model.addAttribute("generos", Genero.values());
        return "sistema/minha-conta";
    }

    @PutMapping
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvar(@ModelAttribute MinhaContaDTO dto, Locale locale) {
        String login = SecurityUtils.loginAtual();
        Long idAtual = usuarioService.buscarPorLogin(login).getId();

        BindingResult resultado = new BeanPropertyBindingResult(dto, "minhaContaDTO");
        smartValidator.validate(dto, resultado);
        new MinhaContaValidator(usuarioService, messageSource, locale, idAtual).validate(dto, resultado);
        if (resultado.hasErrors()) {
            return respostaErros(resultado);
        }
        usuarioService.atualizarPropriaConta(login, dto);
        return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", mensagem("msg.minha-conta.atualizada", locale)));
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

    private String mensagem(String chave, Locale locale) {
        return messageSource.getMessage(chave, null, locale);
    }
}
