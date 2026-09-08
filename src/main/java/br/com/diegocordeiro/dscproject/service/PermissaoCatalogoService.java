package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.permissao.PermissaoCatalogoDTO;
import br.com.diegocordeiro.dscproject.dto.permissao.SincronizacaoCatalogoDTO;
import br.com.diegocordeiro.dscproject.permissao.CatalogoPermissoes;
import br.com.diegocordeiro.dscproject.permissao.PermissaoDefinida;
import br.com.diegocordeiro.dscproject.model.Permissao;
import br.com.diegocordeiro.dscproject.repository.PermissaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Catálogo de permissões: leitura para a tela e sincronização de {@code PERMISSOES}
 * com o catálogo do código. A tela nunca cria nem apaga permissão — só o
 * sincronizador escreve nesta tabela, e ele nunca remove uma linha.
 */
@Service
public class PermissaoCatalogoService {

    private final PermissaoRepository permissaoRepository;

    public PermissaoCatalogoService(PermissaoRepository permissaoRepository) {
        this.permissaoRepository = permissaoRepository;
    }

    @Transactional(readOnly = true)
    public List<PermissaoCatalogoDTO> listar() {
        return permissaoRepository.findByDataExclusaoIsNullOrderByModuloAscNomeAsc().stream()
            .map(PermissaoCatalogoDTO::new)
            .toList();
    }

    /**
     * Insere em {@code PERMISSOES} as permissões do código que faltam, marca como
     * órfã as que existem na tabela mas não no código, e desmarca a flag das que
     * voltaram ao código. Nunca apaga uma linha (vínculos históricos).
     */
    @Transactional
    public SincronizacaoCatalogoDTO sincronizar() {

        List<PermissaoDefinida> catalogo = CatalogoPermissoes.todas();

        Set<String> codigosDoCodigo = catalogo.stream()
            .map(PermissaoDefinida::getCodigo)
            .collect(Collectors.toSet());

        Map<String, Permissao> existentes = permissaoRepository.findAll().stream()
            .collect(Collectors.toMap(Permissao::getCodigo, Function.identity()));

        int inseridas = 0;
        for (PermissaoDefinida definicao : catalogo) {
            Permissao permissao = existentes.get(definicao.getCodigo());
            if (permissao == null) {
                permissaoRepository.save(new Permissao(
                    definicao.getCodigo(), definicao.getNome(), definicao.getDescricao(), definicao.getModulo()));
                inseridas++;
            } else {
                permissao.setNome(definicao.getNome());
                permissao.setDescricao(definicao.getDescricao());
                permissao.setModulo(definicao.getModulo());
                permissao.setOrfa(false);
            }
        }

        int orfas = 0;
        for (Permissao permissao : existentes.values()) {
            if (!codigosDoCodigo.contains(permissao.getCodigo()) && !permissao.isOrfa()) {
                permissao.setOrfa(true);
                orfas++;
            }
        }

        return new SincronizacaoCatalogoDTO(inseridas, orfas);
    }
}
