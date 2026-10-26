package th.ac.sama.academic.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import th.ac.sama.academic.domain.AcademicTerm;

public interface TermRepository extends JpaRepository<AcademicTerm, UUID> {
  List<AcademicTerm> findAllByOrderByAcademicYearDescSemesterDesc();
}
