package th.ac.sama.favorite.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import th.ac.sama.favorite.domain.FavoriteTeacher;

public interface FavoriteRepository extends JpaRepository<FavoriteTeacher, UUID> {
  @EntityGraph(attributePaths = "teacher")
  List<FavoriteTeacher> findByStudentId(UUID studentId);

  boolean existsByStudentIdAndTeacherId(UUID studentId, UUID teacherId);

  void deleteByStudentIdAndTeacherId(UUID studentId, UUID teacherId);
}
