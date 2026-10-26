package th.ac.sama.shared.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Never accept a legacy local session, or a session beyond its Supabase token expiry. */
public class SupabaseSessionFilter extends OncePerRequestFilter {
  private final AuthSettings settings;
  private final Clock clock;

  public SupabaseSessionFilter(AuthSettings settings, Clock clock) {
    this.settings = settings;
    this.clock = clock;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (settings.supabase()
        && authentication != null
        && authentication.getPrincipal() instanceof SamaPrincipal principal) {
      var session = request.getSession(false);
      var state = session == null ? null : session.getAttribute(SupabaseSession.ATTRIBUTE);
      if (!(state instanceof SupabaseSession external)
          || !external.expiresAt().isAfter(clock.instant())
          || !external.subject().equals(principal.authUserId())) {
        SecurityContextHolder.clearContext();
        if (session != null) session.invalidate();
        if (java.util.Set.of("/api/auth/config", "/api/auth/csrf", "/api/auth/login")
            .contains(request.getServletPath())) {
          chain.doFilter(request, response);
          return;
        }
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"เซสชันหมดอายุ กรุณาเข้าสู่ระบบอีกครั้ง\"}");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
