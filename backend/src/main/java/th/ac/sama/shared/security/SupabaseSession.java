package th.ac.sama.shared.security;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/** Server-side only; access tokens are never returned to the browser. */
public record SupabaseSession(UUID subject, String accessToken, Instant expiresAt)
    implements Serializable {
  public static final String ATTRIBUTE = SupabaseSession.class.getName();

  @Override
  public String toString() {
    return "SupabaseSession[credentials=REDACTED]";
  }
}
