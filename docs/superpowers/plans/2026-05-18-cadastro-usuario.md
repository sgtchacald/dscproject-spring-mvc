# Cadastro de Usuário Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar botão "Cadastrar" na página de login que abre modal Tabler responsiva com formulário server-side (POST → flash attributes → reabertura automática em erro).

**Architecture:** Fluxo 100% server-side Thymeleaf. O `UsuarioController` recebe o POST em `/usuarios/inserir` (rota pública), valida com Bean Validation + regras de negócio, e redireciona com `RedirectAttributes` flash para reabrir a modal em caso de erro. Em sucesso, codifica a senha com BCrypt, atribui perfil (ADMIN se login == "dscordeiro86", USER demais) e persiste via `UsuarioService`.

**Tech Stack:** Spring Boot 4.0.5 · Spring Security 7.x · Thymeleaf · Tabler UI 1.4.0 (Bootstrap 5) · Bean Validation (jakarta.validation) · BCrypt · JPA/MySQL · Lombok · JUnit 5 · Mockito · MockMvc

---

## Mapa de Arquivos

| Ação | Arquivo |
|------|---------|
| Modificar | `src/main/java/br/com/diegocordeiro/dscproject/dto/usuario/UsuarioDTO.java` |
| Modificar | `src/main/java/br/com/diegocordeiro/dscproject/service/UsuarioService.java` |
| Modificar | `src/main/java/br/com/diegocordeiro/dscproject/config/SecurityConfig.java` |
| Modificar | `src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/UsuarioController.java` |
| Modificar | `src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/HomeController.java` |
| Modificar | `src/main/resources/templates/login.html` |
| Criar | `src/test/java/br/com/diegocordeiro/dscproject/service/UsuarioServiceInserirTest.java` |
| Criar | `src/test/java/br/com/diegocordeiro/dscproject/web/sistema/controller/UsuarioControllerInserirTest.java` |

---

## Task 1: Atualizar UsuarioDTO — campos e validação

**Files:**
- Modify: `src/main/java/br/com/diegocordeiro/dscproject/dto/usuario/UsuarioDTO.java`

- [ ] **Step 1: Substituir o conteúdo de UsuarioDTO.java**

```java
package br.com.diegocordeiro.dscproject.dto.usuario;

import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.enums.Perfis;
import br.com.diegocordeiro.dscproject.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UsuarioDTO {

    private Long id;

    @NotBlank(message = "Nome é obrigatório.")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
    private String nome;

    @NotNull(message = "Gênero é obrigatório.")
    private Genero genero;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date nascimento;

    @NotBlank(message = "E-mail é obrigatório.")
    @Email(message = "E-mail inválido.")
    private String email;

    @NotBlank(message = "Login é obrigatório.")
    @Size(max = 40, message = "Login deve ter no máximo 40 caracteres.")
    private String login;

    @NotBlank(message = "Senha é obrigatória.")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres.")
    private String senha;

    private String confirmacaoSenha;

    private Perfis perfil;

    public UsuarioDTO(Usuario usuario) {
        this.id      = usuario.getId();
        this.nome    = usuario.getNome();
        this.genero  = usuario.getGenero();
        this.email   = usuario.getEmail();
        this.login   = usuario.getLogin();
        this.senha   = usuario.getSenha();
        this.perfil  = usuario.getPerfil();
    }
}
```

- [ ] **Step 2: Compilar para verificar sem erros**

```bash
cd /home/dscordeiro/DEV_HOME/DSC_HOME/dscproject-spring-mvc
./mvnw compile -q
```

