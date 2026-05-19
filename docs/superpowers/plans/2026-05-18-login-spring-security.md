# Login com Spring Security Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar autenticação session-based com Spring Security no projeto Spring MVC (Thymeleaf), usando o banco de dados compartilhado com o backend REST (usuários com senha BCrypt já existentes).

**Architecture:** Spring Security form login com sessão HTTP — padrão correto para apps Thymeleaf MVC (não JWT, que é exclusivo para APIs REST stateless). A entidade `Usuario` implementa `UserDetails` e é carregada pelo `AutorizacaoService` via `UsuarioRepository`. Todas as rotas admin são protegidas; `/login`, `/webjars/**`, `/css/**`, `/js/**` e `/image/**` são públicas.

**Tech Stack:** Spring Boot 4.0.5 · Spring Security 7.x · Thymeleaf · thymeleaf-extras-springsecurity6 · BCrypt · JPA/Hibernate · MySQL · Tabler UI 1.4.0 · Lombok · JUnit 5 · MockMvc

---

## Mapa de Arquivos

| Ação | Arquivo |
|------|---------|
| Modificar | `pom.xml` |
| Modificar | `src/main/java/br/com/diegocordeiro/dscproject/model/Usuario.java` |
| Modificar | `src/main/java/br/com/diegocordeiro/dscproject/service/UsuarioService.java` |
| Criar | `src/main/java/br/com/diegocordeiro/dscproject/service/AutorizacaoService.java` |
| Criar | `src/main/java/br/com/diegocordeiro/dscproject/config/SecurityConfig.java` |
| Modificar | `src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/HomeController.java` |
| Criar | `src/main/resources/templates/login.html` |
| Modificar | `src/main/resources/templates/sistema/template-admin/fragments/sidebar.html` |
| Modificar | `src/main/resources/templates/sistema/template-admin/fragments/header.html` |
| Criar | `src/test/java/br/com/diegocordeiro/dscproject/service/AutorizacaoServiceTest.java` |
| Criar | `src/test/java/br/com/diegocordeiro/dscproject/config/SecurityConfigTest.java` |

---

## Task 1: Adicionar Dependências ao pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Adicionar as três dependências no bloco `<dependencies>`**

Inserir logo após a dependência do `spring-boot-starter-webmvc`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

- [ ] **Step 2: Verificar que o projeto compila sem erros**

```bash
cd /home/dscordeiro/DEV_HOME/DSC_HOME/dscproject-spring-mvc
./mvnw compile -q
```

Esperado: `BUILD SUCCESS` (sem erros de compilação).

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add spring-security, thymeleaf-security extras and validation dependencies"
```

---

## Task 2: Usuario implementa UserDetails

**Files:**
- Modify: `src/main/java/br/com/diegocordeiro/dscproject/model/Usuario.java`
- Test: `src/test/java/br/com/diegocordeiro/dscproject/model/UsuarioUserDetailsTest.java`

- [ ] **Step 1: Escrever o teste que verifica UserDetails**

Criar `src/test/java/br/com/diegocordeiro/dscproject/model/UsuarioUserDetailsTest.java`:

```java
package br.com.diegocordeiro.dscproject.model;

