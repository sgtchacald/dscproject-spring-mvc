package br.com.diegocordeiro.dscproject.parametro;

import java.util.List;
import java.util.stream.Stream;

/**
 * Catálogo consolidado dos parâmetros globais definidos no código, reunindo os
 * enums de cada módulo. É o único ponto a editar quando um módulo novo trouxer
 * parâmetros — cada módulo declara os seus no próprio enum de catálogo.
 */
public final class CatalogoParametros {

    private CatalogoParametros() {
    }

    /** Todos os parâmetros do código, de todos os módulos. */
    public static List<ParametroDefinido> todos() {
        return Stream.of(
                Stream.of(ParametrosGlobaisCatalogo.values()),
                Stream.of(ParametrosCategoriaCatalogo.values()))
            .flatMap(s -> s)
            .map(ParametroDefinido.class::cast)
            .toList();
    }
}
