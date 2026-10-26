package th.ac.sama.progress.domain;

import org.springframework.stereotype.Component;
import th.ac.sama.activity.domain.RequirementMetric;

@Component
public class DurationProgressCalculator implements ProgressCalculator {
  public RequirementMetric metric() {
    return RequirementMetric.DURATION_MINUTES;
  }

  public long approvedValue(long count, long minutes) {
    return minutes;
  }
}
