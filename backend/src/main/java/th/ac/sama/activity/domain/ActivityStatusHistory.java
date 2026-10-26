package th.ac.sama.activity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import th.ac.sama.shared.domain.BaseEntity;

@Entity
@Table(name = "activity_status_history")
public class ActivityStatusHistory extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Activity activity;

  @Enumerated(EnumType.STRING)
  private ActivityStatus fromStatus;

  @Enumerated(EnumType.STRING)
  private ActivityStatus toStatus;

  private UUID actorId;
  private int sequence;
  private Instant occurredAt;
  private String reason;

  protected ActivityStatusHistory() {}

  public ActivityStatusHistory(
      Activity a,
      ActivityStatus from,
      ActivityStatus to,
      UUID actor,
      int seq,
      Instant at,
      String reason) {
    this.activity = a;
    this.fromStatus = from;
    this.toStatus = to;
    this.actorId = actor;
    this.sequence = seq;
    this.occurredAt = at;
    this.reason = reason;
  }

  public ActivityStatus getFromStatus() {
    return fromStatus;
  }

  public ActivityStatus getToStatus() {
    return toStatus;
  }

  public UUID getActorId() {
    return actorId;
  }

  public int getSequence() {
    return sequence;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getReason() {
    return reason;
  }
}
