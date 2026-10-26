package th.ac.sama.progress.domain;

import org.springframework.stereotype.Component;
import th.ac.sama.activity.domain.RequirementMetric;

@Component
public class CountProgressCalculator implements ProgressCalculator {
  public RequirementMetric metric() {
    return RequirementMetric.COUNT;
  }

  public long approvedValue(long count, long minutes) {
    return count;
  }
}
