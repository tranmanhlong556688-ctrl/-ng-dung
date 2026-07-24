# DANH SÁCH CHỨC NĂNG VÀ QUYỀN

| Chức năng | Khai báo/cơ chế | Mức cần thiết | Đánh giá |
|---|---|---|---|
| Mở giao diện cấu hình | MainActivity, exported=true với LAUNCHER | Bắt buộc | Chỉ Activity khởi chạy được xuất ra ngoài. |
| Điểm nổi kéo thả | `SYSTEM_ALERT_WINDOW`, `TYPE_APPLICATION_OVERLAY` | Bắt buộc cho thiết kế hiện tại | Quyền đặc biệt, người dùng cấp thủ công. Có thể thay bằng nhập tọa độ nhưng mất kéo điểm. |
| Bảng điều khiển nổi | OverlayService, exported=false | Bắt buộc | Không ứng dụng khác gọi trực tiếp được. |
| Duy trì bảng nổi | `FOREGROUND_SERVICE` | Bắt buộc khi bảng nổi hoạt động lâu | Có thông báo foreground; khởi động từ thao tác người dùng. |
| Loại foreground service | `FOREGROUND_SERVICE_SPECIAL_USE`, `foregroundServiceType=specialUse` | Bắt buộc cho targetSdk 35 với use case này | Có mô tả subtype trong Manifest. |
| Nhấp/nhấn giữ/vuốt | Accessibility Service + `canPerformGestures=true` | Bắt buộc | Android chỉ cho `dispatchGesture()` khi khai báo khả năng này. |
| Ngăn chạy sai ứng dụng | `typeWindowStateChanged|typeWindowsChanged` | Bắt buộc cho giới hạn an toàn | Chỉ đọc `event.packageName`. |
| Đọc nội dung màn hình | `canRetrieveWindowContent=false` | Đã loại bỏ | Không truy cập cây node, chữ, mật khẩu hoặc nội dung cửa sổ. |
| Ràng buộc Accessibility | `BIND_ACCESSIBILITY_SERVICE` | Bắt buộc | Quyền cấp ở cấp hệ thống, không phải runtime permission thông thường. |
| Internet | Không khai báo | Loại bỏ | Không gửi dữ liệu. |
| Bộ nhớ dùng chung | Không khai báo quyền đọc/ghi bộ nhớ | Không cần | Xuất/nhập qua trình chọn tài liệu hệ thống. |
| Khóa màn hình/Home/Recent/Thông báo | Không sử dụng | Loại bỏ | Giảm rủi ro và cảnh báo. |

## `android:exported`

- MainActivity: `true`, vì có intent-filter LAUNCHER.
- OverlayService: `false`, đúng vì chỉ ứng dụng tự gọi.
- AccessibilityService: `true` cùng `BIND_ACCESSIBILITY_SERVICE`, để hệ thống Android bind; ứng dụng thường không thể bind nếu không có quyền hệ thống.

## Nguy cơ cảnh báo

- Accessibility Service: cảnh báo cao vì có thể tương tác thay người dùng; ứng dụng công khai rõ phạm vi và không đọc nội dung.
- Overlay: cảnh báo trung bình vì hiển thị trên ứng dụng khác.
- Foreground service: cảnh báo thấp/trung bình; người dùng thấy thông báo đang chạy.
- Không có quyền Internet, SMS, danh bạ, microphone, camera, vị trí, bộ nhớ hoặc quản trị thiết bị.
