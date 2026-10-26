package th.ac.sama;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import th.ac.sama.progress.domain.*;
import th.ac.sama.reporting.application.CsvReportExporter;

class ProgressAndCsvTest {
  @Test
  void strategiesUseCorrectUnitAndCapCompletion() {
    var count = new CountProgressCalculator();
    var duration = new DurationProgressCalculator();
    assertThat(count.approvedValue(3, 180)).isEqualTo(3);
    assertThat(duration.approvedValue(3, 180)).isEqualTo(180);
    assertThat(count.percent(10, 3)).isEqualTo(100);
    assertThat(duration.percent(60, 240)).isEqualTo(25);
  }

  @Test
  void csvEscapesQuotesAndNeutralizesSpreadsheetFormulas() {
    var exporter = new CsvReportExporter();
    assertThat(exporter.cell("=SUM(1,2)")).isEqualTo("\"'=SUM(1,2)\"");
    assertThat(exporter.cell("  +cmd")).startsWith("\"'");
    assertThat(exporter.cell("=SUM(1,2)\nmore text")).startsWith("\"'");
    assertThat(exporter.cell("ชื่อ \"ทดสอบ\"")).isEqualTo("\"ชื่อ \"\"ทดสอบ\"\"\"");
  }
}
