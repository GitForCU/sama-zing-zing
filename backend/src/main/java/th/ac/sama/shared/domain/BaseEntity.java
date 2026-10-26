package th.ac.sama.shared.domain;

import jakarta.persistence.*;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {
  @Id protected UUID id = UUID.randomUUID();

  public UUID getId() {
    return id;
  }
}
