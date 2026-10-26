package th.ac.sama.reporting.application;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import th.ac.sama.progress.application.ProgressService.ClassroomSummary;

@Service
public class CsvReportExporter {
  public byte[] export(ClassroomSummary summary) {
    var out =
        new StringBuilder(
            "\uFEFFเลขที่,รหัสนักเรียน,ชื่อ-นามสกุล,ห้อง,เปอร์เซ็นต์ความคืบหน้า,ผลทั้งเทอม,อนุมัติ,รอตรวจ,ส่งคืน,แบบร่าง,นาทีที่อนุมัติ\r\n");
    for (var s : summary.students())
      out.append(s.rollNumber())
          .append(',')
          .append(cell(s.studentCode()))
          .append(',')
          .append(cell(s.studentName()))
          .append(',')
          .append(cell(s.classroom()))
          .append(',')
          .append(s.percent())
          .append(',')
          .append(
              cell(
                  !s.configured()
                      ? "ยังไม่ได้ตั้งเกณฑ์"
                      : s.completed() ? "เสร็จแล้ว" : "ยังไม่เสร็จ"))
          .append(',')
          .append(s.approved())
          .append(',')
          .append(s.pending())
          .append(',')
          .append(s.rejected())
          .append(',')
          .append(s.draft())
          .append(',')
          .append(s.totalApprovedMinutes())
          .append("\r\n");
    return out.toString().getBytes(StandardCharsets.UTF_8);
  }

  public String cell(String text) {
    String s = text == null ? "" : text;
    String trimmed = s.stripLeading();
    if ((!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0)
        || s.startsWith("\t")
        || s.startsWith("\r")
        || s.startsWith("\n")) s = "'" + s;
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }
}
