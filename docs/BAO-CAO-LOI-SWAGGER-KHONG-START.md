# Báo cáo lỗi: Maven `BUILD SUCCESS` nhưng Swagger / Backend không chạy

**Ngày ghi nhận:** 24/05/2026  
**Người báo:** QA / Manual Test  
**Môi trường:** Local — `.\mvnw.cmd spring-boot:run`  
**Database:** PostgreSQL Azure (`speak-journeyvn123.postgres.database.azure.com`)  
**Port ứng dụng:** `8082`  
**Swagger UI:** `http://localhost:8082/swagger-ui.html`

---

## 1. Tóm tắt cho Backend

Maven in **`BUILD SUCCESS`** **không** có nghĩa server đã lên. Ứng dụng **crash khi khởi động** do Hibernate **`ddl-auto: validate`** phát hiện **schema DB lệch so với Entity JPA**. Tomcat không start → **Swagger không truy cập được**.

**Dấu hiệu app đã chạy thành công:** log có dòng tương đương **`Started Application`** (và Tomcat listen port 8082).

**Nguyên nhân gốc (đề xuất xử lý dứt điểm):**

1. File `src/main/resources/db/changelog/db.changelog-master.xml` hiện chỉ include một phần changeset (~18), **không** còn chuỗi migration đầy đủ (`01-create-initial-schema.xml` → `28-finalize-merged-schema.xml`, …).
2. DB Azure **thiếu bảng/cột** hoặc **khác ràng buộc NULL** so với code Entity hiện tại.
3. QA đã patch tạm bằng changeset `35`, `36` và sửa Entity `ChallengeBank.region` — **chưa đủ** đảm bảo mọi môi trường đồng bộ.

---

## 2. Cấu hình liên quan

| File / thuộc tính | Giá trị |
|-------------------|---------|
| `application.yaml` → `spring.jpa.hibernate.ddl-auto` | `validate` |
| `application.yaml` → `spring.liquibase.change-log` | `classpath:db/changelog/db.changelog-master.xml` |
| Liquibase log (typical) | `Previously run: 17` → sau patch QA: `18` |
| Hibernate | Validate toàn bộ Entity khi startup |

---

## 3. Danh sách lỗi theo thứ tự xuất hiện

### Lỗi #1 — `challenge_bank.region` (nullable mismatch)

**Log gốc:**

```text
Schema validation: column defined as not-null in the database,
but nullable in model - [region] in table [challenge_bank]
```

| Mục | Chi tiết |
|-----|----------|
| **Bảng** | `challenge_bank` |
| **Cột** | `region` |
| **DB** | Cột **NOT NULL** |
| **Entity** | `ChallengeBank.java` — `@Column(name = "region")` **không** khai báo `nullable = false` |
| **Hậu quả** | `EntityManagerFactory` không tạo được → app dừng |

**Gợi ý fix (Backend):**

- Đồng bộ Entity: `@Column(name = "region", length = 20, nullable = false)` **hoặc**
- Đồng bộ Liquibase/DB: cho phép `region` nullable nếu đó là thiết kế đúng.

**Patch tạm QA (nếu đã merge):** sửa `ChallengeBank.java` thêm `nullable = false`.

---

### Lỗi #2 — Thiếu bảng `placement_rule`

**Log gốc:**

```text
Schema validation: missing table [placement_rule]
```

| Mục | Chi tiết |
|-----|----------|
| **Entity** | `PlacementRule.java` → `@Table(name = "placement_rule")` |
| **Service dùng** | `EducatorService` (placement rules), `ErrorTagService` (xóa tag) |
| **DB Azure** | **Không có** bảng `placement_rule` |
| **Migration tạo bảng (cũ)** | `04-add-educator-extensions.xml`, `09-add-missing-relationships.xml` — **không** nằm trong `db.changelog-master.xml` hiện tại |

**Hậu quả:** App dừng sau khi Liquibase báo `Database is up to date`.

**Gợi ý fix (Backend):**

- Khôi phục include migration tạo/sửa `placement_rule` trong master **hoặc**
- Baseline / chạy đủ changeset `04`, `09`, `13`, `28` trên DB shared.

**Patch tạm QA (nếu đã merge):**

- File: `db/changelog/35-create-placement-rule-table.xml`
- Changeset: `35-create-placement-rule-table`
- **Kết quả khi chạy:** `Table placement_rule created` — **thành công**

