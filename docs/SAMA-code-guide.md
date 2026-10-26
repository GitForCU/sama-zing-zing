# คู่มือโค้ด SAMA: OOP และหน้าที่แต่ละไฟล์

เอกสารนี้อธิบายโค้ดปัจจุบันในโปรเจกต์ โดยแยกไฟล์ที่เราเขียนออกจากไฟล์ที่เครื่องมือสร้างให้อัตโนมัติ เส้นทางในตารางอ้างอิงจากโฟลเดอร์ที่ระบุไว้เหนือตาราง

## 1. ระบบนี้เป็น OOP ไหม

**Backend เป็น OOP ด้วย Java** ทั้งข้อมูลและพฤติกรรมของงานอยู่ในคลาส เช่น `Activity` มีข้อมูลกิจกรรมและเมธอดส่งตรวจ/อนุมัติ ส่วน **Frontend ใช้ React แบบ function components และ hooks** จึงไม่ได้ใช้ class-based OOP เป็นหลัก การใช้สองแนวทางร่วมกันเหมาะกับโครงสร้างโค้ดปัจจุบัน

OOP เป็นวิธีออกแบบโค้ด ส่วน Layered Architecture และ Client–Server เป็นการจัดโครงสร้างระบบ จึงใช้ร่วมกันได้

| หลักการ | ตัวอย่างจริงในระบบ | ความหมาย |
|---|---|---|
| Encapsulation — ห่อหุ้มข้อมูล | `Activity` เก็บ `status` เป็น `private` และให้เปลี่ยนผ่าน `submit()`, `approve()`, `returnForRevision()` | ก่อนเปลี่ยนสถานะ คลาสตรวจเจ้าของ ผู้ตรวจ สถานะปัจจุบัน และข้อมูลที่จำเป็น ไม่เปิดให้ตั้งสถานะได้ตามใจ |
| Inheritance — สืบทอด | `Student extends User`, `Teacher extends User` และ `User extends BaseEntity` | นักเรียน/ครูใช้ข้อมูลบัญชีร่วมกัน แต่เพิ่มข้อมูลเฉพาะบทบาทได้ |
| Abstraction — กำหนดสัญญาการทำงาน | `User` เป็น abstract class และ `ProgressCalculator` เป็น interface | ระบุสิ่งที่คลาสลูกต้องทำ เช่น `getRole()` หรือ `approvedValue()` โดยซ่อนรายละเอียดการคำนวณไว้ใน implementation |
| Polymorphism — เรียกผ่านชนิดร่วมกัน | `CountProgressCalculator` และ `DurationProgressCalculator` implement `ProgressCalculator` | `ProgressService` เรียก interface เดียวกัน แต่ได้ผลคำนวณตามเกณฑ์นับครั้งหรือนับนาที |
| Composition — ประกอบออบเจ็กต์ | `User` มี `Profile`; `Activity` มีรายการ `ActivityStatusHistory` | แบ่งความรับผิดชอบเป็นออบเจ็กต์ย่อย แล้วนำมาประกอบกัน |

ตัวอย่างต่อไปนี้ย่อจากโค้ดจริงเพื่อแสดงความสัมพันธ์ ไม่ใช่โค้ดทั้งหมดของคลาส:

```java
public abstract class User extends BaseEntity {
  public abstract String getRole();
}

public class Student extends User {
  @Override
  public String getRole() { return "STUDENT"; }
}

public class Teacher extends User {
  @Override
  public String getRole() { return "TEACHER"; }
}
```

การมีหลายคลาสอย่างเดียวไม่เพียงพอที่จะพิสูจน์ OOP จุดที่เห็นชัดในระบบนี้คือ **คลาสคุมพฤติกรรมของตัวเอง** และ **เรียก implementation หลายแบบผ่าน interface เดียวกัน**

## 2. ภาพรวมสถาปัตยกรรม

```text
Browser: React + TypeScript
       │ HTTP / JSON หรือ multipart เมื่อแนบไฟล์
       ▼
Spring Security: session, CSRF, การยืนยันตัวตน
       ▼
Controller (api)          รับคำขอและส่งผลลัพธ์
       ▼
Service (application)     จัดลำดับงาน ตรวจสิทธิ์ และคุม transaction
       ├── Domain         ออบเจ็กต์และกฎของงาน
       └── Repository     อ่าน/เขียนผ่าน Spring Data JPA
                 ▼
             PostgreSQL
```

- **Client–Server:** React เป็น client; Spring Boot เป็น server ที่ให้ API
- **Layered Architecture:** แบ่งหน้าที่รับคำขอ จัดการงาน กฎธุรกิจ และจัดเก็บข้อมูล
- Backend เป็น **modular monolith**: แบ่งเป็นโมดูล แต่ทำงานในแอป Spring Boot เดียว
- `Repository` ในโค้ดนี้คือ **Repository pattern** สำหรับติดต่อฐานข้อมูล ไม่ได้หมายความว่าเลือก Repository Architecture เป็นสถาปัตยกรรมหลักอีกแบบ

ข้อเท็จจริงของโค้ดปัจจุบัน: ไม่ได้บังคับให้ทุก endpoint ผ่าน service เสมอ เช่น `AcademicController` อ่าน repository โดยตรง และ `IdentityController` อัปเดตโปรไฟล์ใน controller อีกทั้ง `ActivityService` ใช้ DTO จากแพ็กเกจ `api` ด้วย จึงเป็น Layered แบบใช้งานจริง ไม่ใช่การแยกชั้นที่เข้มงวดทุกจุด หากต้องการแยกชั้นให้ชัดขึ้น สามารถย้ายงานเหล่านี้เข้า service และแยก application DTO ภายหลังได้

## 3. คำศัพท์ของโฟลเดอร์ Backend