Esperado: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/diegocordeiro/dscproject/dto/usuario/UsuarioDTO.java
git commit -m "feat: add nascimento, confirmacaoSenha and Bean Validation to UsuarioDTO"
```

---

## Task 2: Adicionar UsuarioService.inserir() com testes

**Files:**
- Modify: `src/main/java/br/com/diegocordeiro/dscproject/service/UsuarioService.java`
- Create (test): `src/test/java/br/com/diegocordeiro/dscproject/service/UsuarioServiceInserirTest.java`

- [ ] **Step 1: Escrever o teste**

Criar `src/test/java/br/com/diegocordeiro/dscproject/service/UsuarioServiceInserirTest.java`:

```java
package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.enums.Perfis;
import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceInserirTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private UsuarioDTO dtoBase() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNome("Diego Cordeiro");
        dto.setGenero(Genero.MASCULINO);
        dto.setEmail("diego@test.com");
        dto.setSenha("senha123");
        return dto;
    }

    @Test
    void inserir_loginDscordeiro86_atribuiAdmin() {
        UsuarioDTO dto = dtoBase();
        dto.setLogin("dscordeiro86");
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.inserir(dto);

        assertThat(result.getPerfil()).isEqualTo(Perfis.ADMIN);
    }

    @Test
    void inserir_loginComum_atribuiUser() {
        UsuarioDTO dto = dtoBase();
        dto.setLogin("joao");
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.inserir(dto);

        assertThat(result.getPerfil()).isEqualTo(Perfis.USER);
    }

    @Test
    void inserir_codificaSenhaComEncoder() {
        UsuarioDTO dto = dtoBase();
        dto.setLogin("qualquer");
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Usuario result = usuarioService.inserir(dto);

        verify(passwordEncoder).encode(eq("senha123"));
        assertThat(result.getSenha()).isEqualTo("$2a$hash");
    }

    @Test
    void inserir_persisteNoBancoChamandoSave() {
        UsuarioDTO dto = dtoBase();
        dto.setLogin("qualquer");
        when(passwordEncoder.encode(any())).thenReturn("$2a$hash");
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        usuarioService.inserir(dto);

        verify(usuarioRepository).save(any(Usuario.class));
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar falha**

```bash
./mvnw test -Dtest=UsuarioServiceInserirTest -q 2>&1 | tail -20
```

Esperado: FAIL — método `inserir(UsuarioDTO)` não existe ainda.

- [ ] **Step 3: Implementar inserir() e injetar PasswordEncoder no UsuarioService**

Substituir o conteúdo de `src/main/java/br/com/diegocordeiro/dscproject/service/UsuarioService.java`:

```java
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
```

- [ ] **Step 4: Rodar o teste para confirmar aprovação**

```bash
./mvnw test -Dtest=UsuarioServiceInserirTest -q
```

Esperado: `BUILD SUCCESS`, 4 testes passam.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/diegocordeiro/dscproject/service/UsuarioService.java \
        src/test/java/br/com/diegocordeiro/dscproject/service/UsuarioServiceInserirTest.java
git commit -m "feat: add UsuarioService.inserir() with BCrypt and role assignment"
```

---

## Task 3: SecurityConfig + UsuarioController + HomeController + testes

**Files:**
- Modify: `src/main/java/br/com/diegocordeiro/dscproject/config/SecurityConfig.java`
- Modify: `src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/UsuarioController.java`
- Modify: `src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/HomeController.java`
- Create (test): `src/test/java/br/com/diegocordeiro/dscproject/web/sistema/controller/UsuarioControllerInserirTest.java`

- [ ] **Step 1: Escrever os testes do controller**

Criar `src/test/java/br/com/diegocordeiro/dscproject/web/sistema/controller/UsuarioControllerInserirTest.java`:

```java
package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.service.AutorizacaoService;
import br.com.diegocordeiro.dscproject.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerInserirTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private AutorizacaoService autorizacaoService;

    @Test
    void inserir_comDadosValidos_redirecionaComSucesso() throws Exception {
        when(usuarioService.verificarSeExisteUsuario(any())).thenReturn(false);
        when(usuarioService.inserir(any())).thenReturn(new Usuario());

        mockMvc.perform(post("/usuarios/inserir")
                .with(csrf())
                .param("nome", "João Silva")
                .param("genero", "MASCULINO")
                .param("email", "joao@test.com")
                .param("login", "joaosilva")
                .param("senha", "senha123")
                .param("confirmacaoSenha", "senha123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?cadastroSucesso=true"));
    }

    @Test
    void inserir_senhasDivergentes_redirecionaComErro() throws Exception {
        mockMvc.perform(post("/usuarios/inserir")
                .with(csrf())
                .param("nome", "João Silva")
                .param("genero", "MASCULINO")
                .param("email", "joao@test.com")
                .param("login", "joaosilva")
                .param("senha", "senha123")
                .param("confirmacaoSenha", "outrasenha"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void inserir_loginJaCadastrado_redirecionaComErro() throws Exception {
        when(usuarioService.verificarSeExisteUsuario("joaosilva")).thenReturn(true);
        when(usuarioService.verificarSeExisteUsuario("joao@test.com")).thenReturn(false);

        mockMvc.perform(post("/usuarios/inserir")
                .with(csrf())
                .param("nome", "João Silva")
                .param("genero", "MASCULINO")
                .param("email", "joao@test.com")
                .param("login", "joaosilva")
                .param("senha", "senha123")
                .param("confirmacaoSenha", "senha123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void inserir_nomeFaltando_redirecionaComErro() throws Exception {
        mockMvc.perform(post("/usuarios/inserir")
                .with(csrf())
                .param("nome", "")
                .param("genero", "MASCULINO")
                .param("email", "joao@test.com")
                .param("login", "joaosilva")
                .param("senha", "senha123")
                .param("confirmacaoSenha", "senha123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar falha**

```bash
./mvnw test -Dtest=UsuarioControllerInserirTest -q 2>&1 | tail -20
```

Esperado: FAIL — endpoint `POST /usuarios/inserir` não existe ainda.

- [ ] **Step 3: Adicionar /usuarios/inserir ao permitAll() no SecurityConfig**

Substituir o conteúdo de `src/main/java/br/com/diegocordeiro/dscproject/config/SecurityConfig.java`:

```java
package br.com.diegocordeiro.dscproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login",
                    "/usuarios/inserir",
                    "/webjars/**", "/css/**", "/js/**", "/image/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 4: Implementar POST /usuarios/inserir no UsuarioController**

Substituir o conteúdo de `src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/UsuarioController.java`:

```java
package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/listar")
    public String listar() {
        return "sistema/modulos/usuario/listar";
    }

    @PostMapping("/inserir")
    public String inserir(@Valid @ModelAttribute UsuarioDTO dto,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {

        List<String> erros = new ArrayList<>();

        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors()
                .forEach(e -> erros.add(e.getDefaultMessage()));
            redirectAttributes.addFlashAttribute("cadastroErros", erros);
            redirectAttributes.addFlashAttribute("cadastroForm", dto);
            return "redirect:/login";
        }

        if (!dto.getSenha().equals(dto.getConfirmacaoSenha())) {
            erros.add("As senhas não conferem.");
        }
        if (usuarioService.verificarSeExisteUsuario(dto.getLogin())) {
            erros.add("Login já cadastrado.");
        }
        if (usuarioService.verificarSeExisteUsuario(dto.getEmail())) {
            erros.add("E-mail já cadastrado.");
        }

        if (!erros.isEmpty()) {
            redirectAttributes.addFlashAttribute("cadastroErros", erros);
            redirectAttributes.addFlashAttribute("cadastroForm", dto);
            return "redirect:/login";
        }

        usuarioService.inserir(dto);
        return "redirect:/login?cadastroSucesso=true";
    }
}
```

- [ ] **Step 5: Atualizar HomeController para passar Genero[] e cadastroForm ao modelo**

Substituir o conteúdo de `src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/HomeController.java`:

```java
package br.com.diegocordeiro.dscproject.web.sistema.controller;

import br.com.diegocordeiro.dscproject.dto.usuario.UsuarioDTO;
import br.com.diegocordeiro.dscproject.enums.Genero;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "sistema/home";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("generos", Genero.values());
        if (!model.containsAttribute("cadastroForm")) {
            model.addAttribute("cadastroForm", new UsuarioDTO());
        }
        return "login";
    }
}
```

- [ ] **Step 6: Rodar os testes para confirmar aprovação**

```bash
./mvnw test -Dtest=UsuarioControllerInserirTest -q
```

Esperado: `BUILD SUCCESS`, 4 testes passam.

- [ ] **Step 7: Rodar todos os testes existentes para garantir que não há regressão**

```bash
./mvnw test -q 2>&1 | tail -20
```

Esperado: `BUILD SUCCESS`, todos os testes passam.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/br/com/diegocordeiro/dscproject/config/SecurityConfig.java \
        src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/UsuarioController.java \
        src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/HomeController.java \
        src/test/java/br/com/diegocordeiro/dscproject/web/sistema/controller/UsuarioControllerInserirTest.java
git commit -m "feat: add POST /usuarios/inserir endpoint with validation and flash redirect"
```

---

## Task 4: Atualizar login.html — botão Cadastrar + modal + scripts

**Files:**
- Modify: `src/main/resources/templates/login.html`

- [ ] **Step 1: Substituir o conteúdo completo de login.html**

```html
<!DOCTYPE html>
<html lang="pt" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Login - DSCProject</title>
    <link rel="icon" th:href="@{/image/favicon.png}">
    <link th:href="@{/webjars/tabler__core/1.4.0/dist/css/tabler.min.css}" rel="stylesheet">
    <link th:href="@{/css/style.css}" rel="stylesheet">
</head>
<body class="antialiased">
<div class="page page-center">
    <div class="container container-tight py-4">

        <div class="text-center mb-4">
            <img th:src="@{/image/logo.svg}" alt="DSCProject" height="36" class="navbar-brand-image">
        </div>

        <!-- Alertas da página de login -->
        <div th:if="${param.error}" class="alert alert-danger mb-3" role="alert">
            Login ou senha incorretos. Verifique suas credenciais.
        </div>
        <div th:if="${param.logout}" class="alert alert-info mb-3" role="alert">
            Você saiu com sucesso.
        </div>
        <div th:if="${param.cadastroSucesso}" class="alert alert-success mb-3" role="alert">
            Cadastro realizado com sucesso! Faça login para continuar.
        </div>

        <!-- Card de login -->
        <div class="card card-md">
            <div class="card-body">
                <h2 class="h2 text-center mb-4">Acessar o sistema</h2>

                <form th:action="@{/login}" method="post">
                    <div class="mb-3">
                        <label class="form-label" for="username">Login ou E-mail</label>
                        <input type="text"
                               id="username"
                               name="username"
                               class="form-control"
                               placeholder="seu login ou e-mail"
                               autofocus
                               autocomplete="username">
                    </div>
                    <div class="mb-2">
                        <label class="form-label" for="password">Senha</label>
                        <input type="password"
                               id="password"
                               name="password"
                               class="form-control"
                               placeholder="sua senha"
                               autocomplete="current-password">
                    </div>
                    <div class="form-footer">
                        <button type="submit" class="btn btn-primary w-100">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
                                 fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                                 stroke-linejoin="round" class="icon icon-tabler icon-tabler-login-2">
                                <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
                                <path d="M9 8v-2a2 2 0 0 1 2 -2h7a2 2 0 0 1 2 2v12a2 2 0 0 1 -2 2h-7a2 2 0 0 1 -2 -2v-2"/>
                                <path d="M3 12h13l-3 -3"/>
                                <path d="M13 15l3 -3"/>
                            </svg>
                            Entrar
                        </button>
                    </div>
                </form>
            </div>
        </div>

        <!-- Link para cadastro -->
        <div class="text-center text-secondary mt-3">
            Não tem conta?
            <a href="#" data-bs-toggle="modal" data-bs-target="#modalCadastro" class="fw-bold">
                Cadastre-se
            </a>
        </div>

        <div class="text-center text-secondary mt-2">
            DSCProject &copy; <span th:text="${#dates.year(#dates.createNow())}">2026</span>
        </div>
    </div>
</div>

<!-- Modal de Cadastro -->
<div class="modal modal-blur fade" id="modalCadastro" tabindex="-1" role="dialog" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg" role="document">
        <div class="modal-content">

            <div class="modal-header">
                <h5 class="modal-title">
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
                         fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                         stroke-linejoin="round" class="icon icon-tabler icon-tabler-user-plus me-2">
                        <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
                        <path d="M8 7a4 4 0 1 0 8 0a4 4 0 0 0 -8 0"/>
                        <path d="M16 19h6"/>
                        <path d="M19 16v6"/>
                        <path d="M6 21v-2a4 4 0 0 1 4 -4h4"/>
                    </svg>
                    Cadastrar Usuário
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
            </div>

            <form th:action="@{/usuarios/inserir}" method="post">

                <div class="modal-body">

                    <!-- Erros de validação -->
                    <div th:if="${cadastroErros != null}" class="alert alert-danger mb-3" role="alert">
                        <ul class="mb-0">
                            <li th:each="erro : ${cadastroErros}" th:text="${erro}"></li>
                        </ul>
                    </div>

                    <div class="row g-3">

                        <!-- Nome -->
                        <div class="col-12">
                            <label class="form-label required" for="nome">Nome completo</label>
                            <input type="text"
                                   id="nome"
                                   name="nome"
                                   class="form-control"
                                   placeholder="seu nome completo"
                                   th:value="${cadastroForm?.nome}"
                                   maxlength="100"
                                   autocomplete="name">
                        </div>

                        <!-- Gênero e Nascimento -->
                        <div class="col-md-6">
                            <label class="form-label required" for="genero">Gênero</label>
                            <select id="genero" name="genero" class="form-select">
                                <option value="" disabled
                                        th:selected="${cadastroForm?.genero == null}">
                                    Selecione...
                                </option>
                                <option th:each="g : ${generos}"
                                        th:value="${g.name()}"
                                        th:text="${g.descricao}"
                                        th:selected="${cadastroForm?.genero == g}">
                                </option>
                            </select>
                        </div>

                        <div class="col-md-6">
                            <label class="form-label" for="nascimento">Data de nascimento</label>
                            <input type="date"
                                   id="nascimento"
                                   name="nascimento"
                                   class="form-control"
                                   th:value="${cadastroForm?.nascimento != null ?
                                       #dates.format(cadastroForm.nascimento, 'yyyy-MM-dd') : ''}">
                        </div>

                        <!-- E-mail -->
                        <div class="col-12">
                            <label class="form-label required" for="email">E-mail</label>
                            <input type="email"
                                   id="email"
                                   name="email"
                                   class="form-control"
                                   placeholder="seu@email.com"
                                   th:value="${cadastroForm?.email}"
                                   maxlength="512"
                                   autocomplete="email">
                        </div>

                        <!-- Login -->
                        <div class="col-12">
                            <label class="form-label required" for="login">Login</label>
                            <input type="text"
                                   id="login"
                                   name="login"
                                   class="form-control"
                                   placeholder="seu login de acesso"
                                   th:value="${cadastroForm?.login}"
                                   maxlength="40"
                                   autocomplete="username">
                        </div>

                        <!-- Senha e Confirmação -->
                        <div class="col-md-6">
                            <label class="form-label required" for="senha">Senha</label>
                            <input type="password"
                                   id="senha"
                                   name="senha"
                                   class="form-control"
                                   placeholder="mínimo 6 caracteres"
                                   autocomplete="new-password">
                        </div>

                        <div class="col-md-6">
                            <label class="form-label required" for="confirmacaoSenha">Confirmar senha</label>
                            <input type="password"
                                   id="confirmacaoSenha"
                                   name="confirmacaoSenha"
                                   class="form-control"
                                   placeholder="repita a senha"
                                   autocomplete="new-password">
                        </div>

                    </div>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-link link-secondary me-auto"
                            data-bs-dismiss="modal">
                        Cancelar
                    </button>
                    <button type="submit" class="btn btn-primary">
                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
                             fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                             stroke-linejoin="round" class="icon icon-tabler icon-tabler-device-floppy">
                            <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
                            <path d="M6 4h10l4 4v10a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-12a2 2 0 0 1 2 -2"/>
                            <path d="M12 14m-2 0a2 2 0 1 0 4 0a2 2 0 1 0 -4 0"/>
                            <path d="M14 4l0 4l-6 0l0 -4"/>
                        </svg>
                        Cadastrar
                    </button>
                </div>

            </form>
        </div>
    </div>
</div>

<script th:src="@{/webjars/tabler__core/1.4.0/dist/js/tabler.min.js}"></script>

<!-- Reabre a modal automaticamente se houver erros de cadastro -->
<script th:if="${cadastroErros != null}">
    document.addEventListener('DOMContentLoaded', function () {
        new bootstrap.Modal(document.getElementById('modalCadastro')).show();
    });
</script>

</body>
</html>
```

- [ ] **Step 2: Compilar**

```bash
cd /home/dscordeiro/DEV_HOME/DSC_HOME/dscproject-spring-mvc
./mvnw compile -q
```

Esperado: `BUILD SUCCESS`.

- [ ] **Step 3: Subir o servidor e testar manualmente**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Abrir `http://localhost:8080/login` e verificar:
- Botão "Cadastre-se" visível abaixo do card de login
- Clicar em "Cadastre-se" → modal abre com todos os 7 campos
- Submeter com senha divergente → modal reabre com lista de erros e campos preservados
- Submeter com dados válidos → modal fecha, alerta verde "Cadastro realizado!"

Parar o servidor: `Ctrl+C`

- [ ] **Step 4: Rodar toda a suite de testes**

```bash
./mvnw test -q 2>&1 | tail -20
```

Esperado: `BUILD SUCCESS`, todos os testes passam.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/login.html
git commit -m "feat: add cadastro modal with Tabler UI, validation feedback and auto-reopen on error"
```

---

## Checklist de Verificação Final

- [ ] `GET /login` exibe botão "Cadastre-se" abaixo do card
- [ ] Clicar em "Cadastre-se" abre modal responsiva com todos os campos (nome, gênero, nascimento, e-mail, login, senha, confirmar senha)
- [ ] Submeter com campo obrigatório vazio → modal reabre com mensagem de erro
- [ ] Submeter com senhas divergentes → modal reabre com "As senhas não conferem."
- [ ] Submeter com login já cadastrado → modal reabre com "Login já cadastrado."
- [ ] Submeter com e-mail já cadastrado → modal reabre com "E-mail já cadastrado."
- [ ] Campos preservados na modal após erro (exceto senha)
- [ ] Login "dscordeiro86" → perfil ADMIN no banco
- [ ] Login qualquer outro → perfil USER no banco
- [ ] Sucesso → alerta verde na página de login
- [ ] Todos os testes passam (`./mvnw test`)
