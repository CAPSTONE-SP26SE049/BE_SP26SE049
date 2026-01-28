# AI Backend Service

Dự án Backend cho dịch vụ AI, được xây dựng dựa trên kiến trúc **Clean Architecture** và **FastAPI**.

## 🏗 Kiến trúc dự án (Clean Architecture)

Dự án được tổ chức thành 4 lớp chính, tuân thủ nguyên tắc Clean Architecture để đảm bảo tính tách biệt, dễ bảo trì và mở rộng:

*   **Domain Layer** (`src/domain`):
    *   Chứa các **Entities** (Business Objects) và **Repository Interfaces**.
    *   Đây là lớp trong cùng, không phụ thuộc vào bất kỳ lớp nào khác.
    *   Chứa logic nghiệp vụ cốt lõi nhất.

*   **Application Layer** (`src/application`):
    *   Chứa các **Use Cases** (Business Logic cụ thể) và **Services**.
    *   Điều phối luồng dữ liệu giữa Presentation và Domain.
    *   Phụ thuộc vào Domain Layer.

*   **Infrastructure Layer** (`src/infrastructure`):
    *   Triển khai các chi tiết kỹ thuật: Database (SQLAlchemy), AI Model Loading, External Services.
    *   Chứa **Repository Implementations** (implement các interface từ Domain).
    *   Chứa cấu hình hệ thống (`config`).

*   **Presentation Layer** (`src/presentation`):
    *   Giao tiếp với bên ngoài qua API (Restful API với FastAPI).
    *   Chứa **Routers**, **Controllers** (Endpoints), **Schemas** (Pydantic Models) và **Middleware**.

### Cấu trúc thư mục

```
src/
├── application/        # Application Business Rules
│   ├── dto/           # Data Transfer Objects
│   └── services/      # Application Services
├── domain/            # Enterprise Business Rules (Core)
│   ├── entities/      # Domain Models
│   ├── repositories/  # Interfaces for Persistence
│   └── use_cases/     # Use Case Interactors
├── infrastructure/    # Frameworks & Drivers
│   ├── ai/            # AI Model Integration
│   ├── config/        # Settings & Configuration
│   └── database/      # DB Connection & ORM Models
└── presentation/      # Interface Adapters
    ├── api/           # API Routes & Endpoints
    ├── middleware/    # HTTP Middleware
    └── schemas/       # Request/Response Schemas
```

## 🚀 Cài đặt và Chạy dự án

### Yêu cầu hệ thống

*   Python 3.10 trở lên
*   Pip
*   Docker (tùy chọn, nếu dùng container)

### 1. Thiết lập môi trường

Khuyến khích sử dụng môi trường ảo (Virtual Environment):

```bash
# Tạo môi trường ảo
python -m venv venv

# Kích hoạt môi trường (MacOS/Linux)
source venv/bin/activate

# Kích hoạt môi trường (Windows)
.\venv\Scripts\activate
```

### 2. Cài đặt thư viện

```bash
pip install -r requirements.txt
```

### 3. Cấu hình biến môi trường

Sao chép file mẫu `.env.example` thành `.env` và cập nhật các thông số cần thiết (Database URL, Secret Key, ...):

```bash
cp .env.example .env
```

### 4. Chạy Server

Chạy ứng dụng với Uvicorn (Hot-reload enabled):

```bash
uvicorn src.presentation.main:app --reload --host 0.0.0.0 --port 8000
```

Hoặc sử dụng script có sẵn (MacOS/Linux):

```bash
./run.sh
```

## 📚 Tài liệu API

Sau khi khởi động server, bạn có thể truy cập tài liệu API tự động tại:

*   **Swagger UI**: [http://localhost:8000/docs](http://localhost:8000/docs)
*   **ReDoc**: [http://localhost:8000/redoc](http://localhost:8000/redoc)

## 🛠 Các tính năng chính

*   **Quản lý Users**: Đăng ký, xác thực (JWT).
*   **AI Prediction**: Endpoint dự đoán sử dụng AI model.
*   **Middleware**:
    *   CORS
    *   Logging
    *   Error Handling
    *   Rate Limiting
    *   Authentication (JWT)

## 🧪 Testing

Chạy các bài kiểm thử (nếu có):

```bash
pytest
```