| โฟลเดอร์ | หน้าที่ |
|---|---|
| `api` | Controller และ DTO สำหรับรับ/ส่ง HTTP |
| `application` | Service ที่จัดการ use case เช่น สร้างกิจกรรม ส่งตรวจ หรือคำนวณสถิติ |
| `domain` | Entity, enum, interface และกฎของงาน |
| `infrastructure` | Repository ที่ติดต่อฐานข้อมูล |
| `shared` | ส่วนกลาง เช่น security, error, base class และข้อมูลสาธิต |

`Entity` คือออบเจ็กต์ที่มีตัวตนและถูกบันทึกลงฐานข้อมูล ส่วน `DTO` คือข้อมูลที่เลือกส่งผ่าน API และ `record` ใน Java เป็นรูปแบบคลาสสำหรับบรรจุข้อมูล ไม่ใช่ตารางฐานข้อมูลโดยอัตโนมัติ

## 4. Backend: ไฟล์เริ่มต้นและตั้งค่า

อ้างอิงจากโฟลเดอร์ `backend/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [pom.xml](../backend/pom.xml) | กำหนด Java 21, Spring Boot และ dependencies เช่น JPA, Security, PostgreSQL, Flyway รวมถึงคำสั่ง build/test ของ Maven |
| [Dockerfile](../backend/Dockerfile) | build Java backend แล้วเตรียม container สำหรับรันไฟล์ JAR |
| [src/main/resources/application.yml](../backend/src/main/resources/application.yml) | ตั้งค่าฐานข้อมูล JPA, session, cookie, port และขนาดไฟล์อัปโหลด โดยรับค่าบางส่วนจาก environment |
| [src/main/resources/application-supabase.yml](../backend/src/main/resources/application-supabase.yml) | profile สำหรับ Supabase แยกตารางใน schema `sama` และตั้ง search path/connection pool |
| [V1__initial_schema.sql](../backend/src/main/resources/db/migration/V1__initial_schema.sql) | Flyway migration สร้างตารางเริ่มต้น เช่น บัญชี ห้องเรียน เทอม กิจกรรม ประวัติ รายการโปรด และ session |
| [V2__activity_evidence.sql](../backend/src/main/resources/db/migration/V2__activity_evidence.sql) | เพิ่มตารางหลักฐานแนบและข้อมูลที่ช่วยควบคุมการแก้ไฟล์/การอัปโหลดซ้ำ |
| [V3__supabase_auth_identity.sql](../backend/src/main/resources/db/migration/V3__supabase_auth_identity.sql) | เพิ่ม `auth_user_id` แบบ unique เพื่อเชื่อมตัวตน Supabase กับบัญชีโรงเรียน |
| [SamaApplication.java](../backend/src/main/java/th/ac/sama/SamaApplication.java) | จุดเริ่มต้น production backend เรียก Spring Boot ให้สร้างและเชื่อมส่วนต่าง ๆ ของแอป |

ไฟล์ migration รันตามลำดับเวอร์ชัน ควรเพิ่มเวอร์ชันใหม่เมื่อเปลี่ยนโครงสร้างฐานข้อมูลที่ใช้งานแล้ว

## 5. Backend: บัญชีผู้ใช้ — identity

อ้างอิงจาก `backend/src/main/java/th/ac/sama/identity/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [domain/User.java](../backend/src/main/java/th/ac/sama/identity/domain/User.java) | abstract entity ของบัญชี เก็บอีเมล password hash ชื่อ โปรไฟล์ และสถานะใช้งาน กำหนด `getRole()` ให้คลาสลูกทำ |
| [domain/Student.java](../backend/src/main/java/th/ac/sama/identity/domain/Student.java) | สืบทอด `User` เพิ่มรหัสนักเรียน และคืนบทบาท `STUDENT` |
| [domain/Teacher.java](../backend/src/main/java/th/ac/sama/identity/domain/Teacher.java) | สืบทอด `User` เพิ่มข้อมูลแผนก และคืนบทบาท `TEACHER` |
| [domain/Profile.java](../backend/src/main/java/th/ac/sama/identity/domain/Profile.java) | เก็บเบอร์โทรและที่อยู่ เป็นออบเจ็กต์ที่ฝังใน `User` ผ่าน JPA `@Embedded` |
| [application/CurrentUser.java](../backend/src/main/java/th/ac/sama/identity/application/CurrentUser.java) | หา user ปัจจุบันจาก Spring Security โหลดบัญชี และตรวจว่าคำขอนี้เป็นนักเรียนหรือครู |
| [application/LoginService.java](../backend/src/main/java/th/ac/sama/identity/application/LoginService.java) | ส่งการยืนยันตัวตนไป Supabase Auth ในโหมด Supabase แล้วโหลดสิทธิ์จากบัญชีที่ผูก User ID; โหมด local ใช้บัญชี demo เดิม |
| [infrastructure/SupabaseAuthClient.java](../backend/src/main/java/th/ac/sama/identity/infrastructure/SupabaseAuthClient.java) | เรียก password grant และ logout ของ Supabase จาก Backend พร้อม timeout และข้อความ error ที่ไม่เปิดเผยข้อมูลลับ |
| [api/IdentityController.java](../backend/src/main/java/th/ac/sama/identity/api/IdentityController.java) | API รับ CSRF token, login, logout, ดูบัญชีตัวเอง และแก้โปรไฟล์ รวม DTO ของงานเหล่านี้ |
| [infrastructure/UserRepository.java](../backend/src/main/java/th/ac/sama/identity/infrastructure/UserRepository.java) | อ่าน/เขียนผู้ใช้ ค้นหาอีเมลโดยไม่แยกตัวพิมพ์เล็กใหญ่ และค้นหาจาก Supabase User ID ที่ผูกไว้ |
| [infrastructure/TeacherRepository.java](../backend/src/main/java/th/ac/sama/identity/infrastructure/TeacherRepository.java) | ค้นหาครูที่เปิดใช้งานตามชื่อ พร้อมแบ่งหน้า |

