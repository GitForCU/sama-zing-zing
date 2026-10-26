package th.ac.sama.identity.infrastructure;

import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import th.ac.sama.identity.domain.Teacher;

public interface TeacherRepository extends JpaRepository<Teacher, UUID> {
  Page<Teacher> findByFullNameContainingIgnoreCaseAndEnabledTrue(String search, Pageable page);
}
