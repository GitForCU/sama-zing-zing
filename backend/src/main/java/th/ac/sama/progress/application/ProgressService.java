package th.ac.sama.progress.application;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import th.ac.sama.academic.domain.*;
import th.ac.sama.academic.infrastructure.*;
import th.ac.sama.activity.domain.*;
import th.ac.sama.activity.infrastructure.ActivityRepository;
import th.ac.sama.identity.application.CurrentUser;
import th.ac.sama.progress.domain.*;
import th.ac.sama.progress.infrastructure.RequirementRepository;
import th.ac.sama.shared.error.BusinessException;

@Service
@Transactional(readOnly = true)
public class ProgressService {
  public record CategoryProgress(
      UUID categoryId,
      String code,
      String name,
      String description,
      String color,
      RequirementMetric metric,
      int target,
      long approvedValue,
      long remaining,
      double percent,
      long approvedCount,
      long pendingCount,
      long rejectedCount,
      long draftCount) {}

  public record Summary(
      UUID studentId,
      String studentName,
      String studentCode,
      String classroom,
      int rollNumber,
      UUID termId,
      String termLabel,
      boolean configured,
      boolean completed,
      double percent,
      long totalActivities,
      long approved,
      long pending,
      long rejected,
      long draft,
      int totalApprovedMinutes,
      List<CategoryProgress> categories) {}

  public record ClassroomSummary(
      UUID classroomId,
      String classroom,
      UUID termId,
      int totalStudents,
      long completedStudents,
      long incompleteStudents,
      List<Summary> students) {}

  private final CurrentUser current;
  private final EnrollmentRepository enrollments;
  private final AdvisorRepository advisors;
  private final RequirementRepository requirements;
  private final ActivityRepository activities;
  private final Map<RequirementMetric, ProgressCalculator> calculators =
      new EnumMap<>(RequirementMetric.class);

  public ProgressService(
      CurrentUser u,
      EnrollmentRepository e,
      AdvisorRepository a,
      RequirementRepository r,
      ActivityRepository activities,
      List<ProgressCalculator> calculators) {
    current = u;
    enrollments = e;
    advisors = a;
    requirements = r;
    this.activities = activities;
    calculators.forEach(c -> this.calculators.put(c.metric(), c));
  }

  public Summary mine(UUID term) {
    var s = current.student();
    return forEnrollment(
        enrollments
            .findByStudentIdAndTermId(s.getId(), term)
            .orElseThrow(() -> BusinessException.invalid("ยังไม่มีข้อมูลลงทะเบียนในเทอมนี้")));
  }

  public Summary individual(UUID student, UUID term) {
    var teacher = current.teacher();
    var e =
        enrollments.findByStudentIdAndTermId(student, term).orElseThrow(BusinessException::missing);
    checkAdvisor(teacher.getId(), e.getClassroom().getId(), term);
    return forEnrollment(e);
  }

  private Summary forEnrollment(Enrollment e) {
    return summarize(
        e,
        requirements.findByTermIdAndGradeLevelOrderByCategoryCode(
            e.getTerm().getId(), e.getClassroom().getGradeLevel()),
        activities.aggregate(List.of(e.getStudent().getId()), e.getTerm().getId()));
  }

  public ClassroomSummary classroom(UUID classroom, UUID term) {
    checkAdvisor(current.teacher().getId(), classroom, term);
    var es = enrollments.findByClassroomIdAndTermIdOrderByRollNumber(classroom, term);
    if (es.isEmpty()) return new ClassroomSummary(classroom, "ห้องเรียน", term, 0, 0, 0, List.of());
    var rules =
        requirements.findByTermIdAndGradeLevelOrderByCategoryCode(
            term, es.getFirst().getClassroom().getGradeLevel());
    var rows = activities.aggregate(es.stream().map(e -> e.getStudent().getId()).toList(), term);
    var summaries = es.stream().map(e -> summarize(e, rules, rows)).toList();
    long completed = summaries.stream().filter(Summary::completed).count();
    return new ClassroomSummary(
        classroom,
        es.getFirst().getClassroom().getLabel(),
        term,
        es.size(),
        completed,
        es.size() - completed,
        summaries);
  }

  private void checkAdvisor(UUID teacher, UUID classroom, UUID term) {
    if (!advisors.existsByTeacherIdAndClassroomIdAndTermId(teacher, classroom, term))
      throw BusinessException.missing();
  }

  private Summary summarize(Enrollment e, List<ActivityRequirement> rules, List<Object[]> all) {
    var rows = all.stream().filter(r -> r[0].equals(e.getStudent().getId())).toList();
    var categories = new ArrayList<CategoryProgress>();
    for (var rule : rules) {
      var c = rule.getCategory();
      long approved = 0, pending = 0, rejected = 0, draft = 0, minutes = 0;
      for (var row : rows) {
        if (!row[1].equals(c.getId())) continue;
        long count = ((Number) row[3]).longValue();
        switch ((ActivityStatus) row[2]) {
          case APPROVED -> {
            approved = count;
            minutes = ((Number) row[4]).longValue();
          }
          case PENDING -> pending = count;
          case REJECTED -> rejected = count;
          case DRAFT -> draft = count;
        }
      }
      var calc = calculators.get(rule.getMetric());
      long value = calc.approvedValue(approved, minutes);
      categories.add(
          new CategoryProgress(
              c.getId(),
              c.getCode(),
              c.getName(),
              c.getDescription(),
              c.getColor(),
              rule.getMetric(),
              rule.getTargetValue(),
              value,
              Math.max(0, rule.getTargetValue() - value),
              calc.percent(value, rule.getTargetValue()),
              approved,
              pending,
              rejected,
              draft));
    }
    long approved = 0, pending = 0, rejected = 0, draft = 0;
    int minutes = 0;
    for (var row : rows) {
      long count = ((Number) row[3]).longValue();
      switch ((ActivityStatus) row[2]) {
        case APPROVED -> {
          approved += count;
          minutes += ((Number) row[4]).intValue();
        }
        case PENDING -> pending += count;
        case REJECTED -> rejected += count;
        case DRAFT -> draft += count;
      }
    }
    double percent =
        Math.round(
                categories.stream().mapToDouble(CategoryProgress::percent).average().orElse(0) * 10)
            / 10.0;
    boolean configured = !categories.isEmpty();
    boolean completed =
        configured && categories.stream().allMatch(c -> c.approvedValue() >= c.target());
    return new Summary(
        e.getStudent().getId(),
        e.getStudent().getFullName(),
        e.getStudent().getStudentCode(),
        e.getClassroom().getLabel(),
        e.getRollNumber(),
        e.getTerm().getId(),
        e.getTerm().getLabel(),
        configured,
        completed,
        percent,
        approved + pending + rejected + draft,
        approved,
        pending,
        rejected,
        draft,
        minutes,
        List.copyOf(categories));
  }
}
