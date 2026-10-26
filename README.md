# SAMA

ระบบบริหารจัดการกิจกรรมนักเรียน แบบ Layered Architecture และ Client-Server

React + TypeScript + Vite / Java 21 + Spring Boot / PostgreSQL

## เริ่มใช้งานบน Windows

เครื่องนี้เตรียม Node.js, Maven และ dependencies ไว้ใน `.tools` แล้ว และมี JDK 21

```powershell
.\start.cmd
```

เปิด http://127.0.0.1:5173 หลัง launcher แจ้งพร้อมใช้งาน เมื่อเสร็จให้รัน:

```powershell
.\stop.cmd
```

Launcher เปิด server เป็น background โดยไม่เปิดหน้าต่าง terminal เพิ่ม ดู log ได้ใน `.local` ค่าเริ่มต้นเป็นโหมด local ซึ่งเก็บ PostgreSQL ใน `.local/postgres` และคงข้อมูลหลัง restart; หากเลือก Supabase จะใช้ฐานข้อมูลบนคลาวด์ตาม `.env`

หากย้ายไปเครื่อง Windows อื่น ให้ติดตั้ง JDK 21 และตั้ง JAVA_HOME แล้วเตรียม dependencies:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup.ps1
```

ExecutionPolicy ในคำสั่งนี้มีผลเฉพาะ process ที่ใช้รันสคริปต์ ไม่แก้ machine policy เครื่องมือจะดาวน์โหลดจาก Node.js และ Maven Central และตรวจ checksum ก่อนแตกไฟล์

ทางเลือกสำหรับดู log โดยตรง: เปิดสอง terminal แล้วรัน `scripts/run-backend.ps1` และ `scripts/run-frontend.ps1` ผ่าน PowerShell แบบเดียวกัน ใช้ Ctrl+C เพื่อหยุด

## บัญชีสาธิตในโหมด local

| บทบาท | อีเมล | รหัสผ่าน |
|---|---|---|
| นักเรียน | student@sama.local | SamaDemo123! |
| นักเรียนคนที่ 2 | student2@sama.local | SamaDemo123! |
| นักเรียนคนที่ 3 | student3@sama.local | SamaDemo123! |
| ครูที่ปรึกษา / ผู้ตรวจ | teacher@sama.local | SamaDemo123! |
| ครูผู้ตรวจอีกคน | teacher2@sama.local | SamaDemo123! |

ข้อมูลตัวอย่างสร้างเมื่อเปิด profile `demo` และฐานข้อมูลยังไม่มีผู้ใช้ เกณฑ์และวันส่งเป็นข้อมูลสาธิต ไม่ใช่เกณฑ์ที่ยืนยันจากโรงเรียน

## สิ่งที่ทำงานได้

- Login/logout ผ่าน Supabase Auth เมื่อเลือกโหมด Supabase พร้อม Spring Security, HttpOnly session และ CSRF protection; โหมด local มีบัญชี demo สำหรับพัฒนา
- บัญชี Student/Teacher พร้อมตรวจบทบาทและ ownership ที่ backend
- นักเรียนดูความคืบหน้ารายหมวด เลือกปี/เทอม และดูกำหนดส่ง
- สร้างแบบร่าง แก้ไข ส่งให้ครู และส่งกิจกรรมที่ถูกส่งคืนใหม่
- เมื่อ session หมดอายุ ฟอร์มและไฟล์ที่เลือกยังอยู่ในแท็บเดิม เข้าสู่ระบบด้วยบัญชีเดิมเพื่อทำต่อได้; การปิด/รีโหลดแท็บหรือเปลี่ยนบัญชีจะล้างข้อมูลที่ยังไม่บันทึก
- เมื่อข้อมูลชนกับอีกหน้าต่าง ระบบหยุดบันทึกและให้เลือกโหลดข้อมูลล่าสุดก่อนแก้ต่อ เพื่อป้องกันการเขียนทับ
- แก้ชื่อกิจกรรมได้ทั้งจากปุ่ม + และจากการ์ดหมวด พร้อมแนบรูป JPG/PNG/WEBP หรือ PDF สูงสุด 5 ไฟล์ ไฟล์ละ 5 MB
- หลักฐานบันทึกพร้อมกิจกรรม เก็บใน PostgreSQL และเปิดดู/ดาวน์โหลดได้เฉพาะเจ้าของกับครูผู้ตรวจที่ได้รับมอบหมาย
- ค้นหา/กรองตามสถานะ หมวด ช่วงวันที่ และแบ่งหน้า
- ครูอนุมัติ/ส่งคืนพร้อมเหตุผล เป็นรายรายการหรือหลายรายการ สูงสุด 50
- Bulk ใช้ transaction เดียวและ optimistic locking; ถ้ารายการหนึ่งผิดพลาดไม่บันทึกรายการอื่นในชุด
- Timeline เก็บทุกการเปลี่ยนสถานะและแสดงชื่อผู้ดำเนินการตามบัญชีจริง แม้เปลี่ยนครูผู้ตรวจภายหลัง
- ครูที่ชื่นชอบและแก้ไขโปรไฟล์
- สถิติห้องและนักเรียนรายบุคคล พร้อม CSV ภาษาไทย
- Responsive UI บนมือถือและ desktop พร้อมฟอนต์ไทยที่เก็บไว้ในโปรเจกต์

## โครงสร้าง

```text
backend/src/main/java/th/ac/sama/
  identity/    บัญชีและการตรวจตัวตน
  academic/    ปี/เทอม ห้อง สมาชิกห้อง ครูที่ปรึกษา
  activity/    กิจกรรมและประวัติสถานะ
  review/      ตรวจและอนุมัติแบบกลุ่ม
  favorite/    ครูที่ชื่นชอบ
  progress/    เกณฑ์และกลยุทธ์คำนวณ COUNT / DURATION_MINUTES
  reporting/   CSV
  shared/      security, config, error และ base class