`User` ใช้ JPA inheritance แบบ `JOINED`: ข้อมูลร่วมอยู่ใน `app_users` ส่วนข้อมูลเฉพาะนักเรียน/ครูอยู่ในตารางของคลาสลูก

## 6. Backend: ปีการศึกษาและห้องเรียน — academic

อ้างอิงจาก `backend/src/main/java/th/ac/sama/academic/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [domain/AcademicTerm.java](../backend/src/main/java/th/ac/sama/academic/domain/AcademicTerm.java) | เก็บปีการศึกษา เทอม วันเริ่ม/จบ และกำหนดส่ง พร้อมชื่อเทอมที่ใช้แสดงผล |
| [domain/Classroom.java](../backend/src/main/java/th/ac/sama/academic/domain/Classroom.java) | ข้อมูลห้องเรียน เช่น ปีการศึกษา ระดับชั้น และชื่อห้อง |
| [domain/Enrollment.java](../backend/src/main/java/th/ac/sama/academic/domain/Enrollment.java) | เชื่อมนักเรียนกับห้องและเทอม รวมเลขที่ ทำให้แยกข้อมูลการอยู่ห้องตามเทอมได้ |
| [domain/ClassroomAdvisor.java](../backend/src/main/java/th/ac/sama/academic/domain/ClassroomAdvisor.java) | เชื่อมครูที่ปรึกษากับห้องและเทอม ใช้กำหนดสิทธิ์ดูสถิติห้อง |
| [api/AcademicController.java](../backend/src/main/java/th/ac/sama/academic/api/AcademicController.java) | API รายการเทอม ห้องที่ครูดูแล และหมวดกิจกรรม/เกณฑ์ที่เกี่ยวข้องกับผู้ใช้ |
| [infrastructure/TermRepository.java](../backend/src/main/java/th/ac/sama/academic/infrastructure/TermRepository.java) | อ่าน/เขียนเทอม และเรียงปี/เทอมล่าสุดก่อน |
| [infrastructure/ClassroomRepository.java](../backend/src/main/java/th/ac/sama/academic/infrastructure/ClassroomRepository.java) | อ่าน/เขียนข้อมูลห้องเรียน |
| [infrastructure/EnrollmentRepository.java](../backend/src/main/java/th/ac/sama/academic/infrastructure/EnrollmentRepository.java) | ค้นหาการลงทะเบียนตามนักเรียน/เทอม หรือห้อง/เทอม |
| [infrastructure/AdvisorRepository.java](../backend/src/main/java/th/ac/sama/academic/infrastructure/AdvisorRepository.java) | ค้นหาห้องที่ครูดูแล และตรวจความสัมพันธ์ครูที่ปรึกษา |

## 7. Backend: กิจกรรมและหลักฐาน — activity

อ้างอิงจาก `backend/src/main/java/th/ac/sama/activity/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [domain/Activity.java](../backend/src/main/java/th/ac/sama/activity/domain/Activity.java) | หัวใจของกิจกรรม เก็บเจ้าของ หมวด เทอม ชื่อ รายละเอียด วันเวลา ผู้ตรวจ สถานะ และประวัติ มีเมธอดแก้ไข ส่งตรวจ อนุมัติ ส่งคืน และตรวจว่าทำได้หรือไม่ |
| [domain/ActivityCategory.java](../backend/src/main/java/th/ac/sama/activity/domain/ActivityCategory.java) | ข้อมูลหมวดกิจกรรม เช่น รหัส ชื่อ คำอธิบาย และสี |
| [domain/ActivityStatus.java](../backend/src/main/java/th/ac/sama/activity/domain/ActivityStatus.java) | enum สถานะ `DRAFT`, `PENDING`, `APPROVED`, `REJECTED` |
| [domain/ActivityStatusHistory.java](../backend/src/main/java/th/ac/sama/activity/domain/ActivityStatusHistory.java) | เหตุการณ์เปลี่ยนสถานะ เก็บสถานะเดิม/ใหม่ ผู้ทำ เวลา ลำดับ และเหตุผล ใช้สร้าง timeline |
| [domain/RequirementMetric.java](../backend/src/main/java/th/ac/sama/activity/domain/RequirementMetric.java) | enum หน่วยของเกณฑ์: นับจำนวน `COUNT` หรือนับนาที `DURATION_MINUTES` |
| [domain/ActivityAttachment.java](../backend/src/main/java/th/ac/sama/activity/domain/ActivityAttachment.java) | Entity หลักฐาน เก็บชื่อไฟล์ ชนิด ขนาด hash, upload ID, เวลา และเนื้อหาไฟล์ในฐานข้อมูล |
| [domain/EvidenceFilePolicy.java](../backend/src/main/java/th/ac/sama/activity/domain/EvidenceFilePolicy.java) | รวมกฎไฟล์: สูงสุด 5 ไฟล์ ไฟล์ละ 5 MiB ตรวจนามสกุลและ signature ของรูป/PDF จัดชื่อไฟล์และสร้าง SHA-256; ไม่ใช่โปรแกรมสแกนมัลแวร์ |
| [application/ActivityService.java](../backend/src/main/java/th/ac/sama/activity/application/ActivityService.java) | ประสานงานสร้าง/แก้/ส่ง/อ่าน/ค้นหากิจกรรม ตรวจสิทธิ์ เกณฑ์ เทอม และ version แล้วเรียกพฤติกรรมใน `Activity` |
| [application/ActivityAttachmentService.java](../backend/src/main/java/th/ac/sama/activity/application/ActivityAttachmentService.java) | จัดการรายการหลักฐาน อัปโหลด ลบ และดาวน์โหลด ตรวจสิทธิ์/สถานะ ใช้ lock และ version ป้องกันคำขอชนกัน และรองรับการส่งอัปโหลดซ้ำอย่างไม่สร้างไฟล์ซ้ำ |
| [api/ActivityController.java](../backend/src/main/java/th/ac/sama/activity/api/ActivityController.java) | รับ HTTP ของกิจกรรม แปลง input/filter แล้วส่งงานให้ `ActivityService` |
| [api/ActivityDtos.java](../backend/src/main/java/th/ac/sama/activity/api/ActivityDtos.java) | รวม record เช่น `Save`, `Submit`, `Item`, `Detail`, `Event`, `PageResult` และแปลง entity เป็นผลลัพธ์ API |
| [api/ActivityAttachmentController.java](../backend/src/main/java/th/ac/sama/activity/api/ActivityAttachmentController.java) | API หลักฐาน รับ multipart อัปโหลด ลบ และส่ง bytes ตอนดาวน์โหลดพร้อม HTTP headers |
| [api/AttachmentInfo.java](../backend/src/main/java/th/ac/sama/activity/api/AttachmentInfo.java) | DTO เฉพาะ metadata ของไฟล์ เพื่อแสดงรายการโดยไม่ส่งเนื้อหาไฟล์ทั้งหมด |
| [infrastructure/ActivityRepository.java](../backend/src/main/java/th/ac/sama/activity/infrastructure/ActivityRepository.java) | CRUD กิจกรรม รองรับเงื่อนไขค้นหา สรุปจำนวน/นาที และ lock แถวกิจกรรมระหว่างแก้หลักฐาน |
| [infrastructure/CategoryRepository.java](../backend/src/main/java/th/ac/sama/activity/infrastructure/CategoryRepository.java) | อ่าน/เขียนหมวดกิจกรรมและเรียงตามรหัส |
| [infrastructure/AttachmentRepository.java](../backend/src/main/java/th/ac/sama/activity/infrastructure/AttachmentRepository.java) | อ่าน/เขียนหลักฐาน นับจำนวน ค้นหา upload ID และอ่าน metadata โดยไม่ดึงเนื้อหาไฟล์มาทั้งก้อน |

