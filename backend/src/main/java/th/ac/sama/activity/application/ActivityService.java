package th.ac.sama.activity.application;

import jakarta.persistence.criteria.Predicate;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import th.ac.sama.academic.infrastructure.*;
import th.ac.sama.activity.api.ActivityDtos;
import th.ac.sama.activity.api.ActivityDtos.*;
import th.ac.sama.activity.domain.*;
import th.ac.sama.activity.infrastructure.*;
import th.ac.sama.identity.application.CurrentUser;
import th.ac.sama.identity.domain.*;
import th.ac.sama.identity.infrastructure.TeacherRepository;
import th.ac.sama.identity.infrastructure.UserRepository;
import th.ac.sama.progress.infrastructure.RequirementRepository;
import th.ac.sama.shared.error.BusinessException;

@Service
@Transactional
public class ActivityService {
  private final ActivityRepository activities;
  private final CategoryRepository categories;
  private final TermRepository terms;
  private final EnrollmentRepository enrollments;
  private final TeacherRepository teachers;
  private final UserRepository users;
  private final RequirementRepository requirements;
  private final CurrentUser current;

  public ActivityService(
      ActivityRepository a,
      CategoryRepository c,
      TermRepository t,
      EnrollmentRepository e,
      TeacherRepository teachers,
      UserRepository users,
      RequirementRepository r,
      CurrentUser u) {
    activities = a;
    categories = c;
    terms = t;
    enrollments = e;
    this.teachers = teachers;
    this.users = users;
    requirements = r;
    current = u;
  }

  public Item create(Save p) {
    var student = current.student();
    var term = terms.findById(p.termId()).orElseThrow(BusinessException::missing);
    var enrollment =
        enrollments
            .findByStudentIdAndTermId(student.getId(), term.getId())
            .orElseThrow(() -> BusinessException.invalid("ไม่พบข้อมูลการลงทะเบียนในเทอมนี้"));
    var category = categories.findById(p.categoryId()).orElseThrow(BusinessException::missing);
    if (requirements
        .findByTermIdAndGradeLevelOrderByCategoryCode(
            term.getId(), enrollment.getClassroom().getGradeLevel())
        .stream()
        .noneMatch(r -> r.getCategory().getId().equals(category.getId())))
      throw BusinessException.invalid("หมวดกิจกรรมนี้ไม่ได้อยู่ในเกณฑ์ของคุณ");
    var a = new Activity(student, term, category, Instant.now());
    a.edit(
        student.getId(),
        p.title(),
        p.description(),
        p.activityDate(),
        p.startTime(),
        p.endTime(),
        Instant.now());
    activities.saveAndFlush(a);
    return ActivityDtos.view(a);
  }

  public Item edit(UUID id, Save p) {
    var a = owned(id);
    checkVersion(a, p.expectedVersion());
    if (!a.getTerm().getId().equals(p.termId()) || !a.getCategory().getId().equals(p.categoryId()))
      throw BusinessException.invalid("เปลี่ยนหมวดหรือเทอมของกิจกรรมเดิมไม่ได้");
    a.edit(
        current.student().getId(),
        p.title(),
        p.description(),
        p.activityDate(),
        p.startTime(),
        p.endTime(),
        Instant.now());
    activities.flush();
    return ActivityDtos.view(a);
  }

  public Item submit(UUID id, Submit p) {
    var a = owned(id);
    checkVersion(a, p.expectedVersion());
    a.submit(
        current.student().getId(),
        teachers.findById(p.reviewerId()).orElseThrow(BusinessException::missing),
        Instant.now());
    activities.flush();
    return ActivityDtos.view(a);
  }

  @Transactional(readOnly = true)
  public Detail detail(UUID id) {
    var a = requireReadable(id);
    var actors = new HashMap<UUID, String>();
    users
        .findAllById(
            a.getHistory().stream().map(ActivityStatusHistory::getActorId).distinct().toList())
        .forEach(user -> actors.put(user.getId(), user.getFullName()));
    return new Detail(
        ActivityDtos.view(a),
        a.getHistory().stream()
            .map(
                h ->
                    new Event(
                        h.getSequence(),
                        h.getFromStatus(),
                        h.getToStatus(),
                        h.getActorId(),
                        actors.getOrDefault(h.getActorId(), "ผู้ใช้เดิม"),
                        h.getOccurredAt(),
                        h.getReason()))
            .toList());
  }

  @Transactional(readOnly = true)
  public Activity requireReadable(UUID id) {
    var a = activities.findById(id).orElseThrow(BusinessException::missing);
    var u = current.get();
    boolean allowed =
        u instanceof Student && a.getStudent().getId().equals(u.getId())
            || u instanceof Teacher
                && a.getReviewer() != null
                && a.getReviewer().getId().equals(u.getId());
    if (!allowed) throw BusinessException.missing();
    return a;
  }

  @Transactional(readOnly = true)
  public PageResult<Item> list(
      boolean review,
      UUID termId,
      ActivityStatus status,
      UUID categoryId,
      LocalDate from,
      LocalDate to,
      String search,
      int page,
      int size) {
    if (page < 0 || size < 1 || size > 50 || search != null && search.length() > 200)
      throw BusinessException.invalid("เงื่อนไขการค้นหาไม่ถูกต้อง");
    if (from != null && to != null && from.isAfter(to))
      throw BusinessException.invalid("วันที่เริ่มต้นต้องไม่เกินวันที่สิ้นสุด");
    UUID actor = review ? current.teacher().getId() : current.student().getId();
    Specification<Activity> spec =
        (root, q, cb) -> {
          var ps = new ArrayList<Predicate>();
          ps.add(cb.equal(root.get(review ? "reviewer" : "student").get("id"), actor));
          if (termId != null) ps.add(cb.equal(root.get("term").get("id"), termId));
          if (status != null) ps.add(cb.equal(root.get("status"), status));
          if (categoryId != null) ps.add(cb.equal(root.get("category").get("id"), categoryId));
          if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("activityDate"), from));
          if (to != null) ps.add(cb.lessThanOrEqualTo(root.get("activityDate"), to));
          if (search != null && !search.isBlank()) {
            String text =
                "%"
                    + search
                        .toLowerCase()
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_")
                    + "%";
            ps.add(
                cb.or(
                    cb.like(cb.lower(root.get("title")), text, '\\'),
                    cb.like(cb.lower(root.get("student").get("fullName")), text, '\\')));
          }
          return cb.and(ps.toArray(Predicate[]::new));
        };
    var result =
        activities.findAll(
            spec,
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, review ? "submittedAt" : "createdAt")
                    .and(Sort.by("id"))));
    return new PageResult<>(
        result.getContent().stream().map(ActivityDtos::view).toList(),
        result.getTotalElements(),
        page,
        result.getTotalPages());
  }

  private Activity owned(UUID id) {
    var a = activities.findById(id).orElseThrow(BusinessException::missing);
    if (!a.getStudent().getId().equals(current.student().getId()))
      throw BusinessException.missing();
    return a;
  }

  public static void checkVersion(Activity a, Long expected) {
    if (expected == null || a.getVersion() != expected)
      throw BusinessException.conflict("ข้อมูลเปลี่ยนแปลงแล้ว กรุณาโหลดใหม่ก่อนดำเนินการ");
  }
}
