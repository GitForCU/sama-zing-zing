package th.ac.sama.academic.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import th.ac.sama.academic.domain.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
  @EntityGraph(attributePaths = {"classroom", "student", "term"})
  Optional<Enrollment> findByStudentIdAndTermId(UUID student, UUID term);

  @EntityGraph(attributePaths = {"classroom", "student", "term"})
  List<Enrollment> findByClassroomIdAndTermIdOrderByRollNumber(UUID classroom, UUID term);
}
