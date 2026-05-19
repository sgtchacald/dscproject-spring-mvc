package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AutorizacaoService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByLoginOrEmail(login, login);
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuário não encontrado: " + login);
        }
        return usuario;
    }
}
