# Centralização de Mensagens i18n — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remover todas as strings hardcoded de HTML e JavaScript, centralizando-as em `messages.properties` e `labels.properties` com suporte nativo a i18n do Spring.

**Architecture:** Dois arquivos `.properties` em `src/main/resources` — `messages` para erros e validações, `labels` para textos de UI. O `WebMvcConfig` declara um `MessageSource` único que unifica os dois basenames e um `LocalValidatorFactoryBean` que aponta para esse `MessageSource`, fazendo Bean Validation e Thymeleaf lerem do mesmo lugar. Mensagens para o JS chegam via atributos `data-*` renderizados pelo Thymeleaf.

**Tech Stack:** Spring Boot 4 / Java 25, Thymeleaf, Bean Validation (Jakarta), Spring `MessageSource`, MockMvc + JUnit 5

---

## Mapa de Arquivos

| Arquivo | Ação |
|---|---|
| `src/main/resources/messages.properties` | Criar — erros de validação, negócio e sistema |
| `src/main/resources/labels.properties` | Criar — textos de UI (toasts) |
| `WebMvcConfig.java` | Modificar — adicionar `MessageSource` e `LocalValidatorFactoryBean` |
| `UsuarioDTO.java` | Modificar — substituir strings por `{chave}` nas anotações |
| `UsuarioValidator.java` | Modificar — receber e usar `MessageSource` |
| `UsuarioController.java` | Modificar — injetar `MessageSource`, passar `Locale` no `@InitBinder` |
| `login.html` | Modificar — `th:text="#{chave}"` nos toasts, `th:data-*` no form |
| `login.js` | Modificar — ler `dataset.erroComunicacao` em vez de string literal |
| `UsuarioControllerInserirTest.java` | Verificar — os asserts de mensagem devem continuar passando |

---

## Task 1: Criar `messages.properties`

**Files:**
- Create: `src/main/resources/messages.properties`

- [ ] **Criar o arquivo com todas as chaves de validação e negócio**

```properties
# Bean Validation — UsuarioDTO
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

- [ ] **Commitar**

```bash
git add src/main/resources/messages.properties
git commit -m "feat(i18n): adiciona messages.properties com chaves de validacao e negocio"
```

---

## Task 2: Criar `labels.properties`

**Files:**
- Create: `src/main/resources/labels.properties`

- [ ] **Criar o arquivo com chaves de UI**

```properties
toast.cadastro.sucesso=Cadastro realizado com sucesso.
toast.logout.sucesso=Você saiu com sucesso.
```

- [ ] **Commitar**

```bash
git add src/main/resources/labels.properties
git commit -m "feat(i18n): adiciona labels.properties com textos de UI"
```

---

## Task 3: Configurar `WebMvcConfig` com `MessageSource` e `LocalValidatorFactoryBean`

**Files:**
- Modify: `src/main/java/br/com/diegocordeiro/dscproject/config/WebMvcConfig.java`

- [ ] **Substituir o conteúdo completo do arquivo**

```java
package br.com.diegocordeiro.dscproject.config;

import br.com.diegocordeiro.dscproject.web.sistema.converter.StringToGeneroConverter;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

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

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToGeneroConverter());
    }
}
```

- [ ] **Rodar os testes existentes para garantir que nada quebrou**

```bash
cd dscproject-spring-mvc
./mvnw test -Dtest=UsuarioControllerInserirTest -q
```

Resultado esperado: todos os 4 testes passando. Se falhar com erro de `MessageSource` não encontrado, adicionar `@Import(WebMvcConfig.class)` na classe de teste — mas normalmente o `@WebMvcTest` carrega `@Configuration` automaticamente.

- [ ] **Commitar**

```bash
git add src/main/java/br/com/diegocordeiro/dscproject/config/WebMvcConfig.java
git commit -m "feat(i18n): configura MessageSource unificado e LocalValidatorFactoryBean"
```

---

## Task 4: Atualizar `UsuarioDTO` — chaves nas anotações Bean Validation

**Files:**
- Modify: `src/main/java/br/com/diegocordeiro/dscproject/dto/usuario/UsuarioDTO.java`

As anotações passam a referenciar chaves com `{chave}`. Bean Validation substitui `{max}` e `{min}` automaticamente pelo valor dos atributos da anotação.

- [ ] **Substituir os campos anotados**

```java
@NotBlank(message = "{usuario.nome.obrigatorio}")
@Size(max = 100, message = "{usuario.nome.tamanho}")
private String nome;

