# BÁO CÁO KIỂM THỬ GIẢ LẬP – AUTO TOUCH MINH v1.2.1

## Phạm vi

- Nền tảng: Android Emulator Android 11 / API 30 / x86_64 / Google APIs.
- Tăng tốc: Linux KVM.
- Không ép bật Accessibility bằng ADB.
- Không vượt quyền cửa sổ nổi bằng ADB.
- Không tuyên bố đã kiểm thử trên Xiaomi Redmi 10 thật.

## APK

### Installation Test

- Package: `com.minh.autotouch.installtest`
- versionCode: 1
- versionName: 1.0.0
- Không quyền đặc biệt, không Accessibility Service, không Overlay Service, không Foreground Service.

### Bản đầy đủ

- Package: `com.minh.autotouch.personal`
- Bản nền: versionCode 3 / versionName 1.2.0.
- Bản cuối: versionCode 4 / versionName 1.2.1.
- minSdk 26; targetSdk 35.

## Kết quả

```text
ANDROID_11_API_30_BOOT_OK
INSTALLATION_TEST_OK
BASELINE_INSTALL_OK
SAME_SIGNATURE_UPDATE_OK
FINAL_LAUNCH_10S_OK
FINAL_RELAUNCH_OK
FINAL_UNINSTALL_REINSTALL_OK
```

Các kiểm tra đã đạt:

1. Cài Installation Test bằng `adb install`.
2. Mở MainActivity và giữ tiến trình tối thiểu 10 giây.
3. Không có `FATAL EXCEPTION` trong logcat của Installation Test.
4. Gỡ Installation Test thành công.
5. Cài bản đầy đủ 1.2.0.
6. Cập nhật tại chỗ lên 1.2.1 bằng `adb install -r`, cùng package và cùng chứng thư.
7. Xác nhận package báo versionName 1.2.1.
8. Mở MainActivity, giữ tiến trình tối thiểu 10 giây và chụp màn hình.
9. Không có `FATAL EXCEPTION` trong logcat của bản đầy đủ.
10. Force-stop rồi mở lại thành công.
11. Gỡ và cài lại bản cuối thành công.
12. Xác nhận package path tồn tại sau khi cài lại.

## Chữ ký bản cuối

- APK Signature Scheme v1: false.
- APK Signature Scheme v2: true.
- APK Signature Scheme v3: true.
- APK Signature Scheme v4 trong APK: false.
- Thuật toán khóa: RSA 3072 bit.
- Certificate SHA-256: `d8e8b3bc6b2ce267913a49cbdcf41611175007db78e6906f9232aaf88ea69b5b`.

## SHA-256 APK

- Installation Test: `d5f0fd6a399a43bb44c36de424f4144b5280bd381f6a5dfe4b7e8d922203580d`
- Baseline 1.2.0: `1129934744c09266b1912d010c8c710b582148ff9ad581450f89bd3825537708`
- Final 1.2.1: `8788f2144e614f4a92420229b864f63e53063899ce9f8031b9d0036665e46837`

## Giới hạn kết luận

Kết quả chứng minh APK có thể cài, cập nhật và mở trên Android 11 chuẩn. Nó không chứng minh Xiaomi Security hoặc cấu hình MIUI cụ thể trên điện thoại thật sẽ không cảnh báo. Muốn xác định chính xác lỗi trên Redmi 10, cần ảnh nguyên màn hình thông báo hoặc mã `INSTALL_FAILED_*` của chính thiết bị đó.
