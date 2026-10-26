package th.ac.sama.academic.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import th.ac.sama.academic.domain.Classroom;

public interface ClassroomRepository extends JpaRepository<Classroom, UUID> {}
