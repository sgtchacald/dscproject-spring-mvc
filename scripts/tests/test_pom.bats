#!/usr/bin/env bats

setup() {
  source "${BATS_TEST_DIRNAME}/../lib/gitflow/pom.sh"
  TEST_POM="$(mktemp /tmp/test_pom_XXXX.xml)"
  cat > "$TEST_POM" <<'POMEOF'
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.5</version>
    <relativePath/>
  </parent>
  <groupId>br.com.diegocordeiro.dscproject</groupId>
  <artifactId>dscproject</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</project>
POMEOF
}

teardown() {
  rm -f "$TEST_POM"
}

@test "atualiza versão do projeto no pom.xml" {
  atualizar_versao_pom "1.2.3" "$TEST_POM"
  grep -q "<version>1.2.3</version>" "$TEST_POM"
}

@test "não altera versão do parent (Spring Boot) no pom.xml" {
  atualizar_versao_pom "1.2.3" "$TEST_POM"
  grep -q "<version>4.0.5</version>" "$TEST_POM"
}

@test "substitui versão SNAPSHOT corretamente" {
  atualizar_versao_pom "2.0.0" "$TEST_POM"
  grep -q "<version>2.0.0</version>" "$TEST_POM"
  ! grep -q "SNAPSHOT" "$TEST_POM"
}

@test "falha com mensagem clara quando pom.xml não existe" {
  run atualizar_versao_pom "1.0.0" "/tmp/nao_existe_${RANDOM}.xml"
  [ "$status" -ne 0 ]
  [[ "$output" == *"não encontrado"* ]]
}

@test "falha se versão não foi aplicada (pom.xml corrompido)" {
  local pom_sem_versao
  pom_sem_versao="$(mktemp /tmp/test_pom_broken_XXXX.xml)"
  echo "<project><groupId>test</groupId></project>" > "$pom_sem_versao"

  run atualizar_versao_pom "1.0.0" "$pom_sem_versao"
  [ "$status" -ne 0 ]

  rm -f "$pom_sem_versao"
}