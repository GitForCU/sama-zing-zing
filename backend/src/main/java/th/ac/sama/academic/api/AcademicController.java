package th.ac.sama.academic.api;

import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import th.ac.sama.academic.infrastructure.*;
import th.ac.sama.activity.domain.RequirementMetric;
import th.ac.sama.identity.application.CurrentUser;
import th.ac.sama.identity.domain.*;
import th.ac.sama.progress.infrastructure.RequirementRepository;
import th.ac.sama.shared.error.BusinessException;

@RestController
@RequestMapping("/api")
@Transactional(readOnly = true)
public class AcademicController {
  public record Term(
      UUID id, String label, LocalDate startsOn, LocalDate endsOn, Instant submissionDeadline) {}

  public record Room(UUID id, String label) {}

  public record Category(
      UUID id,
      String code,
      String name,
      String description,
      String color,
      RequirementMetric metric,
      Integer target) {}

  private final TermRepository terms;
  private final AdvisorRepository advisors;
  private final EnrollmentRepository enrollments;
  private final RequirementRepository requirements;
  private final th.ac.sama.activity.infrastructure.CategoryRepository categories;
  private final CurrentUser current;

  public AcademicController(
      TermRepository t,
      AdvisorRepository a,
      EnrollmentRepository e,
      RequirementRepository r,
      th.ac.sama.activity.infrastructure.CategoryRepository c,
      CurrentUser u) {
    terms = t;
    advisors = a;
    enrollments = e;
    requirements = r;
    categories = c;
    current = u;
  }

  @GetMapping("/terms")
  public List<Term> terms() {
    return terms.findAllByOrderByAcademicYearDescSemesterDesc().stream()
        .map(
            t ->
                new Term(
                    t.getId(),
                    t.getLabel(),
                    t.getStartsOn(),
                    t.getEndsOn(),
                    t.getSubmissionDeadline()))
        .toList();
  }

  @GetMapping("/classrooms")
  public List<Room> classrooms(@RequestParam UUID termId) {
    return advisors.findByTeacherIdAndTermId(current.teacher().getId(), termId).stream()
        .map(a -> new Room(a.getClassroom().getId(), a.getClassroom().getLabel()))
        .toList();
  }

  @GetMapping("/activity-categories")
  public List<Category> categories(@RequestParam UUID termId) {
    var u = current.get();
    if (u instanceof Student s) {
      var e =
          enrollments
              .findByStudentIdAndTermId(s.getId(), termId)
              .orElseThrow(() -> BusinessException.invalid("ยังไม่มีข้อมูลลงทะเบียนในเทอมนี้"));
      return requirements
          .findByTermIdAndGradeLevelOrderByCategoryCode(termId, e.getClassroom().getGradeLevel())
          .stream()
          .map(
              r -> {
                var c = r.getCategory();
                return new Category(
                    c.getId(),
                    c.getCode(),
                    c.getName(),
                    c.getDescription(),
                    c.getColor(),
                    r.getMetric(),
                    r.getTargetValue());
              })
          .toList();
    }
    return categories.findAllByOrderByCode().stream()
        .map(
            c ->
                new Category(
                    c.getId(),
                    c.getCode(),
                    c.getName(),
                    c.getDescription(),
                    c.getColor(),
                    null,
                    null))
        .toList();
  }
}
