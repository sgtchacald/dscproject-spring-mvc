# dscproject-spring-mvc
Este projeto visa fazer tanto o backend quanto o frontend do projeto dscproject no mesmo repositório.

## Desenvolvimento local

### Pré-requisitos
- Java 21+
- Docker

### Subindo o banco de dados

O banco MySQL roda em container Docker. Para subir manualmente:

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

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
