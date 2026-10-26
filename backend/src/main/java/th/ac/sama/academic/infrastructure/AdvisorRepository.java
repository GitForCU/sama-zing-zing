package th.ac.sama.academic.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import th.ac.sama.academic.domain.ClassroomAdvisor;

public interface AdvisorRepository extends JpaRepository<ClassroomAdvisor, UUID> {
  boolean existsByTeacherIdAndClassroomIdAndTermId(UUID teacher, UUID classroom, UUID term);

  @EntityGraph(attributePaths = {"classroom", "term"})
  List<ClassroomAdvisor> findByTeacherIdAndTermId(UUID teacher, UUID term);
}