สถานะทำงานดังนี้:

```text
DRAFT ──ส่งตรวจ──> PENDING ──อนุมัติ──> APPROVED
                     │
                     └──ส่งคืนพร้อมเหตุผล──> REJECTED ──แก้ไข/ส่งใหม่──> PENDING
```

นักเรียนแก้ข้อมูลและไฟล์ได้เมื่อเป็น `DRAFT` หรือ `REJECTED` เท่านั้น `Activity` ตรวจเงื่อนไขสถานะอีกชั้นที่ backend การซ่อนปุ่มบนหน้าเว็บอย่างเดียวไม่ใช่การควบคุมสิทธิ์

## 8. Backend: ตรวจหลายรายการ — review

อ้างอิงจาก `backend/src/main/java/th/ac/sama/review/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [application/BulkReviewService.java](../backend/src/main/java/th/ac/sama/review/application/BulkReviewService.java) | อนุมัติ/ส่งคืนสูงสุด 50 รายการ ตรวจรายการซ้ำ สิทธิ์ สถานะ และ version แล้วทำใน transaction เดียว ถ้ารายการใดผิดพลาดจะไม่บันทึกทั้งชุด |
| [api/ReviewController.java](../backend/src/main/java/th/ac/sama/review/api/ReviewController.java) | API รายการรอตรวจของครูและคำขอตรวจแบบกลุ่ม ส่งต่อไปยัง service ที่เกี่ยวข้อง |

## 9. Backend: ครูที่ชื่นชอบ — favorite

อ้างอิงจาก `backend/src/main/java/th/ac/sama/favorite/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [domain/FavoriteTeacher.java](../backend/src/main/java/th/ac/sama/favorite/domain/FavoriteTeacher.java) | Entity ความสัมพันธ์ระหว่างนักเรียนกับครูที่ชื่นชอบ |
| [application/FavoriteTeacherService.java](../backend/src/main/java/th/ac/sama/favorite/application/FavoriteTeacherService.java) | ค้นหาครู อ่าน/เพิ่ม/ลบรายการโปรด และจัดข้อมูลครูสำหรับหน้าจอ |
| [api/FavoriteTeacherController.java](../backend/src/main/java/th/ac/sama/favorite/api/FavoriteTeacherController.java) | API ค้นหาครูและจัดการครูที่ชื่นชอบของนักเรียนปัจจุบัน |
| [infrastructure/FavoriteRepository.java](../backend/src/main/java/th/ac/sama/favorite/infrastructure/FavoriteRepository.java) | ค้นหา ตรวจว่ามี และลบความสัมพันธ์รายการโปรด |

## 10. Backend: เกณฑ์และความคืบหน้า — progress

