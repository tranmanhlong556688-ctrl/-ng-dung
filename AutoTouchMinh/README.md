# Auto Touch Minh v1.0.0

Ứng dụng Android tự động thực hiện chuỗi thao tác tại tối đa 10 điểm do người dùng tự cấu hình.

## Chức năng đã có

- 10 điểm nổi kéo thả, bật/tắt riêng.
- Chạy theo thứ tự tăng dần và lặp lại.
- Nhấp, nhấp nhiều lần, nhấp đúp, nhấn giữ.
- Vuốt lên/xuống/trái/phải hoặc đến tọa độ đích.
- Chờ, Quay lại, Home, ứng dụng gần đây, mở thông báo.
- Cài thời gian chờ trước/sau, khoảng cách nhấp và thời lượng.
- Bắt đầu, tạm dừng, tiếp tục, dừng, thu nhỏ, khóa điểm.
- Dừng khẩn cấp bằng nút dừng hoặc bấm Giảm âm lượng 3 lần trong 1,5 giây.
- Xuất/nhập cấu hình JSON và xuất nhật ký CSV.
- Không root, không quảng cáo, không đăng nhập, không gửi dữ liệu ra mạng.

## Yêu cầu

- Android 8.0 trở lên (minSdk 26).
- Cấp quyền Hiển thị trên ứng dụng khác.
- Bật Dịch vụ trợ năng `Auto Touch Minh`.
- Trên Xiaomi/Redmi nên đặt Pin thành `Không hạn chế` và bật Tự khởi động.

## Build

Workflow `.github/workflows/build-autotouch-minh.yml` dùng Java 17, Gradle 8.9 và build APK debug tự động.

## Giới hạn an toàn

Không dùng trên màn hình khóa, ứng dụng ngân hàng, màn hình nhập mật khẩu, CAPTCHA hoặc nơi bạn không có quyền tự động hóa. Android có thể dừng dịch vụ nền khi tối ưu pin; hãy cấp lại quyền khi cần.
