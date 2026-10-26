package th.ac.sama.progress.api;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import th.ac.sama.progress.application.ProgressService;

@RestController
@RequestMapping("/api")
public class ProgressController {
  private final ProgressService service;

  public ProgressController(ProgressService service) {
    this.service = service;
  }

  @GetMapping("/me/progress")
  public ProgressService.Summary mine(@RequestParam UUID termId) {
    return service.mine(termId);
  }

  @GetMapping("/students/{id}/progress")
  public ProgressService.Summary student(@PathVariable UUID id, @RequestParam UUID termId) {
    return service.individual(id, termId);
  }

  @GetMapping("/classrooms/{id}/statistics")
  public ProgressService.ClassroomSummary classroom(
      @PathVariable UUID id, @RequestParam UUID termId) {
    return service.classroom(id, termId);
  }
}