อ้างอิงจาก `backend/src/main/java/th/ac/sama/progress/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [domain/ActivityRequirement.java](../backend/src/main/java/th/ac/sama/progress/domain/ActivityRequirement.java) | Entity เกณฑ์ของหมวด ตามเทอมและระดับชั้น ระบุหน่วยและเป้าหมายที่ต้องทำ |
| [domain/ProgressCalculator.java](../backend/src/main/java/th/ac/sama/progress/domain/ProgressCalculator.java) | interface สำหรับเลือกค่าที่นับและคำนวณเปอร์เซ็นต์ เป็นสัญญาร่วมของกลยุทธ์คำนวณ |
| [domain/CountProgressCalculator.java](../backend/src/main/java/th/ac/sama/progress/domain/CountProgressCalculator.java) | implementation สำหรับเกณฑ์จำนวนครั้ง ใช้จำนวนกิจกรรมที่อนุมัติ |
| [domain/DurationProgressCalculator.java](../backend/src/main/java/th/ac/sama/progress/domain/DurationProgressCalculator.java) | implementation สำหรับเกณฑ์เวลา ใช้ผลรวมนาทีที่อนุมัติ |
| [application/ProgressService.java](../backend/src/main/java/th/ac/sama/progress/application/ProgressService.java) | รวมข้อมูลกิจกรรม เลือก calculator ตามเกณฑ์ สรุปต่อหมวด/นักเรียน/ห้อง และตรวจสิทธิ์ครูที่ปรึกษา ใช้ข้อมูลที่อนุมัติในการนับความคืบหน้า |
| [api/ProgressController.java](../backend/src/main/java/th/ac/sama/progress/api/ProgressController.java) | API ความคืบหน้าตัวเอง นักเรียนรายบุคคล และสถิติห้อง |
| [infrastructure/RequirementRepository.java](../backend/src/main/java/th/ac/sama/progress/infrastructure/RequirementRepository.java) | อ่านเกณฑ์ตามเทอมและระดับชั้น พร้อมหมวดที่เกี่ยวข้อง |

โครงสร้าง calculator เป็นตัวอย่าง **Strategy pattern**: ถ้าเพิ่มวิธีคิดใหม่ จะเพิ่ม implementation และเชื่อม metric ที่เกี่ยวข้อง แทนการกระจายสูตรไปตามหน้าจอ

## 11. Backend: รายงาน — reporting

อ้างอิงจาก `backend/src/main/java/th/ac/sama/reporting/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [application/CsvReportExporter.java](../backend/src/main/java/th/ac/sama/reporting/application/CsvReportExporter.java) | แปลงสถิติเป็น CSV มี UTF-8 BOM สำหรับภาษาไทย escape ข้อมูลและลดความเสี่ยงที่โปรแกรมตารางจะตีความข้อความเป็นสูตร |
| [api/ReportController.java](../backend/src/main/java/th/ac/sama/reporting/api/ReportController.java) | รับคำขอดาวน์โหลดรายงาน ใช้สถิติจาก service และส่ง CSV เป็นไฟล์แนบ |

## 12. Backend: ส่วนกลาง — shared

อ้างอิงจาก `backend/src/main/java/th/ac/sama/shared/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [domain/BaseEntity.java](../backend/src/main/java/th/ac/sama/shared/domain/BaseEntity.java) | abstract base class สำหรับ UUID `id` ที่ entity ต่าง ๆ ใช้ร่วมกัน |
| [config/DemoData.java](../backend/src/main/java/th/ac/sama/shared/config/DemoData.java) | สร้างบัญชี เทอม ห้อง เกณฑ์ และกิจกรรมตัวอย่างเมื่อเปิด profile `demo` และยังไม่มีผู้ใช้ |
| [security/SecurityConfig.java](../backend/src/main/java/th/ac/sama/shared/security/SecurityConfig.java) | ตั้งค่า Spring Security, BCrypt, วิธีโหลด user, session, CSRF และเงื่อนไขเข้าถึง API |
| [security/SamaPrincipal.java](../backend/src/main/java/th/ac/sama/shared/security/SamaPrincipal.java) | record ที่ implement `UserDetails` และ `Serializable` เก็บข้อมูลตัวตนที่ Spring Security ใช้ใน session |
| [security/AuthSettings.java](../backend/src/main/java/th/ac/sama/shared/security/AuthSettings.java) | รับและตรวจโหมด login, Project URL และ publishable key |
| [security/SupabaseSession.java](../backend/src/main/java/th/ac/sama/shared/security/SupabaseSession.java) | เก็บ subject, access token และวันหมดอายุเฉพาะฝั่ง server โดยซ่อน credentials ในข้อความ log |
| [security/SupabaseSessionFilter.java](../backend/src/main/java/th/ac/sama/shared/security/SupabaseSessionFilter.java) | ปฏิเสธ session เก่าคนละโหมดหรือเกินอายุ Supabase token |
| [error/BusinessException.java](../backend/src/main/java/th/ac/sama/shared/error/BusinessException.java) | exception ของเงื่อนไขงาน เช่น ข้อมูลไม่ครบ ไม่พบรายการ หรือสถานะขัดแย้ง พร้อม HTTP status |
| [error/ApiExceptionHandler.java](../backend/src/main/java/th/ac/sama/shared/error/ApiExceptionHandler.java) | รวมการแปลง exception เป็นข้อความ/สถานะ HTTP ที่ frontend จัดการได้ เช่น validation, conflict และไฟล์ใหญ่เกิน |

Spring สร้าง service/repository และส่ง dependency ผ่าน constructor เรียกว่า **Dependency Injection** ส่วน repository ที่ extends `JpaRepository` ไม่ต้องเขียน implementation CRUD เอง เพราะ Spring Data สร้างให้ตอนรัน

## 13. Backend: ตัวรัน local และการทดสอบ

อ้างอิงจาก `backend/src/test/java/th/ac/sama/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [LocalApplication.java](../backend/src/test/java/th/ac/sama/LocalApplication.java) | ตัวรันสำหรับเครื่องพัฒนา เปิด PostgreSQL จริงผ่าน embedded launcher เก็บข้อมูลถาวรใน `.local/postgres` และเปิด API ที่ port 8080 |
| [E2eApplication.java](../backend/src/test/java/th/ac/sama/E2eApplication.java) | ตัวรันสำหรับ browser tests ใช้ฐานข้อมูลชั่วคราวแยกจากข้อมูลผู้ใช้ และ API port 8081 |
| [SamaIntegrationTest.java](../backend/src/test/java/th/ac/sama/SamaIntegrationTest.java) | ทดสอบ API ผ่าน HTTP และ PostgreSQL จริง เช่น login, สิทธิ์, สถานะ, bulk rollback, version และหลักฐาน |
| [ProgressAndCsvTest.java](../backend/src/test/java/th/ac/sama/ProgressAndCsvTest.java) | ทดสอบสูตรความคืบหน้าและการสร้าง CSV ที่ escape ข้อมูลได้ถูกต้อง |
| [SupabaseDatabaseTest.java](../backend/src/test/java/th/ac/sama/SupabaseDatabaseTest.java) | ทดสอบ profile Supabase บน PostgreSQL แยก รวม schema, Flyway, entity และ session login |
| [SupabaseAuthIntegrationTest.java](../backend/src/test/java/th/ac/sama/SupabaseAuthIntegrationTest.java) | ทดสอบ Auth ผ่าน HTTP กับ Supabase จำลองและ PostgreSQL แยก รวมสิทธิ์ การหมดอายุ logout และไม่เผย credentials ใน log |

