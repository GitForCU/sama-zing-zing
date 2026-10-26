package th.ac.sama.identity.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "teachers")
public class Teacher extends User {
  private String department;

  protected Teacher() {}

  public Teacher(String email, String hash, String name, String department) {
    super(email, hash, name);
    this.department = department;
  }

  public String getDepartment() {
    return department;
  }

  @Override
  public String getRole() {
    return "TEACHER";
  }
}
