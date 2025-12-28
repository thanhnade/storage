# Quick Start Guide - Chạy project với Jetty

## ⚠️ Lưu ý về Port

Nếu GlassFish đang chạy trên port **8080**, Jetty sẽ chạy trên port **8081** để tránh conflict.

## Cách 1: Chạy với Maven Command (Khuyến nghị)

### Start Jetty Server:
```bash
mvn jetty:run
```

### Stop Jetty Server:
Nhấn `Ctrl+C` trong terminal

Hoặc mở terminal khác và chạy:
```bash
mvn jetty:stop
```

## Cách 2: Sử dụng VSCode Tasks

1. Mở Command Palette: `Ctrl+Shift+P` (hoặc `Cmd+Shift+P` trên Mac)
2. Chọn: **"Tasks: Run Task"**
3. Chọn: **"Maven: Jetty Run"**

Để stop:
1. Command Palette: `Ctrl+Shift+P`
2. Chọn: **"Tasks: Run Task"**
3. Chọn: **"Maven: Jetty Stop"**

## Truy cập ứng dụng

Sau khi Jetty start thành công, mở trình duyệt:

**📍 URL:** `http://localhost:8081`

> **Lưu ý:** Port đã được set mặc định là 8081 để tránh conflict với GlassFish (8080). 
> Để đổi port, sửa trong `pom.xml` → `jetty-maven-plugin` → `httpConnector` → `port`

## Kiểm tra Jetty đã start

Bạn sẽ thấy output tương tự:
```
[INFO] Started Jetty Server
[INFO] Starting scanner at interval of 10 seconds.
[INFO] Started ServerConnector@xxxxx{HTTP/1.1, (http/1.1)}{0.0.0.0:8081}
[INFO] Started o.e.j.m.p.JettyWebAppContext@xxxxx{/,file:///...},/storage
[INFO] Started @xxxxxms
```

## Troubleshooting

### Port đã được sử dụng
- Kiểm tra port: `netstat -ano | findstr :8081` (Windows) hoặc `lsof -i :8081` (Mac/Linux)
- Dừng process Jetty cũ: `mvn jetty:stop` hoặc `Get-Process java | Stop-Process -Force`
- Hoặc đổi port trong `pom.xml` (jetty plugin configuration)

### Webapp directory not found
- Đảm bảo cấu hình `webAppSourceDirectory` trong `pom.xml` trỏ đúng đến folder `web/`
- File `pom.xml` đã được config đúng: `<webAppSourceDirectory>${basedir}/web</webAppSourceDirectory>`

### Jetty không start
- Kiểm tra Java version: `java -version` (cần Java 18+)
- Kiểm tra Maven: `mvn -version`
- Clean và rebuild: `mvn clean compile`
- Xem logs trong terminal để biết lỗi cụ thể

### Ứng dụng không load
- Đảm bảo MySQL đang chạy
- Kiểm tra database `storage` đã được tạo và import `storage.sql`
- Kiểm tra file `src/java/DataBase/JDBC.java` có đúng thông tin kết nối

## Auto-reload

Jetty sẽ tự động reload khi bạn thay đổi code (scanIntervalSeconds: 10 giây).

Không cần restart server mỗi lần edit code!

## Cấu hình đã được fix

✅ **Webapp directory:** Đã config đúng `web/` folder (thay vì `src/main/webapp`)  
✅ **Stop port:** Đã đổi sang 9998 (tránh conflict)  
✅ **HTTP port:** 8081 (tránh conflict với GlassFish)  
✅ **Web.xml:** Đã config đúng path `${basedir}/web/WEB-INF/web.xml`

