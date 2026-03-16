# API Change Report — Database Table Merge Refactoring
**Version:** Post-Merge v2.0  
**Date:** 2026-03-16  
**Author:** Backend Team  
**Affected by:** Gộp bảng (Content + Activity + Social Groups)

> **FE Team: Hãy đọc kỹ và cập nhật lại các API call tương ứng.**

---

## 1. ACTIVITY GROUP — Session & Attempt APIs

### 1.1 `POST /api/v1/gameplay/sessions` — Start Session
> Không thay đổi endpoint. Response thêm field mới.

**Response thay đổi:**

| Field cũ | Field mới | Ghi chú |
|----------|-----------|---------|
| _(không có)_ | `sessionType` | Luôn trả về `"PRACTICE"` |

```json
// Response mới
{
  "id": "uuid",
  "accountId": "uuid",
  "sessionType": "PRACTICE",
  "startedAt": "2026-03-16T10:00:00Z",
  "endedAt": null
}
```

---

### 1.2 `PUT /api/v1/gameplay/sessions/{sessionId}/end` — End Session
> Không thay đổi endpoint. Response giống 1.1.

---

### 1.3 `POST /api/v1/gameplay/attempts` — Submit Attempt
> **BREAKING CHANGE** — xóa field `quizAttemptId` khỏi Request.

**Request thay đổi:**

| Field | Thay đổi | Mô tả |
|-------|----------|-------|
| `sessionId` | ✅ Giữ nguyên | ID của study session |
| `challengeId` | ✅ Giữ nguyên | ID của content item (PRONUNCIATION) |
| `audioUrl` | ✅ Giữ nguyên | URL audio nộp |
| ~~`quizAttemptId`~~ | ❌ **ĐÃ XÓA** | Quiz Attempt đã gộp vào StudySession |

```json
// Request mới (bỏ quizAttemptId)
{
  "sessionId": "uuid",
  "challengeId": "uuid",
  "audioUrl": "https://..."
}
```

**Response thay đổi:**

| Field cũ | Field mới | Ghi chú |
|----------|-----------|---------|
| `sessionId` | `sessionId` | ✅ Không đổi |
| `challengeId` | `challengeId` | ✅ Không đổi — nay map tới `content_item.id` |
| `audioUrl` | `audioUrl` | ✅ Không đổi |
| `scoreOverall` | `scoreOverall` | ✅ Không đổi |
| `isPassed` | `isPassed` | ✅ Không đổi |
| `latencyMs` | `latencyMs` | ✅ Không đổi |
| `createdAt` | `createdAt` | ✅ Không đổi |

---

### 1.4 `GET /api/v1/gameplay/attempts/history` — Attempt History
> Không thay đổi endpoint hoặc response shape. Logic nội bộ đã được cập nhật.

---

## 2. CONTENT GROUP — Dialect, Level, Challenge APIs

### 2.1 `GET /api/v1/dialects` — Get All Dialects
> Không thay đổi endpoint hoặc response. Logic nội bộ đã được cập nhật dùng `learning_unit` (type=DIALECT).

```json
// Response giữ nguyên
[
  { "id": "uuid", "name": "Northern (Hanoi)", "description": "..." }
]
```

---

### 2.2 `GET /api/v1/levels?dialectId={id}` — Get Levels by Dialect
> Không thay đổi endpoint hoặc response shape.  
> Dữ liệu nay lấy từ `learning_unit` (type=LEVEL, parent_id=dialectId).

```json
// Response giữ nguyên
[
  {
    "id": "uuid",
    "dialectId": "uuid",
    "levelOrder": 1,
    "name": "Beginner",
    "description": "...",
    "minStarsRequired": 0,
    "status": "APPROVED"
  }
]
```

---

### 2.3 `GET /api/v1/challenges/level/{levelId}` — Get Challenges by Level
> Không thay đổi endpoint hoặc response shape.  
> Dữ liệu nay lấy từ `content_item` (type=PRONUNCIATION, learning_unit_id=levelId).

