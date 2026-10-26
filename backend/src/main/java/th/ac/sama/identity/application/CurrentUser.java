package th.ac.sama.identity.application;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import th.ac.sama.identity.domain.*;
import th.ac.sama.identity.infrastructure.UserRepository;
import th.ac.sama.shared.error.BusinessException;
import th.ac.sama.shared.security.SamaPrincipal;

@Service
public class CurrentUser {
  private final UserRepository users;

  public CurrentUser(UserRepository users) {
    this.users = users;
  }

  public User get() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof SamaPrincipal p))
      throw new BusinessException(401, "กรุณาเข้าสู่ระบบ");
    var user = users.findById(p.id()).orElseThrow(BusinessException::missing);
    if (!user.isEnabled()) throw new BusinessException(401, "บัญชีนี้ไม่สามารถใช้งานได้");
    if (p.authUserId() != null && !p.authUserId().equals(user.getAuthUserId()))
      throw new BusinessException(401, "ข้อมูลบัญชีเปลี่ยนแปลง กรุณาเข้าสู่ระบบอีกครั้ง");
    return user;
  }

  public Student student() {
    var u = get();
    if (!(u instanceof Student s)) throw new BusinessException(403, "เฉพาะนักเรียนเท่านั้น");
    return s;
  }

  public Teacher teacher() {
    var u = get();
    if (!(u instanceof Teacher t)) throw new BusinessException(403, "เฉพาะครูเท่านั้น");
    return t;
  }
}
