# สถานะการพัฒนา SAMA

วันที่ตรวจ: 26 ตุลาคม 2026

## ผลลัพธ์

มีเว็บแอปที่เชื่อม React -> REST API -> Spring Boot -> PostgreSQL จริง โดยแยกบทบาทนักเรียน/ครูและแบ่ง Layered Architecture ภายในแต่ละโมดูล

สำหรับการให้โรงเรียนใช้งานจริง: **ยังไม่พร้อมเปิด production** ต้องแก้ข้อบกพร่อง Profile ว่างและเตรียมระบบ production/การดูแลข้อมูลตามผลตรวจด้านล่างก่อน ผล build และ functional tests ที่ผ่านยังไม่ครอบคลุมเงื่อนไข production ทั้งหมด

เปิดด้วย `start.cmd` และหยุดด้วย `stop.cmd` รายละเอียดการติดตั้งและบัญชีสาธิตอยู่ใน [README](../README.md)

## การตรวจที่ผ่าน

| การตรวจ | ผล |
|---|---|
| TypeScript และ Vite production build | ผ่าน |
| Maven package | ผ่าน สร้าง executable JAR |
| Backend integration tests | 13 ผ่าน ใช้ HTTP/session/CSRF และ PostgreSQL แยก |
| Supabase schema/profile test | 1 ผ่าน บน PostgreSQL แยก |
| Supabase Auth integration tests | 9 ผ่าน ใช้บริการ HTTP จำลองและ PostgreSQL แยก รวมตรวจไม่เผย credentials ใน debug log |
| Unit tests: progress strategies และ CSV | 2 ผ่าน |
| Chromium end-to-end tests | 13 ผ่าน รวม session recovery, conflict, bulk pagination และเปลี่ยนครูผู้ตรวจ |
| Executable JAR โดยไม่เปิด demo | เริ่มทำงาน, migrate schema `sama`, ไม่สร้างข้อมูล demo, login ผ่าน Auth จำลอง และเก็บ session ได้ |
| Secure/HttpOnly cookie ของ JAR | ผ่านเมื่อตั้ง `COOKIE_SECURE=true`; ยังไม่ใช่การทดสอบ HTTPS ผ่าน reverse proxy จริง |
| npm audit | ไม่พบ advisory ตามผลสแกนวันที่ตรวจ รวม dependency production และ development |
| OSV ตรวจ Java runtime dependencies | ตรวจ 75 packages พบ 18 advisory matches ใน 6 packages ต้องประเมินเงื่อนไขและอัปเดตก่อน production |
| ภาพ desktop และ mobile | เทียบ UI กับ PDF ที่แนบ และตรวจหน้าจอหลักแล้ว |
| Launcher บน Windows | เปิด/ปิดสำเร็จ และเปิดฐานข้อมูลเดิมซ้ำได้โดยข้อมูลสาธิตยังอยู่ |

Browser tests ครอบคลุมนักเรียนส่งกิจกรรม -> ครูอนุมัติ -> Timeline อัปเดต, favorite/profile/CSV, login validation และบันทึกแบบร่างบนมือถือ, bulk return พร้อมเหตุผล รวมทั้งหน้าจอที่ขนาด 360, 390, 768 และ 1440 px และฟอร์มเลือกหมวด -> กรอกกิจกรรม -> เลือกครู -> ส่งตรวจ

เพิ่มการทดสอบชื่อกิจกรรมจากการ์ดหมวดและหลักฐาน: เลือกรูป/PDF, ปฏิเสธชนิดไฟล์ที่ไม่รองรับ, บันทึกแบบร่าง, โหลดหน้าใหม่, แก้ชื่อ/เปลี่ยนไฟล์ และให้ครูดาวน์โหลด bytes ที่ตรงกับต้นฉบับ

Backend tests ครอบคลุมส่งคืน-แก้ไข-ส่งใหม่-อนุมัติ, ห้ามนับซ้ำ, ownership, assigned reviewer, bulk rollback, version conflict, ห้ามแก้ pending, CSRF/logout, advisor authorization และการบันทึก favorite/profile

หลักฐานใช้ Flyway V2 เก็บไฟล์ใน PostgreSQL ตรวจชนิดจากนามสกุลและ signature จำกัด 5 ไฟล์ ไฟล์ละ 5 MB และล็อกกิจกรรมพร้อม version check ระหว่างแก้หลักฐาน ไม่อนุญาตแก้ไฟล์เมื่อ PENDING/APPROVED

