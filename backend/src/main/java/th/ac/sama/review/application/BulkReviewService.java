package th.ac.sama.review.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import th.ac.sama.activity.application.ActivityService;
import th.ac.sama.activity.infrastructure.ActivityRepository;
import th.ac.sama.identity.application.CurrentUser;
import th.ac.sama.shared.error.BusinessException;

@Service
public class BulkReviewService {
  public enum Decision {
    APPROVE,
    REJECT
  }

  public record Selection(@NotNull UUID activityId, @NotNull Long expectedVersion) {}

  public record Request(
      @NotEmpty @Size(max = 50, message = "เลือกได้สูงสุด 50 รายการ")
          List<@NotNull @Valid Selection> items,
      @NotNull Decision decision,
      @Size(max = 1000) String reason) {}

  private final ActivityRepository activities;
  private final CurrentUser current;

  public BulkReviewService(ActivityRepository a, CurrentUser u) {
    activities = a;
    current = u;
  }

  @Transactional
  public Map<String, Integer> review(Request p) {
    var teacher = current.teacher();
    var ids = p.items().stream().map(Selection::activityId).toList();
    if (new HashSet<>(ids).size() != ids.size())
      throw BusinessException.invalid("รายการที่เลือกต้องไม่ซ้ำ");
    var found = activities.findAllById(ids);
    if (found.size() != ids.size()) throw BusinessException.missing();
    var versions = new HashMap<UUID, Long>();
    p.items().forEach(i -> versions.put(i.activityId(), i.expectedVersion()));
    var now = Instant.now();
    for (var a : found) {
      ActivityService.checkVersion(a, versions.get(a.getId()));
      if (p.decision() == Decision.APPROVE) a.approve(teacher.getId(), now);
      else a.returnForRevision(teacher.getId(), p.reason(), now);
    }
    activities.flush();
    return Map.of("updated", found.size());
  }
}