@NotNull(message = "{usuario.genero.obrigatorio}")
private Genero genero;

@NotNull(message = "{usuario.nascimento.obrigatoria}")
@DateTimeFormat(pattern = "yyyy-MM-dd")
private Date nascimento;

@NotBlank(message = "{usuario.email.obrigatorio}")
@Email(message = "{usuario.email.invalido}")
private String email;

@NotBlank(message = "{usuario.login.obrigatorio}")
@Size(max = 40, message = "{usuario.login.tamanho}")
private String login;

@NotBlank(message = "{usuario.senha.obrigatoria}")
@Size(min = 6, message = "{usuario.senha.tamanho}")
private String senha;

@NotBlank(message = "{usuario.confirmacaoSenha.obrigatoria}")
private String confirmacaoSenha;
```

Manter o restante do arquivo (imports, construtores, getters/setters) sem alteração.

- [ ] **Rodar os testes**

```bash
./mvnw test -Dtest=UsuarioControllerInserirTest -q
```

Resultado esperado: todos passando. A mensagem `errosCampos.nome` agora vem de `messages.properties`, mas o conteúdo é o mesmo, então os asserts existentes continuam válidos.

- [ ] **Commitar**

```bash
git add src/main/java/br/com/diegocordeiro/dscproject/dto/usuario/UsuarioDTO.java
git commit -m "feat(i18n): substitui mensagens hardcoded por chaves em UsuarioDTO"
```

---

## Task 5: Atualizar `UsuarioValidator` — usar `MessageSource`

**Files:**
- Modify: `src/main/java/br/com/diegocordeiro/dscproject/web/sistema/validator/UsuarioValidator.java`

Os códigos de erro (`"Differ.usuarioDTO.confirmacaoSenha"`, `"Duplicate.usuarioDTO.login"`, `"Duplicate.usuarioDTO.email"`) são mantidos — o `UsuarioController` usa o prefixo do código para classificar entre `errosCampos` e `errosNegocio`. Apenas o texto da mensagem passa a vir do `MessageSource`.

- [ ] **Substituir o conteúdo completo do arquivo**

```java
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
```

- [ ] **Commitar (sem rodar testes ainda — o controller precisa ser atualizado primeiro)**

```bash
git add src/main/java/br/com/diegocordeiro/dscproject/web/sistema/validator/UsuarioValidator.java
git commit -m "feat(i18n): UsuarioValidator recebe MessageSource para resolver mensagens"
```

---

## Task 6: Atualizar `UsuarioController` — injetar `MessageSource` e `Locale`

**Files:**
- Modify: `src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/UsuarioController.java`

O `@InitBinder` aceita `Locale` como parâmetro — Spring MVC o injeta automaticamente a partir do request. O `MessageSource` é injetado como bean.

- [ ] **Substituir o conteúdo completo do arquivo**

```java
package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.service.UsuarioService;
import br.com.diegocordeiro.dscproject.web.sistema.validator.UsuarioValidator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private MessageSource messageSource;

    @InitBinder("usuarioDTO")
    public void initBinder(WebDataBinder binder, Locale locale) {
        binder.registerCustomEditor(Genero.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue((text == null || text.isBlank()) ? null : Genero.toEnum(text));
            }
        });
        binder.addValidators(new UsuarioValidator(usuarioService, messageSource, locale));
    }

    @GetMapping("/listar")
    public String listar() {
        return "sistema/modulos/usuario/listar";
    }

    @PostMapping("/inserir")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> inserir(@Valid @ModelAttribute UsuarioDTO dto,
                                                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errosCampos  = new LinkedHashMap<>();
            Map<String, String> errosNegocio = new LinkedHashMap<>();
            bindingResult.getFieldErrors().forEach(fe -> {
                String code = fe.getCode();
                if (code != null && (code.startsWith("Differ.") || code.startsWith("Duplicate."))) {
                    errosNegocio.putIfAbsent(fe.getField(), fe.getDefaultMessage());
                } else {
                    errosCampos.putIfAbsent(fe.getField(), fe.getDefaultMessage());
                }
            });
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sucesso", false);
            body.put("errosCampos", errosCampos);
            body.put("errosNegocio", errosNegocio);
            return ResponseEntity.unprocessableEntity().body(body);
        }

        usuarioService.inserir(dto);
        return ResponseEntity.ok(Map.of("sucesso", true));
    }
}
```

- [ ] **Rodar todos os testes**

```bash
./mvnw test -q
```

Resultado esperado: todos os testes passando, incluindo os 4 de `UsuarioControllerInserirTest`. As mensagens em `errosNegocio` agora vêm de `messages.properties` mas têm o mesmo conteúdo que antes.

- [ ] **Commitar**

```bash
git add src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/UsuarioController.java
git commit -m "feat(i18n): UsuarioController injeta MessageSource e passa Locale ao validator"
```

---

## Task 7: Atualizar `login.html`

**Files:**
- Modify: `src/main/resources/templates/login.html`

Três mudanças:
1. Toast de cadastro: `th:text="#{toast.cadastro.sucesso}"`
2. Toast de logout: `th:text="#{toast.logout.sucesso}"`
3. Form: adicionar `th:data-erro-comunicacao="#{erro.comunicacao}"`
4. `invalid-feedback` de nascimento: remover texto hardcoded (backend já envia a mensagem)

- [ ] **Substituir toast de cadastro (localizar pelo id `toastSucesso`)**

```html
<div id="toastSucesso" class="toast align-items-center text-bg-success border-0 mb-2" role="alert" aria-live="assertive" aria-atomic="true">
    <div class="d-flex">
        <div class="toast-body fw-medium" th:text="#{toast.cadastro.sucesso}"></div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" onclick="esconderToast('toastSucesso')" aria-label="Fechar"></button>
    </div>
