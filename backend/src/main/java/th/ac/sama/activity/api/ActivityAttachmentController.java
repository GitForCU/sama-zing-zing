package th.ac.sama.activity.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import th.ac.sama.activity.application.ActivityAttachmentService;

@RestController
@RequestMapping("/api/activities/{activityId}/attachments")
public class ActivityAttachmentController {
  public record Remove(@NotNull Long expectedVersion) {}

  private final ActivityAttachmentService service;

  public ActivityAttachmentController(ActivityAttachmentService service) {
    this.service = service;
  }

  @GetMapping
  public List<AttachmentInfo> list(@PathVariable UUID activityId) {
    return service.list(activityId);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ActivityAttachmentService.UploadResult upload(
      @PathVariable UUID activityId,
      @RequestParam long expectedVersion,
      @RequestParam UUID clientUploadId,
      @RequestPart("file") MultipartFile file) {
    return service.upload(activityId, expectedVersion, clientUploadId, file);
  }

  @DeleteMapping("/{attachmentId}")
  public ActivityAttachmentService.VersionResult remove(
      @PathVariable UUID activityId,
      @PathVariable UUID attachmentId,
      @Valid @RequestBody Remove body) {
    return service.remove(activityId, attachmentId, body.expectedVersion());
  }

  @GetMapping("/{attachmentId}")
  public ResponseEntity<byte[]> download(
      @PathVariable UUID activityId, @PathVariable UUID attachmentId) {
    var file = service.download(activityId, attachmentId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(file.name(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("X-Content-Type-Options", "nosniff")
        .cacheControl(CacheControl.noStore())
        .body(file.bytes());
  }
}
