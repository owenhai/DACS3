# Script kết nối LDPlayer với IntelliJ qua ADB
# Tác giả: GitHub Copilot
# Ngày: 2026

$sdkPath = "C:\Users\Lenovo\AppData\Local\Android\Sdk"
$adbPath = "$sdkPath\platform-tools\adb.exe"
$ldplayerPort = "5555"

# Kiểm tra adb.exe tồn tại
if (-not (Test-Path $adbPath)) {
    Write-Host "❌ Lỗi: Không tìm thấy adb.exe tại $adbPath" -ForegroundColor Red
    Write-Host "Hãy kiểm tra SDK Location trong IntelliJ" -ForegroundColor Yellow
    exit 1
}

Write-Host "✓ Tìm thấy adb.exe" -ForegroundColor Green

# Kill adb server cũ
Write-Host "`n🔄 Dừng adb server cũ..." -ForegroundColor Cyan
& $adbPath kill-server | Out-Null
Start-Sleep -Seconds 1

# Start adb server mới
Write-Host "🔄 Khởi động adb server mới..." -ForegroundColor Cyan
& $adbPath start-server

# Kết nối LDPlayer
Write-Host "`n📱 Kết nối LDPlayer tại 127.0.0.1:$ldplayerPort..." -ForegroundColor Cyan
& $adbPath connect "127.0.0.1:$ldplayerPort"

Start-Sleep -Seconds 2

# Kiểm tra kết nối
Write-Host "`n📋 Danh sách thiết bị đang kết nối:" -ForegroundColor Cyan
$devices = & $adbPath devices
Write-Host $devices -ForegroundColor White

# Kiểm tra xem LDPlayer có online không
if ($devices -like "*127.0.0.1:$ldplayerPort*device*") {
    Write-Host "`n✅ Thành công! LDPlayer đã kết nối" -ForegroundColor Green
    Write-Host "`n📝 Bước tiếp theo:" -ForegroundColor Yellow
    Write-Host "1. Mở project DACS3 trong IntelliJ" -ForegroundColor White
    Write-Host "2. Bấm nút Run (▶) hoặc Shift + F10" -ForegroundColor White
    Write-Host "3. Chọn LDPlayer từ danh sách device" -ForegroundColor White
    Write-Host "4. App sẽ chạy trên LDPlayer" -ForegroundColor White
} else {
    Write-Host "`n⚠️  Cảnh báo: Không thấy LDPlayer online" -ForegroundColor Yellow
    Write-Host "Kiểm tra:" -ForegroundColor Yellow
    Write-Host "1. LDPlayer đã chạy chưa?" -ForegroundColor White
    Write-Host "2. Trong LDPlayer Settings → ADB debugging, chọn 'Enable local connection'" -ForegroundColor White
    Write-Host "3. Thử kết nối lại hoặc thử cổng 5557, 5559" -ForegroundColor White
}