</div>
```

- [ ] **Substituir toast de logout (localizar pelo id `toastLogout`)**

```html
<div id="toastLogout" class="toast align-items-center text-bg-success border-0" role="alert" aria-live="assertive" aria-atomic="true">
    <div class="d-flex">
        <div class="toast-body fw-medium" th:text="#{toast.logout.sucesso}"></div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" onclick="esconderToast('toastLogout')" aria-label="Fechar"></button>
    </div>
</div>
```

- [ ] **Adicionar `th:data-erro-comunicacao` no form (localizar pelo id `formCadastro`)**

```html
<form id="formCadastro"
      th:action="@{/usuarios/inserir}"
      th:data-erro-comunicacao="#{erro.comunicacao}"
      method="post"
      novalidate>
```

- [ ] **Remover texto hardcoded do `invalid-feedback` de nascimento (localizar pelo id `erro-nascimento`)**

```html
<div class="invalid-feedback" id="erro-nascimento"></div>
```

- [ ] **Copiar para `target/classes` (o Spring serve estáticos de lá em modo dev)**

```bash
cp src/main/resources/templates/login.html target/classes/templates/login.html
```

- [ ] **Commitar**

```bash
git add src/main/resources/templates/login.html
git commit -m "feat(i18n): login.html usa th:text e th:data-* para mensagens via MessageSource"
```

---

## Task 8: Atualizar `login.js`

**Files:**
- Modify: `src/main/resources/static/js/login.js`

- [ ] **Substituir a string hardcoded pela leitura do atributo `data-*`**

Localizar no `catch`:
```javascript
alertaTexto.textContent = 'Erro de comunicação com o servidor. Tente novamente.';
```

Substituir por:
```javascript
alertaTexto.textContent = formCadastro.dataset.erroComunicacao;
```

- [ ] **Copiar para `target/classes`**

```bash
cp src/main/resources/static/js/login.js target/classes/static/js/login.js
```

- [ ] **Commitar**

```bash
git add src/main/resources/static/js/login.js
git commit -m "feat(i18n): login.js le mensagem de erro do atributo data-erro-comunicacao"
```

---

## Task 9: Verificação final

- [ ] **Rodar todos os testes**

```bash
./mvnw test -q
```

Resultado esperado: todos passando.

- [ ] **Subir a aplicação e testar manualmente os cenários**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Cenários a verificar no browser:
1. **Cadastro com sucesso** → modal fecha, toast "Cadastro realizado com sucesso." aparece
2. **Senhas diferentes** → mensagem "As senhas não conferem." aparece inline no campo `confirmacaoSenha`
3. **Login duplicado** → mensagem "Login já cadastrado." aparece inline no campo `login`
4. **Nome em branco** → mensagem "Nome é obrigatório." aparece inline no campo `nome`
5. **Servidor inacessível** (parar o servidor e tentar cadastrar) → mensagem "Erro de comunicação com o servidor. Tente novamente." aparece no alerta do modal
6. **Logout** → toast "Você saiu com sucesso." aparece após logout

- [ ] **Verificar no DevTools que os toasts têm o texto correto renderizado pelo Thymeleaf**

Inspecionar o HTML fonte da página: as `div.toast-body` devem ter o texto renderizado (não `th:text` como atributo).