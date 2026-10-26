package th.ac.sama.identity.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import th.ac.sama.identity.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmailIgnoreCase(String email);

  Optional<User> findByAuthUserId(UUID authUserId);
}
