package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.enums.Genero;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "sistema/home";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("generos", Genero.values());
        if (!model.containsAttribute("cadastroForm")) {
            model.addAttribute("cadastroForm", new UsuarioDTO());
        }
        return "login";
    }
}
