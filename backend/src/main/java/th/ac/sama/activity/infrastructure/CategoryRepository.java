package th.ac.sama.activity.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import th.ac.sama.activity.domain.ActivityCategory;

public interface CategoryRepository extends JpaRepository<ActivityCategory, UUID> {
  List<ActivityCategory> findAllByOrderByCode();
}