เพิ่ม Supabase Auth ผ่าน password grant ฝั่ง Backend พร้อม HttpOnly session, CSRF, การผูก `auth_user_id` ผ่าน Flyway V3 และการตรวจสิทธิ์จากบัญชีโรงเรียน โหมด Supabase ไม่ fallback ไปตรวจรหัสผ่าน local ส่วน local demo และ browser tests ยังใช้งานแบบแยกได้ รายละเอียดเปิดใช้จริงอยู่ใน [คู่มือ Supabase Auth](SAMA-supabase-auth.md)

เชื่อมโปรเจกต์ Supabase จริงแล้ว: ตรวจ JDBC และ Auth health สำเร็จ รัน migration ครบ 3 เวอร์ชัน สร้างข้อมูลโรงเรียนสาธิต 5 บัญชี และผูก Supabase User ID ให้นักเรียน/ครูตัวอย่าง 2 บัญชี ระบบที่เปิดในเครื่องใช้ provider `supabase` การเข้าสู่ระบบด้วยรหัสส่วนตัวให้ผู้ใช้เป็นผู้ทดสอบ

## ผลตรวจความพร้อม deploy สำหรับโรงเรียน — 26 ตุลาคม 2026

ค่าของ `.env` ที่ตรวจในเครื่องเป็น Supabase Auth/Database แต่ยังเปิด profile `demo`, ใช้ `COOKIE_SECURE=false` และใช้บัญชีฐานข้อมูล `postgres.*` ค่าเหล่านี้เป็นค่าของเครื่องพัฒนา ต้องเตรียมค่าของ production แยก ไม่เปลี่ยน cookie ของเว็บ HTTP ในเครื่องเป็น Secure โดยไม่มี HTTPS รองรับ

### ต้องแก้หรือเตรียมก่อนเปิดใช้งานจริง

| ลำดับ | สิ่งที่พบและผลกระทบ | การแก้ที่ต้องทำ |
|---|---|---|
| 1 | **ยืนยันข้อบกพร่อง:** เมื่อ `app_users.phone` และ `address` เป็น SQL NULL ทั้งคู่ Hibernate โหลด `Profile` เป็น null; [IdentityController](../backend/src/main/java/th/ac/sama/identity/api/IdentityController.java) เรียก `u.getProfile().getPhone()` โดยตรง ทำให้ login หรือ `/api/me` ตอบ 500 บัญชีที่นำเข้าและไม่กรอกข้อมูลติดต่อมีโอกาสเจอ | รองรับ Profile ว่างใน domain/DTO, กำหนดวิธี normalize ข้อมูลนำเข้า และเพิ่ม regression test สำหรับ SQL NULL |
| 2 | [Compose](../infra/compose.yaml) เป็นชุด demo: สร้าง PostgreSQL ใน container, กำหนด JDBC URL เป็น `database:5432/sama` และ default profile เป็น `demo` จึงไม่ใช่ production configuration สำหรับ Supabase | เตรียม config production ตามโฮสต์ที่เลือก ใช้ Supabase schema `sama`, `SPRING_PROFILES_ACTIVE=supabase`, `SAMA_AUTH_MODE=supabase`; ปิด demo และป้องกันการเริ่มด้วยค่า local โดยไม่ตั้งใจ |
| 3 | [Nginx](../infra/nginx.conf) มี HTTP port 80; ยังไม่มีผลตรวจโดเมน/HTTPS/TLS termination และการส่ง forwarded headers ในโฮสต์จริง; frontend เรียก `/api` แบบ same-origin | จัดโดเมนและ HTTPS, route `/api` ไป backend, ตั้ง `COOKIE_SECURE=true`, ตรวจ trust proxy/headers กับเส้นทางจริง; ถ้ารัน container ให้ bind address/port ตาม platform |
| 4 | Java runtime dependencies มี advisory matches ที่ต้องจัดการ รายละเอียดและเงื่อนไขอยู่ตารางด้านล่าง | เลือกเวอร์ชันที่ได้รับแพตช์และเข้ากันได้ แล้วรัน build/regression และสแกนใหม่ ไม่ตีความจำนวน matches ว่าเป็นจำนวนช่องโหว่ที่ยืนยันโจมตีแอปได้ |
| 5 | Runtime ในเครื่องใช้ฐานข้อมูลด้วยบัญชี `postgres.*`; repository ยังไม่มีการแยก migration identity กับ runtime identity | เตรียม role สำหรับแอปที่เข้าถึงเฉพาะตาราง/การทำงานที่จำเป็น แยก credentials สำหรับ migrations และเก็บ secrets ในระบบของโฮสต์ |
| 6 | ยังไม่มีหลักฐานการตั้งรอบ backup หรือซ้อม restore ที่ตรวจได้ใน repository; ไฟล์หลักฐานเก็บเป็น BYTEA ใน PostgreSQL สูงสุด 5 ไฟล์ × 5 MiB ต่อกิจกรรม | กำหนด backup, retention, ผู้รับผิดชอบและซ้อมกู้คืน พร้อมวางโควตา/งบพื้นที่หรือย้ายไฟล์ไป object storage ก่อนรับข้อมูลทั้งโรงเรียน |
| 7 | ใน [Nginx](../infra/nginx.conf) และ API ยังไม่มี rate limiter ของ SAMA; health endpoint ที่ตรวจ DB, alert และผล load test บน staging ยังไม่มี | จำกัดคำขอที่ login/CSRF/upload ตามสภาพเครือข่ายจริง เพิ่ม readiness/monitoring และทดสอบจำนวนผู้ใช้พร้อมกันตามที่โรงเรียนต้องการ Supabase มี Auth rate limits อยู่แล้ว แต่ต้องทดสอบการรวมคำขอผ่าน backend เดียวด้วย |
| 8 | ยังไม่มีขั้นตอนนำเข้าบัญชี/ห้อง/ครูที่ปรึกษา/เกณฑ์จริงที่ยืนยันแล้ว และไม่มีหน้าผู้ดูแล; หน้า reset password ให้ติดต่อผู้ดูแล; deadline แสดงใน UI แต่ `Activity.submit()` ตรวจเฉพาะวันที่อยู่ในเทอม ไม่ได้ตรวจ submission deadline | เตรียมขั้นตอนของผู้ดูแลและทดสอบกับข้อมูลจำลองครบทั้งวงจร อาจใช้สคริปต์นำเข้าที่ตรวจสอบแล้วก่อนทำหน้า admin; ให้โรงเรียนยืนยันเกณฑ์ผ่านและกติกาส่งช้าก่อนกำหนดการบังคับ deadline |

