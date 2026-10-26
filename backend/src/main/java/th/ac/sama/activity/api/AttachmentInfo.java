package th.ac.sama.activity.api;

import java.time.Instant;
import java.util.UUID;
import th.ac.sama.activity.domain.ActivityAttachment;

public record AttachmentInfo(
    UUID id,
    UUID clientUploadId,
    String originalName,
    String contentType,
    long sizeBytes,
    Instant uploadedAt) {
  public static AttachmentInfo from(ActivityAttachment file) {
    return new AttachmentInfo(
        file.getId(),
        file.getClientUploadId(),
        file.getOriginalName(),
        file.getContentType(),
        file.getSizeBytes(),
        file.getUploadedAt());
  }
}
