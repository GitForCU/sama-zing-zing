package th.ac.sama.shared.config;

import java.time.*;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import th.ac.sama.academic.domain.*;
import th.ac.sama.academic.infrastructure.*;
import th.ac.sama.activity.domain.*;
import th.ac.sama.activity.infrastructure.*;
import th.ac.sama.favorite.domain.FavoriteTeacher;
import th.ac.sama.favorite.infrastructure.FavoriteRepository;
import th.ac.sama.identity.domain.*;
import th.ac.sama.identity.infrastructure.UserRepository;
import th.ac.sama.progress.domain.ActivityRequirement;
import th.ac.sama.progress.infrastructure.RequirementRepository;

@Component
@Profile("demo")
public class DemoData implements CommandLineRunner {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final TermRepository terms;
  private final ClassroomRepository rooms;
  private final EnrollmentRepository enrollments;
  private final AdvisorRepository advisors;
  private final CategoryRepository categories;
  private final RequirementRepository requirements;
  private final ActivityRepository activities;
  private final FavoriteRepository favorites;

  public DemoData(
      UserRepository u,
      PasswordEncoder p,
      TermRepository t,
      ClassroomRepository r,
      EnrollmentRepository e,
      AdvisorRepository a,
      CategoryRepository c,
      RequirementRepository q,
      ActivityRepository acts,
      FavoriteRepository f) {
    users = u;
    passwords = p;
    terms = t;
    rooms = r;
    enrollments = e;
    advisors = a;
    categories = c;
    requirements = q;
    activities = acts;
    favorites = f;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (users.count() > 0) return;
    String hash = passwords.encode("SamaDemo123!");
    var student = users.save(new Student("student@sama.local", hash, "ชยางกูร สอนเสนา", "S26001"));
    var second = users.save(new Student("student2@sama.local", hash, "พิมพ์ชนก วัฒนกุล", "S26002"));
    var third = users.save(new Student("student3@sama.local", hash, "ธนกฤต ใจดี", "S26003"));
    var teacher =
        users.save(
            new Teacher("teacher@sama.local", hash, "ครูอรพรรณ วิริยะ", "กลุ่มสาระวิทยาศาสตร์"));
    var other =
        users.save(
            new Teacher("teacher2@sama.local", hash, "ครูปณิธาน สุขสวัสดิ์", "กลุ่มสาระภาษาไทย"));
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Bangkok"));
    var term =
        terms.save(
            new AcademicTerm(
                today.getYear(),
                1,
                today.minusMonths(3).withDayOfMonth(1),
                today.plusMonths(2).withDayOfMonth(1).minusDays(1),
                today.plusDays(18).atTime(23, 59).atZone(ZoneId.of("Asia/Bangkok")).toInstant()));
    var room = rooms.save(new Classroom(today.getYear(), 4, "1"));
    enrollments.save(new Enrollment(student, room, term, 1));
    enrollments.save(new Enrollment(second, room, term, 2));
    enrollments.save(new Enrollment(third, room, term, 3));
    advisors.save(new ClassroomAdvisor(teacher, room, term));
    var rows =
        List.of(
            new ActivityCategory(
                "01_ADVISOR", "การพบครูที่ปรึกษา", "พูดคุย วางแผน และเติบโตไปด้วยกัน", "purple"),
            new ActivityCategory(
                "02_READING", "การอ่านหนังสือ", "เปิดโลกความรู้ผ่านหนังสือเล่มใหม่", "blue"),
            new ActivityCategory(
                "03_CAMP", "การเข้าค่ายวิชาการ", "เรียนรู้นอกห้องเรียน ค้นหาสิ่งที่ชอบ", "orange"),
            new ActivityCategory(
                "04_EXERCISE", "การออกกำลังกาย", "ดูแลร่างกาย เติมพลังให้ทุกวัน", "pink"),
            new ActivityCategory(
                "05_CLUB", "การเข้าร่วมกิจกรรมชุมนุม", "ลงมือทำสิ่งที่ชอบกับเพื่อน ๆ", "green"),
            new ActivityCategory(
                "06_VOLUNTEER",
                "การบำเพ็ญประโยชน์ต่อโรงเรียน",
                "เริ่มต้นสิ่งดี ๆ ในโรงเรียนของเรา",
                "yellow"),
            new ActivityCategory(
                "07_COMMUNITY",
                "การบำเพ็ญประโยชน์ต่อชุมชน",
                "แบ่งปันเวลา สร้างคุณค่าให้สังคม",
                "teal"));
    categories.saveAll(rows);
    int[] targets = {3, 5, 1, 240, 3, 180, 120};
    for (int i = 0; i < rows.size(); i++) {
      var metric = i == 3 || i >= 5 ? RequirementMetric.DURATION_MINUTES : RequirementMetric.COUNT;
      requirements.save(new ActivityRequirement(term, rows.get(i), 4, metric, targets[i]));
    }
    favorites.save(new FavoriteTeacher(student, teacher));
    for (int i = 0; i < 2; i++)
      seed(
          student,
          teacher,
          term,
          rows.get(0),
          "พบครูที่ปรึกษาประจำสัปดาห์",
          ActivityStatus.APPROVED,
          30,
          i + 8);
    seed(
        student,
        teacher,
        term,
        rows.get(0),
        "วางแผนกิจกรรมประจำเทอม",
        ActivityStatus.PENDING,
        30,
        1);
    seed(
        student,
        teacher,
        term,
        rows.get(1),
        "อ่านหนังสือเรื่องความลับของจักรวาล",
        ActivityStatus.APPROVED,
        60,
        7);
    seed(
        student,
        teacher,
        term,
        rows.get(1),
        "บันทึกการอ่านหนังสือเล่มใหม่",
        ActivityStatus.DRAFT,
        60,
        0);
    for (int i = 0; i < 2; i++)
      seed(
          student,
          teacher,
          term,
          rows.get(3),
          "วิ่งและออกกำลังกายกับเพื่อน",
          ActivityStatus.APPROVED,
          60,
          i + 4);
    seed(
        student,
        teacher,
        term,
        rows.get(3),
        "แบดมินตันหลังเลิกเรียน",
        ActivityStatus.PENDING,
        60,
        2);
    for (int i = 0; i < 3; i++)
      seed(
          student,
          teacher,
          term,
          rows.get(4),
          "กิจกรรมชุมนุมวิทยาศาสตร์",
          ActivityStatus.APPROVED,
          60,
          i + 9);
    seed(
        student,
        teacher,
        term,
        rows.get(5),
        "ช่วยจัดห้องปฏิบัติการวิทยาศาสตร์",
        ActivityStatus.REJECTED,
        60,
        3);
    seed(
        student,
        teacher,
        term,
        rows.get(2),
        "ค่ายวิทยาศาสตร์รุ่นเยาว์",
        ActivityStatus.DRAFT,
        120,
        0);
    seed(
        second,
        teacher,
        term,
        rows.get(1),
        "อ่านหนังสือและแลกเปลี่ยนความคิดเห็น",
        ActivityStatus.PENDING,
        60,
        1);
    seed(
        third, teacher, term, rows.get(5), "ดูแลแปลงผักของโรงเรียน", ActivityStatus.PENDING, 90, 2);
    seed(third, other, term, rows.get(1), "อ่านวรรณกรรมไทย", ActivityStatus.PENDING, 60, 2);
    for (int i = 0; i < rows.size(); i++) {
      int times = (i == 3 || i >= 5) ? 1 : targets[i];
      for (int j = 0; j < times; j++)
        seed(
            second,
            teacher,
            term,
            rows.get(i),
            rows.get(i).getName(),
            ActivityStatus.APPROVED,
            (i == 3 || i >= 5) ? targets[i] : 45,
            j + 12);
    }
  }

