package th.ac.sama.activity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import th.ac.sama.shared.domain.BaseEntity;

@Entity
@Table(name = "activity_attachments")
public class ActivityAttachment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Activity activity;

  @Column(nullable = false)
  private UUID clientUploadId;

  @Column(nullable = false, length = 200)
  private String originalName;

  @Column(nullable = false, length = 100)
  private String contentType;

  private long sizeBytes;

  @Column(nullable = false, length = 64)
  private String sha256;

  @Column(nullable = false, columnDefinition = "bytea")
  private byte[] content;

  private Instant uploadedAt;

  protected ActivityAttachment() {}

  public ActivityAttachment(
      Activity activity,
      UUID clientUploadId,
      String name,
      String type,
      byte[] content,
      String sha256) {
    this.activity = activity;
    this.clientUploadId = clientUploadId;
    this.originalName = name;
    this.contentType = type;
    this.sizeBytes = content.length;
    this.content = content;
    this.sha256 = sha256;
    this.uploadedAt = Instant.now();
  }

  public Activity getActivity() {
    return activity;
  }

  public UUID getClientUploadId() {
    return clientUploadId;
  }

  public String getOriginalName() {
    return originalName;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public byte[] getContent() {
    return content;
  }

  public String getSha256() {
    return sha256;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }
}
