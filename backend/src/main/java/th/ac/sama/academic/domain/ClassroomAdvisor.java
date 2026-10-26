package th.ac.sama.academic.domain;

import jakarta.persistence.*;
import th.ac.sama.identity.domain.Teacher;
import th.ac.sama.shared.domain.BaseEntity;

@Entity
@Table(name = "classroom_advisors")
public class ClassroomAdvisor extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Teacher teacher;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Classroom classroom;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private AcademicTerm term;

  protected ClassroomAdvisor() {}

  public ClassroomAdvisor(Teacher teacher, Classroom c, AcademicTerm t) {
    this.teacher = teacher;
    classroom = c;
    term = t;
  }

  public Classroom getClassroom() {
    return classroom;
  }

  public AcademicTerm getTerm() {
    return term;
  }
}
