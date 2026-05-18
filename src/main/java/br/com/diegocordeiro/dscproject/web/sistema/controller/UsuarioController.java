package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

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
    public String inserir(@Valid @ModelAttribute UsuarioDTO dto,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {

        List<String> erros = new ArrayList<>();

        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors()
                .forEach(e -> erros.add(e.getDefaultMessage()));
            redirectAttributes.addFlashAttribute("cadastroErros", erros);
            redirectAttributes.addFlashAttribute("cadastroForm", dto);
            return "redirect:/login";
        }

        if (!dto.getSenha().equals(dto.getConfirmacaoSenha())) {
            erros.add("As senhas não conferem.");
        }
        if (usuarioService.verificarSeExisteUsuario(dto.getLogin())) {
            erros.add("Login já cadastrado.");
        }
        if (usuarioService.verificarSeExisteUsuario(dto.getEmail())) {
            erros.add("E-mail já cadastrado.");
        }

        if (!erros.isEmpty()) {
            redirectAttributes.addFlashAttribute("cadastroErros", erros);
            redirectAttributes.addFlashAttribute("cadastroForm", dto);
            return "redirect:/login";
        }

        usuarioService.inserir(dto);
        return "redirect:/login?cadastroSucesso=true";
    }
}
