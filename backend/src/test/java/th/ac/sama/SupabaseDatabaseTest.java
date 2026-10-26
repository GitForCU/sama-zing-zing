package th.ac.sama;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Exercises the Supabase schema/profile on isolated PostgreSQL, without cloud credentials. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "sama.auth.mode=local")
@ActiveProfiles({"supabase", "demo"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SupabaseDatabaseTest {
  static EmbeddedPostgres postgres;

  static {
    try {
      postgres =
          EmbeddedPostgres.builder()
              .setPort(0)
              .setServerConfig("listen_addresses", "127.0.0.1")
              .start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
    properties.add("spring.datasource.username", () -> "postgres");
    properties.add("spring.datasource.password", () -> "");
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;

  @AfterAll
  static void close() throws Exception {
    postgres.close();
  }

  @Test
  void migrationsEntitiesAndLoginSessionsUseThePrivateSchema() throws Exception {
    assertThat(jdbc.queryForObject("select current_schema()", String.class)).isEqualTo("sama");
    assertThat(jdbc.queryForObject("select to_regclass('public.app_users')::text", String.class))
        .isNull();
    assertThat(jdbc.queryForObject("select count(*) from sama.app_users", Integer.class))
        .isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from sama.flyway_schema_history where success and version is not"
                    + " null",
                Integer.class))
        .isEqualTo(3);
    assertThat(jdbc.queryForObject("select count(*) from sama.activity_attachments", Integer.class))
        .isZero();

    var client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    var csrfResponse =
        client.send(request("/auth/csrf").GET().build(), HttpResponse.BodyHandlers.ofString());
    assertThat(csrfResponse.statusCode()).isEqualTo(200);
    var csrf = json.readTree(csrfResponse.body());
    var login =
        client.send(
            request("/auth/login")
                .header("Content-Type", "application/json")
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        json.writeValueAsString(
                            Map.of("email", "student@sama.local", "password", "SamaDemo123!"))))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(login.statusCode()).describedAs(login.body()).isEqualTo(200);
    var me = client.send(request("/me").GET().build(), HttpResponse.BodyHandlers.ofString());
    assertThat(me.statusCode()).describedAs(me.body()).isEqualTo(200);
    assertThat(json.readTree(me.body()).get("email").asText()).isEqualTo("student@sama.local");
    assertThat(jdbc.queryForObject("select count(*) from sama.spring_session", Integer.class))
        .isPositive();
  }

  private HttpRequest.Builder request(String path) {
    return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
        .timeout(Duration.ofSeconds(20));
  }
}
