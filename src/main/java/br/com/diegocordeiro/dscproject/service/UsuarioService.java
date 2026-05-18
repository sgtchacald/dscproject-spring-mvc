package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<Usuario> buscarTodos() {
        return usuarioRepository.findAll();
    }

    public boolean verificarSeExisteUsuario(String valor) {
        return !usuarioRepository.findByCredenciaisList(valor).isEmpty();
    }

    @Transactional
    public Usuario insert(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }
}
