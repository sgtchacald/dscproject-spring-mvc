package br.com.diegocordeiro.dscproject.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acesso ao usuário autenticado do contexto de segurança. */
public final class SecurityUtils {

    public static final String AUTOR_SISTEMA = "sistema";

    private SecurityUtils() {
    }

    /** Login autenticado, ou {@code "sistema"} quando não há autenticação. */
    public static String loginAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return AUTOR_SISTEMA;
        }
        return auth.getName();
    }
}
