# Cadastro de Usuário — Design Spec

**Data:** 2026-05-18
**Projeto:** dscproject-spring-mvc (Spring Boot 4.0.5 / Thymeleaf / Tabler UI 1.4.0)

---

## Contexto

O sistema já possui login com Spring Security (session-based). O próximo passo é permitir que novos usuários se cadastrem diretamente pela tela de login, sem sair do padrão Thymeleaf + Tabler UI do projeto. O cadastro deve seguir as mesmas regras de negócio do backend REST existente (dscproject-backend), inclusive BCrypt para senha e auto-atribuição de perfil.

---

## Objetivo

Adicionar um botão "Cadastrar" na página de login que abre uma modal responsiva (Tabler UI / Bootstrap) para cadastro de novos usuários, com validação server-side e feedback inline na própria modal.

---

## Fluxo

```
GET /login
  └── login.html renderiza:
        ├── Formulário de login (existente)
        ├── Botão "Cadastrar" → abre modal #modalCadastro (Tabler/Bootstrap)
        └── Modal com form: POST /usuarios/inserir

POST /usuarios/inserir (UsuarioController — público, sem autenticação)
  ├── Bean Validation (@Valid UsuarioDTO + BindingResult)
  ├── Senhas conferem? (controller)
  ├── Login/e-mail únicos? (UsuarioService via findByCredenciaisList)
  │
  ├── ERRO → RedirectAttributes.addFlashAttribute("cadastroErros", List<String>)
  │          RedirectAttributes.addFlashAttribute("cadastroForm", UsuarioDTO)
  │          redirect:/login  →  login.html detecta flash, reabre modal automaticamente
  │
  └── SUCESSO → BCrypt encode senha
                Perfil: ADMIN se login == "dscordeiro86", USER demais
                usuarioService.inserir(usuario)
                redirect:/login?cadastroSucesso=true
                login.html exibe alerta: "Cadastro realizado! Faça login."
```

---

## Campos do Formulário

| Campo | Nome HTML | Tipo | Validação |
|-------|-----------|------|-----------|
| Nome | `nome` | text | `@NotBlank`, máx 100 chars |
| Gênero | `genero` | select | `@NotNull` (FEMININO / MASCULINO / OUTRO) |
| Data de Nascimento | `nascimento` | date | opcional |
| E-mail | `email` | email | `@NotBlank`, `@Email`, único no banco |
| Login | `login` | text | `@NotBlank`, máx 40 chars, único no banco |
| Senha | `senha` | password | `@NotBlank`, `@Size(min=6)` |
| Confirmar Senha | `confirmacaoSenha` | password | deve ser igual a `senha` (validado no controller) |

---

## Regras de Negócio

- **Perfil:** `ADMIN` se `login.equals("dscordeiro86")`, `USER` para todos os demais
- **Senha:** codificada com BCrypt via o `PasswordEncoder` bean já existente no `SecurityConfig`
- **Unicidade:** verificada via `usuarioRepository.findByCredenciaisList(valor)` já existente
- **Endpoint público:** `/usuarios/inserir` deve ser adicionado à lista de rotas públicas no `SecurityConfig`
- **Nomenclatura padrão do projeto:** `inserir`, `editar`, `listar`, `excluir`

---

## Arquivos Modificados

| Arquivo | O que muda |
|---------|-----------|
| `dto/usuario/UsuarioDTO.java` | Adicionar `confirmacaoSenha` (transient, não persiste); adicionar anotações `@NotBlank`, `@Email`, `@Size` nos campos |
| `service/UsuarioService.java` | Adicionar `inserir(UsuarioDTO dto, PasswordEncoder encoder)`: valida unicidade, codifica senha, atribui perfil, chama `usuarioRepository.save()` |
| `web/sistema/controller/UsuarioController.java` | Adicionar `POST /usuarios/inserir`: recebe `@Valid UsuarioDTO`, trata `BindingResult`, valida senhas, chama service, redireciona com flash |
| `config/SecurityConfig.java` | Adicionar `/usuarios/inserir` nas rotas públicas (`permitAll()`) |
| `templates/login.html` | Botão "Cadastrar" + modal Tabler com form completo + script de reabertura + alertas de sucesso/erro |

---

## Comportamento da Modal

- **Reabertura automática em erro:** `login.html` verifica `th:if="${cadastroErros != null}"` e executa `new bootstrap.Modal(document.getElementById('modalCadastro')).show()` inline (Tabler já carrega Bootstrap JS)
- **Campos preservados:** os valores do `cadastroForm` (flash) são pré-preenchidos com `th:value`
- **Erros exibidos:** lista de erros no topo da modal com `alert alert-danger` do Tabler; campos inválidos recebem classe `is-invalid`
- **Sucesso:** modal fecha, alerta verde aparece na página de login principal: `th:if="${param.cadastroSucesso}"`

---

## Segurança

- Endpoint `POST /usuarios/inserir` é público (sem autenticação) — mesmo comportamento do backend REST existente (`/usuarios/inserir-usuario-site`)
- CSRF protegido automaticamente via `th:action="@{/usuarios/inserir}"` + Thymeleaf Security extras
- Senha nunca trafega em texto plano além do POST (HTTPS em produção)

---

## Testes

- **Unit:** `UsuarioServiceTest` — `inserir()` com login "dscordeiro86" → ADMIN; demais → USER; login duplicado → exceção; email duplicado → exceção
- **Integration:** `UsuarioCadastroControllerTest` (`@SpringBootTest` + `MockMvc`) — POST com dados válidos → redirect com `cadastroSucesso`; POST com senha divergente → flash com erro; POST com login duplicado → flash com erro; GET /usuarios/inserir → 302 (não existe, só POST)
