package th.ac.sama.reporting.api;

import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import th.ac.sama.progress.application.ProgressService;
import th.ac.sama.reporting.application.CsvReportExporter;

@RestController
public class ReportController {
  private final ProgressService progress;
  private final CsvReportExporter exporter;

  public ReportController(ProgressService p, CsvReportExporter e) {
    progress = p;
    exporter = e;
  }

  @GetMapping("/api/classrooms/{id}/statistics.csv")
  public ResponseEntity<byte[]> export(@PathVariable UUID id, @RequestParam UUID termId) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sama-classroom.csv\"")
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(exporter.export(progress.classroom(id, termId)));
  }
}
