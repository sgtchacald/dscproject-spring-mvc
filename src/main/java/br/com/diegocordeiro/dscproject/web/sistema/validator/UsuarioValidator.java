package br.com.diegocordeiro.dscproject.web.sistema.validator;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.service.UsuarioService;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class UsuarioValidator implements Validator {

    private final UsuarioService service;

    public UsuarioValidator(UsuarioService service) {
        this.service = service;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return UsuarioDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (errors.hasErrors()) {
            return;
        }
        UsuarioDTO dto = (UsuarioDTO) target;

        if (!dto.getSenha().equals(dto.getConfirmacaoSenha())) {
            errors.rejectValue("confirmacaoSenha", "Differ.usuarioDTO.confirmacaoSenha", "As senhas não conferem.");
        }
        if (service.verificarSeExisteUsuario(dto.getLogin())) {
            errors.rejectValue("login", "Duplicate.usuarioDTO.login", "Login já cadastrado.");
        }
        if (service.verificarSeExisteUsuario(dto.getEmail())) {
            errors.rejectValue("email", "Duplicate.usuarioDTO.email", "E-mail já cadastrado.");
        }
    }
}
