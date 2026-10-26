package th.ac.sama;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.*;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** Persistent, loopback-only PostgreSQL for local development without Docker. */
public class LocalApplication {
  public static void main(String[] args) throws Exception {
    var root =
        Path.of(System.getProperty("sama.data", "../.local/postgres")).toAbsolutePath().normalize();
    Files.createDirectories(root);
    var pg =
        EmbeddedPostgres.builder()
            .setDataDirectory(root)
            .setCleanDataDirectory(false)
            .setPort(54329)
            .setServerConfig("listen_addresses", "127.0.0.1")
            .setServerConfig("fsync", "on")
            .setServerConfig("synchronous_commit", "on")
            .setServerConfig("max_connections", "50")
            .start();
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
            "--debug=false");
  }
}
