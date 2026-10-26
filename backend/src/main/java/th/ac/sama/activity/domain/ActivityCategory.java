package th.ac.sama.activity.domain;

import jakarta.persistence.*;
import th.ac.sama.shared.domain.BaseEntity;

@Entity
@Table(name = "activity_categories")
public class ActivityCategory extends BaseEntity {
  @Column(unique = true)
  private String code;

  private String name;
  private String description;
  private String color;

  protected ActivityCategory() {}

  public ActivityCategory(String code, String name, String description, String color) {
    this.code = code;
    this.name = name;
    this.description = description;
    this.color = color;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getColor() {
    return color;
  }
}