ตัวรัน local อยู่ใน test source เพื่อไม่ให้ embedded PostgreSQL ติดเข้าไปใน production JAR; ชื่อโฟลเดอร์ `test` จึงไม่ได้แปลว่าทุกไฟล์ในนั้นเป็น test case

## 14. Frontend: จุดเริ่มต้นและส่วนที่ใช้ร่วมกัน

อ้างอิงจาก `frontend/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [index.html](../frontend/index.html) | HTML หลัก มีจุดให้ React แสดงผล title, metadata และเรียกไฟล์เริ่มต้น |
| [src/main.tsx](../frontend/src/main.tsx) | สร้าง React root และ `QueryClientProvider` สำหรับจัดการข้อมูลที่ดึงจาก server |
| [src/App.tsx](../frontend/src/App.tsx) | โครงแอปหลัก โหลดบัญชี/เทอม/หมวด เลือกหน้าตาม hash จัดเมนู desktop/mobile, modal, logout และข้อความแจ้งผล |
| [src/styles.css](../frontend/src/styles.css) | สี ฟอนต์ ระยะห่าง การ์ด ฟอร์ม เมนู และ responsive layout ของทั้งระบบ |
| [src/lib/types.ts](../frontend/src/lib/types.ts) | TypeScript types/interfaces ของข้อมูล API เช่น user, activity, term และ attachment ช่วยตรวจชนิดตอนพัฒนา |
| [src/lib/api.ts](../frontend/src/lib/api.ts) | ตัวกลาง `fetch` จัด JSON/FormData, session credentials, CSRF, error รวม helper วันที่ สถานะ query และดาวน์โหลด CSV |
| [src/components/ui.tsx](../frontend/src/components/ui.tsx) | UI ทั่วไปที่ใช้ซ้ำ เช่น modal, loading, error, badge, empty state และ icon |
| [src/components/reference.tsx](../frontend/src/components/reference.tsx) | ส่วนหน้าจอที่อิงแบบ UI เช่น avatar แบบ SVG ชื่อโรงเรียน welcome card การ์ดหมวดและสถิตินักเรียน |
| [src/components/EvidenceFiles.tsx](../frontend/src/components/EvidenceFiles.tsx) | UI เลือกไฟล์ พรีวิว ลบ/ยกเลิกการลบ และรายการหลักฐานสำหรับดูหรือดาวน์โหลด |

`types.ts` ช่วยตรวจชนิดตอน compile แต่ไม่ตรวจสิทธิ์หรือรับรอง input ตอนรัน ดังนั้น backend ยังต้องตรวจทุกคำขอ

## 15. Frontend: หน้าจอและขั้นตอนใช้งาน

อ้างอิงจาก `frontend/src/features/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [Login.tsx](../frontend/src/features/Login.tsx) | หน้าเข้าสู่ระบบ กรอกบัญชีตัวอย่าง เปิด/ปิดการแสดงรหัสผ่าน และอัปเดตข้อมูลผู้ใช้หลัง login |
| [Dashboard.tsx](../frontend/src/features/Dashboard.tsx) | หน้าหลักนักเรียน/ครูและหน้าหมวดกิจกรรม แสดงความคืบหน้า รายการล่าสุด/รอตรวจ และปุ่มไปทำงานต่อ |
| [Activities.tsx](../frontend/src/features/Activities.tsx) | รายการกิจกรรม ค้นหา กรอง แบ่งหน้า และเลือกหลายรายการให้ครูตรวจเป็นกลุ่ม |
| [ActivityEditor.tsx](../frontend/src/features/ActivityEditor.tsx) | ฟอร์มสร้าง/แก้กิจกรรม จัดชื่อ รายละเอียด วันเวลา ครู และหลักฐาน ประสานการบันทึกกิจกรรม → บันทึกไฟล์ → ส่งตรวจ |
| [ActivityDialogs.tsx](../frontend/src/features/ActivityDialogs.tsx) | แสดงรายละเอียด timeline และหลักฐาน พร้อมปุ่มแก้ไข/อนุมัติ/ส่งคืนตามบทบาท และ re-export ตัว editor |
| [useActivityEvidence.ts](../frontend/src/features/useActivityEvidence.ts) | custom hook จัดไฟล์ที่รออัปโหลด/รอลบ สร้างและคืน preview URL, commit ไฟล์พร้อม version และจัดการ retry ด้วย upload ID |
| [Favorites.tsx](../frontend/src/features/Favorites.tsx) | ค้นหาครูและกดเพิ่ม/ลบครูที่ชื่นชอบ |
| [Profile.tsx](../frontend/src/features/Profile.tsx) | หน้าโปรไฟล์ เมนู ข้อมูลความเป็นส่วนตัว และฟอร์มแก้ชื่อ อีเมล เบอร์โทร ที่อยู่ |
| [Classrooms.tsx](../frontend/src/features/Classrooms.tsx) | ครูดูสถิติห้อง รายชื่อนักเรียน กรองข้อมูล ดูสถิติรายคน และดาวน์โหลดรายงาน |

Component ทำหน้าที่แสดงผลและรับการกระทำ ส่วน custom hook แยก state/ขั้นตอนที่ซับซ้อนออกมา เช่น `useActivityEvidence` ทั้งสองอย่างเป็นรูปแบบของ React ไม่ใช่ Java entity หรือ repository

## 16. Frontend: ตั้งค่า ทดสอบ และ assets

อ้างอิงจาก `frontend/`:

