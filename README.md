# dscproject-spring-mvc
Este projeto visa fazer tanto o backend quanto o frontend do projeto dscproject no mesmo repositório.

## Desenvolvimento local

### Pré-requisitos
- Java 21+
- Docker

### Credenciais locais (`.env`)

Copie `.env.example` para `.env` e preencha. O `.env` fica **fora do git** e alimenta
tanto o `docker-compose.yml` quanto o `application-dev.properties` (que só tem
referências `${VAR}`, sem segredo).

```bash
cp .env.example .env
$EDITOR .env
```

### Subindo o banco de dados

O banco MySQL roda em container Docker (lê `MYSQL_*` do `.env`). Para subir manualmente:

```bash
docker compose up -d
```

Para parar (dados são preservados no volume):

```bash
docker compose down
```

### Configuração do IntelliJ (Run/Debug automático)

Para o banco subir automaticamente ao apertar ▶ ou 🐛:

1. `Run → Edit Configurations...`
2. Selecione a configuração do projeto (Spring Boot)
3. Em **Before launch** → `+` → **Run External Tool** → `+`
4. Preencha:
   - **Name:** `Docker: MySQL up`
   - **Program:** `docker`
   - **Arguments:** `compose -f $ProjectFileDir$/docker-compose.yml up -d`
   - **Working directory:** `$ProjectFileDir$`
5. Clique OK

Repita para a configuração de Debug se existir separada.

### Rodando o projeto

Pela linha de comando, use o `./dev.sh` (exporta o `.env` e chama o Maven wrapper):

```bash
./dev.sh spring-boot:run -Dspring-boot.run.profiles=dev
./dev.sh test
```

Na IntelliJ, a Run Configuration precisa das variáveis do `.env` no ambiente
(plugin **EnvFile** apontando para `.env`, ou copiando os valores em
*Environment variables*). Sem elas, os `${MYSQL_*}` / `${GMAIL_*}` do
`application-dev.properties` não resolvem.