import br.com.diegocordeiro.dscproject.enums.Perfis;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioUserDetailsTest {

    @Test
    void getPassword_deveRetornarSenha() {
        Usuario usuario = new Usuario();
        usuario.setSenha("$2a$10$hash");

        assertThat(usuario.getPassword()).isEqualTo("$2a$10$hash");
    }

    @Test
    void getUsername_deveRetornarLogin() {
        Usuario usuario = new Usuario();
        usuario.setLogin("chacalsgt");

        assertThat(usuario.getUsername()).isEqualTo("chacalsgt");
    }

    @Test
    void getAuthorities_adminDeveRetornarDoisPapeis() {
        Usuario usuario = new Usuario();
        usuario.setPerfil(Perfis.ADMIN);

        Collection<? extends GrantedAuthority> authorities = usuario.getAuthorities();

        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void getAuthorities_userDeveRetornarUmPapel() {
        Usuario usuario = new Usuario();
        usuario.setPerfil(Perfis.USER);

        Collection<? extends GrantedAuthority> authorities = usuario.getAuthorities();

        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar falha**

```bash
./mvnw test -Dtest=UsuarioUserDetailsTest -q
```

Esperado: FAIL — `Usuario` não implementa `UserDetails`.

- [ ] **Step 3: Implementar UserDetails em Usuario.java**

Substituir o conteúdo de `src/main/java/br/com/diegocordeiro/dscproject/model/Usuario.java`:

```java
package br.com.diegocordeiro.dscproject.model;

import br.com.diegocordeiro.dscproject.enums.Genero;
import br.com.diegocordeiro.dscproject.enums.Perfis;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

@Data @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
@Entity
@Table(name="USUARIOS")
//@Audited
@EntityListeners(AuditingEntityListener.class)
public class Usuario implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USU_ID", nullable = false)
    private Long id;

    @Column(name = "USU_NOME", length = 100, nullable = false)
    private String nome;

    @Column(name = "USU_GENERO", length = 1, nullable = false)
    private Genero genero;

    @Column(name = "USU_DT_NASCIMENTO", nullable = true)
    private Date nascimento;

    @Column(name = "USU_EMAIL", length = 512, nullable = false, unique = true)
    private String email;

    @Column(name = "USU_LOGIN", length = 40, nullable = false, unique = true)
    private String login;

    @Column(name = "USU_SENHA", length = 1024, nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "USU_PERFIL", nullable = false)
    private Perfis perfil;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.perfil == Perfis.ADMIN) {
            return List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        return this.login;
    }
}
```

- [ ] **Step 4: Rodar o teste para confirmar aprovação**

```bash
./mvnw test -Dtest=UsuarioUserDetailsTest -q
```

Esperado: `BUILD SUCCESS`, todos os 4 testes passam.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/diegocordeiro/dscproject/model/Usuario.java \
        src/test/java/br/com/diegocordeiro/dscproject/model/UsuarioUserDetailsTest.java
git commit -m "feat: Usuario implements UserDetails for Spring Security integration"
```

---

## Task 3: Corrigir UsuarioService (injeção ausente)

**Files:**
- Modify: `src/main/java/br/com/diegocordeiro/dscproject/service/UsuarioService.java`

- [ ] **Step 1: Adicionar `@Autowired` no repositório**

Substituir o conteúdo de `UsuarioService.java`:

```java
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
```

- [ ] **Step 2: Compilar para confirmar que não há erros**

```bash
./mvnw compile -q
```

Esperado: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/diegocordeiro/dscproject/service/UsuarioService.java
git commit -m "fix: add missing @Autowired on UsuarioRepository in UsuarioService"
```

---

## Task 4: Criar AutorizacaoService (UserDetailsService)

**Files:**
- Create: `src/main/java/br/com/diegocordeiro/dscproject/service/AutorizacaoService.java`
- Test: `src/test/java/br/com/diegocordeiro/dscproject/service/AutorizacaoServiceTest.java`

- [ ] **Step 1: Escrever o teste**

Criar `src/test/java/br/com/diegocordeiro/dscproject/service/AutorizacaoServiceTest.java`:

```java
package br.com.diegocordeiro.dscproject.service;

import br.com.diegocordeiro.dscproject.model.Usuario;
import br.com.diegocordeiro.dscproject.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizacaoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AutorizacaoService autorizacaoService;

    @Test
    void loadUserByUsername_encontraPorLogin() {
        Usuario usuario = new Usuario();
        usuario.setLogin("chacalsgt");
        usuario.setSenha("$2a$10$hash");
        when(usuarioRepository.findByLoginOrEmail("chacalsgt", "chacalsgt")).thenReturn(usuario);

        UserDetails result = autorizacaoService.loadUserByUsername("chacalsgt");

        assertThat(result.getUsername()).isEqualTo("chacalsgt");
    }

    @Test
    void loadUserByUsername_encontraPorEmail() {
        Usuario usuario = new Usuario();
        usuario.setLogin("chacalsgt");
        usuario.setEmail("sgt.chacal.d@gmail.com");
        usuario.setSenha("$2a$10$hash");
        when(usuarioRepository.findByLoginOrEmail("sgt.chacal.d@gmail.com", "sgt.chacal.d@gmail.com"))
            .thenReturn(usuario);

        UserDetails result = autorizacaoService.loadUserByUsername("sgt.chacal.d@gmail.com");

        assertThat(result.getUsername()).isEqualTo("chacalsgt");
    }

    @Test
    void loadUserByUsername_lancaExcecaoSeNaoEncontrar() {
        when(usuarioRepository.findByLoginOrEmail("naoexiste", "naoexiste")).thenReturn(null);

        assertThatThrownBy(() -> autorizacaoService.loadUserByUsername("naoexiste"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("naoexiste");
    }
}
```

- [ ] **Step 2: Rodar o teste para confirmar falha**

```bash
./mvnw test -Dtest=AutorizacaoServiceTest -q
```

Esperado: FAIL — `AutorizacaoService` não existe.

- [ ] **Step 3: Criar AutorizacaoService.java**

Criar `src/main/java/br/com/diegocordeiro/dscproject/service/AutorizacaoService.java`:

```java
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
```

- [ ] **Step 4: Rodar o teste para confirmar aprovação**

```bash
./mvnw test -Dtest=AutorizacaoServiceTest -q
```

Esperado: `BUILD SUCCESS`, todos os 3 testes passam.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/diegocordeiro/dscproject/service/AutorizacaoService.java \
        src/test/java/br/com/diegocordeiro/dscproject/service/AutorizacaoServiceTest.java
git commit -m "feat: add AutorizacaoService implementing UserDetailsService"
```

---

## Task 5: Criar SecurityConfig

**Files:**
- Create: `src/main/java/br/com/diegocordeiro/dscproject/config/SecurityConfig.java`
- Test: `src/test/java/br/com/diegocordeiro/dscproject/config/SecurityConfigTest.java`

- [ ] **Step 1: Escrever o teste de segurança**

Criar `src/test/java/br/com/diegocordeiro/dscproject/config/SecurityConfigTest.java`:

```java
package br.com.diegocordeiro.dscproject.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acessoRaiz_semAutenticacao_redirecionaParaLogin() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void paginaLogin_semAutenticacao_retornaOk() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }

    @Test
    void webjars_semAutenticacao_retornaOkOuNotFound() throws Exception {
        // Deve ser acessível sem login (status != 302 redirect to login)
        var result = mockMvc.perform(get("/webjars/tabler__core/1.4.0/dist/css/tabler.min.css"))
            .andReturn();
        assertThat(result.getResponse().getStatus()).isNotEqualTo(302);
    }
}
```

Adicionar import estático no topo do arquivo (após os imports existentes):

```java
import static org.assertj.core.api.Assertions.assertThat;
```

- [ ] **Step 2: Rodar o teste para confirmar falha**

```bash
./mvnw test -Dtest=SecurityConfigTest -q
```

Esperado: FAIL — Spring Security não configurado ainda (a rota raiz não redireciona para login).

- [ ] **Step 3: Criar SecurityConfig.java**

Criar `src/main/java/br/com/diegocordeiro/dscproject/config/SecurityConfig.java`:

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
                .requestMatchers("/login", "/webjars/**", "/css/**", "/js/**", "/image/**").permitAll()
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

- [ ] **Step 4: Rodar o teste para confirmar aprovação**

```bash
./mvnw test -Dtest=SecurityConfigTest -q
```

Esperado: `BUILD SUCCESS`, todos os 3 testes passam.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/diegocordeiro/dscproject/config/SecurityConfig.java \
        src/test/java/br/com/diegocordeiro/dscproject/config/SecurityConfigTest.java
git commit -m "feat: add Spring Security config with form login and BCrypt password encoder"
```

---

## Task 6: Adicionar rota GET /login no HomeController

**Files:**
- Modify: `src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/HomeController.java`

- [ ] **Step 1: Adicionar mapeamento GET /login**

Substituir o conteúdo de `HomeController.java`:

```java
package br.com.diegocordeiro.dscproject.web.sistema.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "sistema/home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
```

- [ ] **Step 2: Compilar**

```bash
./mvnw compile -q
```

Esperado: `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/diegocordeiro/dscproject/web/sistema/controller/HomeController.java
git commit -m "feat: add GET /login route in HomeController"
```

---

## Task 7: Criar template login.html (Tabler UI)

**Files:**
- Create: `src/main/resources/templates/login.html`

- [ ] **Step 1: Criar a página de login**

Criar `src/main/resources/templates/login.html`:

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

        <div class="card card-md">
            <div class="card-body">
                <h2 class="h2 text-center mb-4">Acessar o sistema</h2>

                <div th:if="${param.error}" class="alert alert-danger mb-3" role="alert">
                    Login ou senha incorretos. Verifique suas credenciais.
                </div>
                <div th:if="${param.logout}" class="alert alert-info mb-3" role="alert">
                    Você saiu com sucesso.
                </div>

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

        <div class="text-center text-secondary mt-3">
            DSCProject &copy; <span th:text="${#dates.year(#dates.createNow())}">2026</span>
        </div>
    </div>
</div>
<script th:src="@{/webjars/tabler__core/1.4.0/dist/js/tabler.min.js}"></script>
</body>
</html>
```

> **Nota:** `th:action="@{/login}"` com `thymeleaf-extras-springsecurity6` no classpath injeta automaticamente o token CSRF oculto no form.

- [ ] **Step 2: Subir o servidor e testar manualmente**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Abrir no navegador: `http://localhost:8080/login`

Verificar:
- Página renderiza sem erros
- Formulário exibe campos de login e senha
- Botão "Entrar" visível
- Tentar login com credenciais válidas do banco → redireciona para `/`
- Tentar login com credenciais inválidas → mostra mensagem de erro

Parar o servidor: `Ctrl+C`

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/login.html
git commit -m "feat: add Tabler UI login page with error and logout feedback"
```

---

## Task 8: Adicionar logout e usuário logado no sidebar/header

**Files:**
- Modify: `src/main/resources/templates/sistema/template-admin/fragments/sidebar.html`
- Modify: `src/main/resources/templates/sistema/template-admin/fragments/header.html`

- [ ] **Step 1: Adicionar namespace sec e botão de logout no sidebar**

No arquivo `sidebar.html`, adicionar o namespace `sec` na tag `<html>` e adicionar o bloco de logout ao final do `<ul class="navbar-nav">`, antes do `</div>` que fecha o `collapse`:

Substituir a tag de abertura `<html>`:
```html
<html xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```

Adicionar antes do `</ul>` que fecha o `navbar-nav`:
```html
<li class="nav-item mt-auto">
    <div class="nav-link text-secondary">
        <span class="nav-link-icon d-md-none d-lg-inline-block">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
                 fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                 stroke-linejoin="round" class="icon icon-tabler icon-tabler-user-circle">
                <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
                <path d="M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0 -18 0"/>
                <path d="M12 10m-3 0a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"/>
                <path d="M6.168 18.849a4 4 0 0 1 3.832 -2.849h4a4 4 0 0 1 3.834 2.855"/>
            </svg>
        </span>
        <span class="nav-link-title" sec:authentication="name">Usuário</span>
    </div>
</li>
<li class="nav-item">
    <form th:action="@{/logout}" method="post" class="nav-link p-0">
        <button type="submit" class="btn btn-outline-danger btn-sm w-100">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
                 fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                 stroke-linejoin="round" class="icon icon-tabler icon-tabler-logout">
                <path stroke="none" d="M0 0h24v24H0z" fill="none"/>
                <path d="M14 8v-2a2 2 0 0 0 -2 -2h-7a2 2 0 0 0 -2 2v12a2 2 0 0 0 2 2h7a2 2 0 0 0 2 -2v-2"/>
                <path d="M9 12h12l-3 -3"/>
                <path d="M18 15l3 -3"/>
            </svg>
            Sair
        </button>
    </form>
</li>
```

- [ ] **Step 2: Compilar para confirmar sem erros**

```bash
./mvnw compile -q
```

Esperado: `BUILD SUCCESS`.

- [ ] **Step 3: Testar manualmente o logout**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Verificar:
- Após login, sidebar exibe nome do usuário logado
- Botão "Sair" aparece no sidebar
- Clicar em "Sair" → redireciona para `/login?logout=true`
- Página de login exibe mensagem "Você saiu com sucesso"
- Tentar acessar `/` sem login → redireciona para `/login`

Parar o servidor: `Ctrl+C`

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/sistema/template-admin/fragments/sidebar.html \
        src/main/resources/templates/sistema/template-admin/fragments/header.html
git commit -m "feat: show logged user name and logout button in sidebar"
```

---

## Task 9: Rodar a suite de testes completa e verificar

- [ ] **Step 1: Rodar todos os testes**

```bash
./mvnw test
```

Esperado: `BUILD SUCCESS` — todos os testes passam.

- [ ] **Step 2: Se houver falhas, investigar antes de prosseguir**

Verificar a causa no stack trace. Causas comuns:
- `DataSource` não disponível nos testes de integração → adicionar `@MockBean` nos repositórios ou usar `@WebMvcTest` em vez de `@SpringBootTest`
- Banco de dados de desenvolvimento não disponível no CI → configurar `application-test.properties` com H2 in-memory

- [ ] **Step 3: Commit final de integração (se necessário)**

```bash
git add .
git commit -m "test: ensure all security tests pass after login feature implementation"
```

---

## Checklist de Verificação Final

Antes de considerar a feature completa:

- [ ] `GET /login` renderiza a página de login sem autenticação
- [ ] `GET /` sem autenticação redireciona para `/login`
- [ ] Login com credenciais válidas do banco redireciona para `/`
- [ ] Login com credenciais inválidas exibe alerta de erro na página
- [ ] Logout via botão "Sair" invalida a sessão e redireciona para `/login?logout=true`
- [ ] Após logout, acessar `/` volta a redirecionar para `/login`
- [ ] Nome do usuário logado aparece no sidebar
- [ ] Recursos estáticos (`/webjars/**`, `/css/**`, `/image/**`) acessíveis sem login
- [ ] Todos os testes unitários e de integração passam (`./mvnw test`)
