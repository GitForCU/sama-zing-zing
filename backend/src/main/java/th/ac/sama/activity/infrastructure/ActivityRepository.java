package th.ac.sama.activity.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import th.ac.sama.activity.domain.*;

public interface ActivityRepository
    extends JpaRepository<Activity, UUID>, JpaSpecificationExecutor<Activity> {
  @Override
  @EntityGraph(attributePaths = {"student", "category", "term", "reviewer"})
  Page<Activity> findAll(Specification<Activity> specification, Pageable pageable);

  @Query(
      "select a.student.id,a.category.id,a.status,count(a),coalesce(sum(a.durationMinutes),0) from"
          + " Activity a where a.student.id in :studentIds and a.term.id=:termId group by"
          + " a.student.id,a.category.id,a.status")
  List<Object[]> aggregate(List<UUID> studentIds, UUID termId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Activity a where a.id=:id")
  Optional<Activity> findForAttachmentUpdate(UUID id);
}
