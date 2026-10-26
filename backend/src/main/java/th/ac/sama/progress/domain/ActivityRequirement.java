package th.ac.sama.progress.domain;

import jakarta.persistence.*;
import th.ac.sama.academic.domain.AcademicTerm;
import th.ac.sama.activity.domain.*;
import th.ac.sama.shared.domain.BaseEntity;

@Entity
@Table(name = "activity_requirements")
public class ActivityRequirement extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private AcademicTerm term;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private ActivityCategory category;

  // Hibernate reads this field for RequirementRepository's grade-level query.
  @SuppressWarnings("unused")
  private int gradeLevel;

  @Enumerated(EnumType.STRING)
  private RequirementMetric metric;

  private int targetValue;

  protected ActivityRequirement() {}

  public ActivityRequirement(
      AcademicTerm t, ActivityCategory c, int grade, RequirementMetric metric, int target) {
    if (target <= 0) throw new IllegalArgumentException("target must be positive");
    this.term = t;
    this.category = c;
    this.gradeLevel = grade;
    this.metric = metric;
    this.targetValue = target;
  }

  public ActivityCategory getCategory() {
    return category;
  }

  public RequirementMetric getMetric() {
    return metric;
  }

  public int getTargetValue() {
    return targetValue;
  }
}
