# Auto Touch Minh v1.2.0 — bản cài đặt đã kiểm thử

Ứng dụng Android cá nhân, không root, thực hiện chuỗi nhấp/nhấn giữ/vuốt cố định do người dùng tự cấu hình.

## Quyền bắt buộc

- **Accessibility Service:** bắt buộc để Android cho phép phát cử chỉ cảm ứng. Bỏ quyền này thì ứng dụng không thể tự nhấp hoặc vuốt.
- **Hiển thị trên ứng dụng khác:** bắt buộc cho các điểm và bảng điều khiển nổi. Có thể bỏ trong một phiên bản khác nếu chấp nhận nhập tọa độ trong màn hình ứng dụng.
- **Foreground Service:** duy trì bảng điều khiển nổi và thông báo đang chạy.

## Những chức năng nhạy cảm đã loại bỏ

- Không bắt phím âm lượng.
- Không có Home, Recent Apps hoặc mở bảng thông báo.
- Không có quyền Internet, rung, đọc bộ nhớ hay đọc nội dung cửa sổ.
- Không chạy vô hạn; số vòng và tốc độ được giới hạn.
- Tự dừng khi rời ứng dụng đã được người dùng cho phép.

## Khắc phục lỗi cài đặt

Phiên bản 1.2.0 dùng applicationId mới `com.minh.autotouch.personal`, vì vậy không bị xung đột chữ ký với APK v1.0/v1.1 đã build trên máy chủ khác. APK release được ký bằng khóa thử nghiệm riêng và được kiểm tra bằng `apksigner` trước khi phát hành.

GitHub Actions cài APK thật lên Android Emulator API 33 và API 35, mở MainActivity, bật thử Accessibility Service, cấp quyền overlay và khởi động OverlayService. Chỉ khi cả hai máy giả lập vượt qua kiểm tra, APK mới được bàn giao.
