package br.com.diegocordeiro.dscproject.service.exceptions;

/**
 * Regra de negócio violada (duplicidade, trava de exclusão, divergência).
 * A mensagem é uma <b>chave de i18n</b> — o advice a resolve via {@code MessageSource}.
 * O {@code campo} é opcional: quando presente, o erro é ligado ao campo na resposta;
 * quando nulo, vai como erro de negócio geral.
 */
public class RegraNegocioException extends RuntimeException {

    private final String campo;

    public RegraNegocioException(String chaveMensagem) {
        this(null, chaveMensagem);
    }

    public RegraNegocioException(String campo, String chaveMensagem) {
        super(chaveMensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
