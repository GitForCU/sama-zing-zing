package th.ac.sama.identity.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student extends User {
  @Column(unique = true, nullable = false)
  private String studentCode;

  protected Student() {}

  public Student(String email, String hash, String name, String code) {
    super(email, hash, name);
    this.studentCode = code;
  }

  public String getStudentCode() {
    return studentCode;
  }

  @Override
  public String getRole() {
    return "STUDENT";
  }
}
