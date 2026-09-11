package br.com.diegocordeiro.dscproject.dto.catalogo;

/** Resumo de uma sincronização do catálogo (permissões, parâmetros): quantas foram inseridas e quantas viraram órfãs. */
public record SincronizacaoCatalogoDTO(int inseridas, int orfas) {
}