| ไฟล์ | หน้าที่ |
|---|---|
| [package.json](../frontend/package.json) | รายการ dependencies และคำสั่ง `dev`, `build`, `preview`, `test:e2e` |
| [package-lock.json](../frontend/package-lock.json) | ล็อกเวอร์ชัน dependency ที่ติดตั้งจริง ใช้กับ `npm ci` เพื่อให้ติดตั้งซ้ำได้ใกล้เคียงกัน |
| [tsconfig.json](../frontend/tsconfig.json) | กำหนดการตรวจ TypeScript และ JSX รวม strict checking |
| [vite.config.ts](../frontend/vite.config.ts) | ตั้ง React plugin และ dev server port 5173 พร้อม proxy `/api` ไป backend |
| [playwright.config.ts](../frontend/playwright.config.ts) | ตั้ง Chromium tests, port ทดสอบแยก 5174/8081, startup/teardown และการเก็บหลักฐานเมื่อทดสอบ |
| [Dockerfile](../frontend/Dockerfile) | build frontend ด้วย Node แล้วนำ static files ไปให้ Nginx เสิร์ฟ |
| [tests/sama.spec.ts](../frontend/tests/sama.spec.ts) | browser tests ของ flow นักเรียน/ครู รวมชื่อกิจกรรม หลักฐาน การส่งตรวจ และผลอนุมัติ |
| [tests/teardown.ts](../frontend/tests/teardown.ts) | ปิด PostgreSQL ชั่วคราวของ E2E หลังตรวจว่าเป็นโฟลเดอร์ทดสอบ ไม่ใช่ฐานข้อมูล local ของผู้ใช้ |
| [tests/fixtures/evidence.png](../frontend/tests/fixtures/evidence.png) | รูปตัวอย่างขนาดเล็กสำหรับทดสอบอัปโหลด |
| [tests/fixtures/evidence.pdf](../frontend/tests/fixtures/evidence.pdf) | PDF ตัวอย่างสำหรับทดสอบอัปโหลด/ดาวน์โหลด |
| [public/favicon.svg](../frontend/public/favicon.svg) | ไอคอนของเว็บบนแท็บ browser |
| [public/fonts/noto-sans-thai.ttf](../frontend/public/fonts/noto-sans-thai.ttf) | ไฟล์ฟอนต์ภาษาไทยที่เสิร์ฟจากแอปเอง |
| [public/fonts/OFL.txt](../frontend/public/fonts/OFL.txt) | ข้อกำหนดสิทธิ์ใช้งานฟอนต์ |

## 17. Root, scripts, infrastructure และเอกสาร

อ้างอิงจากโฟลเดอร์หลักของโปรเจกต์:

| ไฟล์ | หน้าที่ |
|---|---|
| [README.md](../README.md) | วิธีเริ่ม/หยุดระบบ บัญชีสาธิต โครงสร้าง การทดสอบ และข้อจำกัดปัจจุบัน |
| [start.cmd](../start.cmd) | ทางลัดบน Windows เรียก `scripts/start.ps1` |
| [stop.cmd](../stop.cmd) | ทางลัดบน Windows เรียก `scripts/stop.ps1` |
| [.env.example](../.env.example) | ตัวอย่าง environment variables สำหรับการตั้งค่าระบบ |
| [.env.supabase.example](../.env.supabase.example) | ตัวอย่างการตั้งค่าเลือก Supabase และค่าการเชื่อมต่อ JDBC |
| [.gitignore](../.gitignore) | รายการที่ไม่ควรเข้า Git เช่น dependencies, build output, local data และ secrets |
| [.dockerignore](../.dockerignore) | รายการที่ไม่ส่งเข้า Docker build context เพื่อลดไฟล์ที่ไม่เกี่ยวข้อง |
| [scripts/setup.ps1](../scripts/setup.ps1) | เตรียม portable Node/Maven ตรวจ checksum ติดตั้ง frontend dependencies และเตรียม backend ต้องมี Java ตามที่กำหนด |
| [scripts/start.ps1](../scripts/start.ps1) | ตรวจ port เรียก backend/frontend แบบ background เก็บ process/log และรอจนพร้อมใช้งาน |
| [scripts/stop.ps1](../scripts/stop.ps1) | หยุด process ของแอปที่ติดตามไว้และ PostgreSQL local โดยเก็บข้อมูลฐานข้อมูลไว้ |
| [scripts/run-backend.ps1](../scripts/run-backend.ps1) | เรียก Java/Maven และเลือกตัวรัน local, Supabase หรือ E2E ตามโหมด/option |
| [scripts/database-settings.ps1](../scripts/database-settings.ps1) | อ่าน `.env` เป็นข้อความ ตรวจโหมด local/Supabase และค่าการเชื่อมต่อ |
| [scripts/tests/database-settings.test.ps1](../scripts/tests/database-settings.test.ps1) | ทดสอบการอ่านค่าที่มีอักขระพิเศษ การ override และการปฏิเสธ config ผิด |
| [scripts/run-frontend.ps1](../scripts/run-frontend.ps1) | เรียก Vite พร้อม Node ที่เตรียมไว้ และเลือก port/API ของ local หรือ test |
| [infra/compose.yaml](../infra/compose.yaml) | กำหนด service PostgreSQL, backend, frontend และ volume สำหรับรันผ่าน Docker Compose |
| [infra/nginx.conf](../infra/nginx.conf) | เสิร์ฟ frontend ที่ build แล้ว proxy `/api` ไป backend พร้อมกำหนดขนาด request และ headers |
| [docs/SAMA-architecture.md](SAMA-architecture.md) | รายละเอียดแนวออกแบบสถาปัตยกรรมและเทคโนโลยี ใช้เทียบกับโค้ดจริงเมื่ออ่าน |
| [docs/implementation-status.md](implementation-status.md) | บันทึกสถานะการพัฒนาและขอบเขตที่ทำ/ยังไม่ทำ ณ เวลาที่บันทึก |
| [docs/ui-reference-check.md](ui-reference-check.md) | บันทึกการเทียบหน้าจอกับ UI อ้างอิงและภาพตรวจงาน |
| [docs/SAMA-code-guide.md](SAMA-code-guide.md) | คู่มือรายไฟล์และตัวอย่าง OOP ที่กำลังอ่าน |
| [docs/SAMA-supabase.md](SAMA-supabase.md) | วิธีสร้างโปรเจกต์ Supabase ตั้งค่า เปิดระบบ และสลับกลับ local |
| [docs/SAMA-supabase-auth.md](SAMA-supabase-auth.md) | วิธีเปิด Supabase Auth สร้างบัญชี และผูกสิทธิ์นักเรียน/ครู |

