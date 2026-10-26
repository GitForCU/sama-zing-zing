package th.ac.sama.shared.security;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import th.ac.sama.identity.infrastructure.UserRepository;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthSettings.class)
public class SecurityConfig {
  @Bean
  Clock authClock() {
    return Clock.systemUTC();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository users) {
    return email ->
        users
            .findByEmailIgnoreCase(email.trim())
            .map(SamaPrincipal::from)
            .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
  }

  @Bean
  AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
    var provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
  }

  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  CsrfTokenRepository csrfTokenRepository() {
    return new HttpSessionCsrfTokenRepository();
  }

  @Bean
  SecurityFilterChain filterChain(
      HttpSecurity http,
      SecurityContextRepository contexts,
      CsrfTokenRepository tokens,
      AuthSettings settings,
      Clock clock)
      throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/auth/config", "/api/auth/csrf", "/api/auth/login", "/error")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .csrf(
            c ->
                c.csrfTokenRepository(tokens)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .securityContext(c -> c.securityContextRepository(contexts))
        .requestCache(c -> c.disable())
        .formLogin(c -> c.disable())
        .httpBasic(c -> c.disable())
        .logout(c -> c.disable())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(
                        (req, res, ex) -> {
                          res.setStatus(401);
                          res.setContentType("application/json;charset=UTF-8");
                          res.getWriter().write("{\"message\":\"กรุณาเข้าสู่ระบบ\"}");
                        })
                    .accessDeniedHandler(
                        (req, res, ex) -> {
                          res.setStatus(403);
                          res.setContentType("application/json;charset=UTF-8");
                          res.getWriter()
                              .write(
                                  "{\"message\":\"คุณไม่มีสิทธิ์ หรือเซสชันหมดอายุ"
                                      + " กรุณาเข้าสู่ระบบอีกครั้ง\"}");
                        }));
    http.addFilterBefore(
        new SupabaseSessionFilter(settings, clock), AnonymousAuthenticationFilter.class);
    return http.build();
  }
}
