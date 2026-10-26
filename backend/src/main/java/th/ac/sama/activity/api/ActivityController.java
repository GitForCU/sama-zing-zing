package th.ac.sama.activity.api;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import th.ac.sama.activity.api.ActivityDtos.*;
import th.ac.sama.activity.application.ActivityService;
import th.ac.sama.activity.domain.ActivityStatus;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
  private final ActivityService service;

  public ActivityController(ActivityService service) {
    this.service = service;
  }

  @GetMapping
  public PageResult<Item> list(
      @RequestParam(required = false) UUID termId,
      @RequestParam(required = false) ActivityStatus status,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) LocalDate fromDate,
      @RequestParam(required = false) LocalDate toDate,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    return service.list(false, termId, status, categoryId, fromDate, toDate, search, page, size);
  }

  @PostMapping
  public Item create(@Valid @RequestBody Save p) {
    return service.create(p);
  }

  @GetMapping("/{id}")
  public Detail detail(@PathVariable UUID id) {
    return service.detail(id);
  }

  @PatchMapping("/{id}")
  public Item edit(@PathVariable UUID id, @Valid @RequestBody Save p) {
    return service.edit(id, p);
  }

  @PostMapping("/{id}/submit")
  public Item submit(@PathVariable UUID id, @Valid @RequestBody Submit p) {
    return service.submit(id, p);
  }
}
