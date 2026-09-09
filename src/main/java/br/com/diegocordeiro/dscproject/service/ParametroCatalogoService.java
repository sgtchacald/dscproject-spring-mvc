package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.permissao.SincronizacaoCatalogoDTO;
import br.com.diegocordeiro.dscproject.model.ParametroGlobal;
import br.com.diegocordeiro.dscproject.parametro.CatalogoParametros;
import br.com.diegocordeiro.dscproject.parametro.ParametroDefinido;
import br.com.diegocordeiro.dscproject.repository.ParametroGlobalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sincroniza a tabela {@code PARAMETROS_GLOBAIS} com o catálogo de parâmetros do
 * código. A tela nunca cria nem apaga parâmetro — só o sincronizador escreve a
 * existência de uma linha, e ele nunca remove uma (histórico de revisões +
 * leituras antigas por código). Mesmo desenho do {@code PermissaoCatalogoService}.
 */
@Service
public class ParametroCatalogoService {

    private final ParametroGlobalRepository parametroGlobalRepository;

    public ParametroCatalogoService(ParametroGlobalRepository parametroGlobalRepository) {
        this.parametroGlobalRepository = parametroGlobalRepository;
    }

    /**
     * Insere os parâmetros do código que faltam (com {@code valor = valorDefault}),
     * atualiza nome/descrição/módulo/tipo/default dos existentes <b>sem tocar no
     * valor corrente</b>, marca como órfão o que sumiu do código e desmarca o que
     * voltou. Nunca apaga uma linha.
     */
    @Transactional
    public SincronizacaoCatalogoDTO sincronizar() {

        List<ParametroDefinido> catalogo = CatalogoParametros.todos();

        Set<String> codigosDoCodigo = catalogo.stream()
            .map(ParametroDefinido::getCodigo)
            .collect(Collectors.toSet());

        Map<String, ParametroGlobal> existentes = parametroGlobalRepository.findAll().stream()
            .collect(Collectors.toMap(ParametroGlobal::getCodigo, Function.identity()));

        int inseridos = 0;
        for (ParametroDefinido definicao : catalogo) {
            ParametroGlobal parametro = existentes.get(definicao.getCodigo());
            if (parametro == null) {
                parametroGlobalRepository.save(new ParametroGlobal(definicao));
                inseridos++;
            } else {
                parametro.setNome(definicao.getNome());
                parametro.setDescricao(definicao.getDescricao());
                parametro.setModulo(definicao.getModulo());
                parametro.setTipoDado(definicao.getTipo());
                parametro.setValorDefault(definicao.getValorDefault());
                parametro.setOrfa(false);
                // PAGL_VALOR pertence ao operador — nunca sobrescrito aqui
            }
        }

        int orfaos = 0;
        for (ParametroGlobal parametro : existentes.values()) {
            if (!codigosDoCodigo.contains(parametro.getCodigo()) && !parametro.isOrfa()) {
                parametro.setOrfa(true);
                orfaos++;
            }
        }

        return new SincronizacaoCatalogoDTO(inseridos, orfaos);
    }
}
