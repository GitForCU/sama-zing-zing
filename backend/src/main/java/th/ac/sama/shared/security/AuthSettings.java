package th.ac.sama.shared.security;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sama.auth")
public record AuthSettings(String mode, String supabaseUrl, String publishableKey) {
  public AuthSettings {
    mode = mode == null ? "local" : mode.trim().toLowerCase(java.util.Locale.ROOT);
    supabaseUrl = supabaseUrl == null ? "" : supabaseUrl.trim().replaceAll("/+$", "");
    publishableKey = publishableKey == null ? "" : publishableKey.trim();
    if (!mode.equals("local") && !mode.equals("supabase"))
      throw new IllegalArgumentException("SAMA_AUTH_MODE must be local or supabase");
    if (mode.equals("supabase")) {
      try {
        var uri = URI.create(supabaseUrl);
        boolean loopback = "127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost());
        if (uri.getHost() == null
            || uri.getUserInfo() != null
            || uri.getQuery() != null
            || uri.getFragment() != null
            || !("https".equals(uri.getScheme()) || loopback && "http".equals(uri.getScheme()))
            || !uri.getPath().isEmpty()) throw new IllegalArgumentException();
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Set SUPABASE_URL to your HTTPS project URL (without /auth/v1 or credentials)");
      }
      boolean validKey = publishableKey.startsWith("sb_publishable_");
      // Legacy anon keys are allowed, but service_role / secret keys are not.
      if (!validKey && publishableKey.split("\\.").length == 3) {
        try {
          var payload =
              new String(
                  Base64.getUrlDecoder().decode(publishableKey.split("\\.")[1]),
                  StandardCharsets.UTF_8);
          validKey = payload.matches("(?s).*\"role\"\\s*:\\s*\"anon\".*");
        } catch (IllegalArgumentException ignored) {
        }
      }
      if (!validKey || publishableKey.contains("YOUR_"))
        throw new IllegalArgumentException(
            "Set SUPABASE_PUBLISHABLE_KEY to a publishable (or legacy anon) key, never a"
                + " secret/service_role key");
    }
  }

  public boolean supabase() {
    return mode.equals("supabase");
  }
}
