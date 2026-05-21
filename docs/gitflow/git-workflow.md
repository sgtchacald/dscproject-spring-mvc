# Fluxo de Commits e Merges

Guia passo a passo para o fluxo de desenvolvimento, release e deploy em produção.

---

## 1. Trabalho Normal (Feature Branch)

Crie a branch a partir da `main` seguindo a convenção de nomenclatura:

```bash
git checkout main
git checkout -b feature/nome_funcionalidade+breve-descricao+DDMMAAAAHHMM
```

**Convenção do timestamp no nome da branch:**

| Variável | Significado |
|----------|-------------|
| `DD`     | Dia         |
| `MM`     | Mês         |
| `AAAA`   | Ano         |
| `HH`     | Hora        |
| `MM`     | Minuto      |

Faça seus commits e suba a branch normalmente:

```bash
git add .
git commit -m "feat: minha alteração"
git push origin feature/minha-feature
```

---

## 2. Criar a Release

A partir da branch da tarefa, crie a branch de release com o próximo número de versão após a última release:

```bash
git checkout -b release/1.0.0
```

### Atualizar a versão nos arquivos do projeto

**Back-end — `pom.xml`:**
```xml
<version>1.0.0-SNAPSHOT</version>
```

**Back-end — `lgpd.portal.utils.AppVersao.java`:**
```java
public static String VERSAO = "1.0.0";
```

**Front-end — `package.json`:**
```json
"version": "1.0.0"
```

Commite e suba a branch de release:

```bash
git add .
git commit -m "chore: Atualizando o número de versão para 1.0.0"
git push origin release/1.0.0
```

---

## 3. Merge da Release para Homologação

```bash
git checkout homologacao
git merge release/1.0.0
git push origin homologacao
```

> Faça o deploy para homologação e aguarde a aprovação.

---

## 4. Aprovado — Criar Tag de Produção

Com a release aprovada em homologação, crie a tag de produção a partir da branch de release:

```bash
git checkout release/1.0.0
```

Verifique a última tag criada:

```bash
git tag --sort=-v:refname | head -5
```

Crie a nova tag:

```bash
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

---

## 5. Merge da Release para Main

```bash
git checkout main
git merge release/1.0.0
git push origin main
```

---

## Resumo do Fluxo

```
feature/... ──► release/x.x.x ──► homologacao ──► (aprovação) ──► tag vx.x.x + merge main
```
