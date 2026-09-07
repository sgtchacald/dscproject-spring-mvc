package br.com.diegocordeiro.dscproject.enums;

/**
 * Entrada do catálogo de permissões definido no código. Cada enum de módulo
 * (Usuários, Perfis e Permissões, …) implementa esta interface; o sincronizador
 * reflete estas entradas na tabela {@code PERMISSOES}.
 */
public interface PermissaoDefinida {

    String PREFIXO_AUTHORITY = "PERM_";

    String getCodigo();

    String getNome();

    String getDescricao();

    String getModulo();

    default String getAuthority() {
        return PREFIXO_AUTHORITY + getCodigo();
    }
}