Docker configuration มีเตรียมไว้ แต่ยังไม่ได้ยืนยันการรันบนเครื่องที่มี Docker เอกสารสถานะ/ภาพตรวจงานเป็นบันทึก ณ เวลาเขียน; หากต่างจากโค้ด ให้ตรวจ implementation ปัจจุบันด้วย

## 18. โฟลเดอร์ที่เครื่องมือสร้าง ไม่ใช่โค้ดที่ต้องอ่านทีละไฟล์

| ตำแหน่ง | มีอะไร / ควรทำอย่างไร |
|---|---|
| `.tools/` | Node, Maven, formatter และ dependency cache ของเครื่องพัฒนา ติดตั้ง/ดาวน์โหลดใหม่ได้ |
| `.local/` | ฐานข้อมูล PostgreSQL ที่ใช้จริงใน local, logs และ process metadata **อย่าลบทิ้งถ้าต้องการเก็บข้อมูล** |
| `backend/target/` | Java classes, JAR และรายงานการทดสอบที่ Maven สร้างจาก source |
| `frontend/node_modules/` | ไลบรารีจาก npm ไม่ควรเข้าไปแก้เพื่อเปลี่ยนแอป |
| `frontend/dist/` | HTML/CSS/JS ที่ build แล้ว ต้องแก้ `src/` แล้ว build ใหม่ |
| `frontend/tsconfig.tsbuildinfo` | cache ของ TypeScript ถ้ามี ไม่ใช่ business logic |
| `frontend/test-results/`, `frontend/playwright-report/` | ภาพ traces และรายงาน browser tests ถ้ามี |
| `docs/screenshots/` | ภาพหน้าจอประกอบการตรวจ UI |
| `tmp/` | ไฟล์ชั่วคราว เช่น ข้อความ/ภาพจาก PDF และ log การ build/test |

## 19. ตัวอย่างการทำงานข้ามไฟล์: สร้างกิจกรรมพร้อมหลักฐาน

1. ผู้ใช้กรอกใน `ActivityEditor.tsx`; `EvidenceFiles.tsx` แสดงตัวเลือกและ preview ส่วน `useActivityEvidence.ts` จำไฟล์ที่ยังไม่บันทึก
2. `lib/api.ts` ส่งคำขอสร้าง/แก้กิจกรรมพร้อม CSRF token
3. `ActivityController` รับ DTO แล้วเรียก `ActivityService`
4. Service ตรวจตัวผู้ใช้ เทอม การลงทะเบียน และหมวด ก่อนสร้าง/แก้ `Activity`; entity ตรวจเงื่อนไขการแก้ไข
5. `ActivityRepository` บันทึกผ่าน JPA ลง PostgreSQL และส่ง ID/version กลับ
6. Frontend ใช้ ID นี้บันทึกการลบ/เพิ่มไฟล์ผ่าน `ActivityAttachmentController` → `ActivityAttachmentService` → `EvidenceFilePolicy` และ `AttachmentRepository`
7. ถ้ากดส่งตรวจ จะส่งคำขอ submit หลังบันทึกไฟล์สำเร็จ แล้ว `Activity.submit()` ตรวจเงื่อนไขและเปลี่ยนเป็น `PENDING`
8. TanStack Query ปรับ/โหลด cache ใหม่เพื่อให้รายการ รายละเอียด และสถิติแสดงข้อมูลล่าสุด

การบันทึกข้อมูลกับการอัปโหลดหลายไฟล์เป็น **หลาย HTTP requests** ไม่ใช่ transaction เดียวครอบทั้งหมด ถ้าไฟล์ใดผิดพลาด กิจกรรมอาจบันทึกเป็นร่างแล้ว และหน้า editor เก็บสถานะไว้ให้ลองต่อ การ submit จะเกิดหลังบันทึกหลักฐานสำเร็จ

## 20. ลำดับอ่านสำหรับเริ่มทำความเข้าใจ

1. `README.md` → คู่มือนี้: เห็นระบบโดยรวม
2. `User.java` → `Student.java` → `Teacher.java`: เห็น inheritance และ abstraction
3. `Activity.java`: เห็น encapsulation และกฎสถานะ
4. `ProgressCalculator.java` → calculator สองตัว → `ProgressService.java`: เห็น polymorphism/Strategy
5. `ActivityController.java` → `ActivityDtos.java` → `ActivityService.java` → `ActivityRepository.java`: ไล่คำขอจาก API ถึงฐานข้อมูล
6. `main.tsx` → `App.tsx` → `ActivityEditor.tsx` → `lib/api.ts`: ไล่จากหน้าเว็บไป API
7. `useActivityEvidence.ts` และไฟล์ attachment ฝั่ง backend: ไล่งานหลักฐาน
8. Test files: ดูตัวอย่างเงื่อนไขสำเร็จ/ผิดพลาดและพฤติกรรมที่ระบบต้องรักษาไว้

หากอธิบายในรายงาน สามารถใช้ข้อความนี้ได้: “SAMA ใช้ Client–Server และ Layered Architecture โดย backend พัฒนาด้วย Java/Spring Boot ตามหลัก OOP มี inheritance ของผู้ใช้ การห่อหุ้มกฎใน entity และ polymorphism ผ่านกลยุทธ์คำนวณความคืบหน้า ส่วน frontend ใช้ React/TypeScript แบบ function components และ hooks”
