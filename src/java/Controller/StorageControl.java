package Controller;

import DataBase.JDBC;
import Model.InforUser;
import Model.NguoiDung;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;

@WebServlet(name = "StorageControl", urlPatterns = {"/StorageControl"})
public class StorageControl extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(StorageControl.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");
        List<InforUser> users = (List<InforUser>) session.getAttribute("users");

        if (session == null || nguoiDung == null) {
            request.setAttribute("error", "Vui lòng đăng nhập!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        if (users == null || users.isEmpty()) {
            request.setAttribute("error", "Vui lòng kết nối đến máy chủ trước!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        // Kiểm tra quyền admin
        if (!"admin".equals(nguoiDung.getRole())) {
            request.setAttribute("error", "Bạn không có quyền truy cập trang này!");
            request.getRequestDispatcher("trangchu.jsp").forward(request, response);
            return;
        }

        String action = request.getParameter("action");

        if (action != null && action.equals("refreshStats")) {
            // Refresh stats and redirect back
            calculateAndSetStorageStats(request, response);
            response.sendRedirect("main-storage.jsp");
            return;
        }

        // Calculate and set storage statistics
        calculateAndSetStorageStats(request, response);

        // Forward to main storage page
        request.getRequestDispatcher("main-storage.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");

        if (session == null || nguoiDung == null) {
            request.setAttribute("error", "Vui lòng đăng nhập!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        // Kiểm tra quyền admin
        if (!"admin".equals(nguoiDung.getRole())) {
            request.setAttribute("error", "Bạn không có quyền truy cập trang này!");
            request.getRequestDispatcher("trangchu.jsp").forward(request, response);
            return;
        }

        String action = request.getParameter("action");

        if (action != null) {
            switch (action) {
                case "addUser": {
                    try {
                        addUser(request, response);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(StorageControl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;

                case "updateUser":
                    updateUser(request, response);
                    break;
                case "resetPassword":
                    resetPassword(request, response);
                    break;
                case "deleteUser":
                    deleteUser(request, response);
                    break;
                case "bulkUpdate":
                    bulkUpdate(request, response);
                    break;
                default:
                    // Calculate and set storage statistics
                    calculateAndSetStorageStats(request, response);
                    request.getRequestDispatcher("main-storage.jsp").forward(request, response);
            }
        } else {
            // Calculate and set storage statistics
            calculateAndSetStorageStats(request, response);
            request.getRequestDispatcher("main-storage.jsp").forward(request, response);
        }
    }

    private void calculateAndSetStorageStats(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        List<InforUser> servers = (List<InforUser>) session.getAttribute("users");
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");

        if (servers == null || servers.isEmpty() || nguoiDung == null) {
            return;
        }

        try {
            // Tạo map để lưu dung lượng đã sử dụng của từng người dùng
            Map<String, Long> userStorageMap = new HashMap<>();
            System.out.println("=== BAT DAU TINH TOAN THONG KE ===");

            // Thống kê tổng quan
            long totalUsed = 0;
            long totalCapacity = calculateTotalStorageLimit();
            Map<String, Object> fileStats = new HashMap<>();
            int totalFiles = 0;
            int totalFolders = 0;

            // Duyệt qua từng máy chủ
            for (InforUser server : servers) {
                System.out.println("Dang kiem tra may chu: " + server.getHost());

                // Duyệt qua từng người dùng
                for (NguoiDung user : getAllUsers()) {
                    // Đếm số lượng tệp tin và thư mục
                    int[] counts = getFileFolderCount(
                            server.getHost(),
                            server.getPort(),
                            server.getUser(),
                            server.getPassword(),
                            user.getTaiKhoan());

                    totalFiles += counts[0];
                    totalFolders += counts[1];

                    // Lấy dung lượng đã sử dụng của người dùng trên máy chủ này
//                    long usedStorage = getUserStorageUsage(server.getHost(), server.getPort(),
//                            server.getUser(), server.getPassword(),
//                            user.getTaiKhoan());
                    long usedStorage = getUserStorageUsageFromDB(user.getTaiKhoan());
                    System.out.println("Nguoi dung " + user.getTaiKhoan()
                            + " da su dung: " + (usedStorage / 1048576.0) + " MB");

                    // Cộng dồn vào map
                    userStorageMap.merge(user.getTaiKhoan(), usedStorage, Long::sum);

                    // Cộng dồn tổng dung lượng đã sử dụng
                    totalUsed += usedStorage;
                }
            }

            // Tính phần trăm sử dụng
            double usagePercent = totalCapacity > 0 ? (double) totalUsed / totalCapacity * 100 : 0;

            // Cập nhật thống kê
            Map<String, Object> storageStats = new HashMap<>();
            storageStats.put("totalUsed", totalUsed);
            storageStats.put("totalCapacity", totalCapacity);
            storageStats.put("usagePercent", usagePercent);
            fileStats.put("totalFiles", totalFiles);
            fileStats.put("totalFolders", totalFolders);
            session.setAttribute("fileStats", fileStats);

            // Đặt các thuộc tính vào request
            session.setAttribute("storageStats", storageStats);
            session.setAttribute("userStorageMap", userStorageMap);
            session.setAttribute("totalFiles", totalFiles);
            session.setAttribute("totalFolders", totalFolders);
            session.setAttribute("newUsers", (int) getNewUsersCount());

            System.out.println("Tổng dung lượng đã sử dụng: " + (totalUsed / 1048576.0) + " MB");
            System.out.println("Tổng dung lượng giới hạn: " + (totalCapacity / 1048576.0) + " MB");
            System.out.println("Tỷ lệ sử dụng: " + usagePercent + "%");

            System.out.println("=== TONG KET THONG KE ===");
            System.out.println("Tong so tep tin: " + totalFiles);
            System.out.println("Tong so thu muc: " + totalFolders);
            System.out.println("=== KET THUC THONG KE ===");

            // Lấy thông tin máy chủ
            getServerStats(request, servers);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Loi khi tinh toan thong ke", e);
            request.setAttribute("error", "Loi khi tinh toan thong ke: " + e.getMessage());
        }
    }

    private void getServerStats(HttpServletRequest request, List<InforUser> servers) {
        Map<String, Map<String, Long>> serverStats = new HashMap<>();
        HttpSession ss = request.getSession();
        System.out.println("=== BAT DAU LAY THONG TIN MAY CHU ===");

        for (InforUser server : servers) {
            try {
                Session session = InforUser.connect(
                        server.getHost(),
                        server.getPort(),
                        server.getUser(),
                        server.getPassword()
                );

                System.out.println("Dang kiem tra may chu: " + server.getHost());

                // Lệnh lấy thông tin dung lượng ổ đĩa
                String command = "df -B1 /";
                String result = executeSSHCommand(session, command);

                // Phân tích kết quả
                String[] lines = result.split("\n");
                if (lines.length > 1) {
                    String[] parts = lines[1].split("\\s+");
                    if (parts.length >= 4) {
                        Map<String, Long> stats = new HashMap<>();
                        stats.put("totalStorage", Long.parseLong(parts[1]));
                        stats.put("usedStorage", Long.parseLong(parts[2]));
                        stats.put("availableStorage", Long.parseLong(parts[3]));
                        serverStats.put(server.getHost(), stats);

                        System.out.println("May chu " + server.getHost() + ":");
                        System.out.println("- Tong dung luong: " + formatSize(Long.parseLong(parts[1])));
                        System.out.println("- Da su dung: " + formatSize(Long.parseLong(parts[2])));
                        System.out.println("- Con trong: " + formatSize(Long.parseLong(parts[3])));
                    }
                }

                session.disconnect();

            } catch (Exception e) {
                System.err.println("Loi khi lay thong tin may chu " + server.getHost() + ": " + e.getMessage());
            }
        }

        ss.setAttribute("serverStats", serverStats);
        System.out.println("=== KET THUC LAY THONG TIN MAY CHU ===");
    }

    private String formatSize(long bytes) {
        if (bytes >= 1099511627776L) { // TB
            return String.format("%.2f TB", bytes / 1099511627776.0);
        } else if (bytes >= 1073741824L) { // GB
            return String.format("%.2f GB", bytes / 1073741824.0);
        } else if (bytes >= 1048576L) { // MB
            return String.format("%.2f MB", bytes / 1048576.0);
        } else if (bytes >= 1024) { // KB
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return bytes + " B";
        }
    }

    private long getUserStorageUsage(String host, int port, String username, String password, String userAccount) {
        try {
            // Tạo kết nối SSH
            Session session = InforUser.connect(host, port, username, password);

            // Tạo kênh thực thi lệnh
            ChannelExec channel = (ChannelExec) session.openChannel("exec");

            // Lệnh du -sb để lấy dung lượng thư mục (sử dụng -sb để lấy kích thước theo
            // bytes)
            String command = "du -sb /data/" + username + "/" + userAccount + " 2>/dev/null || echo '0'";
            channel.setCommand(command);

            // Lấy kết quả
            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            String line = reader.readLine();
            channel.disconnect();
            session.disconnect();

            if (line != null && !line.isEmpty()) {
                // Kết quả của du -sb có dạng: "size /path"
                String[] parts = line.split("\\s+");
                if (parts.length > 0) {
                    try {
                        return Long.parseLong(parts[0]);
                    } catch (NumberFormatException e) {
                        LOGGER.log(Level.WARNING, "Error parsing storage size: " + line, e);
                        return 0;
                    }
                }
            }

            return 0;
        } catch (JSchException | IOException e) {
            LOGGER.log(Level.WARNING, "Error getting user storage usage for " + userAccount, e);
            return 0;
        }
    }

    private int[] getFileFolderCount(String host, int port, String username, String password, String userAccount) {
        try {
            System.out.println("=== BAT DAU DEM TEP TIN VA THU MUC ===");
            System.out.println("May chu: " + host);

            // Tạo kết nối SSH
            Session session = InforUser.connect(host, port, username, password);

            // Tạo kênh thực thi lệnh
            ChannelExec channel = (ChannelExec) session.openChannel("exec");

            // Đường dẫn cần kiểm tra
            String basePath = "/data/" + username + "/" + userAccount + "/";

            // Lệnh để đếm số lượng tệp và thư mục
            String command = String.format(
                    "if [ -d '%s' ]; then "
                    + "  echo \"$(find '%s' -type f | wc -l)\" && " //đếm số file trong thư mục đó và các thư mục con.
                    + "  echo \"$(find '%s' -mindepth 1 -type d | wc -l)\"; " //đếm số thư mục con (không bao gồm thư mục gốc).
                    + "else "
                    + "  echo '0' && echo '0'; "
                    + "fi",
                    basePath, basePath, basePath);

            channel.setCommand(command);

            // Lấy kết quả
            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            String filesLine = reader.readLine();
            String foldersLine = reader.readLine();

            int files = 0;
            int folders = 0;

            if (filesLine != null && !filesLine.isEmpty()) {
                try {
                    files = Integer.parseInt(filesLine.trim());
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Loi khi doc so luong tep tin: " + filesLine, e);
                }
            }

            if (foldersLine != null && !foldersLine.isEmpty()) {
                try {
                    folders = Integer.parseInt(foldersLine.trim());
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Loi khi doc so luong thu muc: " + foldersLine, e);
                }
            }

            System.out.println("Ket qua dem tren may chu " + host + ":");
            System.out.println("- So tep tin: " + files);
            System.out.println("- So thu muc: " + folders);

            channel.disconnect();
            session.disconnect();

            return new int[]{files, folders};

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Loi khi dem tep tin va thu muc tren may chu " + host, e);
            System.out.println("=== LOI: " + e.getMessage() + " ===");
            return new int[]{0, 0};
        }
    }

    private List<NguoiDung> getAllUsers() {
        List<NguoiDung> users = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            JDBC jdbc = new JDBC();
            conn = jdbc.connect();
            String sql = "SELECT * FROM nguoi_dung ORDER BY created_at DESC";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                NguoiDung user = new NguoiDung();
                user.setTaiKhoan(rs.getString("tai_khoan"));
                user.setMatKhau(rs.getString("mat_khau"));
                user.setThuMuc(rs.getString("thu_muc"));
                user.setRole(rs.getString("role"));
                // Thêm các trường khác nếu cần
                users.add(user);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting all users", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing resources", e);
            }
        }

        return users;
    }

    private int getNewUsersCount() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int count = 0;

        try {
            JDBC jdbc = new JDBC();
            conn = jdbc.connect();
            String sql = "SELECT COUNT(*) FROM nguoi_dung WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting new users", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing resources", e);
            }
        }

        return count;
    }

    private void addUser(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, ClassNotFoundException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String role = request.getParameter("role");

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Kiểm tra dữ liệu đầu vào
            if (username == null || username.trim().isEmpty()
                    || password == null || password.trim().isEmpty()
                    || role == null || role.trim().isEmpty()) {

                request.setAttribute("error", "Vui lòng điền đầy đủ thông tin!");
                calculateAndSetStorageStats(request, response);
                request.getRequestDispatcher("main-storage.jsp").forward(request, response);
                return;
            }

            // Kiểm tra tài khoản đã tồn tại chưa
            JDBC jdbc = new JDBC();
            conn = jdbc.connect();
            String checkSql = "SELECT COUNT(*) FROM nguoi_dung WHERE tai_khoan = ?";
            stmt = conn.prepareStatement(checkSql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                request.setAttribute("error", "Tài khoản đã tồn tại!");
                calculateAndSetStorageStats(request, response);
                request.getRequestDispatcher("main-storage.jsp").forward(request, response);
                return;
            }

            // Thêm người dùng vào CSDL
            String insertSql = "INSERT INTO nguoi_dung (tai_khoan, mat_khau, role, created_at) VALUES (?, ?, ?, NOW())";
            stmt = conn.prepareStatement(insertSql);
            stmt.setString(1, username);
            stmt.setString(2, password); // Trong thực tế nên mã hóa mật khẩu
            stmt.setString(3, role);

            int result = stmt.executeUpdate();

            if (result > 0) {
                // Tạo thư mục cho người dùng trên các máy chủ
                HttpSession session = request.getSession();
                List<InforUser> users = (List<InforUser>) session.getAttribute("users");

                for (InforUser serverUser : users) {
                    createUserDirectory(serverUser.getHost(), serverUser.getPort(),
                            serverUser.getUser(), serverUser.getPassword(), username);
                }

                request.setAttribute("success", "Thêm người dùng thành công!");
            } else {
                request.setAttribute("error", "Thêm người dùng thất bại!");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding user", e);
            request.setAttribute("error", "Lỗi khi thêm người dùng: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing resources", e);
            }
            // Tính toán lại thống kê và hiển thị trang
            calculateAndSetStorageStats(request, response);
            request.getRequestDispatcher("main-storage.jsp").forward(request, response);
        }
    }

    private void updateUser(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String tai_khoan = request.getParameter("tai_khoan");
        String role = request.getParameter("role");
        String storageLimitStr = request.getParameter("storageLimit");

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Kiểm tra dữ liệu đầu vào
            if (tai_khoan == null || tai_khoan.trim().isEmpty()
                    || role == null || role.trim().isEmpty()
                    || storageLimitStr == null || storageLimitStr.trim().isEmpty()) {

                request.setAttribute("error", "Vui lòng điền đầy đủ thông tin!");
                calculateAndSetStorageStats(request, response);
                request.getRequestDispatcher("main-storage.jsp").forward(request, response);
                return;
            }

            // Chuyển đổi dung lượng từ MB sang byte
            long storageLimit = 0;
            try {
                storageLimit = Long.parseLong(storageLimitStr) * 1048576L; // 1MB = 1048576 bytes
            } catch (NumberFormatException e) {
                request.setAttribute("error", "Dung lượng giới hạn không hợp lệ!");
                calculateAndSetStorageStats(request, response);
                request.getRequestDispatcher("main-storage.jsp").forward(request, response);
                return;
            }

            // Cập nhật thông tin người dùng
            JDBC jdbc = new JDBC();
            conn = jdbc.connect();
            String updateSql = "UPDATE nguoi_dung SET role = ?, storage_limit = ? WHERE tai_khoan = ?";
            stmt = conn.prepareStatement(updateSql);
            stmt.setString(1, role);
            stmt.setLong(2, storageLimit);
            stmt.setString(3, tai_khoan);

            int result = stmt.executeUpdate();

            if (result > 0) {
                request.setAttribute("success", "Cập nhật thông tin người dùng thành công!");
            } else {
                request.setAttribute("error", "Cập nhật thông tin người dùng thất bại!");
            }

        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Error updating user", e);
            request.setAttribute("error", "Lỗi khi cập nhật thông tin người dùng: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing resources", e);
            }

            // Tính toán lại thống kê và hiển thị trang
            calculateAndSetStorageStats(request, response);
            request.getRequestDispatcher("main-storage.jsp").forward(request, response);
        }
    }

    private void resetPassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String tai_khoan = request.getParameter("tai_khoan");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Kiểm tra dữ liệu đầu vào
            if (tai_khoan == null || tai_khoan.trim().isEmpty()
                    || newPassword == null || newPassword.trim().isEmpty()
                    || confirmPassword == null || confirmPassword.trim().isEmpty()) {

                request.setAttribute("error", "Vui lòng điền đầy đủ thông tin!");
                calculateAndSetStorageStats(request, response);
                request.getRequestDispatcher("main-storage.jsp").forward(request, response);
                return;
            }

            // Kiểm tra mật khẩu xác nhận
            if (!newPassword.equals(confirmPassword)) {
                request.setAttribute("error", "Mật khẩu xác nhận không khớp!");
                calculateAndSetStorageStats(request, response);
                request.getRequestDispatcher("main-storage.jsp").forward(request, response);
                return;
            }

            // Cập nhật mật khẩu
            JDBC jdbc = new JDBC();
            conn = jdbc.connect();
            String updateSql = "UPDATE nguoi_dung SET mat_khau = ? WHERE tai_khoan = ?";
            stmt = conn.prepareStatement(updateSql);
            stmt.setString(1, newPassword); // Trong thực tế nên mã hóa mật khẩu
            stmt.setString(2, tai_khoan);

            int result = stmt.executeUpdate();

            if (result > 0) {
                request.setAttribute("success", "Đặt lại mật khẩu thành công!");
            } else {
                request.setAttribute("error", "Đặt lại mật khẩu thất bại!");
            }

        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Error resetting password", e);
            request.setAttribute("error", "Lỗi khi đặt lại mật khẩu: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing resources", e);
            }

            // Tính toán lại thống kê và hiển thị trang
