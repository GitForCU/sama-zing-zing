package th.ac.sama.favorite.api;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import th.ac.sama.activity.api.ActivityDtos.PageResult;
import th.ac.sama.favorite.application.FavoriteTeacherService;
import th.ac.sama.favorite.application.FavoriteTeacherService.TeacherView;

@RestController
@RequestMapping("/api")
public class FavoriteTeacherController {
  private final FavoriteTeacherService service;

  public FavoriteTeacherController(FavoriteTeacherService s) {
    service = s;
  }

  @GetMapping("/teachers")
  public PageResult<TeacherView> search(
      @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "0") int page) {
    return service.search(search, page);
  }

  @GetMapping("/me/favorite-teachers")
  public List<TeacherView> list() {
    return service.list();
  }

  @PutMapping("/me/favorite-teachers/{id}")
  public void add(@PathVariable UUID id) {
    service.add(id);
  }

  @DeleteMapping("/me/favorite-teachers/{id}")
  public void remove(@PathVariable UUID id) {
    service.remove(id);
  }
}
