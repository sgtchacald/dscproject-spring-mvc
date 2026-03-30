package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionSort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class UsuarioService {

    private UsuarioRepository usuarioRepository;

    public List<Usuario> buscarTodos() {
        return (List<Usuario>) usuarioRepository.findAll();
    }

    public boolean verificarSeExisteUsuario(String valor) {
        List<Usuario> usuario = usuarioRepository.findByCredenciaisList(valor);
        return !usuario.isEmpty();
    }

    @Transactional
    public Usuario insert(Usuario usuario) {
        usuarioRepository.save(usuario);
        return usuario;
    }

}
