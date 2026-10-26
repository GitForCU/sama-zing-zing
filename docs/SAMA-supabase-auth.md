# เข้าสู่ระบบด้วย Supabase Auth

SAMA รองรับ Supabase Auth สำหรับอีเมล/รหัสผ่านแล้ว โดยคงหน้าฟอร์มเดิมไว้ Java เป็นตัวกลางเรียก Supabase Auth และเชื่อมตัวตนกับบัญชีนักเรียน/ครูของโรงเรียน ผู้ใช้ยังเปิดหลักฐานและดาวน์โหลดรายงานด้วย session cookie ของแอปได้

```text
React: อีเมล/รหัสผ่าน
   ↓ POST /api/auth/login + CSRF
Spring Boot → Supabase Auth: password grant ผ่าน HTTPS
   ↓ ได้ตัวตนที่ Supabase ยืนยัน
ค้นหา app_users.auth_user_id → ตรวจเปิดใช้งาน → โหลดบทบาทจาก Student/Teacher
   ↓
สร้าง HttpOnly session → ส่งข้อมูลผู้ใช้กลับให้ React (ไม่มี access/refresh token)
```

## 1. สร้างโปรเจกต์และตั้งค่า

สร้างโปรเจกต์ที่ [Dashboard](https://supabase.com/dashboard) จากนั้นทำขั้นตอน Database ใน [SAMA-supabase.md](SAMA-supabase.md)

คัดลอก `.env.supabase.example` เป็น `.env` ที่โฟลเดอร์หลัก ถ้ามี `.env` อยู่แล้วให้แก้ของเดิม แล้วกรอกค่าจริงของโปรเจกต์เดียวกัน:

```dotenv
SAMA_DATABASE_MODE=supabase
SAMA_AUTH_MODE=supabase
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_YOUR_KEY
DATABASE_URL=jdbc:postgresql://YOUR_POOLER_HOST:5432/postgres?sslmode=require
DATABASE_USER=postgres.YOUR_PROJECT_REF
DATABASE_PASSWORD="YOUR_DATABASE_PASSWORD"
DATABASE_POOL_SIZE=5
SPRING_PROFILES_ACTIVE=demo
COOKIE_SECURE=false
```

- Project URL และ publishable key หาได้จากหน้า Connect/API Keys ของโปรเจกต์ ใช้ publishable key (หรือ legacy anon key) ไม่ใช้ secret/service_role key
- `SUPABASE_URL` เป็น URL โปรเจกต์ ไม่ต่อ `/auth/v1` ส่วน `DATABASE_URL` เป็น JDBC ของ Session pooler ตามคู่มือฐานข้อมูล
- ค่าถูกอ่านโดย Backend ไม่ต้องเพิ่ม Vite environment หรือ SDK ฝั่ง browser
- เปิด Email/password provider ในการตั้งค่า Authentication
- การปิด Data API สำหรับระบบที่ใช้ JDBC ไม่ใช่การปิด Supabase Auth; เก็บ schema `sama` ไว้นอก Data API เหมือนเดิม
- อย่า commit `.env` หรือส่ง database password ลงแชต

อ้างอิง: [Supabase Auth](https://supabase.com/docs/guides/auth), [API และชนิดของ key](https://supabase.com/docs/guides/api/api-keys)

## 2. เปิดแอปและสร้างข้อมูลโรงเรียน

รัน `stop.cmd` และ `start.cmd` หลังกรอกค่า ตัวรันจะแสดง `Authentication mode: supabase` และ Flyway จะเพิ่มคอลัมน์ `auth_user_id` ผ่าน V3

`SPRING_PROFILES_ACTIVE=demo` สร้างข้อมูลตัวอย่างในฐานข้อมูลว่าง เช่น นักเรียน ครู ห้องและเกณฑ์ **ไม่ได้สร้างบัญชี Supabase Auth ให้** โหมดนี้มีไว้ทดลองก่อนนำเข้าข้อมูลโรงเรียนจริง

หน้า login จะใช้ฟอร์มเดิม แต่ซ่อนปุ่มเติมบัญชี demo เมื่อเลือก Supabase Auth

## 3. สร้างบัญชี Auth และผูกสิทธิ์

1. เปิด **Authentication → Users** ใน Supabase แล้วสร้างบัญชีผ่านเครื่องมือผู้ดูแล สำหรับ demo ให้สร้าง `student@sama.local` และ `teacher@sama.local` พร้อมรหัสผ่านที่คุณเลือก
2. หากใช้อีเมลทดสอบที่รับเมลไม่ได้ ให้ใช้ตัวเลือกยืนยันอีเมลโดยผู้ดูแลในการสร้างบัญชี ผู้ใช้ทั่วไปต้องยืนยันอีเมลตามนโยบายที่ตั้งไว้
3. คัดลอก **User ID** ของแต่ละบัญชี จากนั้นเปิด SQL Editor และผูกกับบัญชีโรงเรียนที่ถูกต้อง ตัวอย่างสำหรับนักเรียน:

```sql
UPDATE sama.app_users
SET auth_user_id = 'ใส่-USER-ID-จาก-SUPABASE'
WHERE email = 'student@sama.local'
  AND auth_user_id IS NULL
RETURNING id, email, auth_user_id;
```

ทำเช่นเดียวกันกับ `teacher@sama.local` โดยใช้ UUID ของครู ตรวจว่าคำสั่งคืนบัญชีที่ตั้งใจไว้ ถ้ารันแล้วไม่คืนแถว ให้ตรวจว่ามีข้อมูล demo แล้วและยังไม่ได้ผูก ID อื่น

4. เปิด [SAMA](http://127.0.0.1:5173) แล้ว login ด้วยรหัสที่ตั้งใน **Supabase Auth**

บทบาทยังมาจากคลาส/ตาราง `Student` และ `Teacher` ฝั่ง SAMA ผู้ใช้ไม่เลือกบทบาทตอน login และข้อมูล `user_metadata` ของ Supabase ไม่ได้ใช้มอบสิทธิ์ครู ระบบไม่ผูกบัญชีจากอีเมลให้อัตโนมัติ

บัญชีที่ Auth ถูกต้องแต่ยังไม่ผูก ID หรือถูกปิดใช้งานจะได้รับข้อความให้ติดต่อผู้ดูแลโรงเรียน การนำเข้าบัญชีโรงเรียนจำนวนมากยังเป็นงานแยกจากการเปลี่ยนระบบ login ครั้งนี้

## 4. Session, logout และโปรไฟล์

- Backend รับตัวตนจากผล password grant ของ Supabase ผ่าน HTTPS ไม่เชื่อ role/ID ที่ browser ส่งมาเอง และไม่เปิด endpoint ให้ client ส่ง JWT ใด ๆ มาแลก session
- Access token อยู่ใน Spring Session ฝั่ง server เท่านั้น refresh token ไม่ถูกบันทึกหรือส่งให้ browser
- Cookie เป็น HttpOnly และคำขอเปลี่ยนข้อมูลยังต้องมี CSRF token deployment จริงต้องใช้ HTTPS และ `COOKIE_SECURE=true`
- Session หมดอายุเมื่อไม่ใช้งานตามเวลาแอป (ค่าเริ่มต้น 30 นาที) หรือถึงอายุ access token ของ Supabase แล้วแต่ว่าอะไรถึงก่อน เมื่อหมดอายุให้ login ใหม่ เวอร์ชันนี้ไม่มี automatic token refresh
- Session เก่าที่สร้างจาก local login ใช้แทน Supabase login ไม่ได้เมื่อเลือกโหมด `supabase`
- Logout ลบ session ของแอปและเรียก Supabase logout ด้วย scope `local` เพื่อออกเฉพาะ session นั้น หาก Supabase ขัดข้อง session ของแอปก็ยังถูกลบ ส่วน JWT ที่ออกไปแล้วมีอายุจนถึงวันหมดอายุตามระบบของ Supabase [เอกสาร logout](https://supabase.com/docs/guides/auth/signout)
- เปลี่ยนชื่อ เบอร์โทรและที่อยู่ได้ตามเดิม ส่วนอีเมลเป็นข้อมูลบัญชี Auth จึงแสดงเป็นอ่านอย่างเดียวในหน้าโปรไฟล์ ให้ผู้ดูแลจัดการอีเมลที่ Supabase และค่าจะ sync เข้าระบบเมื่อ login ครั้งถัดไป
- ปุ่มลืมรหัสผ่านยังใช้ขั้นตอนติดต่อผู้ดูแลโรงเรียนตามระบบเดิม ยังไม่ได้เพิ่มหน้า reset password ผ่านลิงก์อีเมล

## 5. โหมด local สำหรับพัฒนาและการทดสอบ

เมื่อไม่ได้ตั้ง `.env` ตัวรัน local ยังคงเปิด demo แบบไม่ใช้บริการคลาวด์ได้ โดยหน้า login มีปุ่มบัญชีทดลอง การเลือก `SAMA_AUTH_MODE=supabase` จะใช้ Supabase เท่านั้น ไม่มีการลองรหัสผ่าน local เมื่อ Supabase ปฏิเสธหรือขัดข้อง

Browser tests ที่รัน `-Ephemeral` บังคับ `SAMA_AUTH_MODE=local` และใช้ฐานข้อมูลชั่วคราว ไม่อ่าน `.env` เพื่อไม่แก้ข้อมูลหรือใช้ quota ของ Supabase จริง

การตรวจสอบส่วน Auth ใช้ HTTP stub ที่จำลอง password-grant/logout ของ Supabase และ PostgreSQL จริงที่แยกออกมา ครอบคลุมบทบาท การไม่ fallback ไป local บัญชีไม่ผูก ID การหมดอายุ การ logout เมื่อ upstream ขัดข้อง และข้อจำกัดการแก้อีเมล การทดสอบนี้ไม่ได้ยืนยันว่าโปรเจกต์ Supabase จริงถูกตั้งค่าแล้ว

## 6. ไฟล์หลัก

| ไฟล์ | หน้าที่ |
|---|---|
| `identity/application/LoginService.java` | เลือกผู้ยืนยันตัวตนและเชื่อม Supabase ID กับบัญชีโรงเรียน |
| `identity/infrastructure/SupabaseAuthClient.java` | เรียก password grant/logout พร้อม timeout และแปลงข้อผิดพลาด |
| `shared/security/AuthSettings.java` | รับและตรวจค่าโหมด URL และ publishable key |
| `shared/security/SupabaseSession.java` | ข้อมูลตัวตน/token/expiry ที่เก็บเฉพาะฝั่ง server |
| `shared/security/SupabaseSessionFilter.java` | ปฏิเสธ session ผิดโหมด ผิดตัวตน หรือเกินอายุ token |
| `V3__supabase_auth_identity.sql` | เพิ่มคอลัมน์เชื่อม User ID แบบ unique |
| `SupabaseAuthIntegrationTest.java` | ทดสอบ Auth ผ่าน HTTP และฐานข้อมูลแยก |
