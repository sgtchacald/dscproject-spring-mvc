package br.com.diegocordeiro.dscproject.dto.permissao;

/** Resumo de uma sincronização do catálogo de permissões: quantas foram inseridas e quantas viraram órfãs. */
public record SincronizacaoCatalogoDTO(int inseridas, int orfas) {
}
