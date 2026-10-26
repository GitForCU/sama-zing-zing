package th.ac.sama.academic.domain;

import jakarta.persistence.*;
import java.time.*;
import th.ac.sama.shared.domain.BaseEntity;

@Entity
@Table(name = "academic_terms")
public class AcademicTerm extends BaseEntity {
  private int academicYear;
  private int semester;
  private LocalDate startsOn;
  private LocalDate endsOn;
  private Instant submissionDeadline;

  protected AcademicTerm() {}

  public AcademicTerm(int year, int semester, LocalDate start, LocalDate end, Instant deadline) {
    this.academicYear = year;
    this.semester = semester;
    this.startsOn = start;
    this.endsOn = end;
    this.submissionDeadline = deadline;
  }

  public int getAcademicYear() {
    return academicYear;
  }

  public int getSemester() {
    return semester;
  }

  public LocalDate getStartsOn() {
    return startsOn;
  }

  public LocalDate getEndsOn() {
    return endsOn;
  }

  public Instant getSubmissionDeadline() {
    return submissionDeadline;
  }

  public String getLabel() {
    return (academicYear + 543) + "/" + semester;
  }
}
