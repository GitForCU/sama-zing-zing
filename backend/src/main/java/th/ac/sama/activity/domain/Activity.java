package th.ac.sama.activity.domain;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import th.ac.sama.academic.domain.AcademicTerm;
import th.ac.sama.identity.domain.*;
import th.ac.sama.shared.domain.BaseEntity;
import th.ac.sama.shared.error.BusinessException;

@Entity
@Table(name = "activities")
public class Activity extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Student student;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private AcademicTerm term;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private ActivityCategory category;

  @ManyToOne(fetch = FetchType.LAZY)
  private Teacher reviewer;

  private String title;

  @Column(length = 4000)
  private String description;

  private LocalDate activityDate;
  private LocalTime startTime;
  private LocalTime endTime;
  private int durationMinutes;

  @Enumerated(EnumType.STRING)
  private ActivityStatus status;

  @Version private Long version;
  private Instant createdAt;

  // Hibernate persists this audit timestamp through field access.
  @SuppressWarnings("unused")
  private Instant updatedAt;

  private Instant submittedAt;

  // Hibernate detects this change and increments @Version when evidence changes.
  @SuppressWarnings("unused")
  private int evidenceRevision;

  @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL)
  @OrderBy("sequence ASC")
  private List<ActivityStatusHistory> history = new ArrayList<>();

  protected Activity() {}

  public Activity(Student student, AcademicTerm term, ActivityCategory category, Instant now) {
    this.student = student;
    this.term = term;
    this.category = category;
    this.createdAt = now;
    this.updatedAt = now;
    changeStatus(ActivityStatus.DRAFT, student.getId(), now, null);
  }

  public void edit(
      UUID actor,
      String title,
      String description,
      LocalDate date,
      LocalTime start,
      LocalTime end,
      Instant now) {
    requireEditable(actor);
    if (start != null && end != null && !start.isBefore(end))
      throw BusinessException.invalid("เวลาสิ้นสุดต้องมากกว่าเวลาเริ่มต้น");
    this.title = title == null ? "" : title.trim();
    this.description = description == null ? "" : description.trim();
    this.activityDate = date;
    this.startTime = start;
    this.endTime = end;
    this.durationMinutes =
        start != null && end != null ? (int) Duration.between(start, end).toMinutes() : 0;
    this.updatedAt = now;
  }

  public void submit(UUID actor, Teacher reviewer, Instant now) {
    if (!student.getId().equals(actor)) throw BusinessException.missing();
    if (status != ActivityStatus.DRAFT && status != ActivityStatus.REJECTED)
      throw BusinessException.conflict("กิจกรรมนี้ถูกส่งตรวจแล้ว");
    if (title == null
        || title.isBlank()
        || description == null
        || description.isBlank()
        || activityDate == null
        || startTime == null
        || endTime == null
        || durationMinutes <= 0)
      throw BusinessException.invalid("กรุณากรอกชื่อ รายละเอียด วันที่ และเวลาให้ครบก่อนส่งตรวจ");
    if (activityDate.isBefore(term.getStartsOn()) || activityDate.isAfter(term.getEndsOn()))
      throw BusinessException.invalid("วันที่ทำกิจกรรมต้องอยู่ในเทอมที่เลือก");
    if (reviewer == null || !reviewer.isEnabled())
      throw BusinessException.invalid("กรุณาเลือกครูผู้ตรวจที่ใช้งานได้");
    this.reviewer = reviewer;
    this.submittedAt = now;
    changeStatus(ActivityStatus.PENDING, actor, now, null);
  }

  public void approve(UUID actor, Instant now) {
    requireReviewer(actor);
    changeStatus(ActivityStatus.APPROVED, actor, now, null);
  }

  public void requireEditable(UUID actor) {
    if (!student.getId().equals(actor)) throw BusinessException.missing();
    if (status != ActivityStatus.DRAFT && status != ActivityStatus.REJECTED)
      throw BusinessException.conflict("แก้ไขได้เฉพาะแบบร่างหรือกิจกรรมที่ส่งคืน");
  }

  public void attachmentsChanged(UUID actor) {
    requireEditable(actor);
    evidenceRevision++;
    updatedAt = Instant.now();
  }

  public void returnForRevision(UUID actor, String reason, Instant now) {
    requireReviewer(actor);
    if (reason == null || reason.isBlank())
      throw BusinessException.invalid("กรุณาระบุเหตุผลที่ส่งคืน");
    changeStatus(ActivityStatus.REJECTED, actor, now, reason.trim());
  }

  private void requireReviewer(UUID actor) {
    if (reviewer == null || !reviewer.getId().equals(actor)) throw BusinessException.missing();
    if (status != ActivityStatus.PENDING)
      throw BusinessException.conflict("กิจกรรมถูกตรวจหรือเปลี่ยนสถานะแล้ว กรุณาโหลดใหม่");
  }

  private void changeStatus(ActivityStatus next, UUID actor, Instant now, String reason) {
    history.add(
        new ActivityStatusHistory(this, status, next, actor, history.size() + 1, now, reason));
    status = next;
    updatedAt = now;
  }

  public Student getStudent() {
    return student;
  }

  public AcademicTerm getTerm() {
    return term;
  }

  public ActivityCategory getCategory() {
    return category;
  }

  public Teacher getReviewer() {
    return reviewer;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public LocalDate getActivityDate() {
    return activityDate;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public int getDurationMinutes() {
    return durationMinutes;
  }

  public ActivityStatus getStatus() {
    return status;
  }

  public long getVersion() {
    return version == null ? 0 : version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public List<ActivityStatusHistory> getHistory() {
    return Collections.unmodifiableList(history);
  }
}
