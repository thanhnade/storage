# Hướng dẫn chạy project trên VSCode/Cursor

Project này đã được cấu hình để chạy trên VSCode/Cursor với Maven và GlassFish/Jetty server.

## Yêu cầu hệ thống

1. **Java JDK 18** (hoặc Java 17+)
2. **Maven 3.6+**
3. **VSCode/Cursor** với các extensions cần thiết
4. **MySQL/MariaDB** (đã cài đặt và cấu hình database `storage`)
5. **GlassFish Server** (tùy chọn, nếu muốn dùng GlassFish thay vì Jetty)

## Cài đặt Extensions cho VSCode/Cursor

Mở VSCode/Cursor và cài đặt các extensions sau:

### Bắt buộc:
1. **Extension Pack for Java** (`vscjava.vscode-java-pack`)
   - Bao gồm: Java Language Support, Debugger, Test Runner, Maven support

### Tùy chọn nhưng khuyến nghị:
- **XML** (`redhat.vscode-xml`) - Hỗ trợ JSP và XML files
- **Java Dependency Viewer** (`vscjava.vscode-java-dependency`)

**Cách cài đặt:**
- Mở Command Palette (Ctrl+Shift+P / Cmd+Shift+P)
- Gõ: `Extensions: Show Recommended Extensions`
- Click "Install All" hoặc cài từng extension

Hoặc mở file `.vscode/extensions.json` và click "Install All" khi VSCode nhắc.

## Cấu hình Database

1. Đảm bảo MySQL/MariaDB đang chạy
2. Tạo database `storage`:
   ```sql
   CREATE DATABASE storage CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Import file `storage.sql`:
   ```bash
   mysql -u root -p storage < storage.sql
   ```
4. Kiểm tra file `src/java/DataBase/JDBC.java` và cập nhật thông tin kết nối nếu cần:
   - DB_URL: `jdbc:mysql://localhost:3306/storage`
   - DB_USER: `root`
   - DB_PASSWORD: (mật khẩu của bạn)

## Cách chạy project

### Cách 1: Sử dụng Maven với Jetty (Khuyến nghị - nhanh nhất)

Jetty là embedded server, dễ chạy và debug:

```bash
# Build project
mvn clean compile

# Chạy với Jetty (tự động reload khi code thay đổi)
mvn jetty:run
```

Sau đó mở trình duyệt: `http://localhost:8080`

**Dừng server:** Nhấn `Ctrl+C` trong terminal

### Cách 2: Build WAR và deploy lên GlassFish

1. **Build WAR file:**
   ```bash
   mvn clean package
   ```
   File WAR sẽ được tạo tại: `target/Storage.war`

2. **Deploy lên GlassFish:**
   
   **Option A: Sử dụng GlassFish Admin Console**
   - Mở GlassFish Admin Console: `http://localhost:4848`
   - Applications > Deploy > Chọn file `target/Storage.war`
   
   **Option B: Sử dụng asadmin command line**
   ```bash
   asadmin deploy target/Storage.war
   ```
   
   **Option C: Copy WAR vào autodeploy folder**
   ```bash
   copy target\Storage.war "C:\glassfish7\glassfish\domains\domain1\autodeploy\"
   ```

3. Truy cập: `http://localhost:8080/Storage/`

### Cách 3: Sử dụng Maven GlassFish Plugin

Cấu hình trong `pom.xml` (đã có sẵn, cần chỉnh sửa properties):

```bash
# Set environment variables hoặc thêm vào pom.xml
export GLASSFISH_HOME=/path/to/glassfish7
# hoặc trên Windows:
set GLASSFISH_HOME=C:\glassfish7

# Deploy
mvn clean package glassfish:deploy
```

## Debug trên VSCode/Cursor

### Debug với Jetty:

1. Mở file Java cần debug
2. Đặt breakpoint
3. Mở Terminal và chạy:
   ```bash
   mvn jetty:run -Dmaven.jetty.debug=true -Dmaven.jetty.debugPort=5005
   ```
4. Trong VSCode, chọn "Debug Storage App" từ Debug panel
5. Click nút "Start Debugging" (F5)

### Debug với GlassFish:

1. Cấu hình GlassFish để enable debug:
   ```bash
   asadmin create-jvm-options "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
   asadmin restart-domain domain1
   ```
2. Deploy ứng dụng
3. Trong VSCode, chọn "Debug Storage App" và click "Start Debugging"

## Cấu trúc project sau khi chuyển sang Maven

```
storage/
├── pom.xml                 # Maven configuration (MỚI)
├── src/
│   └── java/              # Source code Java
│       ├── Controller/
│       ├── Model/
│       └── DataBase/
├── web/                   # Web resources (JSP, CSS, JS)
│   ├── WEB-INF/
│   └── ...
├── target/                # Build output (Maven tạo ra)
│   └── Storage.war
├── .vscode/               # VSCode settings (MỚI)
│   ├── settings.json
│   ├── launch.json
│   └── extensions.json
├── lib/                   # Local dependencies (có thể bỏ sau khi chuyển Maven)
├── build.xml              # Ant build (vẫn giữ để tương thích NetBeans)
└── nbproject/             # NetBeans config (vẫn giữ)
```

## Troubleshooting

### Lỗi: "Cannot resolve symbol"
- Đảm bảo đã cài Java Extension Pack
- Reload window: `Ctrl+Shift+P` > "Developer: Reload Window"
- Clean và rebuild: `mvn clean compile`

### Lỗi: "Port 8080 already in use"
- Đổi port trong `pom.xml` (Jetty plugin) hoặc tắt ứng dụng đang dùng port 8080
- Kiểm tra: `netstat -ano | findstr :8080` (Windows) hoặc `lsof -i :8080` (Mac/Linux)

### Lỗi: "ClassNotFoundException"
- Đảm bảo dependencies đã được download: `mvn clean install`
- Kiểm tra `pom.xml` đã đúng dependencies

### Database connection error
- Kiểm tra MySQL đang chạy
- Kiểm tra thông tin trong `JDBC.java`
- Test connection: `mysql -u root -p storage`

### JSP không compile
- Đảm bảo web server đã được config để compile JSP
- Với Jetty, cần thêm JSP support (đã có trong pom.xml)
- Nếu vẫn lỗi, kiểm tra `web.xml` configuration

## So sánh với NetBeans

| Tính năng | NetBeans | VSCode/Cursor |
|-----------|----------|---------------|
| Build Tool | Ant | Maven |
| Server | GlassFish (tích hợp) | GlassFish/Jetty (tùy chọn) |
| Auto-deploy | Có | Có (với Jetty) |
| Debug | Có | Có |
| Auto-reload | Có | Có (với Jetty) |
| Code completion | Tốt | Tốt (với Java Extension) |

## Lưu ý

- Project vẫn tương thích với NetBeans (giữ nguyên `build.xml` và `nbproject/`)
- Có thể chạy song song cả Maven và Ant
- File `.vscode/` đã được ignore trong `.gitignore` (local settings)
- Khuyến nghị dùng Jetty cho development vì nhanh và dễ debug
- Dùng GlassFish cho production (giống NetBeans setup)

## Hỗ trợ

Nếu gặp vấn đề, kiểm tra:
1. Java version: `java -version` (cần Java 18)
2. Maven version: `mvn -version` (cần Maven 3.6+)
3. Extension đã cài đầy đủ chưa
4. Logs trong terminal khi chạy `mvn jetty:run`

