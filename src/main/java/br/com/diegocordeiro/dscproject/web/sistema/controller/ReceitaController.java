package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.receita.ReceitaGridDTO;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import br.com.diegocordeiro.dscproject.service.ReceitaService;
import br.com.diegocordeiro.dscproject.service.exceptions.RegistroNaoEncontradoException;
import br.com.diegocordeiro.dscproject.util.SecurityUtils;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.List;

@Controller
public class ReceitaController {

    private final ReceitaService receitaService;
    private final UsuarioRepository usuarioRepository;

    public ReceitaController(ReceitaService receitaService, UsuarioRepository usuarioRepository) {
        this.receitaService = receitaService;
        this.usuarioRepository = usuarioRepository;
    }

    @InitBinder
    public void binderComum(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/receitas/listar")
    public String listar(Model model) {
        return "sistema/modulos/receita/listar";
    }

    @GetMapping("/receitas/listar-dados")
    @ResponseBody
    public List<ReceitaGridDTO> listarDados(Principal principal) {
        Usuario usuario = obterUsuarioAutenticado(principal);
        return receitaService.listarParaGrid(usuario.getId());
    }

    private Usuario obterUsuarioAutenticado(Principal principal) {
        String login = principal != null ? principal.getName() : SecurityUtils.loginAtual();
        return usuarioRepository.findByLogin(login)
            .orElseThrow(() -> new RegistroNaoEncontradoException("usuario.nao.encontrado"));
    }
}
