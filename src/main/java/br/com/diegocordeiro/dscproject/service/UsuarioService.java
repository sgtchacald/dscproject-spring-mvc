package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.usuario.RevisaoUsuarioDTO;
import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioEdicaoDTO;
import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioListaDTO;
import br.com.diegocordeiro.dscproject.model.Perfil;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.PerfilRepository;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import br.com.diegocordeiro.dscproject.service.exceptions.RegistroNaoEncontradoException;
import br.com.diegocordeiro.dscproject.service.exceptions.RegraNegocioException;
import br.com.diegocordeiro.dscproject.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class UsuarioService {

    private static final String PERFIL_ADMIN = "ADMIN";
    private static final String PERFIL_USER = "USER";

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PerfilRepository perfilRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioListaDTO> listarParaGrid() {
        return usuarioRepository.listarParaGrid().stream()
            .map(UsuarioListaDTO::new)
            .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioEdicaoDTO buscarParaEdicao(Long id) {
        return new UsuarioEdicaoDTO(buscarPorId(id));
    }

    /** C2 / RN04 — {@code idAtual} nulo na criação; preenchido na edição. */
    @Transactional(readOnly = true)
    public boolean verificarSeExiste(String valor, Long idAtual) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        return usuarioRepository.contarPorLoginOuEmail(valor.trim(), idAtual) > 0;
    }

    /** EDP04 — cadastro administrativo. */
    @Transactional
    public Usuario inserir(UsuarioDTO dto) {
        Usuario usuario = new Usuario();
        aplicarDados(usuario, dto);
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));  // RN02
        usuario.setPerfil(resolverPerfil(dto.getPerfilCodigo()));  // RN09 — caller já autorizado
        return usuarioRepository.save(usuario);
    }

    /** EDP09 — auto-cadastro público: sempre perfil USER (RN08). */
    @Transactional
    public Usuario autoCadastrar(UsuarioDTO dto) {
        Usuario usuario = new Usuario();
        aplicarDados(usuario, dto);
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setPerfil(resolverPerfil(PERFIL_USER));
        return usuarioRepository.save(usuario);
    }

    /** EDP05 — edição administrativa. */
    @Transactional
    public Usuario editar(Long id, UsuarioDTO dto) {
        Usuario usuario = buscarPorId(id);
        Perfil novoPerfil = resolverPerfil(dto.getPerfilCodigo());

        boolean eraAdmin = PERFIL_ADMIN.equals(usuario.getPerfil().getCodigo());
        boolean seraAdmin = PERFIL_ADMIN.equals(novoPerfil.getCodigo());
        if (eraAdmin && !seraAdmin && usuarioRepository.contarAdminsAtivos() <= 1) {   // RN11
            throw new RegraNegocioException("usuario.ultimo.admin");
        }

        aplicarDados(usuario, dto);
        usuario.setPerfil(novoPerfil);
        if (dto.senhaInformada()) {                                                    // RN07
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        return usuarioRepository.save(usuario);
    }

    /** EDP06 — exclusão lógica com as travas RN10 e RN11. */
    @Transactional
    public void excluir(Long id, String loginUsuarioLogado) {
        Usuario usuario = buscarPorId(id);

        if (loginUsuarioLogado != null
                && (loginUsuarioLogado.equals(usuario.getLogin())
                    || loginUsuarioLogado.equals(usuario.getEmail()))) {               // RN10
            throw new RegraNegocioException("usuario.exclusao.proprio");
        }
        if (PERFIL_ADMIN.equals(usuario.getPerfil().getCodigo())
                && usuarioRepository.contarAdminsAtivos() <= 1) {                       // RN11
            throw new RegraNegocioException("usuario.ultimo.admin");
        }
        if (usuario.isExcluido()) {
            return;
        }
        usuario.setDataExclusao(Instant.now());
        usuario.setExcluidoPor(SecurityUtils.loginAtual());
        usuarioRepository.save(usuario);
    }

    /** EDP08 — histórico (Envers). A senha é zerada pelo DTO (RN03). */
    @Transactional(readOnly = true)
    public Page<RevisaoUsuarioDTO> buscarHistorico(Long id, Pageable pageable) {
        buscarPorId(id);
        return usuarioRepository.findRevisions(id, pageable).map(RevisaoUsuarioDTO::new);
    }

    private void aplicarDados(Usuario usuario, UsuarioDTO dto) {
        usuario.setNome(dto.getNome());
        usuario.setGenero(dto.getGenero());
        usuario.setNascimento(dto.getNascimento());
        usuario.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        usuario.setLogin(dto.getLogin() != null ? dto.getLogin().trim() : null);
    }

    private Perfil resolverPerfil(String codigo) {
        String alvo = (codigo == null || codigo.isBlank()) ? PERFIL_USER : codigo;
        return perfilRepository.findByCodigo(alvo)
            .orElseThrow(() -> new RegraNegocioException("perfilCodigo", "usuario.perfil.invalido"));
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new RegistroNaoEncontradoException("usuario.nao.encontrado"));
    }
}
