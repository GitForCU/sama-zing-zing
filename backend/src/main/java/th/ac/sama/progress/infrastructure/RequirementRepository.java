package th.ac.sama.progress.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import th.ac.sama.progress.domain.ActivityRequirement;

public interface RequirementRepository extends JpaRepository<ActivityRequirement, UUID> {
  @EntityGraph(attributePaths = "category")
  List<ActivityRequirement> findByTermIdAndGradeLevelOrderByCategoryCode(
      UUID termId, int gradeLevel);
}
