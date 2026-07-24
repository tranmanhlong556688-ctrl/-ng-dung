# HƯỚNG DẪN CÀI AUTO TOUCH MINH TRÊN XIAOMI REDMI 10

## A. Kiểm tra bằng bản tối giản

1. Tải và giải nén bộ bàn giao; không mở APK khi còn trong ZIP.
2. So SHA-256 với `SHA256SUMS.txt`.
3. Cài `AutoTouchMinh-Installation-Test-v1.0.0.apk` trước.
4. Nếu bản này không cài được, lỗi không đến từ Accessibility/Overlay/Foreground Service vì bản này không có các quyền đó.
5. Nếu Installation Test cài và mở được, cài `AutoTouchMinh-v1.2.1-Xiaomi-Redmi10-release.apk`.

## B. Cho phép đúng nguồn mở APK

MIUI có thể dùng một trong các đường dẫn sau tùy phiên bản:

- Cài đặt > Cài đặt bổ sung > Quyền riêng tư > Quyền truy cập ứng dụng đặc biệt > Cài ứng dụng không xác định.
- Hoặc tìm “Cài ứng dụng không xác định” trong ô tìm kiếm Cài đặt.

Chọn đúng ứng dụng đang mở APK, ví dụ Chrome hoặc Trình quản lý tệp, rồi bật “Cho phép từ nguồn này”.

## C. Nếu có bản cũ

- Nếu máy đã có `com.minh.autotouch.personal` ký bằng khóa khác, Android sẽ không cho cài đè.
- Sao lưu cấu hình nếu cần, sau đó gỡ bản đó và cài bản mới.
- Bản cũ `com.minh.autotouch` có thể tồn tại song song, nhưng nên gỡ để tránh nhầm biểu tượng/dịch vụ.

## D. Cấp quyền sau khi cài

1. Mở Auto Touch Minh.
2. Nhấn “Cấp quyền nổi” và cho phép hiển thị trên ứng dụng khác.
3. Đọc thông báo trong ứng dụng rồi mở mục Trợ năng.
4. Tự bật dịch vụ Auto Touch Minh; không dùng ADB để ép bật.
5. Android 13 trở lên: nếu Trợ năng bị làm mờ, vào Thông tin ứng dụng > dấu ba chấm > Cho phép cài đặt bị hạn chế, chỉ khi đã kiểm tra SHA-256 và tin cậy APK.

## E. Pin và chạy nền trên MIUI

- Cài đặt > Ứng dụng > Quản lý ứng dụng > Auto Touch Minh > Tiết kiệm pin > Không hạn chế.
- Chỉ bật Tự khởi động khi MIUI thường xuyên dừng bảng điều khiển nổi. Ứng dụng không cần tự khởi động cho lần sử dụng bình thường.
- Không mặc định tắt Tối ưu hóa MIUI. Việc tắt có thể thay đổi quản lý quyền, thông báo và hành vi hệ thống.

## F. Khi Xiaomi Security hoặc Play Protect cảnh báo

- Đọc đúng lý do trên màn hình.
- Không tắt toàn bộ Play Protect hoặc Xiaomi Security.
- Xác nhận tên package, SHA-256 và nguồn APK.
- Chụp toàn bộ màn hình cảnh báo nếu vẫn không tiếp tục được.

## G. Thử chức năng an toàn

1. Chọn ứng dụng thử nghiệm không nhạy cảm.
2. Chọn package trong Auto Touch Minh.
3. Bật một điểm, số vòng nhỏ và thời gian chờ đủ dài.
4. Mở bảng nổi, kéo điểm, chuyển sang ứng dụng thử và chạy.
5. Xác nhận ứng dụng dừng khi chuyển sang ứng dụng khác hoặc khóa màn hình.

**Không dùng trên ngân hàng, CAPTCHA, OTP, thanh toán, mật khẩu hoặc ứng dụng không được phép.**