---

### Lỗi #3 — Thiếu cột `reward_catalog.description`

**Log gốc (sau khi fix lỗi #2):**

```text
Schema validation: missing column [description] in table [reward_catalog]
```

| Mục | Chi tiết |
|-----|----------|
| **Entity** | `RewardCatalog.java` — field `description` (`TEXT`) |
| **DB Azure** | Bảng `reward_catalog` tồn tại (rename từ `achievement` qua migration `27`) nhưng **thiếu cột** `description` |
| **Migration cũ có `description`** | `14-add-achievement-and-leaderboard-tables.xml` (bảng `achievement`) — không chạy trên master hiện tại |

**Hậu quả:** App vẫn dừng; chưa tới `Started Application`.

**Gợi ý fix (Backend):**

```sql
ALTER TABLE reward_catalog ADD COLUMN IF NOT EXISTS description TEXT;
```

Hoặc thêm changeset Liquibase chính thức (không chỉ patch QA).

**Patch tạm QA (đề xuất Backend review & merge):**

- File: `db/changelog/36-add-reward-catalog-description.xml`
- Changeset: `36-add-reward-catalog-description`

---

## 4. Chuỗi sự kiện điển hình trên console

```
Tomcat initialized with port 8082
HikariPool → PostgreSQL Azure OK
Liquibase: Database is up to date (hoặc Run: 1 changeset mới)
Hibernate ORM validate schema
Failed to initialize JPA EntityManagerFactory
Schema validation: <chi tiết lỗi>
Error starting Tomcat context
Application run failed
[INFO] BUILD SUCCESS    ← GÂY HIỂU NHẦM
```

**Lưu ý:** Các dòng `UnsatisfiedDependencyException` liên quan `securityConfig` / `accountRepository` / `customUserDetailsService` là **hệ quả** (JPA chưa khởi tạo), **không** phải lỗi Security độc lập.

---

## 5. Patch tạm QA đã thêm (cần Backend review)

| File | Mục đích |
|------|----------|
| `ChallengeBank.java` | `region` → `nullable = false` |
| `db/changelog/35-create-placement-rule-table.xml` | Tạo bảng `placement_rule` nếu chưa có |
| `db/changelog/36-add-reward-catalog-description.xml` | Thêm cột `description` vào `reward_catalog` |
| `db.changelog-master.xml` | Include changeset 35, 36 |

**Rủi ro:** Có thể còn **lỗi validate tiếp theo** (thiếu cột/bảng khác) cho đến khi DB và Entity đồng bộ hoàn toàn.

---

## 6. Đề xuất xử lý dứt điểm (Backend)

### Ưu tiên cao

1. **Khôi phục `db.changelog-master.xml`** include đầy đủ chuỗi migration (hoặc tạo baseline mới từ schema chuẩn).
2. **Chạy / verify Liquibase** trên DB dev Azure (hoặc DB local chuẩn) một lần, đảm bảo khớp toàn bộ Entity.
3. **Chạy lại** `spring-boot:run` và xác nhận log **`Started Application`**.

### Kiểm tra nhanh sau fix

```powershell
cd BE_SP26SE049
.\mvnw.cmd spring-boot:run
```

- Mở: `http://localhost:8082/swagger-ui.html`
- Test public: `POST /api/v1/auth/login`, `POST /api/v1/auth/register`

### Tùy chọn (chỉ môi trường dev cá nhân, không khuyến nghị DB shared)

- Profile riêng `application-local.yaml` với DB local + Liquibase full — **không** dùng `ddl-auto: update` trên Azure shared.

---

## 7. Checklist xác nhận đã fix

- [ ] Log startup có **`Started Application`**
- [ ] Không còn dòng **`Schema validation:`**
- [ ] `GET http://localhost:8082/swagger-ui.html` trả về UI
- [ ] `GET http://localhost:8082/v3/api-docs` trả về OpenAPI JSON
- [ ] Liquibase master đủ changeset / DB baseline được document cho team

---

## 8. Liên hệ / ngữ cảnh

Báo cáo này phục vụ QA test API Auth (GUEST) trên Swagger. Backend fix schema giúp toàn team (FE, QA, Educator flows) chạy được môi trường local trỏ Azure DB hiện tại.

*Nếu cần log file đầy đủ, QA có thể đính kèm terminal output từ `spring-boot:run`.*
