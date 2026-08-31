# CNJ42 - Hotel Management System

> Ứng dụng Desktop quản lý vận hành khách sạn  
> Đề tài: **CNJ42**  
> Công nghệ: **Java Swing + JDBC + MySQL + Maven**

---

## 1. Giới thiệu

**Hotel Management System** là ứng dụng Desktop được xây dựng nhằm hỗ trợ quản lý các hoạt động cơ bản trong khách sạn.

Hệ thống tập trung vào các nghiệp vụ:

- Đăng nhập và phân quyền người dùng
- Quản lý loại phòng
- Quản lý phòng
- Quản lý khách hàng
- Quản lý đặt phòng
- Nhận phòng (Check-in)
- Trả phòng (Check-out)
- Quản lý dịch vụ
- Ghi nhận dịch vụ khách sử dụng
- Lập hóa đơn
- Thanh toán
- Quản lý lịch sử giao dịch

---

# 2. Công nghệ sử dụng

| Công nghệ | Mục đích |
|---|---|
| Java 23 | Ngôn ngữ lập trình |
| Java Swing | Xây dựng giao diện Desktop |
| JDBC | Kết nối và thao tác với MySQL |
| MySQL | Hệ quản trị cơ sở dữ liệu |
| Maven | Quản lý project và dependencies |
| Git / GitHub | Quản lý mã nguồn |
| IntelliJ IDEA / VS Code | IDE phát triển |

---

# 3. Cấu trúc hệ thống

Hệ thống được thiết kế theo mô hình phân lớp, tách biệt giữa giao diện, xử lý nghiệp vụ và truy cập cơ sở dữ liệu.

```text
CNJ42-Hotel-Management/
│
├── .mvn/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cnj42/hotel/
│   │   │       │
│   │   │       ├── dao/
│   │   │       │   ├── UserDAO.java
│   │   │       │   ├── RoomDAO.java
│   │   │       │   ├── RoomTypeDAO.java
│   │   │       │   ├── GuestDAO.java
│   │   │       │   ├── ReservationDAO.java
│   │   │       │   ├── StayDAO.java
│   │   │       │   ├── ServiceDAO.java
│   │   │       │   ├── ServiceUsageDAO.java
│   │   │       │   ├── InvoiceDAO.java
│   │   │       │   └── PaymentDAO.java
│   │   │       │
│   │   │       ├── model/
│   │   │       │   ├── User.java
│   │   │       │   ├── Room.java
│   │   │       │   ├── RoomType.java
│   │   │       │   ├── Guest.java
│   │   │       │   ├── Reservation.java
│   │   │       │   ├── Stay.java
│   │   │       │   ├── Service.java
│   │   │       │   ├── ServiceUsage.java
│   │   │       │   ├── Invoice.java
│   │   │       │   └── Payment.java
│   │   │       │
│   │   │       ├── service/
│   │   │       │   ├── AuthService.java
│   │   │       │   ├── ReservationService.java
│   │   │       │   ├── CheckInService.java
│   │   │       │   └── BillingService.java
│   │   │       │
│   │   │       ├── ui/
│   │   │       │   ├── LoginFrame.java
│   │   │       │   ├── MainFrame.java
│   │   │       │   ├── RoomPanel.java
│   │   │       │   ├── GuestPanel.java
│   │   │       │   ├── ReservationPanel.java
│   │   │       │   ├── ServicePanel.java
│   │   │       │   └── InvoicePanel.java
│   │   │       │
│   │   │       ├── utils/
│   │   │       │   ├── DBConnection.java
│   │   │       │   └── Session.java
│   │   │       │
│   │   │       └── Main.java
│   │   │
│   │   └── resources/
│   │
│   └── test/
│
├── database/
│   └── hotel_management.sql
│
├── docs/
│
├── .gitignore
├── pom.xml
└── README.md
---
# 4. Cấu trúc database
users
  │
  └── quản lý nhân viên đăng nhập

room_types ────────< rooms
                         │
guests ──< reservations >── room
              │
              └──< reservation_guests

reservations ──< stays
                    │
                    ├──< service_usages >── services
                    │
                    └── invoice
                           │
                           ├──< invoice_details
                           │
                           └──< payments
# 5. Luồng nghiệp vụ hệ thống
Hệ thống vận hành theo các nhóm nghiệp vụ chính:

STT	Nghiệp vụ	            Mô tả
1	Đăng nhập	            Nhân viên đăng nhập và truy cập hệ thống theo quyền được cấp
2	Quản lý danh mục	    Quản lý phòng, loại phòng, khách hàng và dịch vụ
3	Đặt phòng	            Tiếp nhận yêu cầu, kiểm tra phòng trống và tạo thông tin đặt phòng
4	Lưu trú	Thực            hiện Check-in, quản lý phòng và quá trình khách lưu trú
5	Dịch vụ	                Ghi nhận các dịch vụ khách sử dụng trong thời gian lưu trú
6	Check-out	            Kết thúc quá trình lưu trú và tổng hợp các khoản chi phí
7	Hóa đơn & thanh toán	Lập hóa đơn, ghi nhận thanh toán và hoàn tất giao dịch
8	Cập nhật trạng thái	    ập nhật trạng thái phòng, đặt phòng và lưu trú sau mỗi nghiệp vụ