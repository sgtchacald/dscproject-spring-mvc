package br.com.diegocordeiro.dscproject.enums;

/**
 * Catálogo, no código, das permissões do módulo Perfis e Permissões.
 * Convenção domínio-primeiro; cada uma vira a autoridade {@code PERM_{CODIGO}}.
 */
public enum PermissaoPerfilCatalogo implements PermissaoDefinida {

    PERFIS_LISTAR("Listar perfis",
        "Abrir a tela de Perfis e Permissões, listar os perfis e ver o catálogo de permissões em leitura. "
            + "Controla a visibilidade do menu 'Perfis e Permissões'."),
    PERFIS_MANTER("Manter perfis",
        "Criar, editar e excluir perfil, e ligar/desligar os vínculos perfil x permissão."),
    PERFIS_SINCRONIZAR_CATALOGO("Sincronizar catálogo de permissões",
        "Disparar a sincronização do catálogo de PERMISSOES com o do código.");

    public static final String MODULO = "Perfis e Permissões";

    private final String nome;
    private final String descricao;

    PermissaoPerfilCatalogo(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    @Override
    public String getCodigo() {
        return name();
    }

    @Override
    public String getNome() {
        return nome;
    }

    @Override
    public String getDescricao() {
        return descricao;
    }

    @Override
    public String getModulo() {
        return MODULO;
    }
}
