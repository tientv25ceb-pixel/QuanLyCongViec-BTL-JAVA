# HỆ THỐNG QUẢN LÝ CÔNG VIỆC - TASK MANAGEMENT SYSTEM

## 1. Giới thiệu
Ứng dụng Java Desktop quản lý công việc cá nhân/nhóm theo mô hình **Client/Server**.
- **Server**: xử lý logic, phân quyền, kết nối MySQL
- **Client**: giao diện JavaFX cho người dùng

## 2. Yêu cầu hệ thống
- Java 17+
- MySQL Server (XAMPP / MySQL / MariaDB)
- Apache Maven

## 3. Cài đặt Database
1. Khởi động MySQL Server
2. Chạy file SQL: `database/schema.sql`
   ```bash
   mysql -u root -p < database/schema.sql
   ```

## 4. Cấu hình
Chỉnh sửa `server/src/main/resources/config.properties`:
```properties
database.url=jdbc:mysql://localhost:3306/task_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
database.user=taskapp
database.password=123456789
server.port=5555
```

## 5. Chạy Server
```bash
cd server
mvn clean compile
mvn exec:java -Dexec.mainClass="server.main.ServerMain"
```

## 6. Chạy Client
```bash
cd client
mvn clean compile
mvn javafx:run
```

**Lưu ý:** Server phải chạy trước Client.

## 7. Tài khoản mẫu
| Username | Password | Role |
|----------|----------|------|
| admin    | 123456   | Admin |
| user1    | 123456   | User |
| user2    | 123456   | User |

**Khóa đăng nhập:** Sai mật khẩu 5 lần → khóa tạm 5 phút.

## 8. Chức năng đã hoàn thiện

### Admin
- Quản lý Users (CRUD, vô hiệu hóa/kích hoạt)
- Quản lý Groups (CRUD)
- Quản lý thành viên nhóm (thêm/xóa)
- Quản lý Tasks (CRUD, tìm kiếm, lọc, sắp xếp)
- Dashboard thống kê + biểu đồ
- Export/Import CSV (import có transaction, rollback khi lỗi)
- Xem system log

### User
- Xem/sửa công việc được giao hoặc tự tạo
- Tạo công việc cá nhân
- Tìm kiếm, lọc, sắp xếp
- Dashboard cá nhân

## 9. Kiến trúc
```
Client (JavaFX)
  UI Controller → ServerConnector (Socket JSON)
Server
  ServerMain → ClientHandler (ThreadPool)
  → TaskService (phân quyền + nghiệp vụ)
  → DAO (JDBC) → MySQL
```

## 10. Bảo mật
- **Password:** hash SHA-256 trước khi lưu DB
- **Số điện thoại:** mã hóa AES trước khi lưu DB
- **Phân quyền:** kiểm tra token + role ở server
- **Session:** token sau đăng nhập, hết hạn khi logout

## 11. Định dạng file CSV

### Header (bắt buộc)
```
ID,Tieu de,Nhom,Trang thai,Do uu tien,Han chot,Nguoi thuc hien,Mo ta
```

### Ví dụ dòng dữ liệu
```
1,"Lam bao cao","Nhom A","TODO","HIGH","2026-06-15","user1","Mo ta cong viec"
```

### Quy tắc
| Cột | Bắt buộc | Giá trị hợp lệ |
|-----|----------|----------------|
| Tieu de | Có | Không rỗng |
| Trang thai | Có | TODO, IN_PROGRESS, DONE |
| Do uu tien | Có | LOW, MEDIUM, HIGH |
| Han chot | Có | yyyy-MM-dd |
| ID | Không | Số nguyên (bỏ trống khi import mới) |

Import dùng **transaction**: nếu một dòng lỗi → rollback toàn bộ, dữ liệu cũ không bị ảnh hưởng.

## 12. Thư viện sử dụng
| Thư viện | Mục đích |
|----------|----------|
| JavaFX 21 | Giao diện Desktop |
| Gson 2.10.1 | JSON client/server |
| MySQL Connector/J 8.0.33 | JDBC |
| Maven | Build & quản lý dependency |

## 13. Yêu cầu nâng cao đã triển khai
- [x] Ghi log hệ thống (file + database)
- [x] Transaction khi import CSV
- [x] Dashboard thống kê + biểu đồ PieChart
- [x] Cấu hình hệ thống (`config.properties`)
- [x] Giao diện không bị treo (`javafx.concurrent.Task`)
- [x] Kiểm tra dữ liệu đầu vào (phone, password, task)
- [x] Phân quyền server-side + khóa đăng nhập

## 14. Cấu trúc thư mục
```
QuanLyCongViec/
├── database/schema.sql
├── server/
│   ├── pom.xml
│   └── src/main/java/
│       ├── server/main/ServerMain.java
│       ├── server/handler/ClientHandler.java
│       ├── server/service/TaskService.java
│       ├── server/dao/
│       ├── server/util/
│       └── common/model/
└── client/
    ├── pom.xml
    └── src/main/java/
        ├── client/main/ClientMain.java
        ├── client/ui/controller/
        ├── client/service/
        └── common/model/
```

## 15. Log
Server ghi log vào `server.log` và bảng `system_logs`.