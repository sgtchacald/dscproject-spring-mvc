# Centralização de Mensagens com i18n

**Data:** 2026-05-24
**Projeto:** dscproject-spring-mvc
**Escopo:** `login.html`, `login.js`, `UsuarioDTO`, `UsuarioValidator`, `WebMvcConfig`

## Objetivo

Remover todas as strings de mensagem hardcoded do HTML e do JavaScript, centralizando-as em arquivos `.properties` no backend. O resultado é uma única fonte de verdade para todas as mensagens do sistema, com suporte a internacionalização (i18n) nativo do Spring.

## Arquivos de Mensagens

Dois arquivos em `src/main/resources/`:

### `messages.properties` — erros e validações

```properties
# Bean Validation - UsuarioDTO
usuario.nome.obrigatorio=Nome é obrigatório.
usuario.nome.tamanho=Nome deve ter no máximo {max} caracteres.
usuario.genero.obrigatorio=Gênero é obrigatório.
usuario.nascimento.obrigatoria=Data de nascimento é obrigatória.
usuario.email.obrigatorio=E-mail é obrigatório.
usuario.email.invalido=E-mail inválido.
usuario.login.obrigatorio=Login é obrigatório.
usuario.login.tamanho=Login deve ter no máximo {max} caracteres.
usuario.senha.obrigatoria=Senha é obrigatória.
usuario.senha.tamanho=Senha deve ter no mínimo {min} caracteres.
usuario.confirmacaoSenha.obrigatoria=Confirmação de senha é obrigatória.

# Negócio
usuario.confirmacaoSenha.diferente=As senhas não conferem.
usuario.login.duplicado=Login já cadastrado.
usuario.email.duplicado=E-mail já cadastrado.

# Sistema
erro.comunicacao=Erro de comunicação com o servidor. Tente novamente.
```

### `labels.properties` — textos de UI

```properties
toast.cadastro.sucesso=Cadastro realizado com sucesso.
toast.logout.sucesso=Você saiu com sucesso.
```

**Regra de separação:**
- `messages.properties` → mensagens geradas por lógica (validação, negócio, sistema)
- `labels.properties` → textos estáticos de interface (toasts, títulos, botões)

## Mudanças por Camada

### `WebMvcConfig`

Dois beans novos:

1. **`MessageSource`** configurado com ambos os basenames (`messages`, `labels`), encoding UTF-8.
2. **`LocalValidatorFactoryBean`** apontando para o `MessageSource` acima — isso faz o Bean Validation ler chaves `{chave}` do mesmo arquivo `.properties`.

```java
@Bean
public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
    ms.setBasenames("classpath:messages", "classpath:labels");
    ms.setDefaultEncoding("UTF-8");
    return ms;
}

@Bean
public LocalValidatorFactoryBean validator() {
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setValidationMessageSource(messageSource());
    return factory;
}

@Override
public Validator getValidator() {
    return validator();
}
```

### `UsuarioDTO`

As anotações Bean Validation passam a referenciar chaves com `{chave}`. O Spring substitui automaticamente `{max}` e `{min}` pelos valores dos atributos da anotação:

```java
@NotBlank(message = "{usuario.nome.obrigatorio}")
@Size(max = 100, message = "{usuario.nome.tamanho}")
private String nome;

@NotNull(message = "{usuario.genero.obrigatorio}")
private Genero genero;

// ... mesmo padrão para todos os campos
```

### `UsuarioValidator`

Recebe `MessageSource` e `Locale` para resolver mensagens programaticamente. O `UsuarioController` injeta o `MessageSource` e o repassa ao construir o validator no `@InitBinder`:

```java
// UsuarioValidator
private final UsuarioService service;
private final MessageSource messageSource;
private final Locale locale;

public UsuarioValidator(UsuarioService service, MessageSource messageSource, Locale locale) { ... }

@Override
public void validate(Object target, Errors errors) {
    if (errors.hasErrors()) return;
    UsuarioDTO dto = (UsuarioDTO) target;

    if (!dto.getSenha().equals(dto.getConfirmacaoSenha())) {
        errors.rejectValue("confirmacaoSenha", "usuario.confirmacaoSenha.diferente",
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
```

```java
// UsuarioController — @InitBinder
@Autowired
private MessageSource messageSource;

@InitBinder("usuarioDTO")
public void initBinder(WebDataBinder binder, Locale locale) {
    binder.registerCustomEditor(Genero.class, new PropertyEditorSupport() { ... });
    binder.addValidators(new UsuarioValidator(usuarioService, messageSource, locale));
}
```

### `login.html`

Toasts substituem texto hardcoded por `th:text="#{chave}"`:

```html
<!-- Toast de cadastro -->
<div class="toast-body fw-medium" th:text="#{toast.cadastro.sucesso}"></div>

<!-- Toast de logout -->
<div class="toast-body fw-medium" th:text="#{toast.logout.sucesso}"></div>
```

A mensagem do JS é embarcada no formulário como atributo `data-*`:

```html
<form id="formCadastro"
      th:action="@{/usuarios/inserir}"
      th:data-erro-comunicacao="#{erro.comunicacao}"
      method="post" novalidate>
```

O `invalid-feedback` hardcoded de nascimento é removido (o backend já envia essa mensagem via Bean Validation):

```html
<!-- Antes -->
<div class="invalid-feedback" id="erro-nascimento">Data de nascimento é obrigatória.</div>

<!-- Depois -->
<div class="invalid-feedback" id="erro-nascimento"></div>
```

### `login.js`

A string hardcoded é substituída pela leitura do atributo `data-*`:

```javascript
// Antes
alertaTexto.textContent = 'Erro de comunicação com o servidor. Tente novamente.';

// Depois
alertaTexto.textContent = formCadastro.dataset.erroComunicacao;
```

## Fluxo de Resolução de Mensagens

```
messages.properties  ─┐
                       ├─► MessageSource ─► Thymeleaf #{chave}
labels.properties    ─┘         │
                                 ├─► LocalValidatorFactoryBean ─► Bean Validation {chave}
                                 └─► UsuarioValidator.getMessage() ─► rejectValue()
```

## O que NÃO muda

- Estrutura do JSON de resposta da API (`sucesso`, `errosCampos`, `errosNegocio`)
- Lógica de exibição inline de erros no JS (`aplicarErros`)
- Fluxo de validação no controller (`BindingResult`)

## Arquivos Alterados

| Arquivo | Tipo de mudança |
|---|---|
| `src/main/resources/messages.properties` | Novo |
| `src/main/resources/labels.properties` | Novo |
| `WebMvcConfig.java` | Adiciona `MessageSource` e `LocalValidatorFactoryBean` |
| `UsuarioDTO.java` | Substitui strings por `{chave}` nas anotações |
| `UsuarioValidator.java` | Recebe e usa `MessageSource` |
| `UsuarioController.java` | Injeta `MessageSource`, passa `Locale` ao `@InitBinder` |
| `login.html` | Substitui strings por `#{chave}` e adiciona `data-*` |
| `login.js` | Lê `data-*` em vez de string hardcoded |