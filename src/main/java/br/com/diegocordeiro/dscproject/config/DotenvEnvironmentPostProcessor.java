package br.com.diegocordeiro.dscproject.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Carrega um arquivo {@code .env} (não versionado, na raiz do projeto) como fonte
 * de propriedades. Assim os {@code ${MYSQL_*}} / {@code ${GMAIL_*}} do
 * {@code application-dev.properties} resolvem tanto pelo {@code ./mvnw} quanto
 * pelo ▶ da IntelliJ, sem configuração de ambiente por Run Configuration.
 *
 * <p>Registrado em
 * {@code META-INF/spring/org.springframework.boot.EnvironmentPostProcessor.imports}.
 * Fica com a MENOR precedência: variável de ambiente real ou {@code -D} sempre vence.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "dotenvFile";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path env = locate();
        if (env == null) {
            return;
        }
        Map<String, Object> values = parse(env);
        if (!values.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, values));
        }
    }

    private Path locate() {
        for (String candidate : new String[] {".env", "../.env"}) {
            Path p = Path.of(candidate);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    private Map<String, Object> parse(Path file) {
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int eq = line.indexOf('=');
                String key = line.substring(0, eq).strip();
                String value = line.substring(eq + 1).strip();
                if ((value.startsWith("\"") && value.endsWith("\"") && value.length() > 1)
                        || (value.startsWith("'") && value.endsWith("'") && value.length() > 1)) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isEmpty()) {
                    map.put(key, value);
                }
            }
        } catch (IOException e) {
            // .env ilegível: segue sem ele (o app falha adiante com mensagem clara
            // de credencial ausente, que é o comportamento desejado).
        }
        return map;
    }
}
