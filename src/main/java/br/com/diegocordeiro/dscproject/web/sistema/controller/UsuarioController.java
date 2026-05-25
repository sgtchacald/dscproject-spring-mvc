package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.service.UsuarioService;
import br.com.diegocordeiro.dscproject.web.sistema.validator.UsuarioValidator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private MessageSource messageSource;

    @InitBinder("usuarioDTO")
    public void initBinder(WebDataBinder binder, Locale locale) {
        binder.registerCustomEditor(Genero.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue((text == null || text.isBlank()) ? null : Genero.toEnum(text));
            }
        });
        binder.addValidators(new UsuarioValidator(usuarioService, messageSource, locale));
    }

    @GetMapping("/listar")
    public String listar() {
        return "sistema/modulos/usuario/listar";
    }

    @PostMapping("/inserir")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> inserir(@Valid @ModelAttribute UsuarioDTO dto,
                                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errosCampos  = new LinkedHashMap<>();
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

        usuarioService.inserir(dto);
        return ResponseEntity.ok(Map.of("sucesso", true));
    }
}