package br.com.diegocordeiro.dscproject.web.sistema.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final String appVersao;

    public GlobalModelAdvice(@Value("${app.version:dev}") String appVersao) {
        this.appVersao = appVersao;
    }

    @ModelAttribute("appVersao")
    public String appVersao() {
        return "v" + appVersao;
    }

    /** URI da requisição atual — a sidebar usa para marcar o item de menu ativo (Thymeleaf 3.1 não expõe mais {@code #request}). */
    @ModelAttribute("uriAtual")
    public String uriAtual(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
