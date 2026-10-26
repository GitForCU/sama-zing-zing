package th.ac.sama.shared.security;

import java.util.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import th.ac.sama.identity.domain.User;

public record SamaPrincipal(
    UUID id, String email, String passwordHash, String role, boolean enabled, UUID authUserId)
    implements UserDetails {
  public static SamaPrincipal from(User u) {
    return new SamaPrincipal(
        u.getId(),
        u.getEmail(),
        u.getPasswordHash(),
        u.getRole(),
        u.isEnabled(),
        u.getAuthUserId());
  }

  public static SamaPrincipal external(User u) {
    return new SamaPrincipal(
        u.getId(), u.getEmail(), "", u.getRole(), u.isEnabled(), u.getAuthUserId());
  }

  @Override
  public String toString() {
    return "SamaPrincipal[id=" + id + ", role=" + role + ", credentials=REDACTED]";
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
