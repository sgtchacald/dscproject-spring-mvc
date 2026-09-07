package br.com.diegocordeiro.dscproject.service.exceptions;

/** Registro inexistente. A mensagem é uma chave de i18n resolvida pelo advice. */
public class RegistroNaoEncontradoException extends RuntimeException {

    public RegistroNaoEncontradoException(String chaveMensagem) {
        super(chaveMensagem);
    }
}
