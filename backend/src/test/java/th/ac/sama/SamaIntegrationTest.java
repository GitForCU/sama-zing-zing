package th.ac.sama;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;

/** Real HTTP, real session cookies, Flyway migrations and isolated PostgreSQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SamaIntegrationTest {
  static EmbeddedPostgres postgres;

  static {
    try {
      postgres =
          EmbeddedPostgres.builder()
              .setPort(0)
              .setServerConfig("listen_addresses", "127.0.0.1")
              .start();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
    r.add("spring.datasource.username", () -> "postgres");
    r.add("spring.datasource.password", () -> "");
  }

  @LocalServerPort int port;
  @Autowired ObjectMapper json;

  @AfterAll
  static void close() throws Exception {
    postgres.close();
  }

  class Client {
    final HttpClient http =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();

    HttpResponse<String> request(String method, String path, Object body, boolean csrf)
        throws Exception {
      var req =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
              .timeout(Duration.ofSeconds(20));
      if (csrf) {
        var token = get("/auth/csrf");
        req.header(token.get("headerName").asText(), token.get("token").asText());
      }
      if (body != null) req.header("Content-Type", "application/json");
      return http.send(
          req.method(
                  method,
                  body == null
                      ? HttpRequest.BodyPublishers.noBody()
                      : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
              .build(),
          HttpResponse.BodyHandlers.ofString());
    }

    JsonNode get(String path) throws Exception {
      var r = request("GET", path, null, false);
      assertThat(r.statusCode()).describedAs(r.body()).isEqualTo(200);
      return json.readTree(r.body());
    }

    JsonNode write(String method, String path, Object body, int expected) throws Exception {
      var r = request(method, path, body, true);
      assertThat(r.statusCode()).describedAs(r.body()).isEqualTo(expected);
      return r.body().isBlank() ? json.nullNode() : json.readTree(r.body());
    }

    JsonNode upload(
        String activity,
        String filename,
        String mime,
        byte[] bytes,
        long version,
        UUID uploadId,
        int expected)
        throws Exception {
      String boundary = "SamaBoundary" + UUID.randomUUID();
      var body = new ByteArrayOutputStream();
      body.write(
          ("--"
                  + boundary
                  + "\r\nContent-Disposition: form-data; name=\"expectedVersion\"\r\n\r\n"
                  + version
                  + "\r\n--"
                  + boundary
                  + "\r\nContent-Disposition: form-data; name=\"clientUploadId\"\r\n\r\n"
                  + uploadId
                  + "\r\n--"
                  + boundary
                  + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                  + filename
                  + "\"\r\nContent-Type: "
                  + mime
                  + "\r\n\r\n")
              .getBytes(StandardCharsets.UTF_8));
      body.write(bytes);
      body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
      var token = get("/auth/csrf");
      var req =
          HttpRequest.newBuilder(
                  URI.create(
                      "http://127.0.0.1:" + port + "/api/activities/" + activity + "/attachments"))
              .header(token.get("headerName").asText(), token.get("token").asText())
              .header("Content-Type", "multipart/form-data; boundary=" + boundary)
              .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
              .build();
      var response = http.send(req, HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode()).describedAs(response.body()).isEqualTo(expected);
      return json.readTree(response.body());
    }

    HttpResponse<byte[]> download(String path) throws Exception {
      return http.send(
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
              .GET()
              .build(),
          HttpResponse.BodyHandlers.ofByteArray());
    }
  }

  Client login(String email) throws Exception {
    var c = new Client();
    c.write("POST", "/auth/login", Map.of("email", email, "password", "SamaDemo123!"), 200);
    assertThat(c.get("/me").get("email").asText()).isEqualTo(email);
    return c;
  }

  String term(Client c) throws Exception {
    return c.get("/terms").get(0).get("id").asText();
  }

  Map<String, Object> fields(Client c, String term) throws Exception {
    var data = new LinkedHashMap<String, Object>();
    data.put("termId", term);
    data.put("categoryId", c.get("/activity-categories?termId=" + term).get(0).get("id").asText());
    data.put("title", "Integration " + UUID.randomUUID());
    data.put("description", "กิจกรรมทดสอบกระบวนการอนุมัติจริง");
    data.put("activityDate", LocalDate.now(ZoneId.of("Asia/Bangkok")).toString());
    data.put("startTime", "15:00");
    data.put("endTime", "16:00");
    return data;
  }

  String teacher(Client student, String email) throws Exception {
    for (var t : student.get("/teachers").get("items")) {
      if (t.get("fullName")
          .asText()
          .contains(email.equals("teacher@sama.local") ? "อรพรรณ" : "ปณิธาน"))
        return t.get("id").asText();
    }
    throw new AssertionError("teacher missing");
  }

  JsonNode draft(Client c, String term) throws Exception {
    return c.write("POST", "/activities", fields(c, term), 200);
  }

  JsonNode submit(Client c, JsonNode a, String teacher) throws Exception {
    return c.write(
        "POST",
        "/activities/" + a.get("id").asText() + "/submit",
        Map.of("reviewerId", teacher, "expectedVersion", a.get("version").asLong()),
        200);
  }

  Map<String, Object> selection(JsonNode a) {
    return Map.of("activityId", a.get("id").asText(), "expectedVersion", a.get("version").asLong());
  }

  JsonNode detail(Client c, JsonNode a) throws Exception {
    return c.get("/activities/" + a.get("id").asText());
  }

  @Test
  void completeReturnResubmitApproveFlowCountsOnlyOnce() throws Exception {
    var student = login("student@sama.local");
    var teacher = login("teacher@sama.local");
    String term = term(student);
    String reviewer = teacher(student, "teacher@sama.local");
    long before = student.get("/me/progress?termId=" + term).get("approved").asLong();
    var a = submit(student, draft(student, term), reviewer);
    assertThat(student.get("/me/progress?termId=" + term).get("approved").asLong())
        .isEqualTo(before);
    teacher.write(
        "POST",
        "/reviews/bulk",
        Map.of(
            "items", List.of(selection(a)), "decision", "REJECT", "reason", "กรุณาเพิ่มรายละเอียด"),
        200);
    var returned = detail(student, a).get("activity");
    assertThat(returned.get("status").asText()).isEqualTo("REJECTED");
    var edits = fields(student, term);
    edits.put("expectedVersion", returned.get("version").asLong());
    returned = student.write("PATCH", "/activities/" + returned.get("id").asText(), edits, 200);
    a = submit(student, returned, reviewer);
    teacher.write(
        "POST",
        "/reviews/bulk",
        Map.of("items", List.of(selection(a)), "decision", "APPROVE"),
        200);
    var finalDetail = detail(student, a);
    assertThat(finalDetail.get("activity").get("status").asText()).isEqualTo("APPROVED");
    assertThat(finalDetail.get("history").size()).isEqualTo(5);
    assertThat(student.get("/me/progress?termId=" + term).get("approved").asLong())
        .isEqualTo(before + 1);
    teacher.write(
        "POST",
        "/reviews/bulk",
        Map.of("items", List.of(selection(a)), "decision", "APPROVE"),
        409);
    assertThat(student.get("/me/progress?termId=" + term).get("approved").asLong())
        .isEqualTo(before + 1);
  }

  @Test
  void historyKeepsTheActualTeacherAfterReviewerChanges() throws Exception {
    var student = login("student@sama.local");
    var firstTeacher = login("teacher@sama.local");
    var firstProfile = firstTeacher.get("/me");
    var activity = submit(student, draft(student, term(student)), firstProfile.get("id").asText());
    firstTeacher.write(
        "POST",
        "/reviews/bulk",
        Map.of(
            "items",
            List.of(selection(activity)),
            "decision",
            "REJECT",
            "reason",
            "ตรวจโดยครูคนแรก"),
        200);
    var returned = detail(student, activity).get("activity");
    activity = submit(student, returned, teacher(student, "teacher2@sama.local"));
    var events = detail(student, activity).get("history");
    var rejection = events.get(2);
    assertThat(rejection.get("toStatus").asText()).isEqualTo("REJECTED");
    assertThat(rejection.get("actorId").asText()).isEqualTo(firstProfile.get("id").asText());
    assertThat(rejection.get("actorName").asText())
        .isEqualTo(firstProfile.get("fullName").asText());
  }

  @Test
  void nullBulkSelectionsAreRejectedWithoutChangingAnyActivity() throws Exception {
    var student = login("student@sama.local");
    var reviewer = login("teacher@sama.local");
    var activity =
        submit(student, draft(student, term(student)), teacher(student, "teacher@sama.local"));
    reviewer.write(
        "POST",
        "/reviews/bulk",
        Map.of("items", Collections.singletonList(null), "decision", "APPROVE"),
        400);
    reviewer.write(
        "POST",
        "/reviews/bulk",
        Map.of("items", Arrays.asList(selection(activity), null), "decision", "APPROVE"),
        400);
    var unchanged = detail(student, activity);
    assertThat(unchanged.get("activity").get("status").asText()).isEqualTo("PENDING");
    assertThat(unchanged.get("activity").get("version").asLong())
        .isEqualTo(activity.get("version").asLong());
    assertThat(unchanged.get("history").size()).isEqualTo(2);
  }

  @Test
  void ownershipAndAssignedReviewerAreEnforced() throws Exception {
    var owner = login("student@sama.local");
    var other = login("student2@sama.local");
    var wrongTeacher = login("teacher2@sama.local");
    String term = term(owner);
    var a = submit(owner, draft(owner, term), teacher(owner, "teacher@sama.local"));
    other.write("PATCH", "/activities/" + a.get("id").asText(), fields(owner, term), 404);
    assertThat(
            other.request("GET", "/activities/" + a.get("id").asText(), null, false).statusCode())
        .isEqualTo(404);
    wrongTeacher.write(
        "POST",
        "/reviews/bulk",
        Map.of("items", List.of(selection(a)), "decision", "APPROVE"),
        404);
    assertThat(owner.request("GET", "/reviews", null, false).statusCode()).isEqualTo(403);
  }

  @Test
  void bulkRollsBackWholeBatchWhenOneItemIsUnauthorized() throws Exception {
    var student = login("student3@sama.local");
    var reviewer = login("teacher@sama.local");
    String term = term(student);
    var own = submit(student, draft(student, term), teacher(student, "teacher@sama.local"));
    var alien = submit(student, draft(student, term), teacher(student, "teacher2@sama.local"));
    reviewer.write(
        "POST",
        "/reviews/bulk",
        Map.of("items", List.of(selection(own), selection(alien)), "decision", "APPROVE"),
        404);
    assertThat(detail(student, own).get("activity").get("status").asText()).isEqualTo("PENDING");
    assertThat(detail(student, alien).get("activity").get("status").asText()).isEqualTo("PENDING");
    assertThat(detail(student, own).get("history").size()).isEqualTo(2);
  }

  @Test
  void staleEditsAndPendingEditsAreRejected() throws Exception {
    var s = login("student@sama.local");
    String term = term(s);
    var a = draft(s, term);
    var edit = fields(s, term);
    edit.put("expectedVersion", a.get("version").asLong());
    var changed = s.write("PATCH", "/activities/" + a.get("id").asText(), edit, 200);
    s.write("PATCH", "/activities/" + a.get("id").asText(), edit, 409);
    var pending = submit(s, changed, teacher(s, "teacher@sama.local"));
    edit.put("expectedVersion", pending.get("version").asLong());
    s.write("PATCH", "/activities/" + a.get("id").asText(), edit, 409);
  }

  @Test
  void sessionCsrfValidationAndLogoutWork() throws Exception {
    var anonymous = new Client();
    assertThat(anonymous.request("GET", "/me", null, false).statusCode()).isEqualTo(401);
    assertThat(
            anonymous
                .request(
                    "POST",
                    "/auth/login",
                    Map.of("email", "student@sama.local", "password", "SamaDemo123!"),
                    false)
                .statusCode())
        .isEqualTo(403);
    anonymous.write(
        "POST", "/auth/login", Map.of("email", "student@sama.local", "password", "wrong"), 401);
    var c = login("student@sama.local");
    var bad = fields(c, term(c));
    bad.put("endTime", "14:00");
    c.write("POST", "/activities", bad, 400);
    c.write("POST", "/auth/logout", null, 200);
    assertThat(c.request("GET", "/me", null, false).statusCode()).isEqualTo(401);
  }

  @Test
  void classroomCsvAndAdvisorAuthorizationAgree() throws Exception {
    var teacher = login("teacher@sama.local");
    String term = term(teacher);
    String room = teacher.get("/classrooms?termId=" + term).get(0).get("id").asText();
    var stats = teacher.get("/classrooms/" + room + "/statistics?termId=" + term);
    assertThat(stats.get("totalStudents").asInt()).isEqualTo(3);
    var csv =
        teacher.request(
            "GET", "/classrooms/" + room + "/statistics.csv?termId=" + term, null, false);
    assertThat(csv.statusCode()).isEqualTo(200);
    assertThat(csv.body()).startsWith("\uFEFF").contains("ชยางกูร สอนเสนา");
    assertThat(csv.body().lines().count()).isEqualTo(stats.get("totalStudents").asLong() + 1);
    var wrong = login("teacher2@sama.local");
    assertThat(
            wrong
                .request("GET", "/classrooms/" + room + "/statistics?termId=" + term, null, false)
                .statusCode())
        .isEqualTo(404);
  }

  @Test
  void favoriteIsIdempotentAndProfilePersists() throws Exception {
    var student = login("student2@sama.local");
    String id = teacher(student, "teacher2@sama.local");
    student.write("PUT", "/me/favorite-teachers/" + id, null, 200);
    student.write("PUT", "/me/favorite-teachers/" + id, null, 200);
    assertThat(student.get("/me/favorite-teachers").size()).isEqualTo(1);
    student.write("DELETE", "/me/favorite-teachers/" + id, null, 200);
    assertThat(student.get("/me/favorite-teachers").size()).isZero();
    var me = student.get("/me");
    student.write(
        "PATCH",
        "/me",
        Map.of(
            "fullName",
            me.get("fullName").asText(),
            "email",
            me.get("email").asText(),
            "phone",
            "0812345678",
            "address",
            "ที่อยู่ทดสอบ"),
        200);
    assertThat(student.get("/me").get("phone").asText()).isEqualTo("0812345678");
  }

  static final byte[] PNG =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aR9sAAAAASUVORK5CYII=");

  @Test
  void evidencePersistsAndOnlyOwnerOrAssignedReviewerCanRead() throws Exception {
    var owner = login("student@sama.local");
    var other = login("student2@sama.local");
    var reviewer = login("teacher@sama.local");
    var wrong = login("teacher2@sama.local");
    var a = draft(owner, term(owner));
    String id = a.get("id").asText();
    String path = "/activities/" + id + "/attachments";
    var uploaded =
        owner.upload(
            id,
            "ภาพกิจกรรม.png",
            "image/png",
            PNG,
            a.get("version").asLong(),
            UUID.randomUUID(),
            200);
    String fileId = uploaded.get("attachment").get("id").asText();
    assertThat(owner.get(path).size()).isEqualTo(1);
    var download = owner.download(path + "/" + fileId);
    assertThat(download.statusCode()).isEqualTo(200);
    assertThat(download.body()).isEqualTo(PNG);
    assertThat(download.headers().firstValue("content-disposition").orElseThrow())
        .contains("attachment");
    assertThat(download.headers().firstValue("cache-control").orElseThrow()).contains("no-store");
    assertThat(other.request("GET", path, null, false).statusCode()).isEqualTo(404);
    assertThat(other.download(path + "/" + fileId).statusCode()).isEqualTo(404);
    assertThat(reviewer.download(path + "/" + fileId).statusCode()).isEqualTo(404);
    ((com.fasterxml.jackson.databind.node.ObjectNode) a)
        .put("version", uploaded.get("activityVersion").asLong());
    a = submit(owner, a, teacher(owner, "teacher@sama.local"));
    assertThat(reviewer.download(path + "/" + fileId).body()).isEqualTo(PNG);
    assertThat(wrong.download(path + "/" + fileId).statusCode()).isEqualTo(404);
    owner.upload(
        id, "ใหม่.png", "image/png", PNG, a.get("version").asLong(), UUID.randomUUID(), 409);
    owner.write(
        "DELETE", path + "/" + fileId, Map.of("expectedVersion", a.get("version").asLong()), 409);
    assertThat(owner.get(path).size()).isEqualTo(1);
  }

  @Test
  void uploadRetryIsIdempotentAndValidatesContentVersionAndSize() throws Exception {
    var owner = login("student@sama.local");
    var a = draft(owner, term(owner));
    String id = a.get("id").asText();
    String path = "/activities/" + id + "/attachments";
    long originalVersion = a.get("version").asLong();
    UUID uploadId = UUID.randomUUID();
    var first =
        owner.upload(
            id, "proof.png", "application/octet-stream", PNG, originalVersion, uploadId, 200);
    var again = owner.upload(id, "proof.png", "image/png", PNG, originalVersion, uploadId, 200);
    assertThat(again.get("attachment").get("id")).isEqualTo(first.get("attachment").get("id"));
    assertThat(again.get("activityVersion")).isEqualTo(first.get("activityVersion"));
    assertThat(again.get("attachment").get("contentType").asText()).isEqualTo("image/png");
    owner.upload(id, "second.png", "image/png", PNG, originalVersion, UUID.randomUUID(), 409);
    long version = first.get("activityVersion").asLong();
    owner.upload(
        id,
        "fake.png",
        "image/png",
        "<html>not an image</html>".getBytes(StandardCharsets.UTF_8),
        version,
        UUID.randomUUID(),
        400);
    owner.upload(id, "script.svg", "image/svg+xml", PNG, version, UUID.randomUUID(), 400);
    owner.upload(id, "empty.pdf", "application/pdf", new byte[0], version, UUID.randomUUID(), 400);
    var tooLarge = new byte[5 * 1024 * 1024 + 1];
    System.arraycopy(PNG, 0, tooLarge, 0, PNG.length);
    owner.upload(id, "too-large.png", "image/png", tooLarge, version, UUID.randomUUID(), 413);
    assertThat(owner.get(path).size()).isEqualTo(1);
  }

  @Test
  void evidenceLimitAndRemovalAreTransactionalAndVersioned() throws Exception {
    var owner = login("student3@sama.local");
    var a = draft(owner, term(owner));
    String id = a.get("id").asText();
    String path = "/activities/" + id + "/attachments";
    long version = a.get("version").asLong();
    String firstId = null;
    for (int i = 0; i < 5; i++) {
      var file =
          owner.upload(id, "proof" + i + ".png", "image/png", PNG, version, UUID.randomUUID(), 200);
      version = file.get("activityVersion").asLong();
      if (firstId == null) firstId = file.get("attachment").get("id").asText();
    }
    owner.upload(id, "sixth.png", "image/png", PNG, version, UUID.randomUUID(), 400);
    assertThat(owner.get(path).size()).isEqualTo(5);
    var removed =
        owner.write("DELETE", path + "/" + firstId, Map.of("expectedVersion", version), 200);
    assertThat(removed.get("activityVersion").asLong()).isGreaterThan(version);
    owner.write("DELETE", path + "/" + firstId, Map.of("expectedVersion", version), 200);
    assertThat(owner.get(path).size()).isEqualTo(4);
    var b = draft(owner, term(owner));
    String otherId = b.get("id").asText();
    var otherFile =
        owner.upload(
            otherId,
            "other.png",
            "image/png",
            PNG,
            b.get("version").asLong(),
            UUID.randomUUID(),
            200);
    owner.write(
        "DELETE",
        path + "/" + otherFile.get("attachment").get("id").asText(),
        Map.of("expectedVersion", removed.get("activityVersion").asLong()),
        404);
    assertThat(owner.get("/activities/" + otherId + "/attachments").size()).isEqualTo(1);
  }

  @Test
  void returnedActivityAllowsEvidenceChangesAndKeepsCustomName() throws Exception {
    var owner = login("student@sama.local");
    var reviewer = login("teacher@sama.local");
    String term = term(owner);
    var a = draft(owner, term);
    String id = a.get("id").asText();
    String path = "/activities/" + id + "/attachments";
    byte[] pdf = "%PDF-1.4\n% test evidence\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    var file =
        owner.upload(
            id,
            "../../หลักฐาน.pdf",
            "application/pdf",
            pdf,
            a.get("version").asLong(),
            UUID.randomUUID(),
            200);
    assertThat(file.get("attachment").get("originalName").asText()).isEqualTo("หลักฐาน.pdf");
    String fileId = file.get("attachment").get("id").asText();
    assertThat(
            owner.download(path + "/" + fileId).headers().firstValue("content-type").orElseThrow())
        .contains("application/pdf");
    ((com.fasterxml.jackson.databind.node.ObjectNode) a)
        .put("version", file.get("activityVersion").asLong());
    a = submit(owner, a, teacher(owner, "teacher@sama.local"));
    reviewer.write(
        "POST",
        "/reviews/bulk",
        Map.of(
            "items",
            List.of(selection(a)),
            "decision",
            "REJECT",
            "reason",
            "กรุณาแก้ไขชื่อและหลักฐาน"),
        200);
    var returned = detail(owner, a).get("activity");
    var fields = fields(owner, term);
    fields.put("title", "พบครูเพื่อวางแผนการเรียน");
    fields.put("expectedVersion", returned.get("version").asLong());
    var edited = owner.write("PATCH", "/activities/" + id, fields, 200);
    owner.write(
        "DELETE",
        path + "/" + fileId,
        Map.of("expectedVersion", edited.get("version").asLong()),
        200);
    assertThat(owner.get(path).size()).isZero();
    assertThat(detail(owner, a).get("activity").get("title").asText())
        .isEqualTo("พบครูเพื่อวางแผนการเรียน");
    assertThat(detail(owner, a).get("activity").get("status").asText()).isEqualTo("REJECTED");
  }
}
