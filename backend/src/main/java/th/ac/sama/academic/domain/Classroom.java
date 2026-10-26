package th.ac.sama.academic.domain;

import jakarta.persistence.*;
import th.ac.sama.shared.domain.BaseEntity;

@Entity
@Table(name = "classrooms")
public class Classroom extends BaseEntity {
  // Hibernate reads the academic year through field access.
  @SuppressWarnings("unused")
  private int academicYear;

  private int gradeLevel;
  private String roomLabel;

  protected Classroom() {}

  public Classroom(int year, int grade, String room) {
    this.academicYear = year;
    this.gradeLevel = grade;
    this.roomLabel = room;
  }

  public int getGradeLevel() {
    return gradeLevel;
  }

  public String getLabel() {
    return "ม." + gradeLevel + "/" + roomLabel;
  }
}
