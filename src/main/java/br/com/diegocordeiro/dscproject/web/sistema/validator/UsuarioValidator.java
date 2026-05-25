package br.com.diegocordeiro.dscproject.web.sistema.validator;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.service.UsuarioService;
import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Locale;

public class UsuarioValidator implements Validator {

    private final UsuarioService service;
    private final MessageSource messageSource;
    private final Locale locale;

    public UsuarioValidator(UsuarioService service, MessageSource messageSource, Locale locale) {
        this.service = service;
        this.messageSource = messageSource;
        this.locale = locale;
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
            errors.rejectValue("confirmacaoSenha", "Differ.usuarioDTO.confirmacaoSenha",
                messageSource.getMessage("usuario.confirmacaoSenha.diferente", null, locale));
        }
        if (service.verificarSeExisteUsuario(dto.getLogin())) {
            errors.rejectValue("login", "Duplicate.usuarioDTO.login",
                messageSource.getMessage("usuario.login.duplicado", null, locale));
        }
        if (service.verificarSeExisteUsuario(dto.getEmail())) {
            errors.rejectValue("email", "Duplicate.usuarioDTO.email",
                messageSource.getMessage("usuario.email.duplicado", null, locale));
        }
    }
}