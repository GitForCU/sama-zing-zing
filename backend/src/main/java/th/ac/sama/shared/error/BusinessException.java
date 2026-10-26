package th.ac.sama.shared.error;

public class BusinessException extends RuntimeException {
  private final int status;

  public BusinessException(int status, String message) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }

  public static BusinessException invalid(String message) {
    return new BusinessException(400, message);
  }

  public static BusinessException conflict(String message) {
    return new BusinessException(409, message);
  }

  public static BusinessException missing() {
    return new BusinessException(404, "ไม่พบข้อมูลหรือคุณไม่มีสิทธิ์เข้าถึง");
  }
}
