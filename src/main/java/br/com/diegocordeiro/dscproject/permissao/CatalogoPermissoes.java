package br.com.diegocordeiro.dscproject.permissao;

import java.util.List;
import java.util.stream.Stream;

/**
 * Catálogo consolidado das permissões definidas no código, reunindo os enums de
 * cada módulo. É o único ponto a editar quando um módulo novo trouxer permissões.
 */
public final class CatalogoPermissoes {

    private CatalogoPermissoes() {
    }

    /** Todas as permissões do código, de todos os módulos. */
    public static List<PermissaoDefinida> todas() {
        return Stream.of(
                Stream.of(PermissaoUsuarioCatalogo.values()),
                Stream.of(PermissaoPerfilCatalogo.values()),
                Stream.of(PermissaoParametroCatalogo.values()))
            .flatMap(s -> s)
            .map(PermissaoDefinida.class::cast)
            .toList();
    }
}
