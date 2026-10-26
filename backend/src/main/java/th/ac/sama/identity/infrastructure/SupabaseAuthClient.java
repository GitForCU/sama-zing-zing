package th.ac.sama.identity.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import th.ac.sama.shared.error.BusinessException;
import th.ac.sama.shared.security.*;

/** Password authentication is delegated to the trusted Supabase Auth HTTPS endpoint. */
@Component
public class SupabaseAuthClient {
  private record PasswordGrant(String email, String password) {
    @Override
    public String toString() {
      return "PasswordGrant[credentials=REDACTED]";
    }
  }

  public record Identity(UUID subject, String email, SupabaseSession session) {
    @Override
    public String toString() {
      return "Identity[credentials=REDACTED]";
    }
  }

  private final AuthSettings settings;
  private final Clock clock;
  private final RestClient http;

  public SupabaseAuthClient(AuthSettings settings, Clock clock) {
    this.settings = settings;
    this.clock = clock;
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    factory.setReadTimeout(Duration.ofSeconds(10));
    http = RestClient.builder().requestFactory(factory).build();
  }

  public Identity signIn(String email, String password) {
    try {
      var response =
          http.post()
              .uri(settings.supabaseUrl() + "/auth/v1/token?grant_type=password")
              .header("apikey", settings.publishableKey())
              .contentType(MediaType.APPLICATION_JSON)
              .body(new PasswordGrant(email.trim(), password))
              .retrieve()
              .body(JsonNode.class);
      if (response == null
          || response.path("access_token").asText().isBlank()
          || !response.path("token_type").asText().equalsIgnoreCase("bearer")
          || response.path("expires_in").asLong() <= 0) throw unavailable();
      var user = response.path("user");
      var id = UUID.fromString(user.path("id").asText());
      if (user.path("email").asText().isBlank() || user.path("is_anonymous").asBoolean(false))
        throw unavailable();
      var expiry = clock.instant().plusSeconds(response.path("expires_in").asLong());
      if (response.has("expires_at"))
        expiry =
            Instant.ofEpochSecond(
                Math.min(expiry.getEpochSecond(), response.path("expires_at").asLong()));
      if (!expiry.isAfter(clock.instant())) throw unavailable();
      return new Identity(
          id,
          user.path("email").asText(),
          new SupabaseSession(id, response.path("access_token").asText(), expiry));
    } catch (RestClientResponseException e) {
      int status = e.getStatusCode().value();
      if (status == 400 || status == 401 || status == 403 || status == 422)
        throw new BusinessException(
            401, "เข้าสู่ระบบไม่สำเร็จ กรุณาตรวจสอบอีเมล รหัสผ่าน และการยืนยันอีเมล");
      if (status == 429)
        throw new BusinessException(429, "เข้าสู่ระบบบ่อยเกินไป กรุณารอสักครู่แล้วลองใหม่");
      throw unavailable();
    } catch (RestClientException | IllegalArgumentException e) {
      throw unavailable();
    }
  }

  public void signOut(SupabaseSession session) {
    try {
      http.post()
          .uri(settings.supabaseUrl() + "/auth/v1/logout?scope=local")
          .header("apikey", settings.publishableKey())
          .header("Authorization", "Bearer " + session.accessToken())
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException ignored) {
      // The application session is always invalidated, including during upstream outages.
    }
  }

  private BusinessException unavailable() {
    return new BusinessException(503, "ระบบยืนยันตัวตนยังไม่พร้อมใช้งาน กรุณาลองใหม่ภายหลัง");
  }
}