frontend/src/
  features/    หน้าจอและ flow ตามงาน
  components/  ส่วน UI ที่ใช้ร่วมกัน
  lib/         API client, types และ date formatting
infra/         Docker Compose และ Nginx
scripts/       setup/start/stop และ local launchers
```

แต่ละโมดูล backend แยก `api -> application -> domain / infrastructure` โดย Controller รับ DTO, Service จัด use case/transaction, Domain คุมกฎสถานะ และ Repository ติดต่อ JPA

ตัวอย่าง OOP: `Student`/`Teacher` สืบทอด `User`; `Activity` ซ่อน status และเปลี่ยนผ่าน method; `CountProgressCalculator`/`DurationProgressCalculator` ใช้ interface เดียวกันผ่าน dependency injection

## ฐานข้อมูลและการตั้งค่า

ใช้ Supabase: [คู่มือตั้งค่าฐานข้อมูล](docs/SAMA-supabase.md) และ [คู่มือ Supabase Auth](docs/SAMA-supabase-auth.md) — คัดลอก `.env.supabase.example` เป็น `.env`, กรอกค่า Database/Project URL/Publishable key แล้ว restart ด้วย `stop.cmd` / `start.cmd` ระบบสร้างตารางใน schema `sama` ต้องสร้างบัญชี Auth และผูก `auth_user_id` กับบัญชีโรงเรียนก่อน login ข้อมูล local ไม่ย้ายอัตโนมัติ

- Flyway migrations: `V1__initial_schema.sql`, `V2__activity_evidence.sql`, `V3__supabase_auth_identity.sql` ใน `backend/src/main/resources/db/migration/`
- ใช้ PostgreSQL จริงทั้ง local และ integration tests; local launcher ใช้ Zonky Embedded PostgreSQL จึงไม่ต้องติดตั้ง Docker
- API อยู่ที่ `127.0.0.1:8080`, Vite อยู่ที่ `127.0.0.1:5173` และ proxy `/api` ไป backend
- Production backend อ่าน `DATABASE_URL` แบบ JDBC, `DATABASE_USER`, `DATABASE_PASSWORD`, `SERVER_ADDRESS`, `PORT`, `COOKIE_SECURE`
- ถ้าต้อง deploy ให้ปิด profile `demo`, ตั้ง credentials จริง, ใช้ HTTPS และ `COOKIE_SECURE=true`
- นักเรียนแก้ไขอีเมลได้ แต่ไม่แก้บทบาท รหัสนักเรียน ห้อง และเทอมจากหน้าโปรไฟล์

## Docker

เมื่อมี Docker Desktop:

```sh
docker compose -f infra/compose.yaml up --build
```

เปิด http://127.0.0.1:8088 เป็นการรัน demo แบบ origin เดียวผ่าน Nginx ข้อมูลเก็บใน named volume `sama-data` ส่วน compose ที่ให้มาผูก port กับ loopback สำหรับเครื่องพัฒนา

## ทดสอบ

Backend (ใช้ Maven ที่ติดตั้ง หรือ `.tools/apache-maven-3.9.11/bin/mvn.cmd`):

```sh
cd backend
mvn test
```

ทดสอบผ่าน HTTP จริงกับฐานข้อมูล PostgreSQL แยก ไม่แก้ข้อมูล local มีกรณี login/CSRF/logout, ownership, reviewer authorization, ส่งคืน-ส่งใหม่-อนุมัติ, history, optimistic version, bulk rollback, CSV/สิทธิ์ที่ปรึกษา, favorite และ profile รวมทั้งสูตร count/duration และ CSV escaping มีการทดสอบหลักฐานเพิ่มเติมเรื่อง bytes ที่จัดเก็บ, สิทธิ์ดาวน์โหลด, ประเภท/ขนาด/จำนวนไฟล์, การส่งซ้ำ, version conflict และการแก้ไฟล์ในรายการส่งคืน

Frontend:

```sh
cd frontend
npm ci
npm run build
npx playwright install chromium
npm run test:e2e
```

Browser tests บน Windows เริ่ม backend พร้อมฐานข้อมูลแยกที่ port 8081 และ frontend 5174 โดยอัตโนมัติ ใช้ port ทั้งสองนี้ว่างก่อนทดสอบ ภาพหน้าจออยู่ใน `frontend/test-results` หลังรันเทสต์

## ขอบเขตที่ยังเหลือ

- ยืนยันเกณฑ์ผ่านจริง สูตรเปอร์เซ็นต์รวม และนโยบาย deadline กับผู้ใช้
- สมัครบัญชี/ลืมรหัสผ่าน/นำเข้าบัญชี และหน้าผู้ดูแลเทอม/เกณฑ์ ยังไม่ได้ทำ
- ยกเลิกผลตรวจ ลบบัญชี และชุมชน ยังไม่รวม เพราะ SRS ยังไม่กำหนด flow ครบ
- หลักฐานแนบยังเป็นช่องไม่บังคับทุกหมวด ต้องยืนยันเกณฑ์โรงเรียนก่อนกำหนดหมวดที่บังคับแนบ
- ยังไม่ได้ทดสอบ 1,000 concurrent users หรือรับรอง response-time ตาม SRS
- Docker configuration เตรียมไว้ แต่ต้องทดสอบบนเครื่องที่มี Docker
- Browser suite เริ่มจาก Chromium; ยังต้องทดสอบ Safari/WebKit ก่อนส่งใช้งานจริง

แบบออกแบบละเอียด: [docs/SAMA-architecture.md](docs/SAMA-architecture.md)

คำอธิบาย OOP และหน้าที่แต่ละไฟล์: [docs/SAMA-code-guide.md](docs/SAMA-code-guide.md)

รายละเอียด UI และภาพหน้าจอที่ปรับตาม PDF: [docs/ui-reference-check.md](docs/ui-reference-check.md)

ฟอนต์ Noto Sans Thai ใช้ SIL Open Font License ดู `frontend/public/fonts/OFL.txt`

