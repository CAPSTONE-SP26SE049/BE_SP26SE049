# API Integration Report: Error Tag CRUD & Standardization

Dưới đây là mô tả chi tiết các API phục vụ quản lý danh mục Lỗi Phát Âm (Error Tags) và các thay đổi chuẩn hóa dữ liệu cho Front-end.

---

## 1. Chuẩn hóa Level API (Standardization)

### Thay đổi field name
- **DTO**: `LevelUpdateRequest`
- **Thay đổi**: `title` -> `name` (để đồng nhất với lúc Create).

### Response DTO (Enriched Data)
Các API trả về Level hiện đã trả về Object `ErrorTagResponse` đầy đủ thay vì chỉ trả về ID đơn lẻ.

- **Endpoints**: 
  - `GET /api/v1/educator/curriculum/{region}`
  - `POST /api/v1/educator/curriculum/levels`
  - `PATCH /api/v1/educator/curriculum/levels/{id}`

- **JSON Output (Level Schema)**:
```json
{
  "id": "uuid",
  "name": "Level 1",
  "aiThreshold": 75,
  "errorTag": {
    "id": "uuid-error",
    "tagCode": "L_N_CONFUSION",
    "name": "Ngọng L/N",
    "regions": ["NORTH"]
  }
}
```

---

## 2. Chi tiết API CRUD cho Error Tags

### A. Lấy danh sách Error Tags (Retrieval)
Lấy thư viện lỗi để hiển thị trong các dropdown.

- **Endpoint**: `GET /api/v1/educator/curriculum/error-tags`
- **Method**: `GET`
- **Input (Query Params)**:
    - `dialectId` (UUID, Optional): ID vùng miền. Nếu không truyền sẽ trả về **Toàn bộ** các lỗi hiện có.

- **Output JSON**:
```json
{
  "status": "success",
  "message": "Lấy danh sách mã lỗi thành công",
  "data": [
    {
      "id": "uuid-1",
      "tagCode": "L_N_CONFUSION",
      "name": "Lẫn lộn L/N",
      "description": "...",
      "regions": ["NORTH"]
    }
  ]
}
```

### B. Thêm mới Error Tag (Create)
- **Endpoint**: `POST /api/v1/admin/error-tags`
- **Method**: `POST`
- **Input (JSON Body)**:
```json
{
  "tagCode": "NG_NGH_CONFUSION",
  "name": "Ngọng NG/NGH",
  "description": "Lỗi phát âm sai âm mũi ng/ngh"
}
```

- **Output JSON**:
```json
{
  "status": "success",
  "data": {
    "id": "uuid-new",
    "tagCode": "NG_NGH_CONFUSION",
    "name": "Ngọng NG/NGH",
    "description": "..."
  }
}
```

### C. Cập nhật Error Tag (Update)
- **Endpoint**: `PUT /api/v1/admin/error-tags/{id}`
- **Method**: `PUT`
- **Input (Request Params)**:
    - `tagCode` (String, Optional)
    - `name` (String, Optional)
    - `description` (String, Optional)

- **Input Ví dụ**: `PUT /api/v1/admin/error-tags/uuid?name=Ngọng L-N (Bắc Bộ)`

- **Output JSON**:
```json
{
  "status": "success",
  "message": "Updated successfully",
  "data": { "id": "uuid", "name": "Ngọng L-N (Bắc Bộ)", ... }
}
```

### D. Xóa Error Tag (Delete)
- **Endpoint**: `DELETE /api/v1/admin/error-tags/{id}`
- **Method**: `DELETE`

- **Output Thành công (200 OK)**:
```json
{ "status": "success", "message": "Deleted successfully" }
```

- **Output Thất bại (412 Precondition Failed)**:
*Xảy ra khi xóa lỗi đang có Level tham chiếu tới.*
```json
{
  "status": "error",
  "message": "Không thể xóa mã lỗi đang được sử dụng trong các Level",
  "errors": "PRECONDITION_FAILED"
}
```
