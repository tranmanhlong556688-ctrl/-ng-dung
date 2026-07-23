# Auto Kiểm Tra Bình – Minh v1.0

Ứng dụng Android nội bộ dùng `AccessibilityService` để tự động thao tác hai điểm P1/P2 trên ứng dụng kiểm tra bình chữa cháy.

## Thiết bị mục tiêu

- CAT S42
- Android 12
- Không cần root
- Không cần ADB sau khi cài đặt
- Không yêu cầu Internet

## Trình tự mỗi vòng

1. Chạm P1 lần thứ nhất.
2. Chờ 150 ms.
3. Chạm P1 lần thứ hai.
4. Chờ 300 ms.
5. Chạm P2 một lần.
6. Chờ 400 ms rồi bắt đầu vòng tiếp theo.

Mọi khoảng thời gian đều chỉnh được trên bảng điều khiển nổi.

## Chức năng chính

- Hai điểm P1/P2 kéo thả trực tiếp trên màn hình.
- Hiển thị tọa độ X/Y.
- Nút thử riêng P1 và P2.
- Nhập số vòng hoặc chạy không giới hạn.
- Đếm ngược trước khi chạy.
- Bắt đầu, tạm dừng, tiếp tục và dừng khẩn cấp.
- Thu nhỏ bảng điều khiển thành nút nổi.
- Tự lưu tọa độ, thời gian, số vòng và vị trí bảng nổi.
- Tự tạm dừng khi đổi khỏi ứng dụng mục tiêu hoặc tắt màn hình.
- Dừng khi xoay màn hình và yêu cầu hiệu chỉnh lại.
- Có điều khiển dừng/tạm dừng trong thông báo hệ thống.

## Cài đặt lần đầu trên CAT S42 Android 12

1. Cài file `AutoKiemTraBinh_Minh_v1.0.apk`.
2. Mở ứng dụng.
3. Bấm **MỞ CÀI ĐẶT TRỢ NĂNG**.
4. Chọn **Auto Kiểm Tra Bình – Minh** và bật dịch vụ.
5. Quay lại, bấm **HIỆN BẢNG ĐIỀU KHIỂN NỔI**.
6. Mở ứng dụng kiểm tra bình.
7. Bấm **THIẾT LẬP**, kéo P1 vào giữa nút **TẤT CẢ BÌNH THƯỜNG**.
8. Kéo P2 vào giữa nút **LƯU KẾT QUẢ TUẦN TRA**.
9. Bấm **KHÓA P1/P2**.
10. Dùng **THỬ P1** và **THỬ P2** để kiểm tra.
11. Nhập số vòng rồi bấm **BẮT ĐẦU**.

## Build bằng GitHub Actions

Workflow nằm tại:

`.github/workflows/build-apk.yml`

Workflow tự chạy khi push lên nhánh `auto-kiem-tra-binh-minh`, hoặc chạy thủ công bằng **Run workflow**.

APK xuất ra trong phần **Actions → Artifacts** với tên:

`AutoKiemTraBinh_Minh_v1.0_APK`

## Build thủ công

Yêu cầu:

- JDK 17
- Android SDK Platform 35
- Gradle 8.7

Lệnh:

```bash
gradle --no-daemon clean :app:assembleRelease
```

APK nằm tại:

`app/build/outputs/apk/release/app-release.apk`

## Chữ ký APK

Bản v1.0 dùng khóa ký `app/minh-release.jks` để các bản sau có thể cài cập nhật đè lên bản trước. Không thay hoặc xóa khóa này khi tạo phiên bản mới.

## Quyền riêng tư

- Không có quyền Internet.
- Không quảng cáo.
- Không gửi IMEI, MAC, tọa độ hoặc dữ liệu màn hình ra ngoài.
- Chỉ dùng quyền Trợ năng để hiển thị bảng điều khiển và gửi thao tác chạm do người dùng cấu hình.

## Lịch sử phiên bản

### v1.0.0

- Tạo bảng điều khiển nổi.
- Tạo hai điểm kéo thả P1/P2.
- Thêm chu trình P1 → P1 → P2.
- Thêm số vòng, thời gian tùy chỉnh, tạm dừng và dừng khẩn cấp.
- Thêm GitHub Actions build APK tự động.
