package br.com.diegocordeiro.dscproject.service.exceptions;

/**
 * Token de recuperação de senha rejeitado.
 * A mensagem é uma chave de i18n resolvida pelo advice —
 * {@code msg.recuperacao.token.invalido} / {@code .expirado} / {@code .tentativas}.
 */
public class TokenRecuperacaoException extends RuntimeException {

    public static TokenRecuperacaoException invalido() {
        return new TokenRecuperacaoException("msg.recuperacao.token.invalido");
    }

    public static TokenRecuperacaoException expirado() {
        return new TokenRecuperacaoException("msg.recuperacao.token.expirado");
    }

    public static TokenRecuperacaoException tentativasExcedidas() {
        return new TokenRecuperacaoException("msg.recuperacao.token.tentativas");
    }

    private TokenRecuperacaoException(String chaveMensagem) {
        super(chaveMensagem);
    }
}
