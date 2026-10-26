package th.ac.sama.progress.domain;

import th.ac.sama.activity.domain.RequirementMetric;

public interface ProgressCalculator {
  RequirementMetric metric();

  long approvedValue(long count, long minutes);

  default double percent(long value, int target) {
    return Math.round(Math.min(100, 100.0 * value / target) * 10) / 10.0;
  }
}
