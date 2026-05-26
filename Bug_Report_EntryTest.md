# Bug Report — Luồng Entry Test & AI (Learner)

| Thông tin | Chi tiết |
|-----------|----------|
| **Người lập** | QA Lead |
| **Đối tượng** | Team Dev (Tuấn) — Backend `BE_SP26SE049` |
| **Phạm vi** | Entry Test, AI Pronunciation, Placement Set |
| **Môi trường tham chiếu** | Nhánh `development` — phân tích tĩnh từ source |
| **Ngày** | 2026-05-23 |

---

## Tóm tắt

| ID | Endpoint | Severity |
|----|----------|----------|
| BUG-001 | `POST /api/v1/test/analyze-step`, `POST /api/v1/ai/evaluate-pronunciation`, `POST /api/v1/ai/feedback` | **High** |
| BUG-002 | `POST /api/v1/test/analyze-step` | **Medium** |
| BUG-003 | `POST /api/v1/test/finish` | **High** |
| BUG-004 | `GET /api/v1/test/placement-set` | **Medium** |

---

## 🐞 [BUG-001] Audio upload APIs — Thiếu validate định dạng `.webm`

**Tiêu đề:** Hệ thống chấp nhận file `.mp3`/`.wav` và trả về HTTP 200 thay vì từ chối upload/chấm điểm

**Mức độ nghiêm trọng (Severity):** **High**

**Mô tả chi tiết:**

Theo yêu cầu nghiệp vụ dự án (SpeakVN / luyện phát âm chuẩn), client **bắt buộc** gửi audio định dạng **`.webm`**. Trên code hiện tại:

1. **`EntryTestController.analyzeStep`** — nhận `@RequestParam("file") MultipartFile` nhưng **không** kiểm tra extension/MIME; gọi thẳng `EntryTestService.analyzeEntryTestStep` → `FirebaseStorageService.uploadFile` với `file.getContentType()` từ client.
2. **`AIController.evaluatePronunciation`** — chỉ validate `audio.isEmpty()`; ném `ApiException` khi rỗng, **không** kiểm tra `.webm`.
3. **`AIController.getFeedback`** — nhận `audioUrl` trong JSON body; **không** validate đuôi file trên URL.

`FirebaseStorageService.uploadFile` lưu byte stream theo `contentType` client gửi, không có whitelist `audio/webm`.

**Kết quả thực tế (Actual Result):**

- Gửi file `.mp3` hoặc `.wav` (> 0 byte) qua Swagger/Postman.
- **HTTP 200 OK** với payload chấm điểm/upload thành công (vd. `{"url":"..."}`, `accuracy`, `feedback`).
- Không có message lỗi validation định dạng.

**Kết quả mong đợi (Expected Result):**

- Chỉ chấp nhận multipart có extension `.webm` và/hoặc `Content-Type: audio/webm` (và có thể kiểm tra magic bytes nếu cần chặt).
- File sai định dạng → **HTTP 400 Bad Request** với message rõ ràng, ví dụ: `"Chỉ chấp nhận file âm thanh định dạng .webm"` (hoặc mã lỗi `INVALID_AUDIO_FORMAT`).
- **HTTP 200** chỉ khi file `.webm` hợp lệ và pipeline ASR/AI xử lý được.

**Hướng fix logic (gợi ý cho Dev):**

- Tạo helper dùng chung (vd. `AudioUploadValidator.validateWebm(MultipartFile file)`) gọi từ `FileController`, `EntryTestController`, `AIController`, `DailyChallengeController`.
- Với `/feedback`: validate `audioUrl` kết thúc bằng `.webm` hoặc path Firebase chứa `.webm` nếu bắt buộc đồng bộ FE.

**File tham chiếu:**

- `controller/EntryTestController.java` — `analyzeStep` (L49–61)
- `controller/AIController.java` — `evaluatePronunciation` (L39–59), `getFeedback` (L67–187)
- `service/FirebaseStorageService.java` — `uploadFile` (L23–37)