  private void seed(
      Student s,
      Teacher t,
      AcademicTerm term,
      ActivityCategory category,
      String title,
      ActivityStatus status,
      int duration,
      int days) {
    LocalDate date = LocalDate.now(ZoneId.of("Asia/Bangkok")).minusDays(days);
    Instant at = date.atTime(15, 0).atZone(ZoneId.of("Asia/Bangkok")).toInstant();
    var a = new Activity(s, term, category, at);
    a.edit(
        s.getId(),
        title,
        "เข้าร่วม" + title + " เพื่อพัฒนาทักษะและเรียนรู้ร่วมกับเพื่อน ๆ",
        date,
        LocalTime.of(15, 0),
        LocalTime.of(15, 0).plusMinutes(duration),
        at);
    if (status != ActivityStatus.DRAFT) {
      a.submit(s.getId(), t, at.plusSeconds(60));
      if (status == ActivityStatus.APPROVED) a.approve(t.getId(), at.plusSeconds(3600));
      if (status == ActivityStatus.REJECTED)
        a.returnForRevision(
            t.getId(),
            "กรุณาเพิ่มรายละเอียดงานที่ทำ และระบุหน้าที่ของตนเองให้ชัดเจน",
            at.plusSeconds(3600));
    }
    activities.save(a);
  }
}
