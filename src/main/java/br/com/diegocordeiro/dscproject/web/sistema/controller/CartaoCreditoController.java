package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.cartaocredito.CartaoCreditoGridDTO;
import br.com.diegocordeiro.dscproject.enums.BandeiraCartao;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.CartaoCreditoRepository;
import br.com.diegocordeiro.dscproject.repository.ContaRepository;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import br.com.diegocordeiro.dscproject.service.CartaoCreditoService;
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
public class CartaoCreditoController {

    private final CartaoCreditoService cartaoCreditoService;
    private final CartaoCreditoRepository cartaoCreditoRepository;
    private final ContaRepository contaRepository;
    private final UsuarioRepository usuarioRepository;

    public CartaoCreditoController(CartaoCreditoService cartaoCreditoService, CartaoCreditoRepository cartaoCreditoRepository, ContaRepository contaRepository, UsuarioRepository usuarioRepository) {
        this.cartaoCreditoService = cartaoCreditoService;
        this.cartaoCreditoRepository = cartaoCreditoRepository;
        this.contaRepository = contaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @InitBinder
    public void binderComum(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/cartoes/listar")
    public String listar(Model model) {
        model.addAttribute("bandeiras", BandeiraCartao.values());
        return "sistema/modulos/cartao/listar";
    }

    @GetMapping("/cartoes/listar-dados")
    @ResponseBody
    public List<CartaoCreditoGridDTO> listarDados(Principal principal) {
        Usuario usuario = obterUsuarioAutenticado(principal);
        return cartaoCreditoService.listarParaGrid(usuario.getId());
    }

    private Usuario obterUsuarioAutenticado(Principal principal) {
        String login = principal != null ? principal.getName() : SecurityUtils.loginAtual();
        return usuarioRepository.findByLogin(login)
            .orElseThrow(() -> new RegistroNaoEncontradoException("msg.usuario.nao-encontrado"));
    }
}