Supabase แนะนำการสำรองนอกระบบสำหรับ Free Plan และมี daily backups ในแผนที่ระบุใน [Database Backups](https://supabase.com/docs/guides/platform/backups) ต้องตรวจแผน/การตั้งค่าจริงของโรงเรียน ในภาพ dashboard ที่ผู้ใช้ส่งเป็น Free Plan ซึ่งมี [database quota 500 MB ต่อโปรเจกต์](https://supabase.com/docs/guides/platform/billing-on-supabase); การเก็บหลักฐานใน DB อาจใช้โควตาเร็ว ไม่ใช่โควตาของ Storage bucket

การตรวจ load, backup และสิทธิ์ผู้ดูแลเพิ่มเติมอ้างอิง [Supabase Production Checklist](https://supabase.com/docs/guides/deployment/going-into-prod) การตรวจรอบนี้ไม่ได้ตรวจสอบการตั้งค่า account MFA, SMTP หรือ backup ใน dashboard ทุกหน้า จึงยังรับรองส่วนดังกล่าวไม่ได้

### ผลตรวจ dependency และเงื่อนไขที่เกี่ยวข้อง

OSV พบ 18 package/version matches ใน 75 runtime packages: severity ที่ฐานข้อมูลสแกนระบุเป็น Critical 5, High 6 และ Moderate 7 รายการ เป็นผลคัดกรองตามเวอร์ชัน ไม่ใช่ผลพิสูจน์การโจมตี SAMA; ระดับจากผู้พัฒนาอาจต่างจากฐาน advisory และต้องให้น้ำหนักเงื่อนไขการใช้งานจริง

| Package ที่ใช้อยู่ | จำนวน matches | ข้อพิจารณาจาก source และเอกสารผู้พัฒนา |
|---|---:|---|
| `log4j-api:2.24.3` | 1 | มีเงื่อนไขเกี่ยวกับ MapMessage JSON serialization; backend ใช้ Logback และยังไม่ได้ยืนยันเส้นทางที่เข้าเงื่อนไข |
| `jackson-databind:2.21.4` | 9 | หลายรายการต้องใช้ JsonView/JsonUnwrapped, polymorphic types หรือชนิดข้อมูลเฉพาะ ซึ่งไม่พบใน DTO ปัจจุบัน ต้องประเมิน/อัปเดตพร้อม Jackson ชุดเดียวกัน |
| `jackson-core:2.21.4` | 2 | มี advisory เกี่ยวกับ parser/number coercion ที่ต้องประเมิน โดย [ผู้พัฒนาระบุแพตช์ ReDoS ในสาย 2.21 ที่ 2.21.7](https://github.com/FasterXML/jackson-core/security/advisories/GHSA-p6pp-m3f8-5c89); ยังไม่ได้พิสูจน์ exploit บน endpoint SAMA |
| `tomcat-embed-core:10.1.55` | 3 | matches ที่ได้เกี่ยวกับ DIGEST/FORM authenticator และ container security constraints ซึ่งแอปใช้ Spring Security แทน; ยังควรอัปเดตตาม [vendor security page](https://tomcat.apache.org/security-10.html) ที่มีแพตช์ใหม่เพิ่มเติม ไม่ใช้ผล OSV เป็นรายการที่ครบทั้งหมด |
| `spring-webmvc:6.2.19` | 2 | SSE view fragments / XsltView ไม่พบในแอป REST + React ปัจจุบัน; ตัวอย่าง [CVE-2026-47890](https://spring.io/security/cve-2026-47890/) ผู้พัฒนาจัดเป็น Low และระบุเงื่อนไข SSE ชัดเจน แม้ฐานสแกนจัด Critical |
| `postgresql:42.7.11` | 1 | [GHSA-j92g-9f8w-j867](https://github.com/pgjdbc/pgjdbc/security/advisories/GHSA-j92g-9f8w-j867) เกิดภายใต้ `channelBinding=require` และเงื่อนไข TLS ตาม advisory; ผู้พัฒนาระบุแก้ใน 42.7.12 ไม่ควรสรุปว่า connection ปัจจุบันถูกโจมตีได้จากชื่อเวอร์ชันอย่างเดียว |

### หลักฐานและขอบเขตการตรวจ

- ผล Backend เดิม 25 tests และ Chromium 13 tests ผ่าน; รอบนี้เพิ่มการตรวจ executable JAR โดยใช้ PostgreSQL/HTTP Auth จำลองที่แยกจาก Supabase จริง
- JAR ผ่าน startup, migrations, ไม่สร้าง demo เมื่อปิด profile, Secure/HttpOnly cookie, login ผ่าน Auth จำลอง และ session ที่อ่านกลับจาก DB เมื่อข้อมูล Profile ปกติ
- บัญชีที่มี phone/address เป็น SQL NULL ทำซ้ำ login failure ได้ และเมื่อทำให้ Profile ของบัญชีที่ login แล้วเป็น NULL การเรียก `/api/me` ตอบ **500** ยืนยันว่าต้องแก้ก่อน production
- ไม่มี Docker CLI ในเครื่องนี้ จึงยังไม่ได้ build/run container image หรือทดสอบ Nginx/TLS/load balancer ของโฮสต์จริง
- ยังไม่มีผล restore drill, load test ตามจำนวนผู้ใช้จริง หรือ school UAT บน staging
- หลักฐานในเครื่องอยู่ใต้ `.local/qa/deploy-*` รวม dependency audit และ packaged smoke logs โดยไม่เก็บ credentials จริงในรายงาน
- ไม่ควรเผยแพร่ production ด้วย config ปัจจุบัน ขั้นถัดไปคือแก้ข้อบกพร่องและเตรียม staging บนโฮสต์ที่เลือก แล้วตรวจ acceptance criteria ของแต่ละรายการก่อนเปิดให้โรงเรียน

## ภาพหน้าจอ

- [Dashboard นักเรียนบนคอม](screenshots/sama-desktop.png)
- [Dashboard บนมือถือ](screenshots/sama-mobile.png)
- [สถิติห้องเรียน](screenshots/sama-classroom.png)
- [หน้าจอที่ปรับตามแบบอ้างอิง](ui-reference-check.md)

## ยังไม่ยืนยัน

- 1,000 concurrent users และเป้าหมายเวลาใน SRS
- Docker runtime และ Safari/WebKit
- Production deployment, backup/restore และการดูแลข้อมูลจริง
- การทดสอบ login จริงด้วยรหัสส่วนตัวของผู้ใช้; ยังไม่มี automatic token refresh หรือหน้า reset password ทางอีเมล
- เกณฑ์โรงเรียนจริงและนโยบายส่งช้า

## ฟีเจอร์นอก MVP ที่ยังต้องกำหนดกติกา

บัญชีใหม่/ลืมรหัสผ่าน/ผู้ดูแลข้อมูล, ยกเลิกผลตรวจ, ลบบัญชี และชุมชน ส่วนการบังคับแนบหลักฐานแยกตามหมวดต้องยืนยันกติกาก่อน
