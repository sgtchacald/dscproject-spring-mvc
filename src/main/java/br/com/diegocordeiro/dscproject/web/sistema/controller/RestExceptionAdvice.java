package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.service.exceptions.RegistroNaoEncontradoException;
import br.com.diegocordeiro.dscproject.service.exceptions.RegraNegocioException;
import br.com.diegocordeiro.dscproject.service.exceptions.TokenRecuperacaoException;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Traduz as exceções de domínio dos endpoints AJAX em resposta JSON,
 * no formato do Apêndice A1 do guia (422 com baldes separados).
 */
@RestControllerAdvice(basePackages = "br.com.diegocordeiro.dscproject.web")
public class RestExceptionAdvice {

    private final MessageSource messageSource;

    public RestExceptionAdvice(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> regraNegocio(RegraNegocioException ex, Locale locale) {
        String mensagem = resolver(ex.getMessage(), locale);
        Map<String, String> errosNegocio = new LinkedHashMap<>();
        errosNegocio.put(ex.getCampo() != null ? ex.getCampo() : "geral", mensagem);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sucesso", false);
        body.put("errosCampos", new LinkedHashMap<>());
        body.put("errosNegocio", errosNegocio);
        body.put("mensagem", mensagem);
        return ResponseEntity.unprocessableEntity().body(body);
    }

    @ExceptionHandler(TokenRecuperacaoException.class)
    public ResponseEntity<Map<String, Object>> tokenRecuperacao(TokenRecuperacaoException ex, Locale locale) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sucesso", false);
        body.put("mensagem", resolver(ex.getMessage(), locale));
        return ResponseEntity.unprocessableEntity().body(body);
    }

    @ExceptionHandler(RegistroNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(RegistroNaoEncontradoException ex, Locale locale) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sucesso", false);
        body.put("mensagem", resolver(ex.getMessage(), locale));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    private String resolver(String chaveOuTexto, Locale locale) {
        try {
            return messageSource.getMessage(chaveOuTexto, null, locale);
        } catch (NoSuchMessageException e) {
            return chaveOuTexto;
        }
    }
}