---

## 🐞 [BUG-002] `POST /api/v1/test/analyze-step`

**Tiêu đề:** `questionId` không tồn tại trả HTTP 500 do catch-all thay vì 404 Not Found

**Mức độ nghiêm trọng (Severity):** **Medium**

**Mô tả chi tiết:**

`EntryTestService.analyzeEntryTestStep` ném `ResourceNotFoundException("Question not found")` khi `questionRepository.findById(questionId)` rỗng.

Tuy nhiên `EntryTestController.analyzeStep` bọc toàn bộ logic trong:

```java
try {
    ...
} catch (Exception e) {
    return ResponseEntity.internalServerError()
        .body(ApiResponse.error("Phát sinh lỗi khi chẩn đoán"));
}
```

Mọi exception (kể cả `ResourceNotFoundException`) bị nuốt và map thành **500**, không đi qua `GlobalExceptionHandler` (handler 404 cho `ResourceNotFoundException`).

**Kết quả thực tế (Actual Result):**

- Request: `POST /api/v1/test/analyze-step` + JWT USER hợp lệ + `questionId` = UUID không có trong DB + file audio hợp lệ.
- **HTTP 500 Internal Server Error**
- Body: `{"status":"error","message":"Phát sinh lỗi khi chẩn đoán"}`

**Kết quả mong đợi (Expected Result):**

- **HTTP 404 Not Found**
- Message: `"Question not found"` (hoặc message tiếng Việt thống nhất API, vd. `"Không tìm thấy câu hỏi kiểm tra đầu vào"`)
- Không log/stacktrace như lỗi hệ thống nội bộ đối với case nghiệp vụ “không tìm thấy”.

**Hướng fix logic (gợi ý cho Dev):**

- Bỏ `catch (Exception e)` rộng; để exception propagate tới `@RestControllerAdvice`, **hoặc**
- `catch (ResourceNotFoundException e)` → `ResponseEntity.status(404).body(...)`, **hoặc**
- Dùng `ApiException("NOT_FOUND", ...)` thống nhất với các API khác.

**File tham chiếu:**

- `controller/EntryTestController.java` — `analyzeStep` (L55–61)
- `service/EntryTestService.java` — `analyzeEntryTestStep` (L151–153)
- `exception/GlobalExceptionHandler.java` — `handleResourceNotFoundException` (L26–33)

---

## 🐞 [BUG-003] `POST /api/v1/test/finish`

**Tiêu đề:** Mảng `stepResults` rỗng gây chia cho 0 và HTTP 500

**Mức độ nghiêm trọng (Severity):** **High**

**Mô tả chi tiết:**

`EntryTestService.saveFinalResult` tính điểm trung bình:

```java
double overallScore = totalAccuracy / stepResults.size();
```

Khi client gửi `stepResults: []`, `stepResults.size()` = **0** → `ArithmeticException` (divide by zero) → không được validate trước → request fail với lỗi runtime (**500**).

Không có guard clause kiểm tra `stepResults == null || stepResults.isEmpty()` ở controller hoặc service.

**Kết quả thực tế (Actual Result):**

- Request: `POST /api/v1/test/finish` + JWT USER + body `[]`
- **HTTP 500 Internal Server Error**
- Log server: `ArithmeticException: / by zero` (hoặc message runtime tương đương qua handler)

**Kết quả mong đợi (Expected Result):**

- **HTTP 400 Bad Request**
- Message: `"Danh sách kết quả bước không được rỗng"` / `"stepResults must contain at least 1 item"`
- Không ghi `EntryTestResult`, không unlock level, không tạo roadmap khi input invalid.

**Hướng fix logic (gợi ý cho Dev):**

