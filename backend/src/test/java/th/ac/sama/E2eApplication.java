package th.ac.sama;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.*;
import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** Isolated database for browser tests; never touches local development data. */
public class E2eApplication {
  public static void main(String[] args) throws Exception {
    var data = Files.createTempDirectory("sama-e2e-");
    var pg =
        EmbeddedPostgres.builder()
            .setDataDirectory(data)
            .setPort(0)
            .setServerConfig("listen_addresses", "127.0.0.1")
            .start();
    var control = Path.of("../.local/e2e-db.json");
    Files.createDirectories(control.toAbsolutePath().getParent());
    new ObjectMapper().writeValue(control.toFile(), Map.of("dataDirectory", data.toString()));
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    pg.close();
                  } catch (Exception ignored) {
                  }
                }));
    new SpringApplicationBuilder(SamaApplication.class)
        .profiles("demo")
        .run(
            "--spring.datasource.url=" + pg.getJdbcUrl("postgres", "postgres"),
            "--spring.datasource.username=postgres",
            "--spring.datasource.password=",
            "--server.address=127.0.0.1",
            "--server.port=8081",
            "--debug=false");
  }
}
