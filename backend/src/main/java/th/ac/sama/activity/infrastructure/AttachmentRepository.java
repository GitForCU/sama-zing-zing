package th.ac.sama.activity.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import th.ac.sama.activity.api.AttachmentInfo;
import th.ac.sama.activity.domain.ActivityAttachment;

public interface AttachmentRepository extends JpaRepository<ActivityAttachment, UUID> {
  @Query(
      "select new"
          + " th.ac.sama.activity.api.AttachmentInfo(a.id,a.clientUploadId,a.originalName,a.contentType,a.sizeBytes,a.uploadedAt)"
          + " from ActivityAttachment a where a.activity.id=:activityId order by a.uploadedAt,a.id")
  List<AttachmentInfo> metadata(UUID activityId);

  long countByActivityId(UUID activityId);

  Optional<ActivityAttachment> findByActivityIdAndClientUploadId(
      UUID activityId, UUID clientUploadId);

  Optional<ActivityAttachment> findByIdAndActivityId(UUID id, UUID activityId);
}
