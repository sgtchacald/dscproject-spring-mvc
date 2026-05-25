package br.com.diegocordeiro.dscproject.web.sistema.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    @Autowired
    private BuildProperties buildProperties;

    @ModelAttribute("appVersao")
    public String appVersao() {
        return "v" + buildProperties.getVersion();
    }
}
