package br.com.diegocordeiro.dscproject.web.sistema.validator;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.service.UsuarioService;
import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Locale;

/**
 * Validação de fronteira que depende do banco / do estado (RN04, RT08)
 * e a obrigatoriedade condicional da senha (criação x edição — RN07).
 *
 * <p>Prefixo do código de erro carrega a natureza:
 * {@code Duplicate.} / {@code Differ.} = erro de negócio; os demais = erro de campo.</p>
 */
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
        UsuarioDTO dto = (UsuarioDTO) target;

        // Senha é obrigatória na criação; opcional na edição (RN07).
        if (!dto.isEdicao() && !dto.senhaInformada() && !errors.hasFieldErrors("senha")) {
            errors.rejectValue("senha", "NotBlank.usuarioDTO.senha",
                mensagem("usuario.senha.obrigatoria"));
        }

        // RT08 / MSG07 — confirmação só é exigida quando a senha foi informada.
        if (dto.senhaInformada() && !dto.getSenha().equals(dto.getConfirmacaoSenha())) {
            errors.rejectValue("confirmacaoSenha", "Differ.usuarioDTO.confirmacaoSenha",
                mensagem("usuario.confirmacaoSenha.diferente"));
        }

        // RN04 / C2 — login e e-mail únicos entre usuários não excluídos.
        if (hasTexto(dto.getLogin())
                && !errors.hasFieldErrors("login")
                && service.verificarSeExiste(dto.getLogin(), dto.getId())) {
            errors.rejectValue("login", "Duplicate.usuarioDTO.login",
                mensagem("usuario.login.duplicado"));
        }
        if (hasTexto(dto.getEmail())
                && !errors.hasFieldErrors("email")
                && service.verificarSeExiste(dto.getEmail(), dto.getId())) {
            errors.rejectValue("email", "Duplicate.usuarioDTO.email",
                mensagem("usuario.email.duplicado"));
        }
    }

    private boolean hasTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String mensagem(String chave) {
        return messageSource.getMessage(chave, null, locale);
    }
}
