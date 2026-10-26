package th.ac.sama.academic.domain;

import jakarta.persistence.*;
import th.ac.sama.identity.domain.Student;
import th.ac.sama.shared.domain.BaseEntity;

@Entity
@Table(name = "enrollments")
public class Enrollment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Student student;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Classroom classroom;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private AcademicTerm term;

  private int rollNumber;

  protected Enrollment() {}

  public Enrollment(Student s, Classroom c, AcademicTerm t, int roll) {
    student = s;
    classroom = c;
    term = t;
    rollNumber = roll;
  }

  public Student getStudent() {
    return student;
  }

  public Classroom getClassroom() {
    return classroom;
  }

  public AcademicTerm getTerm() {
    return term;
  }

  public int getRollNumber() {
    return rollNumber;
  }
}
