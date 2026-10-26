package th.ac.sama.favorite.domain;

import jakarta.persistence.*;
import th.ac.sama.identity.domain.*;
import th.ac.sama.shared.domain.BaseEntity;

@Entity
@Table(
    name = "favorite_teachers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "teacher_id"}))
public class FavoriteTeacher extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Student student;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Teacher teacher;

  protected FavoriteTeacher() {}

  public FavoriteTeacher(Student s, Teacher t) {
    student = s;
    teacher = t;
  }

  public Teacher getTeacher() {
    return teacher;
  }
}
