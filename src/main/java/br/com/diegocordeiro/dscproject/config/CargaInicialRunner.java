package br.com.diegocordeiro.dscproject.config;

import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.enums.PermissaoCatalogo;
import br.com.diegocordeiro.dscproject.model.Perfil;
import br.com.diegocordeiro.dscproject.model.PerfilPermissao;
import br.com.diegocordeiro.dscproject.model.Permissao;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.PerfilPermissaoRepository;
import br.com.diegocordeiro.dscproject.repository.PerfilRepository;
import br.com.diegocordeiro.dscproject.repository.PermissaoRepository;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga inicial idempotente do RBAC (Documento 0 §6.4):
 * perfis {@code ADMIN}/{@code USER}, catálogo de permissões do módulo Usuários,
 * vínculos do perfil ADMIN e — se o banco não tiver nenhum usuário — o ADMIN
 * inicial (lido de {@code app.admin.*}, sem senha em código: Obs. 2 do documento).
 */
@Component
public class CargaInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaInicialRunner.class);

    private final PerfilRepository perfilRepository;
    private final PermissaoRepository permissaoRepository;
    private final PerfilPermissaoRepository perfilPermissaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminLogin;
    private final String adminEmail;
    private final String adminSenha;
    private final String adminNome;

    public CargaInicialRunner(PerfilRepository perfilRepository,
                              PermissaoRepository permissaoRepository,
                              PerfilPermissaoRepository perfilPermissaoRepository,
                              UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${app.admin.login:}") String adminLogin,
                              @Value("${app.admin.email:}") String adminEmail,
                              @Value("${app.admin.senha:}") String adminSenha,
                              @Value("${app.admin.nome:Administrador}") String adminNome) {
        this.perfilRepository = perfilRepository;
        this.permissaoRepository = permissaoRepository;
        this.perfilPermissaoRepository = perfilPermissaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminLogin = adminLogin;
        this.adminEmail = adminEmail;
        this.adminSenha = adminSenha;
        this.adminNome = adminNome;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Perfil admin = obterOuCriarPerfil("ADMIN", "Administrador",
            "Administrador do sistema. Recebe todas as permissões.", true);
        obterOuCriarPerfil("USER", "Usuário",
            "Usuário comum. É o perfil do auto-cadastro.", true);

        for (PermissaoCatalogo entrada : PermissaoCatalogo.values()) {
            Permissao permissao = obterOuCriarPermissao(entrada);
            vincular(admin, permissao);
        }

        criarAdminInicialSeNecessario(admin);
    }

    private Perfil obterOuCriarPerfil(String codigo, String nome, String descricao, boolean sistema) {
        return perfilRepository.findByCodigo(codigo).orElseGet(() -> {
            log.info("Carga inicial: criando perfil {}", codigo);
            return perfilRepository.save(new Perfil(codigo, nome, descricao, sistema));
        });
    }

    private Permissao obterOuCriarPermissao(PermissaoCatalogo entrada) {
        return permissaoRepository.findByCodigo(entrada.getCodigo()).orElseGet(() -> {
            log.info("Carga inicial: criando permissão {}", entrada.getCodigo());
            return permissaoRepository.save(new Permissao(
                entrada.getCodigo(), entrada.getNome(), entrada.getDescricao(), entrada.getModulo()));
        });
    }

    private void vincular(Perfil perfil, Permissao permissao) {
        if (!perfilPermissaoRepository.existsByPerfilAndPermissao(perfil, permissao)) {
            log.info("Carga inicial: vinculando {} -> {}", perfil.getCodigo(), permissao.getCodigo());
            perfilPermissaoRepository.save(new PerfilPermissao(perfil, permissao));
        }
    }

    private void criarAdminInicialSeNecessario(Perfil admin) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (adminLogin.isBlank() || adminEmail.isBlank() || adminSenha.isBlank()) {
            log.warn("Nenhum usuário no banco e app.admin.login/email/senha não configurados — "
                + "ADMIN inicial NÃO criado. Configure as três propriedades e reinicie.");
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setNome(adminNome);
        usuario.setGenero(Genero.OUTRO);
        usuario.setLogin(adminLogin.trim());
        usuario.setEmail(adminEmail.trim());
        usuario.setSenha(passwordEncoder.encode(adminSenha));
        usuario.setPerfil(admin);
        usuarioRepository.save(usuario);
        log.info("Carga inicial: usuário ADMIN '{}' criado.", adminLogin);
    }
}
