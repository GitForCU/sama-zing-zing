package th.ac.sama.identity.application;

import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import th.ac.sama.identity.infrastructure.*;
import th.ac.sama.shared.error.BusinessException;
import th.ac.sama.shared.security.*;

@Service
public class LoginService {
  public record LoginResult(Authentication authentication, SupabaseSession session) {}

  private final AuthSettings settings;
  private final AuthenticationManager local;
  private final SupabaseAuthClient supabase;
  private final UserRepository users;

  public LoginService(
      AuthSettings settings,
      AuthenticationManager local,
      SupabaseAuthClient supabase,
      UserRepository users) {
    this.settings = settings;
    this.local = local;
    this.supabase = supabase;
    this.users = users;
  }

  @Transactional
  public LoginResult login(String email, String password) {
    if (!settings.supabase())
      return new LoginResult(
          local.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password)),
          null);
    var identity = supabase.signIn(email, password);
    var user = users.findByAuthUserId(identity.subject()).orElse(null);
    if (user == null || !user.isEnabled()) {
      supabase.signOut(identity.session());
      throw new BusinessException(
          403, "บัญชีนี้ยังไม่ได้รับสิทธิ์ใช้งาน กรุณาติดต่อผู้ดูแลโรงเรียน");
    }
    // Subject mapping is explicit; email or user-editable metadata never assigns a role.
    user.updateProfile(user.getFullName(), identity.email(), user.getProfile());
    var principal = SamaPrincipal.external(user);
    var auth =
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.getAuthorities());
    return new LoginResult(auth, identity.session());
  }

  public void logout(SupabaseSession session) {
    if (settings.supabase() && session != null) supabase.signOut(session);
  }
}
