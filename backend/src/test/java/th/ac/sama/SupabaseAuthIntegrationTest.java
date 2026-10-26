package th.ac.sama;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import th.ac.sama.identity.infrastructure.UserRepository;
import th.ac.sama.shared.security.*;

/**
 * Real app/HTTP/PostgreSQL with a loopback Supabase Auth protocol stub; no cloud account needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
@Import(SupabaseAuthIntegrationTest.TestTime.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SupabaseAuthIntegrationTest {
  static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-000000000001");
  static final UUID TEACHER = UUID.fromString("00000000-0000-0000-0000-000000000002");
  static final UUID UNLINKED = UUID.fromString("00000000-0000-0000-0000-000000000003");
  static final ObjectMapper JSON = new ObjectMapper();
  static final AtomicInteger logouts = new AtomicInteger();
  static final AtomicBoolean failLogout = new AtomicBoolean();
  static EmbeddedPostgres postgres;
  static HttpServer authServer;

  static {
    try {
      postgres =
          EmbeddedPostgres.builder()
              .setPort(0)
              .setServerConfig("listen_addresses", "127.0.0.1")
              .start();
      authServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      authServer.createContext(
          "/auth/v1/token",
          exchange -> {
            int status = 200;
            Map<String, Object> result;
            var input = JSON.readTree(exchange.getRequestBody());
            String email = input.path("email").asText();
            String password = input.path("password").asText();
            if (!"POST".equals(exchange.getRequestMethod())
                || !"grant_type=password".equals(exchange.getRequestURI().getRawQuery())
                || !"sb_publishable_test".equals(exchange.getRequestHeaders().getFirst("apikey"))) {
              status = 400;
              result = Map.of("message", "Bad protocol");
            } else if (email.equals("outage@sama.local")) {
              status = 503;
              result = Map.of("message", "UPSTREAM_PRIVATE_DETAILS_MUST_NOT_LEAK");
            } else if (!password.equals("SupabaseOnly123!") && !password.equals("Unlinked123!")) {
              status = 400;
              result = Map.of("error_code", "invalid_credentials");
            } else {
              UUID subject =
                  password.equals("Unlinked123!")
                      ? UNLINKED
                      : email.equals("teacher@sama.local") ? TEACHER : STUDENT;
              result =
                  Map.of(
                      "access_token",
                      "opaque-access-" + subject,
                      "refresh_token",
                      "never-send-this-to-the-browser",
                      "token_type",
                      "bearer",
                      "expires_in",
                      60,
                      "user",
                      Map.of(
                          "id",
                          subject.toString(),
                          "email",
                          email,
                          "user_metadata",
                          Map.of("role", "TEACHER")));
            }
            byte[] bytes = JSON.writeValueAsBytes(result);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
          });
      authServer.createContext(
          "/auth/v1/logout",
          exchange -> {
            if ("POST".equals(exchange.getRequestMethod())
                && "scope=local".equals(exchange.getRequestURI().getRawQuery())
                && exchange
                    .getRequestHeaders()
                    .getFirst("Authorization")
                    .startsWith("Bearer opaque-access-")) logouts.incrementAndGet();
            exchange.sendResponseHeaders(failLogout.get() ? 503 : 204, -1);
            exchange.close();
          });
      authServer.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry p) {
    p.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
    p.add("spring.datasource.username", () -> "postgres");
    p.add("spring.datasource.password", () -> "");
    p.add("sama.auth.mode", () -> "supabase");
    p.add("sama.auth.supabase-url", () -> "http://127.0.0.1:" + authServer.getAddress().getPort());
    p.add("sama.auth.publishable-key", () -> "sb_publishable_test");
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired UserRepository users;
  @Autowired AuthSettings settings;
  @Autowired MutableClock clock;

  @BeforeEach
  void reset() {
    clock.reset();
    logouts.set(0);
    failLogout.set(false);
    jdbc.update("update app_users set enabled = true");
    jdbc.update(
        "update app_users set auth_user_id = ? where email = ?", STUDENT, "student@sama.local");
    jdbc.update(
        "update app_users set auth_user_id = ? where email = ?", TEACHER, "teacher@sama.local");
  }

  @AfterAll
  static void close() throws Exception {
    authServer.stop(0);
    postgres.close();
  }

  class Client {
    final HttpClient http =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();

    HttpResponse<String> request(String method, String path, Object body) throws Exception {
      var builder =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
              .timeout(Duration.ofSeconds(20));
      if (!method.equals("GET")) {
        var csrf = JSON.readTree(request("GET", "/auth/csrf", null).body());
        builder.header(csrf.path("headerName").asText(), csrf.path("token").asText());
      }
      if (body != null) builder.header("Content-Type", "application/json");
      return http.send(
          builder
              .method(
                  method,
                  body == null
                      ? HttpRequest.BodyPublishers.noBody()
                      : HttpRequest.BodyPublishers.ofString(
                          JSON.writeValueAsString(body), StandardCharsets.UTF_8))
              .build(),
          HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> login(String email, String password) throws Exception {
      return request("POST", "/auth/login", Map.of("email", email, "password", password));
    }

    void login() throws Exception {
      assertThat(login("student@sama.local", "SupabaseOnly123!").statusCode()).isEqualTo(200);
    }
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void remoteLoginUsesMappedSchoolRoleAndKeepsTokensOutOfResponses(CapturedOutput output)
      throws Exception {
    var c = new Client();
    var configuration = JSON.readTree(c.request("GET", "/auth/config", null).body());
    assertThat(configuration.path("provider").asText()).isEqualTo("supabase");
    assertThat(configuration.path("demoAvailable").asBoolean()).isFalse();
    var login = c.login("student@sama.local", "SupabaseOnly123!");
    assertThat(login.statusCode()).describedAs(login.body()).isEqualTo(200);
    assertThat(login.body()).doesNotContain("opaque-access", "refresh_token", "passwordHash");
    var me = JSON.readTree(c.request("GET", "/me", null).body());
    assertThat(me.path("role").asText()).isEqualTo("STUDENT");
    assertThat(me.path("emailEditable").asBoolean()).isFalse();
    String term = JSON.readTree(c.request("GET", "/terms", null).body()).get(0).path("id").asText();
    assertThat(c.request("GET", "/classrooms?termId=" + term, null).statusCode()).isEqualTo(403);
    var teacher = new Client();
    assertThat(teacher.login("teacher@sama.local", "SupabaseOnly123!").statusCode()).isEqualTo(200);
    assertThat(JSON.readTree(teacher.request("GET", "/me", null).body()).path("role").asText())
        .isEqualTo("TEACHER");
    assertThat(output.getAll())
        .doesNotContain("SupabaseOnly123!", "opaque-access-", "never-send-this-to-the-browser");
  }

  @Test
  void oldLocalPasswordIsNotAnAuthenticationFallback() throws Exception {
    var c = new Client();
    assertThat(c.login("student@sama.local", "SamaDemo123!").statusCode()).isEqualTo(401);
    assertThat(c.request("GET", "/me", null).statusCode()).isEqualTo(401);
  }

  @Test
  void matchingEmailWithoutExplicitSubjectMappingDoesNotGrantAccess() throws Exception {
    var c = new Client();
    assertThat(c.login("student@sama.local", "Unlinked123!").statusCode()).isEqualTo(403);
    assertThat(jdbc.queryForObject("select count(*) from app_users", Integer.class)).isEqualTo(5);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from app_users where auth_user_id = ?", Integer.class, UNLINKED))
        .isZero();
    assertThat(logouts.get()).isEqualTo(1);
  }

  @Test
  void expiryInvalidatesCookieSessionAndAllowsFreshLogin() throws Exception {
    var c = new Client();
    c.login();
    clock.advance(Duration.ofSeconds(61));
    assertThat(c.request("GET", "/me", null).statusCode()).isEqualTo(401);
    assertThat(c.request("GET", "/auth/csrf", null).statusCode()).isEqualTo(200);
    c.login();
    assertThat(c.request("GET", "/me", null).statusCode()).isEqualTo(200);
  }

  @Test
  void logoutInvalidatesCookieEvenDuringSupabaseOutage() throws Exception {
    var c = new Client();
    c.login();
    failLogout.set(true);
    assertThat(c.request("POST", "/auth/logout", null).statusCode()).isEqualTo(200);
    assertThat(logouts.get()).isEqualTo(1);
    assertThat(c.request("GET", "/me", null).statusCode()).isEqualTo(401);
  }

  @Test
  void disabledOrUnlinkedSchoolAccountLosesAccess() throws Exception {
    var c = new Client();
    c.login();
    jdbc.update("update app_users set auth_user_id = null where email = ?", "student@sama.local");
    assertThat(c.request("GET", "/me", null).statusCode()).isEqualTo(401);
    jdbc.update(
        "update app_users set auth_user_id = ?, enabled = false where email = ?",
        STUDENT,
        "student@sama.local");
    assertThat(new Client().login("student@sama.local", "SupabaseOnly123!").statusCode())
        .isEqualTo(403);
  }

  @Test
  void profileCannotChangeAuthEmailButCanEditOtherFields() throws Exception {
    var c = new Client();
    c.login();
    var body =
        new HashMap<String, String>(
            Map.of(
                "email",
                "different@sama.local",
                "fullName",
                "ชื่อใหม่",
                "phone",
                "0123",
                "address",
                "บ้าน"));
    assertThat(c.request("PATCH", "/me", body).statusCode()).isEqualTo(400);
    body.put("email", "student@sama.local");
    assertThat(c.request("PATCH", "/me", body).statusCode()).isEqualTo(200);
  }

  @Test
  void upstreamFailureDoesNotExposeDetailsOrUseLocalLogin() throws Exception {
    var response = new Client().login("outage@sama.local", "SamaDemo123!");
    assertThat(response.statusCode()).isEqualTo(503);
    assertThat(response.body())
        .doesNotContain("UPSTREAM_PRIVATE", "SupabaseOnly", "sb_publishable");
  }

  @Test
  void legacyLocalSessionIsRejectedWhenSupabaseIsSelected() throws Exception {
    var principal =
        SamaPrincipal.from(users.findByEmailIgnoreCase("student@sama.local").orElseThrow());
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.getAuthorities()));
    SecurityContextHolder.setContext(context);
    try {
      var request = new MockHttpServletRequest("GET", "/api/me");
      request.setServletPath("/api/me");
      request.getSession(true);
      var response = new MockHttpServletResponse();
      new SupabaseSessionFilter(settings, clock).doFilter(request, response, new MockFilterChain());
      assertThat(response.getStatus()).isEqualTo(401);
      assertThat(request.getSession(false)).isNull();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  static class MutableClock extends Clock {
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.now());

    void reset() {
      now.set(Instant.now());
    }

    void advance(Duration duration) {
      now.updateAndGet(value -> value.plus(duration));
    }

    @Override
    public java.time.ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now.get();
    }
  }

  @TestConfiguration
  static class TestTime {
    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock();
    }
  }
}
