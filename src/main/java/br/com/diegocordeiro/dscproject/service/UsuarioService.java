package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.enums.Perfis;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Usuario> buscarTodos() {
        return usuarioRepository.findAll();
    }

    public boolean verificarSeExisteUsuario(String valor) {
        return !usuarioRepository.findByCredenciaisList(valor).isEmpty();
    }

    @Transactional
    public Usuario inserir(UsuarioDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setGenero(dto.getGenero());
        usuario.setNascimento(dto.getNascimento());
        usuario.setEmail(dto.getEmail());
        usuario.setLogin(dto.getLogin());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setPerfil("dscordeiro86".equals(dto.getLogin()) ? Perfis.ADMIN : Perfis.USER);
        return usuarioRepository.save(usuario);
    }
}
