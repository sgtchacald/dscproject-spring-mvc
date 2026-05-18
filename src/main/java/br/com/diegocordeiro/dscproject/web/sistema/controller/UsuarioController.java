package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/listar")
    public String listar() {
        return "sistema/modulos/usuario/listar";
    }

    @PostMapping("/inserir")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> inserir(@Valid @ModelAttribute UsuarioDTO dto,
                                                       BindingResult bindingResult) {

        // Erros de campo (Bean Validation) — exibidos inline, sem alerta no topo
        Map<String, String> errosCampos = new LinkedHashMap<>();
        if (bindingResult.hasErrors()) {
            bindingResult.getFieldErrors()
                .forEach(fe -> errosCampos.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        }

        // Erros de negócio (servidor) — exibidos no alerta vermelho do topo + inline
        Map<String, String> errosNegocio = new LinkedHashMap<>();
        if (errosCampos.isEmpty()) {
            if (!dto.getSenha().equals(dto.getConfirmacaoSenha())) {
                errosNegocio.put("confirmacaoSenha", "As senhas não conferem.");
            }
            if (usuarioService.verificarSeExisteUsuario(dto.getLogin())) {
                errosNegocio.put("login", "Login já cadastrado.");
            }
            if (usuarioService.verificarSeExisteUsuario(dto.getEmail())) {
                errosNegocio.put("email", "E-mail já cadastrado.");
            }
        }

        if (!errosCampos.isEmpty() || !errosNegocio.isEmpty()) {
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
