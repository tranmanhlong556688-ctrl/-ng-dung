# BÁO CÁO CHẨN ĐOÁN CÀI ĐẶT – AUTO TOUCH MINH

## Kết luận hiện tại

Không có ảnh lỗi từ Xiaomi Redmi 10 hoặc mã lỗi `adb install` trong các tệp hiện truy cập được, vì vậy chưa thể khẳng định một nguyên nhân duy nhất trên điện thoại thật. Báo cáo phân biệt các nhóm lỗi dựa trên APK, mã nguồn và bằng chứng giả lập.

## APK đã kiểm tra

- Gói chính: `com.minh.autotouch.personal`
- Bản nền: versionCode 3 / versionName 1.2.0
- Bản cuối: versionCode 4 / versionName 1.2.1
- minSdk 26; targetSdk 35.
- Bản Installation Test: `com.minh.autotouch.installtest`, không có quyền và không có service.

## Phân loại hiện tượng

### 1. Không mở được file APK
File tải chưa hoàn tất, bị đổi đuôi, mở APK trong ZIP, Package Installer không nhận file hoặc nguồn mở APK chưa được phép cài ứng dụng không xác định. So SHA-256, giải nén trước và mở bằng đúng trình duyệt/trình quản lý tệp đã được cấp quyền.

### 2. Trình cài đặt từ chối cài
Cần lấy đúng thông báo hoặc mã `INSTALL_FAILED_*`.

- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: cùng package nhưng chứng thư ký khác.
- `INSTALL_FAILED_VERSION_DOWNGRADE`: versionCode thấp hơn bản đang cài.
- `INSTALL_FAILED_INVALID_APK` / `PARSE_FAILED_*`: APK hỏng hoặc bị sửa sau khi ký.
- `INSTALL_FAILED_OLDER_SDK`: Android thấp hơn minSdk 26.
- `INSTALL_FAILED_INSUFFICIENT_STORAGE`: thiếu dung lượng.

Gói mới `com.minh.autotouch.personal` tránh xung đột với các bản cũ dùng `com.minh.autotouch`. Bản 1.2.1 được thiết kế để kiểm tra cập nhật từ 1.2.0 bằng cùng chứng thư.

### 3. Play Protect cảnh báo
Đây là cảnh báo đánh giá ứng dụng cài ngoài Google Play, không đồng nghĩa APK hỏng. Không tắt toàn bộ Play Protect; chỉ tiếp tục khi SHA-256 đúng và APK đến từ bộ bàn giao này.

### 4. Xiaomi Security chặn
Kiểm tra nguồn mở APK đã được cấp quyền “Cài ứng dụng không xác định”. Nếu ứng dụng Bảo mật hiển thị lý do cụ thể, chụp toàn bộ màn hình gồm tiêu đề và nút để đối chiếu. Không tắt toàn bộ Xiaomi Security.

### 5. Cài được nhưng không bật được Trợ năng
Đây là giai đoạn sau cài đặt. Trên Android 13 trở lên, ứng dụng cài ngoài có thể cần “Cho phép cài đặt bị hạn chế” trong Thông tin ứng dụng. Android 11/12 không dùng đúng cơ chế Android 13 này, nhưng MIUI vẫn có thể hiển thị cảnh báo riêng.

### 6. Cài được nhưng mở lên bị văng
Cần logcat có `FATAL EXCEPTION`. CI kiểm tra mở 10 giây, PID, đóng/mở lại và lọc `FATAL EXCEPTION`.

## Chữ ký

- APK phải qua `apksigner verify --verbose --print-certs`.
- Bản cuối yêu cầu v2 và v3 hợp lệ.
- V4 tạo file `.idsig` riêng; không có v4 nhúng trong APK không phải lỗi cài đặt.
- Không sửa, giải nén rồi nén lại APK sau khi ký.

## Manifest

Ba quyền của bản đầy đủ không phải điều kiện phần cứng và không làm Redmi 10 mất tương thích cài đặt:

- `SYSTEM_ALERT_WINDOW`: điểm và bảng điều khiển nổi.
- `FOREGROUND_SERVICE`: giữ bảng nổi có thông báo khi người dùng chủ động bật.
- `FOREGROUND_SERVICE_SPECIAL_USE`: loại foreground service cho Android mới.

Dịch vụ Trợ năng dùng `BIND_ACCESSIBILITY_SERVICE`, `canPerformGestures=true`, `canRetrieveWindowContent=false`, chỉ nhận package đang mở và không đọc cây giao diện.

## Dữ liệu cần lấy nếu điện thoại vẫn chặn

1. Ảnh nguyên màn hình lỗi.
2. Phiên bản Android và MIUI.
3. Tên ứng dụng dùng để mở APK.
4. Installation Test có cài được hay không.
5. Bản đầy đủ bị chặn ở cài đặt, quyền nổi hay Trợ năng.
