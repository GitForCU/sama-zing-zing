package th.ac.sama.activity.api;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import th.ac.sama.activity.domain.*;

public final class ActivityDtos {
  private ActivityDtos() {}

  public record Save(
      @NotNull UUID termId,
      @NotNull UUID categoryId,
      @Size(max = 200) String title,
      @Size(max = 4000) String description,
      LocalDate activityDate,
      LocalTime startTime,
      LocalTime endTime,
      Long expectedVersion) {}

  public record Submit(@NotNull UUID reviewerId, @NotNull Long expectedVersion) {}

  public record Item(
      UUID id,
      String title,
      String description,
      UUID categoryId,
      String categoryCode,
      String categoryName,
      String color,
      UUID termId,
      String termLabel,
      LocalDate activityDate,
      LocalTime startTime,
      LocalTime endTime,
      int durationMinutes,
      ActivityStatus status,
      long version,
      Instant createdAt,
      Instant submittedAt,
      UUID studentId,
      String studentName,
      UUID reviewerId,
      String reviewerName) {}

  public record Event(
      int sequence,
      ActivityStatus fromStatus,
      ActivityStatus toStatus,
      UUID actorId,
      String actorName,
      Instant occurredAt,
      String reason) {}

  public record Detail(Item activity, List<Event> history) {}

  public record PageResult<T>(List<T> items, long total, int page, int totalPages) {}

  public static Item view(Activity a) {
    return new Item(
        a.getId(),
        a.getTitle(),
        a.getDescription(),
        a.getCategory().getId(),
        a.getCategory().getCode(),
        a.getCategory().getName(),
        a.getCategory().getColor(),
        a.getTerm().getId(),
        a.getTerm().getLabel(),
        a.getActivityDate(),
        a.getStartTime(),
        a.getEndTime(),
        a.getDurationMinutes(),
        a.getStatus(),
        a.getVersion(),
        a.getCreatedAt(),
        a.getSubmittedAt(),
        a.getStudent().getId(),
        a.getStudent().getFullName(),
        a.getReviewer() == null ? null : a.getReviewer().getId(),
        a.getReviewer() == null ? null : a.getReviewer().getFullName());
  }
}
