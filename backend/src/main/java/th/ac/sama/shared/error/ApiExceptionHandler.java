package th.ac.sama.shared.error;

import java.util.Map;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  ResponseEntity<?> business(BusinessException e) {
    return error(e.getStatus(), e.getMessage());
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<?> auth() {
    return error(401, "อีเมลหรือรหัสผ่านไม่ถูกต้อง");
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<?> denied() {
    return error(403, "คุณไม่มีสิทธิ์ดำเนินการนี้");
  }

  @ExceptionHandler({
    ObjectOptimisticLockingFailureException.class,
    DataIntegrityViolationException.class
  })
  ResponseEntity<?> conflict() {
    return error(409, "ข้อมูลเปลี่ยนแปลงแล้วหรือมีข้อมูลซ้ำ กรุณาโหลดใหม่");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    return error(
        400,
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getDefaultMessage())
            .findFirst()
            .orElse("กรุณาตรวจสอบข้อมูล"));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<?> malformed() {
    return error(400, "รูปแบบข้อมูลไม่ถูกต้อง");
  }

  private ResponseEntity<?> error(int status, String message) {
    return ResponseEntity.status(status).body(Map.of("message", message));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<?> uploadTooLarge() {
    return error(413, "ไฟล์หลักฐานต้องมีขนาดไม่เกิน 5 MB ต่อไฟล์");
  }
}
