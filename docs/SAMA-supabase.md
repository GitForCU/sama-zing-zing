# ใช้ Supabase กับ SAMA

โหมดนี้ให้ Java/Spring Boot เชื่อม Supabase PostgreSQL ผ่าน JDBC โดยยังใช้ React, OOP และ services เดิม ตอนนี้รองรับ Supabase Auth แล้ว ให้ตั้งค่า login เพิ่มตาม [คู่มือ Supabase Auth](SAMA-supabase-auth.md) หลักฐานยังเก็บเป็น bytes ในฐานข้อมูล

```text
React → Spring Boot → Supabase PostgreSQL (schema: sama)
```

## 1. สร้างโปรเจกต์ Supabase

1. ไปที่ [Supabase Dashboard](https://supabase.com/dashboard) และสร้างโปรเจกต์ใหม่สำหรับ SAMA
2. ตั้ง database password และเก็บไว้ในเครื่อง
3. รอฐานข้อมูลพร้อม แล้วเปิด **Connect → Session pooler**
4. คัดลอก host, port, database และ username จริงจากหน้าดังกล่าว ตัวอย่างทั่วไปคือ port `5432`, database `postgres`, username `postgres.<project-ref>`

Session pooler รองรับ IPv4 และเหมาะกับ backend ที่รันต่อเนื่องบนเครื่องที่ไม่มี IPv6 ส่วน direct connection ใช้ได้เมื่อเครือข่ายรองรับ อย่าเดา pooler hostname จากชื่อ region ให้คัดลอกจาก Dashboard [เอกสาร connection](https://supabase.com/docs/guides/database/connecting-to-postgres)

ตัวรันนี้ใช้ session/direct connection และจะไม่รับ transaction pooler port `6543` เพื่อไม่ต้องปรับพฤติกรรม prepared statements/session state ของแอป

## 2. เก็บตารางภายในให้ Backend เข้าถึง

โปรเจกต์นี้ใช้ JDBC จาก Java จึงปิด **Enable Data API** ในหน้า Data API ของ Supabase ได้ ส่วน JDBC ยังเป็นช่องทางที่ Backend ใช้ ไม่ต้องใช้ publishable/anon key หรือ service-role key ใน React [เอกสาร Data API](https://supabase.com/docs/guides/api/securing-your-api)

profile `supabase` จะสร้างตารางใน schema **`sama`** แยกจาก `public`, `auth` และ `storage` ให้เก็บ `sama` ไว้นอก Exposed schemas ของ Data API Supabase Auth ยืนยันตัวตน ส่วน Spring Security และ services ของ SAMA ตรวจสิทธิ์นักเรียน/ครู

## 3. ตั้งค่าไฟล์ .env

ที่โฟลเดอร์หลัก `C:\Users\porep\Desktop\samahaha` ให้สร้าง `.env` จากตัวอย่าง ถ้ามีไฟล์อยู่แล้วให้แก้ของเดิม:

```powershell
if (!(Test-Path -LiteralPath .env)) {
    Copy-Item -LiteralPath .env.supabase.example -Destination .env
}
notepad .env
```

ใส่ค่าจริงจาก Connect และ database password:

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

- `DATABASE_URL` ต้องเริ่มด้วย `jdbc:postgresql://` ใส่ host/port/database จริง ไม่ใช่ URL หน้าเว็บ Supabase หรือ URI ที่มี user/password รวมอยู่
- `DATABASE_USER` และ `DATABASE_PASSWORD` แยกจาก URL; password เป็นรหัสฐานข้อมูล ไม่ใช่รหัสบัญชีเข้า Dashboard
- password ใน `.env` เป็นข้อความตามที่พิมพ์ `$`, `#`, `=` ไม่ถูกประมวลผลเป็น PowerShell หรือตัดเป็น comment; เครื่องหมาย quote ที่ครอบค่าจะถูกนำออก ไม่รองรับ multiline และ inline comments
- ตัวแปรใน process environment มีลำดับความสำคัญสูงกว่า `.env` ถ้าตั้งไว้ก่อนหน้านี้ต้องปรับให้ตรงกัน
- `.env` อยู่ใน `.gitignore` และ `.dockerignore` กรอกความลับในเครื่อง ไม่ส่ง password หรือ connection string ที่มี password ลงแชต/Git
- `SPRING_PROFILES_ACTIVE=demo` ใช้สร้างข้อมูลโรงเรียนสาธิตในฐานข้อมูลที่ยังไม่มีผู้ใช้ ตัวรันเพิ่ม profile `supabase` ให้อัตโนมัติ บัญชี Supabase Auth ต้องสร้างและผูก User ID แยกตามคู่มือ Auth
- หากเป็นฐานข้อมูลใช้งานจริง ให้เอา `demo` ออกและเตรียมบัญชี/ข้อมูลโรงเรียนด้วยกระบวนการนำเข้าที่เหมาะสม ตอนนี้แอปยังไม่มีหน้าสมัคร/ผู้ดูแลสร้างบัญชี
- `COOKIE_SECURE=false` สำหรับเว็บที่เปิดในเครื่องผ่าน HTTP; deployment ที่ใช้ HTTPS ต้องตั้ง `true`

ตัวอย่างใช้ `sslmode=require` เพื่อบังคับเข้ารหัส โดยโหมดนี้ไม่ตรวจตัวตน server ถ้าต้องการตรวจด้วย ให้ดาวน์โหลด root certificate จาก Supabase และใช้ `sslmode=verify-full&sslrootcert=C:/path/to/root.crt` ตามเอกสาร driver/connection [เอกสาร SSL](https://supabase.com/docs/guides/database/connecting-to-postgres#ssl)

## 4. เปิดระบบ

หลังแก้ `.env` และบันทึก ให้หยุดระบบเดิมแล้วเปิดใหม่:

```powershell
.\stop.cmd
.\start.cmd
```

เมื่อพร้อม ตัวรันจะแสดง `Database mode: supabase` แล้วเปิด [SAMA](http://127.0.0.1:5173)

ในโหมดนี้ตัวรันเรียก `SamaApplication` โดยไม่เปิด PostgreSQL local; Flyway สร้าง schema และรัน migration V1/V2/V3 ให้เอง ไม่ต้องสร้างตารางด้วยมือ โปรเจกต์ Supabase ใหม่เริ่มจากฐานข้อมูลว่าง ข้อมูลเก่าจาก `.local/postgres` **ไม่ได้ย้ายอัตโนมัติ**

หากเปิด `demo` จะมีข้อมูลบัญชีโรงเรียนต่อไปนี้ ให้สร้างบัญชีใน Supabase Auth และผูก User ID ก่อน login (รหัสผ่านใช้ค่าที่ตั้งใน Supabase):

| บทบาท | อีเมล |
|---|---|
| นักเรียน | student@sama.local |
| ครู | teacher@sama.local |

การตั้งค่านี้เปลี่ยนเฉพาะที่เก็บฐานข้อมูล เว็บและ Java ยังรันบนเครื่องนี้ ถ้าต้องการ URL ให้คนอื่นใช้ ต้อง deploy frontend/backend เพิ่ม

## 5. ตรวจผลและแก้ปัญหา

- ใน Supabase SQL Editor ตรวจด้วย `select count(*) from sama.app_users;` หรือเลือก schema `sama` ใน Table Editor
- ลอง login สร้างร่าง ส่งตรวจ และให้ครูอนุมัติ ตรวจว่า restart แล้วยังเห็นข้อมูล
- ดู log ที่ `.local/backend.log` และ `.local/backend-error.log` ก่อนส่ง log ให้ผู้อื่น ให้ตรวจว่าปราศจากความลับ
- `UnknownHost`/timeout: คัดลอก Session pooler host จริง ตรวจว่าโปรเจกต์พร้อมและเครือข่ายเชื่อม port ได้
- Authentication failed: ตรวจ database password และ username ซึ่ง session pooler มักมี project reference ต่อท้าย
- ไม่มีตาราง: ตรวจว่า profile `supabase` ทำงานและผู้ใช้ DB มีสิทธิ์สร้าง schema/table; ให้ Flyway ทำงานครบ
- เปิดแล้วไม่พบข้อมูลเดิม: local และ Supabase เป็นคนละฐานข้อมูล ขั้นตอนนี้ไม่ได้ import ข้อมูลเก่า

### Log: `schema "pg_pgrst_no_exposed_schemas" does not exist`

Supabase มีปัญหาที่ทราบแล้วเมื่อปิด Data API: PostgREST ยังพยายามโหลด schema ที่ใช้แทนค่าการปิด API จึงเกิด `3F000` ซ้ำ และอาจพบ `/rest-admin/v1/ready` ตอบ 503 ปัญหานี้ไม่ได้แปลว่าการเชื่อม JDBC ของ SAMA ล้มเหลว ให้ตรวจ log ของ backend แยกด้วย

หากต้องการหยุด missing-schema errors ใหม่ ใช้วิธีชั่วคราวจาก [เอกสาร Supabase](https://supabase.com/kb/troubleshooting/schema-pg_pgrst_no_exposed_schemas-does-not-exist) ใน SQL Editor โดยให้ PostgREST ชี้ไปที่ schema ว่าง:

```sql
create schema if not exists pgrst_no_exposed_schemas;
alter role authenticator set pgrst.db_schemas = 'pgrst_no_exposed_schemas';
notify pgrst;
```

ใช้ชื่อ `pgrst_no_exposed_schemas` ตามตัวอย่าง ไม่ใช้ชื่อที่ขึ้นต้น `pg_` และไม่เพิ่ม schema `sama` ลงใน Exposed schemas ขั้นตอนนี้เปลี่ยนการตั้งค่า PostgREST เท่านั้น ไม่แก้ตารางกิจกรรมหรือเปิดเผยตารางของแอป รายการ log เก่าจะยังอยู่ ให้ตรวจช่วงเวลาหลังรัน SQL

ก่อนเปิด Data API ใช้งานจริงในอนาคต ให้คืนค่าตามเอกสาร:

```sql
alter role authenticator reset pgrst.db_schemas;
notify pgrst;
```

## 6. สลับกลับ local

ตั้ง `SAMA_DATABASE_MODE=local` และ `SAMA_AUTH_MODE=local` ใน `.env` แล้ว `stop.cmd` / `start.cmd` อีกครั้ง ระบบจะกลับมาใช้ demo และข้อมูลเดิมที่ `.local/postgres` โดยไม่ลบข้อมูล Supabase

## 7. ไฟล์ที่เกี่ยวข้องและการทดสอบ

| ไฟล์ | หน้าที่ |
|---|---|
| [../.env.supabase.example](../.env.supabase.example) | ตัวอย่างค่าที่ผู้ใช้ต้องกรอกใน `.env` |
| [../scripts/database-settings.ps1](../scripts/database-settings.ps1) | อ่าน `.env` แบบ literal ตรวจโหมด/ค่าที่จำเป็น โดยไม่แสดง password |
| [../scripts/run-backend.ps1](../scripts/run-backend.ps1) | เลือก LocalApplication, SamaApplication หรือ E2eApplication |
| [application-supabase.yml](../backend/src/main/resources/application-supabase.yml) | แยก schema, search path และ connection pool สำหรับ Supabase |
| [SupabaseDatabaseTest.java](../backend/src/test/java/th/ac/sama/SupabaseDatabaseTest.java) | ทดสอบ profile นี้กับ PostgreSQL ชั่วคราว รวม migrations, entities และ session login |
| [database-settings.test.ps1](../scripts/tests/database-settings.test.ps1) | ทดสอบการอ่านค่าลับ การ override และการปฏิเสธ config ที่ไม่ครบ |

Browser tests ที่ใช้ `-Ephemeral` จะไม่อ่าน `.env` และบังคับใช้ฐานข้อมูลทดสอบแยก เพื่อไม่แก้ข้อมูล Supabase

การทดสอบด้วย PostgreSQL ในเครื่องยืนยันโครงสร้างและการทำงานของ profile ได้ แต่การยืนยัน host, password, SSL, เครือข่ายและข้อจำกัดของ Supabase จริงต้องทำหลังกรอกค่าของโปรเจกต์
