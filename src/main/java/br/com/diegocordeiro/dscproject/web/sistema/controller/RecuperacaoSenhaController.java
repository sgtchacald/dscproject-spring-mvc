package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.recuperacaosenha.RecuperarSenhaConfirmacaoDTO;
import br.com.diegocordeiro.dscproject.dto.recuperacaosenha.RecuperarSenhaSolicitacaoDTO;
import br.com.diegocordeiro.dscproject.service.RecuperacaoSenhaService;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Fluxo público de recuperação de senha. */
@Controller
@RequestMapping("/usuarios/recuperar-senha")
public class RecuperacaoSenhaController {

    private final RecuperacaoSenhaService recuperacaoSenhaService;
    private final MessageSource messageSource;
    private final SmartValidator smartValidator;

    public RecuperacaoSenhaController(RecuperacaoSenhaService recuperacaoSenhaService, MessageSource messageSource, SmartValidator smartValidator) {
        this.recuperacaoSenhaService = recuperacaoSenhaService;
        this.messageSource = messageSource;
        this.smartValidator = smartValidator;
    }

    @GetMapping
    public String pagina(@RequestParam(required = false) String token, Model model) {
        boolean etapaDois = token != null && !token.isBlank();
        model.addAttribute("token", token);
        model.addAttribute("etapa", etapaDois ? 2 : 1);
        return "sistema/publico/recuperar-senha";
    }

    /** Resposta idêntica exista ou não a conta, para não revelar cadastro. */
    @PostMapping("/solicitar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> solicitar(@ModelAttribute RecuperarSenhaSolicitacaoDTO dto, Locale locale) {
        BindingResult resultado = new BeanPropertyBindingResult(dto, "recuperarSenhaSolicitacaoDTO");
        smartValidator.validate(dto, resultado);
        if (resultado.hasErrors()) {
            return respostaErros(resultado);
        }
        recuperacaoSenhaService.solicitar(dto.getEmail());
        return ResponseEntity.ok(Map.of(
            "sucesso", true,
            "mensagem", mensagem("msg.recuperacao.solicitacao.enviada", locale)));
    }

    /** Troca a senha; token inválido/expirado/excedido cai no advice (422). */
    @PostMapping("/confirmar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmar(@ModelAttribute RecuperarSenhaConfirmacaoDTO dto, Locale locale) {
        BindingResult resultado = new BeanPropertyBindingResult(dto, "recuperarSenhaConfirmacaoDTO");
        smartValidator.validate(dto, resultado);
        if (!dto.senhasConferem() && !resultado.hasFieldErrors("senha")) {
            resultado.rejectValue("confirmacaoSenha", "Differ.recuperarSenhaConfirmacaoDTO.confirmacaoSenha",
                mensagem("usuario.confirmacaoSenha.diferente", locale));
        }
        if (resultado.hasErrors()) {
            return respostaErros(resultado);
        }
        recuperacaoSenhaService.confirmar(dto.getToken(), dto.getSenha());
        return ResponseEntity.ok(Map.of(
            "sucesso", true,
            "mensagem", mensagem("msg.recuperacao.senha.redefinida", locale)));
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