- Đầu `saveFinalResult`: if empty → `throw new BadRequestException(...)` hoặc `ApiException("BAD_REQUEST", ...)`.
- Tùy chọn: validate số phần tử khớp số câu placement (vd. = 10) trước khi tính `overallScore`.

**File tham chiếu:**

- `controller/EntryTestController.java` — `finishTest` (L64–75)
- `service/EntryTestService.java` — `saveFinalResult` (L252–293)

---

## 🐞 [BUG-004] `GET /api/v1/test/placement-set`

**Tiêu đề:** Tham số `region` sai giá trị không báo lỗi — im lặng fallback trộn 3 miền

**Mức độ nghiêm trọng (Severity):** **Medium**

**Mô tả chi tiết:**

`EntryTestService.getPlacementSet(String region)` map `region` qua `switch`:

- `NORTH` → `NORTH_NL`
- `CENTRAL` → `CENTRAL_DGIR`
- `SOUTH` → `SOUTH_TRCH`
- **default** → `categoryMap = null`

Khi client gửi giá trị không hợp lệ (vd. `region=HANG_NGAY`), `categoryMap` = null, không có exception. Logic tiếp theo: nếu `questions.size() < 10` → **clear và fallback** lấy 3+3+3+1 câu trộn miền.

Client/API consumer không biết param bị bỏ qua — hành vi giống như “không truyền region”.

**Kết quả thực tế (Actual Result):**

- Request: `GET /api/v1/test/placement-set?region=HANG_NGAY` + JWT USER
- **HTTP 200 OK**
- `message`: `"Lấy bộ câu hỏi kiểm tra đầu vào thành công"`
- `data`: danh sách câu **trộn 3 miền** (fallback), không phản ánh `region` client gửi

**Kết quả mong đợi (Expected Result):**

- Giá trị `region` không thuộc `{NORTH, CENTRAL, SOUTH}` (case-insensitive) → **HTTP 400 Bad Request**
- Message: `"Giá trị region không hợp lệ. Chấp nhận: NORTH, CENTRAL, SOUTH"`
- Chỉ fallback trộn miền khi **không truyền** `region` hoặc `region` blank (theo spec product), không khi truyền **sai enum**.

**Hướng fix logic (gợi ý cho Dev):**

- Sau khi parse `region`: nếu non-blank và `categoryMap == null` → throw `BadRequestException`.
- Hoặc đổi `@RequestParam` sang enum `RegionCode` để Spring tự validate (400 khi invalid).

**File tham chiếu:**

- `controller/EntryTestController.java` — `getPlacementSet` (L38–46)
- `service/EntryTestService.java` — `getPlacementSet` (L105–147)

---

## Phụ lục — Cách reproduce nhanh (Swagger)

| Bug | Steps |
|-----|--------|
| BUG-001 | Upload `.mp3` tại `analyze-step` / `evaluate-pronunciation`; gửi `audioUrl` `.mp3` tại `feedback` |
| BUG-002 | `analyze-step` + `questionId` = UUID random không có trong DB |
| BUG-003 | `finish` + body `[]` |
| BUG-004 | `placement-set?region=HANG_NGAY` |

---

## Trạng thái

| ID | Status | Assignee | Ghi chú |
|----|--------|----------|---------|
| BUG-001 | **Fixed** (Entry Test + AI `/evaluate-pronunciation`, `/feedback`) | Tuấn | Dùng chung `WebmAudioValidator` |
| BUG-002 | **Fixed** | Tuấn | `ResourceNotFoundException` → 404 qua `GlobalExceptionHandler` |
| BUG-003 | **Fixed** | Tuấn | |
| BUG-004 | **Fixed** (re-open: tách if-else, `VALID_PLACEMENT_REGIONS`) | Tuấn | |

---

*Tài liệu được sinh từ rà soát tĩnh mã nguồn phục vụ Manual Test / Test Plan Excel. Vui lòng cập nhật cột Status sau khi fix và verify trên môi trường staging.*
