package th.ac.sama.favorite.application;

import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import th.ac.sama.activity.api.ActivityDtos.PageResult;
import th.ac.sama.favorite.domain.FavoriteTeacher;
import th.ac.sama.favorite.infrastructure.FavoriteRepository;
import th.ac.sama.identity.application.CurrentUser;
import th.ac.sama.identity.infrastructure.TeacherRepository;
import th.ac.sama.shared.error.BusinessException;

@Service
@Transactional
public class FavoriteTeacherService {
  public record TeacherView(UUID id, String fullName, String department, boolean favorite) {}

  private final FavoriteRepository favorites;
  private final TeacherRepository teachers;
  private final CurrentUser current;

  public FavoriteTeacherService(FavoriteRepository f, TeacherRepository t, CurrentUser u) {
    favorites = f;
    teachers = t;
    current = u;
  }

  @Transactional(readOnly = true)
  public PageResult<TeacherView> search(String search, int page) {
    var student = current.student();
    if (page < 0 || search.length() > 200) throw BusinessException.invalid("เงื่อนไขไม่ถูกต้อง");
    var ids =
        new HashSet<>(
            favorites.findByStudentId(student.getId()).stream()
                .map(f -> f.getTeacher().getId())
                .toList());
    var result =
        teachers.findByFullNameContainingIgnoreCaseAndEnabledTrue(
            search, PageRequest.of(page, 50, org.springframework.data.domain.Sort.by("fullName")));
    return new PageResult<>(
        result.stream()
            .map(
                t ->
                    new TeacherView(
                        t.getId(), t.getFullName(), t.getDepartment(), ids.contains(t.getId())))
            .toList(),
        result.getTotalElements(),
        page,
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public List<TeacherView> list() {
    return favorites.findByStudentId(current.student().getId()).stream()
        .map(
            f -> {
              var t = f.getTeacher();
              return new TeacherView(t.getId(), t.getFullName(), t.getDepartment(), true);
            })
        .toList();
  }

  public void add(UUID teacher) {
    var student = current.student();
    var t = teachers.findById(teacher).orElseThrow(BusinessException::missing);
    if (!favorites.existsByStudentIdAndTeacherId(student.getId(), teacher))
      favorites.save(new FavoriteTeacher(student, t));
  }

  public void remove(UUID teacher) {
    favorites.deleteByStudentIdAndTeacherId(current.student().getId(), teacher);
  }
}
