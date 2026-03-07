# Yêu Cầu Cập Nhật API: Bổ sung Trạng Thái và Lý Do Từ Chối cho Danh Sách Bài Tập (Challenge)

**Kính gửi Team Backend,**

Hiện tại, trong Quá trình Phê duyệt Nội dung (Content Approval Workflow), tính năng Từ chối Bài tập (Challenge) đã ghi nhận được thông tin nhưng lại đang gặp vấn đề hiển thị phía Educator. 

Cụ thể, Educator **không thể xem được lý do từ chối** trực tiếp trên danh sách bài tập. 

Qua kiểm tra source code Frontend, nguyên nhân không nằm ở UI/UX mà nằm ở dữ liệu API trả về. 

Dưới đây là báo cáo lỗi chi tiết và yêu cầu cập nhật API:

---

## 1. Mô Tả Vấn Đề

* **Phía Admin (Gửi yêu cầu):** Frontend đã gửi dữ liệu từ chối thành công thông qua API `PUT /api/v1/admin/content/challenges/{id}/review` với payload bao gồm đầy đủ `status: "REJECTED"` và `rejectionReason: "Lý do..."`.
* **Phía Educator (Nhận dữ liệu):** Khi tải danh sách các bài tập con thuộc một Level (API `GET /challenges/level/{levelId}`), model `ChallengeResponse` trả về **bị thiếu 2 thuộc tính cực kỳ quan trọng**:
  1. `status` (Trạng thái hiện tại: PENDING, APPROVED, REJECTED, DRAFT)
  2. `rejectionReason` (Lý do từ chối - String)

Vì API bị thiếu 2 trường này, UI Frontend không có dữ liệu để kích hoạt khối block "Lý do từ chối" đã được code sẵn.

---

## 2. Giải Pháp (Hành động yêu cầu cho BE)

Vui lòng cập nhật API Endpoint: `GET /challenges/level/{levelId}`

**Yêu cầu:** Bổ sung trường `status` và `rejectionReason` vào đối tượng `ChallengeResponse` khi liệt kê các bài tập.

**Cấu trúc JSON mong đợi:**

```json
{
  "status": "success",
  "data": [
    {
      "id": "uuid-challenge-1",
      "levelId": "uuid-level",
      "type": "WORD",
      "contentText": "Năng lực",
      "phoneticTranscriptionIpa": "naŋ˧ lak̚˧",
      "referenceAudioUrl": "https://...",
      "focusPhonemes": "N, L",
      
      // === 🔴 CÁC TRƯỜNG CẦN BỔ SUNG ===
      "status": "REJECTED", 
      "rejectionReason": "Phiên âm IPA sai dấu thanh nhé",
      // ===================================
      
      "createdAt": "2026-03-05...",
      "updatedAt": "2026-03-05..."
    }
  ]
}
```

## 3. Lưu ý thêm

Ngay khi Backend cập nhật xong 2 field trên và deploy, tính năng hiển thị lỗi màu đỏ phía Educator sẽ tự động nổi lên mà Frontend **không cần phải sửa thêm dòng code nào**.

Rất mong BE ưu tiên xử lý ticket này sớm để luồng Review bài tập được khép kín và mang lại trải nghiệm đầy đủ nhất cho người tạo nội dung.

Cảm ơn team Backend!

---

## 4. Phản Hồi Từ Team Backend (Đã Xử Lý)

**Dear Frontend Team,**

Backend đã tiếp nhận report và hoàn tất việc cập nhật API theo đúng yêu cầu.

### Chi Tiết Cập Nhật:
1. **Đối tượng Cập nhật:** `ChallengeResponse` và `LevelResponse`.
2. **Trường dữ liệu được bổ sung:**
    - `status`: String (Enum: `DRAFT`, `PENDING`, `APPROVED`, `REJECTED`)
    - `rejectionReason`: String (Lý do từ chối chi tiết, chỉ có dữ liệu khi `status` là `REJECTED`).
3. **Phạm vi áp dụng:**
    - API `GET /api/v1/educators/challenges/level/{levelId}`
    - API `GET /api/v1/admin/challenges/level/{levelId}` 
    - Tất cả các API trả về danh sách (`getAll`) hoặc chi tiết (`getById`) của Level/Challenge.

### Mẫu JSON Response Trả Về Hiện Phía Bạn Có Thể Get Ngay:

```json
{
  "status": "success",
  "data": [
    {
      "id": "uuid-challenge-1",
      "levelId": "uuid-level",
      "type": "WORD",
      "contentText": "Năng lực",
      "phoneticTranscriptionIpa": "naŋ˧ lak̚˧",
      "referenceAudioUrl": "https://...",
      "focusPhonemes": "N, L",
      "status": "REJECTED", 
      "rejectionReason": "Phiên âm IPA sai dấu thanh nhé",
      "createdAt": "2026-03-05T10:00:00Z"
    }
  ]
}
```

Các bạn chỉ cần pull code mới và test lại giao diện. Block "Lý do từ chối" của FE sẽ hoạt động trơn tru.
Nếu có bất kỳ vấn đề gì, hãy ping lại team BE nhé! 

**Trân trọng,**
**Backend Team.**
