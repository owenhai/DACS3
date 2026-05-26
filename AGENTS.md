# Agent Guide: DACS3 Android Movie App

This project is an Android application for movie browsing and ticket booking, powered by Firebase.

## Big Picture Architecture
- **Type**: Native Android (Kotlin)
- **UI Architecture**: Activity-based with View Binding. Follows a layered structure: `activity/`, `adapter/`, `model/`.
- **Backend/Persistence**: 
  - **Firebase Auth**: User authentication.
  - **Firebase Realtime Database**: Stores movie data (`Items`, `Upcoming`, `Banners`) and user profiles.
  - **SharedPreferences (`UserPrefs`)**: Stores session details (`customUserId`, `firebaseUid`, `username`).

## Critical Knowledge
- **User Identification**: The app uses a dual ID system. `firebaseUid` for Auth and a generated `customUserId` (e.g., `MV1`, `MV2`) for database paths. Use `customUserId` when accessing user-specific data in RTDB.
- **Database Paths**:
  - `Users/{customUserId}/`: User profile and favorites.
  - `Items/`: Standard movie list (referred to as "Top Movies").
  - `Upcoming/` (or `Upcomming`): Movies in the upcoming section. Note the legacy typo fallback.
  - `Banners/`: Data for the home screen slider.
- **Session Management**: `LoginActivity` performs a "migration" check to ensure every Firebase Auth user has a corresponding `customUserId` in the database.

## Key Developer Workflows
- **Running**: Standard Android Studio Run command.
- **Firebase**: Requires `google-services.json` (present in `app/`).
- **Debugging**: Check `Log.d("LoginActivity", ...)` for auth/migration flow issues.

## Conventions & Patterns
- **View Binding**: Avoid `findViewById`. Use `binding.viewId` (e.g., `ActivityMainBinding.inflate(layoutInflater)`).
- **Navigation**: Uses `com.github.ismaeldivita:chip-navigation-bar` for the bottom menu.
- **Images**: Glide is preferred for all image loading.
- **Edge-to-Edge**: Activities should call `enableEdgeToEdge()` in `onCreate`.

## Important Files
- `MainActivity.kt`: Core logic for home screen and movie list initialization.
- `LoginActivity.kt`: Crucial for understanding user session and migration logic.
- `DetailFilmActivity.kt`: Handles film details and favorites logic.
- `Film.kt`: The main movie data model.
- `app/build.gradle.kts`: Source of truth for dependencies.

## Feature
1:
1. Quản lý Phim
data class Movie(val id: String, var title: String, var duration: Int, var genre: String)

2. Quản lý Lịch chiếu (Day / Time Slot)
data class ShowDate(val id: String, var date: String, var timeSlots: List<String>)

3. Quản lý Ghế & Giá tiền (Cấu hình theo loại ghế)
data class SeatConfig(val row: String, val type: String, var price: Double)

4. Thực thể Vé (Để Admin quét và đối chiếu)
data class Ticket(
    val ticketId: String,
    val movieTitle: String,
    val showDate: String,
    val seatNo: String,
    val totalPrice: Double,
    var isCheckedIn: Boolean = false // Trạng thái soát vé
)
5. Quy trình đặt vé:
- User clicks "Buy Tickets": Frontend sends ticket information (ticket type, quantity, total price, movie details) to Backend.
- Backend creates Bill: Saves the invoice to the database with the status "Pending" (Waiting for payment) -> Returns the bill_id and transfers payment information.
- Frontend shows Bill & QR: Receives data from Backend, displays the authentication screen along with a QR code (using the VietQR API or a QR code generation library).
- User completes payment: After the user completes the payment, the Backend updates the bill status to "Paid" and generates a unique QR code for ticket verification.
- User shows QR code: The user can show the QR code at the cinema for ticket verification.
- Cinema staff verifies QR code: The cinema staff scans the QR code using a verification app, which checks the validity of the ticket against the database and updates the ticket status to "Used" after
6. Chỗ Ngồi 
- Khi đặt vé, người dùng có thể chọn chỗ ngồi cụ thể. Hệ thống sẽ cập nhật trạng thái chỗ ngồi đó là "Đã đặt" để tránh việc người khác đặt cùng chỗ.
- Khi người dùng hủy vé, hệ thống sẽ cập nhật trạng thái chỗ ngồi đó là "Trống" để cho phép người khác đặt.

Từ số 7 tạo 1 cái admin dashboard để quản lý các chức năng trên:
7. Quản lý Lịch chiếu
- Admin có thể thêm, sửa, xóa lịch chiếu cho từng phim.
- Lịch chiếu bao gồm ngày, giờ và số lượng vé có sẵn.
8. Quản lý Phim
- Admin có thể thêm, sửa, xóa thông tin phim như tiêu đề, thời lượng, thể loại, mô tả, hình ảnh, trailer, v.v.
9. Quản lý Vé
   - Admin có thể xem danh sách vé đã bán, trạng thái vé (Đã bán, Đã sử dụng, Hủy), và có thể hủy vé nếu cần thiết.
   - Admin có thể quét QR code để xác thực vé tại rạp, cập nhật trạng thái vé thành "Đã sử dụng" sau khi xác thực thành công.
   - Admin có thể tạo báo cáo doanh thu dựa trên số lượng vé bán ra, loại vé, và thời gian bán vé để phân tích hiệu suất kinh doanh.
10. Dữ Liệu Phim
- Admin có thể xem thống kê về số lượng vé bán ra cho từng phim, doanh thu từ mỗi phim, và đánh giá của người dùng để đưa ra quyết định về việc tiếp tục chiếu phim đó hay không.
- Thêm data giờ chiếu vào cơ sở dữ liệu để quản lý lịch chiếu chính xác hơn , ví dụ bắt đầu 2h chiếu thì giờ kết thúc sẽ là 5p delay + thời lượng của phim( phim 120p thì kết thúc sẽ là 2h + 120p + 5p delay = 4h5p) để tránh việc trùng lịch chiếu giữa các phim.
11. Fix lại QR cho quét được 
- Sử dụng thư viện QR code phổ biến để tạo và quét QR code.
- Đảm bảo rằng QR code chứa đủ thông tin cần thiết để xác thực vé, bao gồm `ticketId`, `movieTitle`, `showDate`, và `seatNo`.
- Khi quét QR code, hệ thống sẽ kiểm tra thông tin trong QR code với cơ sở dữ liệu để xác thực vé và cập nhật trạng thái vé thành "Đã sử dụng" sau khi xác thực thành công.
- khi quét QR in ra thông tin trong vé phim cho khách hàng xem như tên phim, ngày chiếu, giờ chiếu, số ghế, giá tiền để khách hàng có thể kiểm tra lại thông tin vé của mình trước khi xác nhận sử dụng vé.
12. Tính năng bổ sung:
- Khi xác nhận vé xong thì có chức năng print vé ra giấy hoặc gửi vé qua email cho khách hàng để tiện lợi hơn trong việc quản lý vé và tránh mất vé.