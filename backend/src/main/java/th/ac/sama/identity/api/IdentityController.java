package th.ac.sama.identity.api;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import th.ac.sama.identity.application.CurrentUser;
import th.ac.sama.identity.application.LoginService;
import th.ac.sama.identity.domain.*;
import th.ac.sama.identity.infrastructure.UserRepository;
import th.ac.sama.shared.error.BusinessException;
import th.ac.sama.shared.security.*;

@RestController
@RequestMapping("/api")
public class IdentityController {
  private final CurrentUser current;
  private final UserRepository users;
  private final LoginService auth;
  private final SecurityContextRepository contexts;
  private final CsrfTokenRepository csrf;
  private final AuthSettings settings;
  private final Clock clock;
  private final Environment environment;

  public IdentityController(
      CurrentUser c,
      UserRepository u,
      LoginService a,
      SecurityContextRepository contexts,
      CsrfTokenRepository csrf,
      AuthSettings settings,
      Clock clock,
      Environment environment) {
    current = c;
    users = u;
    auth = a;
    this.contexts = contexts;
    this.csrf = csrf;
    this.settings = settings;
    this.clock = clock;
    this.environment = environment;
  }

  public record Login(@NotBlank @Email String email, @NotBlank String password) {
    @Override
    public String toString() {
      return "Login[credentials=REDACTED]";
    }
  }

  public record ProfileRequest(
      @NotBlank(message = "กรุณากรอกชื่อ") @Size(max = 200) String fullName,
      @NotBlank @Email @Size(max = 254) String email,
      @Size(max = 30) String phone,
      @Size(max = 1000) String address) {}

  public record Me(
      UUID id,
      String email,
      String fullName,
      String role,
      String studentCode,
      String department,
      String phone,
      String address,
      boolean emailEditable) {}

  public Me view(User u) {
    return new Me(
        u.getId(),
        u.getEmail(),
        u.getFullName(),
        u.getRole(),
        u instanceof Student s ? s.getStudentCode() : null,
        u instanceof Teacher t ? t.getDepartment() : null,
        u.getProfile().getPhone(),
        u.getProfile().getAddress(),
        !settings.supabase());
  }

  @GetMapping("/auth/config")
  public Map<String, Object> configuration() {
    return Map.of(
        "provider",
        settings.mode(),
        "demoAvailable",
        !settings.supabase() && Arrays.asList(environment.getActiveProfiles()).contains("demo"));
  }

  @GetMapping("/auth/csrf")
  public Map<String, String> token(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @PostMapping("/auth/login")
  public Me login(@Valid @RequestBody Login body, HttpServletRequest req, HttpServletResponse res) {
    var result = auth.login(body.email(), body.password());
    var session = req.getSession(true);
    req.changeSessionId();
    if (result.session() != null) {
      session.setAttribute(SupabaseSession.ATTRIBUTE, result.session());
      int remaining =
          (int)
              Math.max(
                  1, Duration.between(clock.instant(), result.session().expiresAt()).getSeconds());
      session.setMaxInactiveInterval(Math.min(session.getMaxInactiveInterval(), remaining));
    } else {
      session.removeAttribute(SupabaseSession.ATTRIBUTE);
    }
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(result.authentication());
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, req, res);
    csrf.saveToken(null, req, res);
    return view(current.get());
  }

  @PostMapping("/auth/logout")
  public void logout(HttpServletRequest req, HttpServletResponse res) {
    var session = req.getSession(false);
    var external = session == null ? null : session.getAttribute(SupabaseSession.ATTRIBUTE);
    csrf.saveToken(null, req, res);
    if (session != null) session.invalidate();
    SecurityContextHolder.clearContext();
    if (external instanceof SupabaseSession state) auth.logout(state);
  }

  @GetMapping("/me")
  public Me me() {
    return view(current.get());
  }

  @PatchMapping("/me")
  @Transactional
  public Me update(@Valid @RequestBody ProfileRequest p) {
    var u = current.get();
    if (settings.supabase() && !u.getEmail().equalsIgnoreCase(p.email().trim()))
      throw BusinessException.invalid("หากต้องการเปลี่ยนอีเมล กรุณาติดต่อผู้ดูแลบัญชี");
    u.updateProfile(
        p.fullName(),
        settings.supabase() ? u.getEmail() : p.email(),
        new Profile(p.phone() == null ? "" : p.phone(), p.address() == null ? "" : p.address()));
    users.flush();
    return view(u);
  }
}
