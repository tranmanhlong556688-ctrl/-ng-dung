# Điều khiển điều hòa LG bằng IR — Redmi 10

Ứng dụng Android Kotlin ngoại tuyến dùng `ConsumerIrManager` để phát tín hiệu hồng ngoại 38 kHz từ Xiaomi Redmi 10 và dò các biến thể phổ biến của điều hòa LG.

## Chức năng

- Kiểm tra IR Blaster trên điện thoại.
- Dò 15 mã bật thử gồm LG, LG2 và các timing thay thế.
- Lưu cấu hình hoạt động bằng `SharedPreferences`.
- Bật/tắt, chỉnh 16–30°C, đổi chế độ, tốc độ quạt, đảo gió và gửi lại trạng thái.
- Không tài khoản, không quảng cáo, không Internet.

## Cách dò mã

1. Cài APK, mở ứng dụng và hướng đầu trên điện thoại về mắt nhận IR của điều hòa.
2. Nhấn **PHÁT MÃ THỬ**.
3. Nếu điều hòa bật hoặc kêu bíp, nhấn **MÃ NÀY HOẠT ĐỘNG – LƯU**.
4. Nếu không phản hồi, nhấn **Mã tiếp** và thử lại.

## Tải APK từ GitHub Actions

1. Mở tab **Actions** của repository.
2. Chọn workflow **Build LG IR Remote APK**.
3. Mở lần chạy mới nhất.
4. Tải artifact `LG-IR-Remote-debug`.
5. Giải nén và cài `app-debug.apk` trên Redmi 10.

## Cấu hình build

- JDK 17
- Android Gradle Plugin 8.13.2
- Gradle 8.13
- Kotlin 2.3.21
- compileSdk/targetSdk 35
- minSdk 23

Điều hòa LG thường truyền toàn bộ trạng thái trong khung 28-bit. Một số máy cửa sổ hoặc di động có thể dùng giao thức khác; bản v1.0 tập trung vào các dòng treo tường dùng LG/LG2.
