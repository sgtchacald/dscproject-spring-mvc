# dscproject-spring-mvc
Este projeto visa fazer tanto o backend quanto o frontend do projeto dscproject no mesmo repositório.

## Desenvolvimento local

### Pré-requisitos
- Java 21+
- Docker

### Credenciais locais (`.env`)

Copie `.env.example` para `.env` e preencha:

```bash
cp .env.example .env
$EDITOR .env
```

O `.env` fica **fora do git** e alimenta tudo:
- o `docker-compose.yml` (interpolação nativa do Compose);
- o `application-dev.properties` — que só tem referências `${MYSQL_*}` / `${GMAIL_*}`,
  sem segredo. Um `EnvironmentPostProcessor` (`DotenvEnvironmentPostProcessor`) carrega
  o `.env` na subida da aplicação, então `./mvnw` e o ▶ da IntelliJ funcionam sem
  configuração de ambiente por Run Configuration.

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

### Hooks de git

```bash
./scripts/instalar-hooks.sh
```

Aponta o clone para `scripts/hooks/`. O `pre-commit` roda
`scripts/lint/verificar_assinaturas_java.py` e barra o commit quando alguma
assinatura de método/construtor fere a convenção de quebra de linha (até 5
parâmetros numa linha; 6+ um por linha).

### Rodando o projeto

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
./mvnw test
```

O `.env` é carregado automaticamente (ver seção _Credenciais locais_). O container
do MySQL precisa estar de pé (`docker compose up -d`).