```json
// Response giữ nguyên
[
  {
    "id": "uuid",
    "levelId": "uuid",
    "type": "PRONUNCIATION",
    "skillType": "...",
    "contentText": "Xin chào",
    "phoneticTranscriptionIpa": "...",
    "referenceAudioUrl": "https://...",
    "focusPhonemes": "x,s",
    "status": "APPROVED"
  }
]
```

---

## 3. SOCIAL GROUP — Badge & Achievement APIs

### 3.1 `GET /api/v1/badges` — Get All Badges
> Không thay đổi endpoint hoặc response.  
> Dữ liệu nay lấy từ `reward_catalog` (reward_type=BADGE).

---

### 3.2 `GET /api/v1/badges/my-badges` — Get My Badges
> Không thay đổi endpoint hoặc response.  
> Dữ liệu nay lấy từ `account_reward` JOIN `reward_catalog`.

---

## 4. ADMIN GROUP — Content Management APIs

### 4.1 Admin Challenge APIs
> `POST /api/v1/admin/content/challenges`  
> `PUT /api/v1/admin/content/challenges/{id}`

Không thay đổi request/response. Dữ liệu lưu vào `content_item` (type=PRONUNCIATION).

### 4.2 Admin Level APIs
> `POST /api/v1/admin/content/levels`  
> `PUT /api/v1/admin/content/levels/{id}`

Không thay đổi request/response. Dữ liệu lưu vào `learning_unit` (type=LEVEL).

### 4.3 Admin Dialect APIs
> `POST /api/v1/admin/content/dialects`  
> `PUT /api/v1/admin/content/dialects/{id}`

Không thay đổi request/response. Dữ liệu lưu vào `learning_unit` (type=DIALECT).

### 4.4 Error Tag APIs
> `GET /api/v1/public/error-tags`  
> `POST /api/v1/admin/error-tags`  
> `PUT /api/v1/admin/error-tags/{id}`  
> `DELETE /api/v1/admin/error-tags/{id}`

Không thay đổi endpoint hoặc response. Dữ liệu nay lưu trong `learning_unit` (type=ERROR_TAG).

---

## 5. TÓM TẮT BREAKING CHANGES

| # | API | Loại thay đổi | Mức độ |
|---|-----|--------------|--------|
| 1 | `POST /api/v1/gameplay/attempts` | Xóa field `quizAttemptId` khỏi request body | **🔴 BREAKING** |
| 2 | `POST /api/v1/gameplay/sessions` | Response thêm field `sessionType` | 🟡 Additive |
| 3 | `PUT /api/v1/gameplay/sessions/{id}/end` | Response thêm field `sessionType` | 🟡 Additive |

---

## 6. DATABASE TABLE MAPPING (Tham khảo)

| Bảng cũ | Bảng mới | Phân biệt qua |
|---------|---------|--------------|
| `dialect` | `learning_unit` | `type = 'DIALECT'` |
| `level` | `learning_unit` | `type = 'LEVEL'`, `parent_id` = dialect |
| `error_tag` | `learning_unit` | `type = 'ERROR_TAG'` |
| `challenge` | `content_item` | `type = 'PRONUNCIATION'` |
| `quiz` | `content_item` | `type = 'QUIZ'` |
| `quiz_question` | `content_item.items_json` | Embedded JSON |
| `practice_session` | `study_session` | `session_type = 'PRACTICE'` |
| `quiz_attempt` | `study_session` | `session_type = 'QUIZ'` |
| `daily_challenge` | `study_session` | `session_type = 'DAILY'` |
| `attempt` | `session_detail` | — |
| `achievement` | `reward_catalog` | `reward_type = 'ACHIEVEMENT'` |
| `badge` | `reward_catalog` | `reward_type = 'BADGE'` |
| `account_achievement` | `account_reward` | — |
| `account_badge` | `account_reward` | — |
| `user_profile` | `account` | Fields merged into account |
