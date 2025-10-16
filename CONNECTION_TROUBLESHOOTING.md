# 🔧 Hướng dẫn sửa lỗi kết nối IRC

## ✅ **Đã sửa các lỗi kết nối phổ biến:**

### 🚨 **Vấn đề: Connect/Disconnect liên tục**

**Nguyên nhân chính:**
1. **Nickname bị trùng** - Server từ chối kết nối
2. **Internet không ổn định** - Mất kết nối liên tục
3. **Server quá tải** - Server từ chối kết nối mới
4. **Firewall/Proxy** - Chặn kết nối IRC

### 🔧 **Các cải tiến đã thực hiện:**

#### 1. **Unique Nickname Generation**
- Tự động tạo nickname duy nhất (thêm số ngẫu nhiên)
- Làm sạch nickname (loại bỏ ký tự không hợp lệ)
- Giới hạn độ dài nickname (tối đa 15 ký tự)

#### 2. **Internet Connection Check**
- Kiểm tra kết nối internet trước khi kết nối IRC
- Hiển thị thông báo rõ ràng nếu không có internet

#### 3. **Improved Reconnection Logic**
- Thêm delay 3 giây trước khi reconnect
- Giới hạn số lần thử kết nối (tránh loop vô tận)
- Exponential backoff với giới hạn tối đa

#### 4. **Better Error Handling**
- Hiển thị lỗi chi tiết hơn
- Xử lý các trường hợp lỗi khác nhau
- Thông báo rõ ràng cho người dùng

### 🎮 **Commands mới để debug:**

- `/status` - Kiểm tra trạng thái kết nối
- `/reconnect` - Reset và kết nối lại
- `/who` - Xem thông tin của bạn
- `/help` - Xem tất cả commands

### 🚀 **Cách sử dụng khi gặp lỗi:**

#### **Bước 1: Kiểm tra kết nối**
```
Gõ: /status
```
- Nếu thấy "✅ Connected" → Kết nối OK
- Nếu thấy "❌ Not connected" → Có vấn đề

#### **Bước 2: Reset kết nối**
```
Gõ: /reconnect
```
- Sẽ reset toàn bộ kết nối
- Thử kết nối lại từ đầu

#### **Bước 3: Kiểm tra internet**
- Đảm bảo có kết nối internet
- Thử mở website khác
- Kiểm tra WiFi/mobile data

#### **Bước 4: Thử server khác**
- App sẽ tự động thử server khác nếu lỗi
- Các server: Libera, OFTC, Rizon, Freenode

### 🔍 **Debug Information:**

#### **Thông báo lỗi thường gặp:**
- `"No internet connection"` → Kiểm tra mạng
- `"Nickname already in use"` → App sẽ tự tạo nickname mới
- `"Connection refused"` → Server từ chối, app sẽ thử server khác
- `"Max reconnection attempts reached"` → Dừng thử kết nối

#### **Thông báo thành công:**
- `"Connected (TLS irc.libera.chat:6697)"` → Kết nối thành công
- `"Joining #channel..."` → Đang vào kênh
- `"You can now chat with other users online!"` → Sẵn sàng chat

### 🎯 **Tips để tránh lỗi:**

1. **Sử dụng nickname đơn giản** - Tránh ký tự đặc biệt
2. **Kết nối internet ổn định** - WiFi tốt hơn mobile data
3. **Thử vào kênh ít người** - #general có thể đông
4. **Kiên nhẫn** - Kết nối IRC có thể mất vài giây
5. **Sử dụng /reconnect** - Nếu gặp vấn đề

### 🆘 **Nếu vẫn lỗi:**

1. **Restart app** - Đóng và mở lại app
2. **Clear app data** - Xóa cache app
3. **Check firewall** - Tắt firewall tạm thời
4. **Try different network** - Thử WiFi khác
5. **Contact support** - Báo cáo lỗi chi tiết

### 📱 **Test kết nối:**

1. Mở app → Login
2. Chọn server `irc.libera.chat`
3. Vào kênh `#general`
4. Gõ `/status` để kiểm tra
5. Nếu OK, gõ "Hello!" để test chat

**Chúc bạn kết nối thành công! 🎉**

