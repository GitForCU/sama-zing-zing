package th.ac.sama.review.api;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import th.ac.sama.activity.api.ActivityDtos.*;
import th.ac.sama.activity.application.ActivityService;
import th.ac.sama.activity.domain.ActivityStatus;
import th.ac.sama.review.application.BulkReviewService;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
  private final ActivityService activities;
  private final BulkReviewService reviews;

  public ReviewController(ActivityService a, BulkReviewService r) {
    activities = a;
    reviews = r;
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
    return activities.list(true, termId, status, categoryId, fromDate, toDate, search, page, size);
  }

  @PostMapping("/bulk")
  public Map<String, Integer> bulk(@Valid @RequestBody BulkReviewService.Request p) {
    return reviews.review(p);
  }
}
