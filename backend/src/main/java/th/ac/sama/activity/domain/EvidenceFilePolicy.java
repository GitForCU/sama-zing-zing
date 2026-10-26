package th.ac.sama.activity.domain;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Component;
import th.ac.sama.shared.error.BusinessException;

@Component
public class EvidenceFilePolicy {
  public static final int MAX_FILES = 5;
  public static final int MAX_BYTES = 5 * 1024 * 1024;

  public record Validated(String name, String contentType, String sha256) {}

  public Validated validate(String filename, byte[] content) {
    if (content.length == 0) throw BusinessException.invalid("ไฟล์หลักฐานต้องไม่เป็นไฟล์ว่าง");
    if (content.length > MAX_BYTES)
      throw new BusinessException(413, "ไฟล์หลักฐานต้องมีขนาดไม่เกิน 5 MB ต่อไฟล์");
    String name = filename == null ? "" : filename.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").strip();
    int dot = name.lastIndexOf('.');
    if (dot <= 0) throw BusinessException.invalid("รองรับเฉพาะรูป JPG, PNG, WEBP และไฟล์ PDF");
    String extension = name.substring(dot).toLowerCase(Locale.ROOT);
    String mime =
        switch (extension) {
          case ".jpg", ".jpeg" -> "image/jpeg";
          case ".png" -> "image/png";
          case ".webp" -> "image/webp";
          case ".pdf" -> "application/pdf";
          default -> throw BusinessException.invalid("รองรับเฉพาะรูป JPG, PNG, WEBP และไฟล์ PDF");
        };
    boolean valid =
        switch (mime) {
          case "image/png" ->
              startsWith(content, new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
          case "image/jpeg" -> startsWith(content, new byte[] {(byte) 255, (byte) 216, (byte) 255});
          case "image/webp" ->
              content.length >= 16
                  && ascii(content, 0, 4).equals("RIFF")
                  && ascii(content, 8, 4).equals("WEBP")
                  && ascii(content, 12, 3).equals("VP8");
          case "application/pdf" ->
              startsWith(content, "%PDF-".getBytes(StandardCharsets.US_ASCII));
          default -> false;
        };
    if (!valid)
      throw BusinessException.invalid(
          "เนื้อหาไฟล์ไม่ตรงกับชนิดไฟล์ที่รองรับ กรุณาเลือกรูปภาพหรือ PDF ที่ถูกต้อง");
    if (name.length() > 200) name = name.substring(0, 200 - extension.length()) + extension;
    try {
      return new Validated(
          name,
          mime,
          HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static boolean startsWith(byte[] bytes, byte[] signature) {
    if (bytes.length < signature.length) return false;
    for (int i = 0; i < signature.length; i++) if (bytes[i] != signature[i]) return false;
    return true;
  }

  private static String ascii(byte[] bytes, int offset, int length) {
    return new String(bytes, offset, length, StandardCharsets.US_ASCII);
  }
}
