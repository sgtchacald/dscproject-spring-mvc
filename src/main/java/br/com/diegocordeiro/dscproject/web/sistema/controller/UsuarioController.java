package br.com.diegocordeiro.dscproject.web.sistema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @GetMapping("/listar")
    public String listar(){
        return "sistema/modulos/usuario/listar";
    }
}