//            calculateAndSetStorageStats(request, response);
            request.getRequestDispatcher("main-storage.jsp").forward(request, response);
        }
    }

    private void deleteUser(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String tai_khoan = request.getParameter("tai_khoan");

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            // Kiểm tra dữ liệu đầu vào
            if (tai_khoan == null || tai_khoan.trim().isEmpty()) {
                request.setAttribute("error", "Không tìm thấy người dùng!");
                calculateAndSetStorageStats(request, response);
                request.getRequestDispatcher("main-storage.jsp").forward(request, response);
                return;
            }

            // Xóa người dùng khỏi CSDL
            JDBC jdbc = new JDBC();
            conn = jdbc.connect();
            String deleteSql = "DELETE FROM nguoi_dung WHERE tai_khoan = ?";
            stmt = conn.prepareStatement(deleteSql);
            stmt.setString(1, tai_khoan);

            int result = stmt.executeUpdate();

            if (result > 0) {
                // Xóa thư mục của người dùng trên các máy chủ
                HttpSession session = request.getSession();
                List<InforUser> users = (List<InforUser>) session.getAttribute("users");

                for (InforUser serverUser : users) {
                    deleteUserDirectory(serverUser.getHost(), serverUser.getPort(),
                            serverUser.getUser(), serverUser.getPassword(), tai_khoan);
                }

                request.setAttribute("success", "Xóa người dùng thành công!");
            } else {
                request.setAttribute("error", "Xóa người dùng thất bại!");
            }

        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Error deleting user", e);
            request.setAttribute("error", "Lỗi khi xóa người dùng: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing resources", e);
            }

            // Tính toán lại thống kê và hiển thị trang
            calculateAndSetStorageStats(request, response);
            request.getRequestDispatcher("main-storage.jsp").forward(request, response);
        }
    }

    //Cập nhật hàng loạt
    private void bulkUpdate(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String[] userIds = request.getParameterValues("selectedUsers");
        String action = request.getParameter("bulkAction");
        String storageLimitStr = request.getParameter("bulkStorageLimit");
        String role = request.getParameter("bulkRole");

        if (userIds == null || userIds.length == 0) {
            request.setAttribute("error", "Vui lòng chọn ít nhất một người dùng!");
            calculateAndSetStorageStats(request, response);
            request.getRequestDispatcher("main-storage.jsp").forward(request, response);
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            JDBC jdbc = new JDBC();
            conn = jdbc.connect();

            switch (action) {
                case "updateStorage":
                    if (storageLimitStr == null || storageLimitStr.trim().isEmpty()) {
                        request.setAttribute("error", "Vui lòng nhập dung lượng giới hạn!");
                        break;
                    }

                    // Chuyển đổi dung lượng từ GB sang byte
                    long storageLimit = 0;
                    try {
                        storageLimit = Long.parseLong(storageLimitStr) * 1073741824L; // 1GB = 1073741824 bytes
                    } catch (NumberFormatException e) {
                        request.setAttribute("error", "Dung lượng giới hạn không hợp lệ!");
                        break;
                    }

                    // Cập nhật dung lượng giới hạn cho các người dùng đã chọn
                    String updateStorageSql = "UPDATE nguoi_dung SET storage_limit = ? WHERE tai_khoan = ?";
                    stmt = conn.prepareStatement(updateStorageSql);

                    int successCount = 0;
                    for (String userId : userIds) {
                        stmt.setLong(1, storageLimit);
                        stmt.setString(2, userId);
                        successCount += stmt.executeUpdate();
                    }

                    if (successCount > 0) {
                        request.setAttribute("success",
                                "Cập nhật dung lượng giới hạn cho " + successCount + " người dùng thành công!");
                    } else {
                        request.setAttribute("error", "Cập nhật dung lượng giới hạn thất bại!");
                    }
                    break;

                case "updateRole":
                    if (role == null || role.trim().isEmpty()) {
                        request.setAttribute("error", "Vui lòng chọn vai trò!");
                        break;
                    }

                    // Cập nhật vai trò cho các người dùng đã chọn
                    String updateRoleSql = "UPDATE nguoi_dung SET role = ? WHERE tai_khoan = ?";
                    stmt = conn.prepareStatement(updateRoleSql);

                    successCount = 0;
                    for (String userId : userIds) {
                        stmt.setString(1, role);
                        stmt.setString(2, userId);
                        successCount += stmt.executeUpdate();
                    }

                    if (successCount > 0) {
                        request.setAttribute("success",
                                "Cập nhật vai trò cho " + successCount + " người dùng thành công!");
                    } else {
                        request.setAttribute("error", "Cập nhật vai trò thất bại!");
                    }
                    break;

                case "deleteUsers":
                    // Xóa các người dùng đã chọn
                    successCount = 0;

                    for (String userId : userIds) {
                        // Xóa người dùng khỏi CSDL
                        String deleteSql = "DELETE FROM nguoi_dung WHERE tai_khoan = ?";
                        stmt = conn.prepareStatement(deleteSql);
                        stmt.setString(1, userId);

                        int result = stmt.executeUpdate();

                        if (result > 0) {
                            // Xóa thư mục của người dùng trên các máy chủ
                            HttpSession session = request.getSession();
                            List<InforUser> users = (List<InforUser>) session.getAttribute("users");

                            for (InforUser serverUser : users) {
                                deleteUserDirectory(serverUser.getHost(), serverUser.getPort(),
                                        serverUser.getUser(), serverUser.getPassword(), userId);
                            }

                            successCount++;
                        }
                    }

                    if (successCount > 0) {
                        request.setAttribute("success", "Xóa " + successCount + " người dùng thành công!");
                    } else {
                        request.setAttribute("error", "Xóa người dùng thất bại!");
                    }
                    break;

                default:
                    request.setAttribute("error", "Hành động không hợp lệ!");
                    break;
            }

        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Error performing bulk update", e);
            request.setAttribute("error", "Lỗi khi thực hiện cập nhật hàng loạt: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing resources", e);
            }

            // Tính toán lại thống kê và hiển thị trang
            calculateAndSetStorageStats(request, response);
            request.getRequestDispatcher("main-storage.jsp").forward(request, response);
        }
    }

    private void createUserDirectory(String host, int port, String username, String password, String userAccount) {
        try {
            // Tạo kết nối SSH
            Session session = InforUser.connect(host, port, username, password);

            // Tạo kênh thực thi lệnh
            ChannelExec channel = (ChannelExec) session.openChannel("exec");

            // Lệnh tạo thư mục và cấp quyền
            String command = "mkdir -p /data/" + username + "/" + userAccount + " && "
                    + "chmod 755 /data/" + username + "/" + userAccount;
            channel.setCommand(command);

            channel.connect();

            // Đợi lệnh thực thi xong
            while (channel.isConnected()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            channel.disconnect();
            session.disconnect();

            // Cập nhật đường dẫn thư mục trong CSDL
            updateUserDirectory(userAccount, "/data/" + username + "/" + userAccount);

        } catch (JSchException e) {
            LOGGER.log(Level.WARNING, "Error creating directory for user " + userAccount, e);
        }
    }

    private void deleteUserDirectory(String host, int port, String username, String password, String userAccount) {
        try {
            // Tạo kết nối SSH
            Session session = InforUser.connect(host, port, username, password);

            // Tạo kênh thực thi lệnh
            ChannelExec channel = (ChannelExec) session.openChannel("exec");

            // Lệnh xóa thư mục
            String command = "rm -rf /data/" + username + "/" + userAccount;
            channel.setCommand(command);

            channel.connect();

            // Đợi lệnh thực thi xong
            while (channel.isConnected()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            channel.disconnect();
            session.disconnect();

        } catch (JSchException e) {
            LOGGER.log(Level.WARNING, "Error deleting directory for user " + userAccount, e);
        }
    }

    private void updateUserDirectory(String userAccount, String directoryPath) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            JDBC jdbc = new JDBC();
            conn = jdbc.connect();
            String sql = "UPDATE nguoi_dung SET thu_muc = ? WHERE tai_khoan = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, directoryPath);
            stmt.setString(2, userAccount);
            stmt.executeUpdate();
        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Error updating user directory", e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error closing resources", e);
            }
        }
    }

    /**
     * Tính tổng dung lượng giới hạn của tất cả người dùng từ CSDL
     *
     * @return Tổng dung lượng giới hạn (bytes)
     */
    private long calculateTotalStorageLimit() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        long totalLimit = 0L;

        try {
            System.out.println("=== BAT DAU TINH TONG DUNG LUONG GIOI HAN ===");

            // Kết nối CSDL
            JDBC jdbc = new JDBC();
            conn = jdbc.connect();

            // Truy vấn lấy dung lượng giới hạn của tất cả người dùng
            String sql = "SELECT tai_khoan, storage_limit FROM nguoi_dung";
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            int totalUsers = 0;
            int unlimitedUsers = 0;

            // Duyệt qua kết quả và tính tổng
            while (rs.next()) {
                String taiKhoan = rs.getString("tai_khoan");
                long storageLimit = rs.getLong("storage_limit");

                System.out.println("Nguoi dung: " + taiKhoan);

                if (storageLimit > 0) {
                    totalLimit += storageLimit;
                    System.out.println("- Dung luong gioi han: "
                            + String.format("%.2f MB", storageLimit / 1048576.0));
                } else {
                    System.out.println("- Khong gioi han dung luong");
                    unlimitedUsers++;
                }

                totalUsers++;
            }

            System.out.println("=== KET QUA TINH TOAN ===");
            System.out.println("Tong so nguoi dung: " + totalUsers);
            System.out.println("So nguoi dung co gioi han: " + (totalUsers - unlimitedUsers));
            System.out.println("So nguoi dung khong gioi han: " + unlimitedUsers);
            System.out.println("Tong dung luong gioi han: "
                    + String.format("%.2f GB", totalLimit / 1073741824.0));

            return totalLimit;

        } catch (SQLException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Loi khi tinh tong dung luong gioi han tu CSDL", e);
            System.out.println("=== LOI: " + e.getMessage() + " ===");
            return 0L;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Loi khi dong ket noi", e);
            }
        }
    }

    private long getUserStorageUsageFromDB(String taiKhoan) {
        long totalSize = 0;
        JDBC jdbc = new JDBC();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = jdbc.connect();
            String sql = "SELECT SUM(size) FROM path_on_server WHERE tai_khoan = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, taiKhoan);
            rs = stmt.executeQuery();

            if (rs.next()) {
                totalSize = rs.getLong(1); // Tổng dung lượng tính bằng bytes
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi lấy dung lượng người dùng từ DB: " + taiKhoan, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Lỗi khi đóng tài nguyên DB", ex);
            }
        }

        return totalSize;
    }

    // Hàm thực thi SSH command và trả về output
    private String executeSSHCommand(Session session, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        channel.setOutputStream(responseStream);
        channel.connect();

        while (channel.getExitStatus() == -1) {
            Thread.sleep(50); // Giảm độ trễ
        }

        channel.disconnect();
        return responseStream.toString().trim();
    }

}
