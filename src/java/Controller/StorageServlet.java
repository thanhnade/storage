package Controller;

import Model.InforUser;
import com.jcraft.jsch.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.*;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import DataBase.JDBC;
import Model.NguoiDung;
import java.sql.ResultSet;
import jakarta.servlet.http.Part;
import com.google.gson.Gson;
import java.sql.SQLException;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/StorageServlet")
@MultipartConfig
public class StorageServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(StorageServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        HttpSession session = request.getSession();

        // Kiểm tra đăng nhập
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");
        if (nguoiDung == null) {
            request.setAttribute("error", "Vui lòng đăng nhập!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        // Lấy danh sách máy chủ từ session
        List<InforUser> users = (List<InforUser>) session.getAttribute("users");
        if (users == null || users.isEmpty()) {
            request.setAttribute("error", "Không có thông tin máy chủ! Vui lòng kết nối lại.");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        try {
            if (action != null && action.equals("logout")) {
                logout(request, response);
                return;
            }
            // Xử lý đường dẫn mặc định khi đăng nhập
            String currentPath = request.getParameter("path");
            if (currentPath == null || currentPath.isEmpty()) {
                // Tạo thư mục data cho người dùng trên tất cả các máy chủ
                for (InforUser server : users) {
                    try {
                        Session sshSession = InforUser.connect(server.getHost(), server.getPort(), server.getUser(),
                                server.getPassword());
                        String dataPath = "/data/" + server.getUser() + "/" + nguoiDung.getTaiKhoan();

                        // Kiểm tra và tạo thư mục nếu chưa tồn tại
                        String checkCommand = String.format("test -d '%s' && echo 'exists'", dataPath);
                        String checkResult = executeSSHCommand(sshSession, checkCommand);

                        if (!checkResult.contains("exists")) {
                            String createCommand = String.format(
                                    "echo '%s' | sudo -S mkdir -p '%s' && sudo chmod 755 '%s' && sudo chown %s:%s '%s' && ls -ld '%s'",
                                    server.getPassword(), dataPath, dataPath, server.getUser(), server.getUser(),
                                    dataPath, dataPath);
                            executeCommand(sshSession, createCommand);

                            // Cập nhật CSDL
                            updateUserDirectories(nguoiDung.getTaiKhoan(), dataPath);
                        }
                        // Cập nhật CSDL
                        updateUserDirectories(nguoiDung.getTaiKhoan(), dataPath);
                        sshSession.disconnect();
                    } catch (Exception e) {
                        System.err.println(
                                "Lỗi khi tạo thư mục data trên máy chủ " + server.getHost() + ": " + e.getMessage());
                    }
                }
            }

            // Hiển thị danh sách files
            listFiles(request, response, users);

        } catch (ServletException | IOException e) {
            request.setAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("storage.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        HttpSession session = request.getSession();

        // Lấy danh sách máy chủ từ session
        List<InforUser> users = (List<InforUser>) session.getAttribute("users");

        if (users == null || users.isEmpty()) {
            request.setAttribute("error", "Không có thông tin máy chủ! Vui lòng kết nối lại.");
            // request.getRequestDispatcher("storage.jsp").forward(request, response);
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        try {
            switch (action) {
                case "folder":
                    createFolder(request, response);
                    break;
                case "file":
                    createFile(request, response);
                    break;
                case "delete":
                    deleteItem(request, response);
                    break;
                case "deleteMultiple":
                    deleteMultipleItems(request, response);
                    break;
                case "logout":
                    logout(request, response);
                    return;
                case "download":
                    downloadItem(request, response);
                    break;
                case "uploadFile":
                    uploadFile(request, response);
                    break;
                case "uploadFolder":
                    uploadFolder(request, response);
                    break;
                case "rename":
                    renameItem(request, response);
                    break;
                case "shareNFS":
                    shareNFS(request, response, users);
                    break;
                case "checkUser":
                    checkUserExists(request, response);
                    break;
                case "openFolder":
                    // Lấy thông tin đường dẫn và server
                    String currentPath = request.getParameter("path");
                    String name = request.getParameter("name");
                    String serverHost = request.getParameter("server");

                    // Kiểm tra thông tin đầu vào
                    if (currentPath == null || serverHost == null) {
                        request.setAttribute("error", "Thiếu thông tin để mở thư mục!");
                        listFiles(request, response, users);
                        return;
                    }

                    // Xây dựng đường dẫn đầy đủ
                    String fullPath = name != null && !name.isEmpty() ? currentPath + "/" + name : currentPath;

                    // Cập nhật request với đường dẫn mới
                    request.setAttribute("path", fullPath);
                    request.setAttribute("server", serverHost);
                    System.out.println("Duong dan hien tai: " + fullPath);
                    // Điều hướng đến thư mục
                    navigateDirectory(request, response, users);
                    break;
                default:
                    listFiles(request, response, users);
                    break;
            }

        } catch (Exception e) {
            request.setAttribute("error", "Lỗi: " + e.getMessage());
            request.getRequestDispatcher("storage.jsp").forward(request, response);
        }
    }

    private InforUser selectStorageServer(List<InforUser> users) {
        if (users == null || users.isEmpty()) {
            return null;
        }

        // Lọc tất cả các máy chủ có sẵn
        List<InforUser> servers = new ArrayList<>();
        Map<InforUser, Long> serverStorage = new HashMap<>();

        for (InforUser user : users) {
            // Kiểm tra thông tin máy chủ
            if (user == null || user.getHost() == null || user.getUser() == null
                    || user.getPassword() == null || user.getPort() <= 0) {
                System.err.println("Thông tin máy chủ không hợp lệ: " + (user != null ? user.getHost() : "null"));
                continue;
            }

            try {
                Session sshSession = InforUser.connect(
                        user.getHost(),
                        user.getPort(),
                        user.getUser(),
                        user.getPassword());

                // Kiểm tra dung lượng ổ đĩa trống bằng df -h
                String command = "df -B1 / | tail -1 | awk '{print $4}'"; // Lấy dung lượng trống tính bằng bytes
                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
                channel.setCommand(command);

                ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                channel.setOutputStream(responseStream);
                channel.connect();

                while (channel.isConnected()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                String output = responseStream.toString().trim();
                if (output == null || output.isEmpty()) {
                    throw new Exception("Không thể đọc dung lượng ổ đĩa");
                }

                long freeSpace = Long.parseLong(output);
                if (freeSpace <= 0) {
                    throw new Exception("Dung lượng ổ đĩa không hợp lệ");
                }

                servers.add(user);
                serverStorage.put(user, freeSpace);

                channel.disconnect();
                sshSession.disconnect();

                // Log thông tin để debug
                System.out.println("May " + user.getHost() + " co dung luong trong: " + formatSize(freeSpace));

            } catch (Exception e) {
                System.err.println("Lỗi khi kiểm tra ổ đĩa máy chủ " + user.getHost() + ": " + e.getMessage());
            }
        }

        if (servers.isEmpty()) {
            return null;
        }

        // Tìm máy chủ có dung lượng trống lớn nhất
        InforUser selectedServer = null;
        long maxFreeSpace = -1;

        for (Map.Entry<InforUser, Long> entry : serverStorage.entrySet()) {
            if (entry.getValue() > maxFreeSpace) {
                maxFreeSpace = entry.getValue();
                selectedServer = entry.getKey();
            }
        }

        // Log thông tin máy chủ được chọn
        if (selectedServer != null) {
            System.out.println("Da chon may chu: " + selectedServer.getHost());
            System.out.println("Dung luong trong: " + formatSize(maxFreeSpace));
        } else {
            System.err.println("Không tìm thấy máy chủ phù hợp");
        }

        return selectedServer;
    }

    private String formatSize(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    private String getRelativePath(String fullPath, String serverUser, String userAccount) {
        String basePath = "/data/" + serverUser + "/" + userAccount + "/";
        if (fullPath.startsWith(basePath)) {
            return fullPath.substring(basePath.length());
        }
        return fullPath;
    }

    private void createFolder(HttpServletRequest request,
            HttpServletResponse response) throws JSchException, IOException, InterruptedException, ServletException {
        String name = request.getParameter("name");
        String currentPath = request.getParameter("currentPath");
        String targetServer = request.getParameter("server");
        HttpSession session = request.getSession();
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");

        // Chuẩn hóa tên thư mục
        String sanitizedFolderName = sanitizeFileName(name).trim();

        System.out.println("=== BAT DAU TAO THU MUC ===");
        System.out.println("Ten thu muc: " + sanitizedFolderName);
        System.out.println("Duong dan hien tai: " + currentPath);
        System.out.println("Server muc tieu: " + targetServer);

        if (nguoiDung == null) {
            System.out.println("LOI: Khong tim thay thong tin nguoi dung trong session");
            request.setAttribute("error", "Không tìm thấy thông tin người dùng trong session.");
            request.getRequestDispatcher("storage.jsp").forward(request, response);
            return;
        }

        String taiKhoan = nguoiDung.getTaiKhoan();
        if (taiKhoan == null || taiKhoan.isEmpty()) {
            System.out.println("LOI: Tai khoan nguoi dung khong hop le");
            request.setAttribute("error", "Tài khoản người dùng không hợp lệ.");
            request.getRequestDispatcher("storage.jsp").forward(request, response);
            return;
        }

        // Tìm máy chủ đích
        List<InforUser> users = (List<InforUser>) session.getAttribute("users");
        InforUser targetInforUser = null;

        if (targetServer != null && !targetServer.isEmpty()) {
            for (InforUser user : users) {
                if (user.getHost().equals(targetServer)) {
                    targetInforUser = user;
                    break;
                }
            }
        }

        if (targetInforUser == null) {
            System.out.println("Khong tim thay server muc tieu, dang tim server phu hop...");
            targetInforUser = (InforUser) session.getAttribute("storage_server");
            if (targetInforUser == null) {
                targetInforUser = selectStorageServer(users);
                if (targetInforUser != null) {
                    session.setAttribute("storage_server", targetInforUser);
                    System.out.println("Da chon server: " + targetInforUser.getHost());
                } else {
                    System.out.println("LOI: Khong tim thay server phu hop");
                    request.setAttribute("error", "Không tìm thấy máy chủ phù hợp!");
                    request.getRequestDispatcher("storage.jsp").forward(request, response);
                    return;
                }
            }
        }

        // Kiểm tra xem tên thư mục đã tồn tại trên bất kỳ server nào chưa
        String basePath = "/data/";
        boolean folderExists = false;
        String existingServer = "";

        for (InforUser user : users) {
            try {
                Session checkSession = InforUser.connect(user.getHost(), user.getPort(),
                        user.getUser(), user.getPassword());

                String checkPath = (currentPath != null && !currentPath.isEmpty())
                        ? currentPath + "/" + sanitizedFolderName
                        : basePath + user.getUser() + "/" + taiKhoan + "/" + sanitizedFolderName;
                // Kiem tra tren CSDL
                if (checkPath.equals(basePath + user.getUser() + "/" + taiKhoan + "/" + sanitizedFolderName)) {
                    boolean checkPathRelative = checkFileNameExistsInDB(sanitizedFolderName, taiKhoan);
                    if (checkPathRelative) {
                        System.out.println("Ket qua kiem tra " + checkPathRelative);
                        System.out.println("Thu muc " + sanitizedFolderName + " da ton tai");
                        folderExists = true;
                        existingServer = "CSDL path_on_server";
                        checkSession.disconnect();
                        break;
                    }
                }
                // Lệnh kiểm tra thư mục
                String checkCommand = String.format("[ -d '%s' ] && echo 'exists' || echo 'not exists'", checkPath);
                String checkResult = executeSSHCommand(checkSession, checkCommand);

                System.out.println("Kiem tra thu muc tai " + user.getHost() + ": " + checkPath);
                System.out.println("Ket qua kiem tra: " + checkResult);

                if ("exists".equals(checkResult.trim())) {
                    folderExists = true;
                    existingServer = user.getHost();
                    checkSession.disconnect();
                    break;
                }

                checkSession.disconnect();
            } catch (Exception e) {
                System.out.println("LOI khi kiem tra server " + user.getHost() + ": " + e.getMessage());
            }
        }

        if (folderExists) {
            System.out.println("LOI: Thu muc '" + sanitizedFolderName + "' da ton tai tren may chu " + existingServer);
            // request.setAttribute("error", "Thư mục '" + sanitizedFolderName + "' đã tồn
            // tại trên máy chủ
            // " + existingServer);
            request.setAttribute("error", "Thư mục '" + sanitizedFolderName + "' đã tồn tại");
            request.setAttribute("path", currentPath);
            listFiles(request, response, users);
            return;
        }

        try {
            Session targetSession = InforUser.connect(targetInforUser.getHost(), targetInforUser.getPort(),
                    targetInforUser.getUser(), targetInforUser.getPassword());

            String serverPath = (currentPath != null && !currentPath.isEmpty())
                    ? currentPath + "/" + sanitizedFolderName
                    : basePath + targetInforUser.getUser() + "/" + taiKhoan + "/" + sanitizedFolderName;

            // Kiểm tra lại một lần nữa trước khi tạo thư mục
            String finalCheckCommand = String.format("[ -d '%s' ] && echo 'exists' || echo 'not exists'", serverPath);
            String finalCheckResult = executeSSHCommand(targetSession, finalCheckCommand);

            if ("exists".equals(finalCheckResult.trim())) {
                System.out.println("LOI: Thu muc '" + sanitizedFolderName + "' da ton tai tren may chu "
                        + targetInforUser.getHost());
                // request.setAttribute("error","Thư mục '" + sanitizedFolderName + "' đã tồn
                // tại trên máy chủ
                // " + targetInforUser.getHost());
                request.setAttribute("error", "Thư mục '" + sanitizedFolderName + "' đã tồn tại");
                request.setAttribute("path", currentPath);
                listFiles(request, response, users);
                return;
            }

            System.out.println("Dang tao thu muc: " + serverPath);

            String command = String.format(
                    "echo '%s' | sudo -S bash -c 'mkdir -p \"%s\" 2>/dev/null && "
                    + "chmod 755 \"%s\" 2>/dev/null && "
                    + "chown %s:%s \"%s\" 2>/dev/null'",
                    targetInforUser.getPassword(),
                    serverPath,
                    serverPath,
                    targetInforUser.getUser(),
                    targetInforUser.getUser(),
                    serverPath);

            String result = executeSSHCommand(targetSession, command);
            System.out.println("Ket qua tao thu muc: " + result);

            // Kiểm tra xem thư mục đã được tạo thành công chưa
            String verifyCommand = String.format("[ -d '%s' ] && echo 'success' || echo 'failed'", serverPath);
            String verifyResult = executeSSHCommand(targetSession, verifyCommand);

            if ("success".equals(verifyResult.trim())) {
                // Lưu đường dẫn vào CSDL
                savePathOnServer(serverPath, targetInforUser.getUser(), taiKhoan, true, 4096);
                System.out.println("=== KET QUA: Da tao thu muc thanh cong: " + sanitizedFolderName + " ===");
                request.setAttribute("success", "Đã tạo thư mục: " + sanitizedFolderName);
            } else {
                System.out.println("=== LOI: Khong the tao thu muc ===");
                request.setAttribute("error", "Không thể tạo thư mục: " + sanitizedFolderName);
            }

            request.setAttribute("path", currentPath);
            listFiles(request, response, users);

        } catch (Exception e) {
            System.out.println("=== LOI: Khong the tao thu muc: " + e.getMessage() + " ===");
            request.setAttribute("error", "Lỗi khi tạo thư mục: " + e.getMessage());
            request.setAttribute("path", currentPath);
            listFiles(request, response, users);
        }
    }

    private void createFile(HttpServletRequest request,
            HttpServletResponse response) throws JSchException, IOException, InterruptedException, ServletException {
        String name = request.getParameter("name");
        String currentPath = request.getParameter("currentPath");
        String targetServer = request.getParameter("server");
        HttpSession session = request.getSession();
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");

        // Chuẩn hóa tên file
        String sanitizedFileName = sanitizeFileName(name).trim();

        System.out.println("=== BAT DAU TAO TAP TIN ===");
        System.out.println("Ten tap tin: " + sanitizedFileName);
        System.out.println("Duong dan hien tai: " + currentPath);
        System.out.println("Server muc tieu: " + targetServer);

        if (nguoiDung == null) {
            System.out.println("LOI: Khong tim thay thong tin nguoi dung trong session");
            request.setAttribute("error", "Không tìm thấy thông tin người dùng trong session.");
            request.getRequestDispatcher("storage.jsp").forward(request, response);
            return;
        }

        String taiKhoan = nguoiDung.getTaiKhoan();
        if (taiKhoan == null || taiKhoan.isEmpty()) {
            System.out.println("LOI: Tai khoan nguoi dung khong hop le");
            request.setAttribute("error", "Tài khoản người dùng không hợp lệ.");
            request.getRequestDispatcher("storage.jsp").forward(request, response);
            return;
        }

        // Tìm máy chủ đích
        List<InforUser> users = (List<InforUser>) session.getAttribute("users");
        InforUser targetInforUser = null;

        if (targetServer != null && !targetServer.isEmpty()) {
            for (InforUser user : users) {
                if (user.getHost().equals(targetServer)) {
                    targetInforUser = user;
                    break;
                }
            }
        }

        if (targetInforUser == null) {
            System.out.println("Khong tim thay server muc tieu, dang tim server phu hop...");
            targetInforUser = (InforUser) session.getAttribute("storage_server");
            if (targetInforUser == null) {
                targetInforUser = selectStorageServer(users);
                if (targetInforUser != null) {
                    session.setAttribute("storage_server", targetInforUser);
                    System.out.println("Da chon server: " + targetInforUser.getHost());
                } else {
                    System.out.println("LOI: Khong tim thay server phu hop");
                    request.setAttribute("error", "Không tìm thấy máy chủ phù hợp!");
                    request.getRequestDispatcher("storage.jsp").forward(request, response);
                    return;
                }
            }
        }

        // Kiểm tra xem tên file đã tồn tại trên bất kỳ server nào chưa
        String basePath = "/data/";
        boolean fileExists = false;
        String existingServer = "";

        for (InforUser user : users) {
            try {
                Session checkSession = InforUser.connect(user.getHost(), user.getPort(),
                        user.getUser(), user.getPassword());

                String checkPath = (currentPath != null && !currentPath.isEmpty())
                        ? currentPath + "/" + sanitizedFileName
                        : basePath + user.getUser() + "/" + taiKhoan + "/" + sanitizedFileName;
                // Kiem tra tren CSDL
                if (checkPath.equals(basePath + user.getUser() + "/" + taiKhoan + "/" + sanitizedFileName)) {
                    boolean checkPathRelative = checkFileNameExistsInDB(sanitizedFileName, taiKhoan);
                    if (checkPathRelative) {
                        System.out.println("Ket qua kiem tra " + checkPathRelative);
                        System.out.println("Thu muc " + sanitizedFileName + " da ton tai");
                        fileExists = true;
                        existingServer = "CSDL path_on_server";
                        checkSession.disconnect();
                        break;
                    }
                }
                // Lệnh kiểm tra file
                String checkCommand = String.format("[ -f '%s' ] && echo 'exists' || echo 'not exists'", checkPath);
                String checkResult = executeSSHCommand(checkSession, checkCommand);

                System.out.println("Kiem tra tap tin tai " + user.getHost() + ": " + checkPath);
                System.out.println("Ket qua kiem tra: " + checkResult);

                if ("exists".equals(checkResult.trim())) {
                    fileExists = true;
                    existingServer = user.getHost();
                    checkSession.disconnect();
                    break;
                }

                checkSession.disconnect();
            } catch (Exception e) {
                System.out.println("LOI khi kiem tra server " + user.getHost() + ": " + e.getMessage());
            }
        }

        if (fileExists) {
            System.out.println("LOI: Tap tin '" + sanitizedFileName + "' da ton tai tren may chu " + existingServer);
            // request.setAttribute("error", "Tập tin '" + sanitizedFileName + "' đã tồn tại
            // trên máy chủ
            // " + existingServer);
            request.setAttribute("error", "Tập tin '" + sanitizedFileName + "' đã tồn tại");
            request.setAttribute("path", currentPath);
            listFiles(request, response, users);
            return;
        }

        try {
            Session targetSession = InforUser.connect(targetInforUser.getHost(), targetInforUser.getPort(),
                    targetInforUser.getUser(), targetInforUser.getPassword());

            String serverPath = (currentPath != null && !currentPath.isEmpty())
                    ? currentPath + "/" + sanitizedFileName
                    : basePath + targetInforUser.getUser() + "/" + taiKhoan + "/" + sanitizedFileName;

            // Kiểm tra lại một lần nữa trước khi tạo file
            String finalCheckCommand = String.format("[ -f '%s' ] && echo 'exists' || echo 'not exists'", serverPath);
            String finalCheckResult = executeSSHCommand(targetSession, finalCheckCommand);

            if ("exists".equals(finalCheckResult.trim())) {
                System.out.println("LOI: Tap tin '" + sanitizedFileName + "' da ton tai tren may chu "
                        + targetInforUser.getHost());
                // request.setAttribute("error","Tập tin '" + sanitizedFileName + "' đã tồn tại
                // trên máy chủ
                // " + targetInforUser.getHost());
                request.setAttribute("error", "Tập tin '" + sanitizedFileName + "' đã tồn tại");
                request.setAttribute("path", currentPath);
                listFiles(request, response, users);
                return;
            }

            System.out.println("Dang tao tap tin: " + serverPath);

            String command = String.format(
                    "echo '%s' | sudo -S bash -c 'touch \"%s\" 2>/dev/null && "
                    + "chmod 644 \"%s\" 2>/dev/null && "
                    + "chown %s:%s \"%s\" 2>/dev/null'",
                    targetInforUser.getPassword(),
                    serverPath,
                    serverPath,
                    targetInforUser.getUser(),
                    targetInforUser.getUser(),
                    serverPath);

            String result = executeSSHCommand(targetSession, command);
            System.out.println("Ket qua tao tap tin: " + result);

            // Kiểm tra xem file đã được tạo thành công chưa
            String verifyCommand = String.format("[ -f '%s' ] && echo 'success' || echo 'failed'", serverPath);
            String verifyResult = executeSSHCommand(targetSession, verifyCommand);

            if ("success".equals(verifyResult.trim())) {
                // Lưu đường dẫn vào CSDL
                savePathOnServer(serverPath, targetInforUser.getUser(), taiKhoan, false, 0);
                System.out.println("=== KET QUA: Da tao tap tin thanh cong: " + sanitizedFileName + " ===");
                request.setAttribute("success", "Đã tạo tập tin: " + sanitizedFileName);
            } else {
                System.out.println("=== LOI: Khong the tao tap tin ===");
                request.setAttribute("error", "Không thể tạo tập tin: " + sanitizedFileName);
            }

            request.setAttribute("path", currentPath);
            listFiles(request, response, users);

        } catch (Exception e) {
            System.out.println("=== LOI: Khong the tao tap tin: " + e.getMessage() + " ===");
            request.setAttribute("error", "Lỗi khi tạo tập tin: " + e.getMessage());
            request.setAttribute("path", currentPath);
            listFiles(request, response, users);
        }
    }

    private Map<String, Object> getStorageInfo(List<InforUser> users, String userAccount, HttpServletRequest request) {
        Map<String, Object> storageInfo = new HashMap<>();
        long totalUserUsed = 0; // Tổng dung lượng người dùng đã sử dụng
        Map<String, Map<String, Object>> serverStorage = new HashMap<>();
        long totalSizeOnDB = 0; // Tổng dung lượng của người dùng trên máy chưa kết nối
        // Lấy giới hạn dung lượng từ CSDL
        JDBC jdbc = new JDBC();
        long storageLimit = 1024L * 1024L * 1024L; // Mặc định 1GB

        try (Connection conn = jdbc.connect()) {
            String sql = "SELECT storage_limit FROM nguoi_dung WHERE tai_khoan = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userAccount);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                storageLimit = rs.getLong("storage_limit");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy giới hạn dung lượng từ CSDL: " + e.getMessage());
        }

        for (InforUser user : users) {
            try {
                Session sshSession = InforUser.connect(user.getHost(), user.getPort(), user.getUser(), user.getPassword());

                // Lấy dung lượng đã sử dụng của thư mục người dùng
                String userPath = "/data/" + user.getUser() + "/" + userAccount;
                String duCommand = String.format("du -sb '%s' 2>/dev/null || echo '0'", userPath);
                String usedSpace = executeSSHCommand(sshSession, duCommand);

                long userUsed = Long.parseLong(usedSpace.split("\\s+")[0]); // Lấy số đầu tiên từ kết quả du
                totalUserUsed += userUsed; // Cộng dồn dung lượng người dùng đã sử dụng

                // Chuyển đổi sang MB cho server hiện tại
                double userUsedMB = userUsed / (1024.0 * 1024.0);

                // Lưu thông tin cho từng server
                Map<String, Object> serverInfo = new HashMap<>();
                serverInfo.put("used", userUsedMB); // Lưu dung lượng theo MB
                serverStorage.put(user.getHost(), serverInfo);

                sshSession.disconnect();
            } catch (Exception e) {
                System.err.println(
                        "Lỗi khi lấy thông tin dung lượng từ server " + user.getHost() + ": " + e.getMessage());
            }
        }
        // Lấy danh sách file của người dùng trên máy chủ chưa được kết nối
        try {
            HttpSession session = request.getSession();
            NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");
            Connection conn = jdbc.connect();
            // Lấy danh sách path_relative từ bảng path_on_server
            String pathSql = "SELECT * FROM path_on_server WHERE tai_khoan = ?";
            PreparedStatement pathStmt = conn.prepareStatement(pathSql);
            pathStmt.setString(1, nguoiDung.getTaiKhoan());
            ResultSet pathRs = pathStmt.executeQuery();

            // Tạo map để lưu trữ thông tin path
            Map<String, Map<String, Object>> pathOnServer = new HashMap<>();

            while (pathRs.next()) {
                Map<String, Object> pathInfo = new HashMap<>();
                pathInfo.put("path_base", pathRs.getString("path_base"));
                pathInfo.put("path_relative", pathRs.getString("path_relative"));
                pathInfo.put("isDirectory", pathRs.getBoolean("isDirectory"));
                pathInfo.put("size", pathRs.getLong("size"));
                pathInfo.put("exists", false); // Mặc định là chưa tồn tại
                pathOnServer.put(pathRs.getString("path_relative"), pathInfo);
            }
            // Kiểm tra sự tồn tại của các path trên các server
            for (InforUser server : users) {
                try {
                    Session sshSession = InforUser.connect(
                            server.getHost(),
                            server.getPort(),
                            server.getUser(),
                            server.getPassword());

                    for (Map.Entry<String, Map<String, Object>> entry : pathOnServer.entrySet()) {
                        String pathRelative = entry.getKey();
                        Map<String, Object> pathInfo = entry.getValue();
                        String basePath = (String) pathInfo.get("path_base");
                        boolean isDirectory = (boolean) pathInfo.get("isDirectory");
                        Long size = (Long) pathInfo.get("size");
                        String fullPath = basePath + pathRelative;
                        String checkCommand;
                        if (isDirectory) {
                            checkCommand = String.format("test -d '%s' && echo 'exists'", fullPath);
                        } else {
                            checkCommand = String.format("test -f '%s' && echo 'exists'", fullPath);
                        }
                        String result = executeSSHCommand(sshSession, checkCommand);
                        System.out.println("Ket qua kiem tra path_on_server:" + result);

                        // Nếu đã tồn tại thì tiếp tục đặt exists, ngược lại tính tổng dung lượng các tệp tin
                        if (result.contains("exists")) {
                            pathInfo.put("exists", true);
                        } else {
                            totalSizeOnDB += size;
                        }
                    }
                    sshSession.disconnect();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Lỗi khi kiểm tra path trên server " + server.getHost(), e);
                }
            }
            // Lưu thông tin vào session
            session.setAttribute("pathOnServer", pathOnServer);
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println(e);
        }
        System.out.println("Tong dung luong cua nguoi dung tren may chua ket noi: " + (totalSizeOnDB / users.size()));
        System.out.println("Tong dung luong nguoi dung da su dung: " + totalUserUsed);
        // Chuyển đổi tổng dung lượng sang MB
        double totalUserUsedMB = (totalUserUsed + totalSizeOnDB / users.size()) / (1024.0 * 1024.0);
        double storageLimitMB = storageLimit / (1024.0 * 1024.0);

        storageInfo.put("totalUsed", totalUserUsedMB); // Tổng dung lượng người dùng đã sử dụng (MB)
        storageInfo.put("storageLimit", storageLimitMB); // Giới hạn dung lượng (MB)
        //storageInfo.put("usagePercent", (totalUserUsed * 100.0) / storageLimit); // Phần trăm sử dụng
        storageInfo.put("usagePercent", ((totalUserUsed + totalSizeOnDB / users.size()) * 100.0) / storageLimit);
        storageInfo.put("serverStorage", serverStorage);

        return storageInfo;
    }

    protected void listFiles(HttpServletRequest request, HttpServletResponse response, List<InforUser> users)
            throws ServletException, IOException {
        try {
            HttpSession session = request.getSession();
            NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");

            if (nguoiDung == null) {
                request.setAttribute("error", "Không tìm thấy thông tin người dùng trong session.");
                request.getRequestDispatcher("storage.jsp").forward(request, response);
                return;
            }

            String taiKhoan = nguoiDung.getTaiKhoan();
            if (taiKhoan == null || taiKhoan.isEmpty()) {
                request.setAttribute("error", "Tài khoản người dùng không hợp lệ.");
                request.getRequestDispatcher("storage.jsp").forward(request, response);
                return;
            }

            // Lấy thông tin dung lượng
            Map<String, Object> storageInfo = getStorageInfo(users, taiKhoan, request);
            request.setAttribute("storageInfo", storageInfo);
            session.setAttribute("storageInfo", storageInfo);

            String currentPath = request.getAttribute("path") != null ? (String) request.getAttribute("path")
                    : request.getParameter("path");

            InforUser currentServer = (InforUser) session.getAttribute("storage_server");

            List<Map<String, String>> userDirectories = getUserDirectoriesFromDB(taiKhoan);
            List<Map<String, String>> allFiles = new ArrayList<>();
            List<Map<String, String>> validDirectories = new ArrayList<>();

            // Lấy danh sách savedPaths từ session
            Map<String, Map<String, Object>> savedPaths = (Map<String, Map<String, Object>>) session.getAttribute("savedPaths");
            if (savedPaths == null) {
                savedPaths = new HashMap<>();
                session.setAttribute("savedPaths", savedPaths);
            }

            if (currentPath != null && !currentPath.isEmpty() && currentServer != null) {
                Session sshSession = null;
                try {
                    sshSession = InforUser.connect(currentServer.getHost(), currentServer.getPort(),
                            currentServer.getUser(), currentServer.getPassword());

                    String checkCommand = String.format("test -d '%s' && echo 'exists'", currentPath);
                    String checkResult = executeSSHCommand(sshSession, checkCommand);

                    if (!checkResult.contains("exists")) {
                        String createCommand = String.format("mkdir -p '%s' && chmod 755 '%s'",
                                currentPath, currentPath);
                        executeCommand(sshSession, createCommand);

                        Map<String, String> dirInfo = new HashMap<>();
                        dirInfo.put("path", currentPath);
                        dirInfo.put("server", currentServer.getHost());
                        dirInfo.put("serverUser", currentServer.getUser());
                        validDirectories.add(dirInfo);

                        // updateUserDirectories(taiKhoan, currentPath);
                    }

                    String listCommand = String.format("ls -la --time-style=long-iso '%s'", currentPath);
                    String output = executeSSHCommand(sshSession, listCommand);

                    String[] lines = output.split("\n");
                    for (String line : lines) {
                        if (line.startsWith("total") || line.trim().isEmpty()) {
                            continue;
                        }

                        String[] parts = line.split("\\s+");
                        if (parts.length >= 8) {
                            String permissions = parts[0];
                            String size = parts[4];
                            String modified = parts[5] + " " + parts[6];
                            String fileName = String.join(" ", Arrays.copyOfRange(parts, 7, parts.length));

                            if (fileName.equals(".") || fileName.equals("..")) {
                                continue;
                            }

                            Map<String, String> fileInfo = new HashMap<>();
                            fileInfo.put("name", fileName);
                            fileInfo.put("size", size);
                            fileInfo.put("modified", modified);
                            fileInfo.put("directory", permissions.startsWith("d") ? "true" : "false");
                            fileInfo.put("path", currentPath);
                            fileInfo.put("server", currentServer.getHost());
                            fileInfo.put("serverUser", currentServer.getUser());
                            String relativePath = getRelativePath(currentPath + "/" + fileName,
                                    currentServer.getUser(), taiKhoan);
                            fileInfo.put("relativePath", relativePath);
                            allFiles.add(fileInfo);

                            // Cập nhật trạng thái exists trong savedPaths nếu có
                            String fullPath = currentPath + "/" + fileName;
                            if (savedPaths.containsKey(fullPath)) {
                                Map<String, Object> pathInfo = savedPaths.get(fullPath);
                                pathInfo.put("exists", true);
                                savedPaths.put(fullPath, pathInfo);
                            }
                        }
                    }
                } finally {
                    if (sshSession != null && sshSession.isConnected()) {
                        sshSession.disconnect();
                    }
                }
            } else {
                // Nếu không có đường dẫn hiện tại, liệt kê từ tất cả các server
                for (InforUser server : users) {
                    Session sshSession = null;
                    try {
                        sshSession = InforUser.connect(server.getHost(), server.getPort(),
                                server.getUser(), server.getPassword());
                        String defaultPath = "/data/" + server.getUser() + "/" + taiKhoan;

                        // Kiểm tra và tạo thư mục nếu chưa tồn tại
                        String checkCommand = String.format("test -d '%s' && echo 'exists'", defaultPath);
                        String checkResult = executeSSHCommand(sshSession, checkCommand);

                        if (!checkResult.contains("exists")) {
                            String createCommand = String.format("mkdir -p '%s' && chmod 755 '%s'",
                                    defaultPath, defaultPath);
                            executeCommand(sshSession, createCommand);

                            Map<String, String> dirInfo = new HashMap<>();
                            dirInfo.put("path", defaultPath);
                            dirInfo.put("server", server.getHost());
                            dirInfo.put("serverUser", server.getUser());
                            validDirectories.add(dirInfo);

                            // updateUserDirectories(taiKhoan, defaultPath);
                        }

                        String listCommand = String.format("ls -la --time-style=long-iso '%s'", defaultPath);
                        String output = executeSSHCommand(sshSession, listCommand);

                        String[] lines = output.split("\n");
                        for (String line : lines) {
                            if (line.startsWith("total") || line.trim().isEmpty()) {
                                continue;
                            }

                            String[] parts = line.split("\\s+");
                            if (parts.length >= 8) {
                                String permissions = parts[0];
                                String size = parts[4];
                                String modified = parts[5] + " " + parts[6];
                                String fileName = String.join(" ", Arrays.copyOfRange(parts, 7, parts.length));

                                if (fileName.equals(".") || fileName.equals("..")) {
                                    continue;
                                }

                                Map<String, String> fileInfo = new HashMap<>();
                                fileInfo.put("name", fileName);
                                fileInfo.put("size", size);
                                fileInfo.put("modified", modified);
                                fileInfo.put("directory", permissions.startsWith("d") ? "true" : "false");
                                fileInfo.put("path", defaultPath);
                                fileInfo.put("server", server.getHost());
                                fileInfo.put("serverUser", server.getUser());
                                String relativePath = getRelativePath(defaultPath + "/" + fileName,
                                        server.getUser(), taiKhoan);
                                fileInfo.put("relativePath", relativePath);
                                allFiles.add(fileInfo);

                                // Cập nhật trạng thái exists trong savedPaths nếu có
                                String fullPath = defaultPath + "/" + fileName;
                                if (savedPaths.containsKey(fullPath)) {
                                    Map<String, Object> pathInfo = savedPaths.get(fullPath);
                                    pathInfo.put("exists", true);
                                    savedPaths.put(fullPath, pathInfo);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err
                                .println("Lỗi khi liệt kê files từ server " + server.getHost() + ": " + e.getMessage());
                    } finally {
                        if (sshSession != null && sshSession.isConnected()) {
                            sshSession.disconnect();
                        }
                    }
                }
            }

            // Sắp xếp files: thư mục trước, file sau và theo tên
            Collections.sort(allFiles, (a, b) -> {
                boolean aIsDir = "true".equals(a.get("directory"));
                boolean bIsDir = "true".equals(b.get("directory"));
                if (aIsDir != bIsDir) {
                    return aIsDir ? -1 : 1;
                }
                return a.get("name").compareToIgnoreCase(b.get("name"));
            });

            session.setAttribute("fileList", allFiles);
            session.setAttribute("validDirectories", validDirectories);
            session.setAttribute("userDirectories", userDirectories);
            session.setAttribute("currentPath", currentPath);

            request.setAttribute("fileList", allFiles);
            request.setAttribute("userDirectories", userDirectories);
            request.setAttribute("validDirectories", validDirectories);
            request.setAttribute("currentPath", currentPath);

            if (!response.isCommitted()) {
                request.getRequestDispatcher("storage.jsp").forward(request, response);
            }

        } catch (Exception e) {
            request.setAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("storage.jsp").forward(request, response);
        }
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

    private void executeCommand(Session session, String command) throws JSchException, IOException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        channel.setOutputStream(responseStream);
        channel.connect();

        while (channel.isConnected()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        channel.disconnect();
    }

    private List<Map<String, String>> createBreadcrumbs(String path) {
        List<Map<String, String>> breadcrumbs = new ArrayList<>();
        String[] parts = path.split("/");
        StringBuilder currentPath = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                currentPath.append("/").append(part);
                Map<String, String> crumb = new HashMap<>();
                crumb.put("name", part);
                crumb.put("path", currentPath.toString());
                breadcrumbs.add(crumb);
            }
        }

        return breadcrumbs;
    }

    private void logout(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Lấy session hiện tại
        HttpSession session = request.getSession(false);

        // Nếu session tồn tại, chỉ xóa thuộc tính nguoi_dung
        if (session != null) {
            session.removeAttribute("nguoi_dung");
            session.removeAttribute("currentPath");
            session.removeAttribute("storage_server");
            session.removeAttribute("fileList");
            session.removeAttribute("error");
            session.removeAttribute("success");
            // session.removeAttribute("breadcrumbs");
            // session.removeAttribute("serverInfo");
        }

        // Xóa tất cả các thuộc tính của request
        if (request != null) {
            Enumeration<String> attributeNames = request.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String attributeName = attributeNames.nextElement();
                request.removeAttribute(attributeName);
            }
        }

        // Chuyển hướng về trang đăng nhập
        response.sendRedirect("login.jsp");
    }

    private void navigateDirectory(HttpServletRequest request, HttpServletResponse response, List<InforUser> users)
            throws ServletException, IOException {
        try {
            HttpSession session = request.getSession();
            NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");

            if (nguoiDung == null) {
                request.setAttribute("error", "Không tìm thấy thông tin người dùng trong session.");
                request.getRequestDispatcher("storage.jsp").forward(request, response);
                return;
            }

            String currentPath = request.getParameter("path");
            String name = request.getParameter("name");
            String serverHost = request.getParameter("server");

            // Tìm máy chủ tương ứng
            InforUser storageServer = null;
            for (InforUser user : users) {
                if (user.getHost().equals(serverHost)) {
                    storageServer = user;
                    break;
                }
            }

            if (storageServer == null) {
                request.setAttribute("error", "Không tìm thấy máy chủ!");
                request.getRequestDispatcher("storage.jsp").forward(request, response);
                return;
            }

            // Xây dựng đường dẫn đầy đủ
            String fullPath;
            if (name != null && !name.isEmpty()) {
                fullPath = currentPath + "/" + name;
            } else {
                fullPath = currentPath;
            }

            // Kết nối SSH đến máy chủ
            Session sshSession = InforUser.connect(
                    storageServer.getHost(),
                    storageServer.getPort(),
                    storageServer.getUser(),
                    storageServer.getPassword());

            try {
                // Kiểm tra đường dẫn có tồn tại không
                String checkCommand = String.format("test -e '%s' && echo 'exists'", fullPath);
                String result = executeSSHCommand(sshSession, checkCommand);

                if (!result.contains("exists")) {
                    request.setAttribute("error", "Đường dẫn không tồn tại: " + fullPath);
                    listFiles(request, response, users);
                    return;
                }

                // Lấy danh sách file và thư mục
                String listCommand = String.format("ls -la --time-style=long-iso '%s'", fullPath);
                String output = executeSSHCommand(sshSession, listCommand);

                List<Map<String, String>> fileList = new ArrayList<>();
                String[] lines = output.split("\n");

                for (String line : lines) {
                    if (line.startsWith("total") || line.trim().isEmpty()) {
                        continue;
                    }

                    String[] parts = line.split("\\s+");
                    if (parts.length >= 8) {
                        String permissions = parts[0];
                        String size = parts[4];
                        String modified = parts[5] + " " + parts[6];
                        String fileName = String.join(" ", Arrays.copyOfRange(parts, 7, parts.length));

                        if (fileName.equals(".") || fileName.equals("..")) {
                            continue;
                        }

                        Map<String, String> fileInfo = new HashMap<>();
                        fileInfo.put("name", fileName);
                        fileInfo.put("size", size);
                        fileInfo.put("modified", modified);
                        fileInfo.put("directory", permissions.startsWith("d") ? "true" : "false");
                        fileInfo.put("path", fullPath);
                        fileInfo.put("server", storageServer.getHost());
                        fileInfo.put("serverUser", storageServer.getUser());
                        fileList.add(fileInfo);
                    }
                }

                // Sắp xếp: thư mục trước, file sau và theo tên
                Collections.sort(fileList, (a, b) -> {
                    boolean aIsDir = "true".equals(a.get("directory"));
                    boolean bIsDir = "true".equals(b.get("directory"));
                    if (aIsDir != bIsDir) {
                        return aIsDir ? -1 : 1;
                    }
                    return a.get("name").compareToIgnoreCase(b.get("name"));
                });

                // Tạo breadcrumbs
                List<Map<String, String>> breadcrumbs = createBreadcrumbs(fullPath);

                // Lưu thông tin vào session và request
                session.setAttribute("fileList", fileList);
                session.setAttribute("currentPath", fullPath);
                session.setAttribute("storage_server", storageServer);
                session.setAttribute("breadcrumbs", breadcrumbs);

                request.setAttribute("fileList", fileList);
                request.setAttribute("breadcrumbs", breadcrumbs);

                request.getRequestDispatcher("storage.jsp").forward(request, response);

            } finally {
                if (sshSession != null && sshSession.isConnected()) {
                    sshSession.disconnect();
                }
            }
        } catch (Exception e) {
            request.setAttribute("error", "Lỗi khi duyệt thư mục: " + e.getMessage());
            request.getRequestDispatcher("storage.jsp").forward(request, response);
        }
    }

    // Hàm lấy danh sách thư mục từ CSDL
    private List<Map<String, String>> getUserDirectoriesFromDB(String taiKhoan) {
        List<Map<String, String>> directories = new ArrayList<>();
        JDBC jdbc = new JDBC();

        try (Connection conn = jdbc.connect(); PreparedStatement stmt = conn.prepareStatement("SELECT thu_muc FROM nguoi_dung WHERE tai_khoan = ?")) {

            stmt.setString(1, taiKhoan);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String thuMuc = rs.getString("thu_muc");
                if (thuMuc != null && !thuMuc.isEmpty()) {
                    String[] paths = thuMuc.split(" ");
                    for (String path : paths) {
                        if (!path.trim().isEmpty()) {
                            Map<String, String> dirInfo = new HashMap<>();
                            // Phân tích đường dẫn để lấy thông tin server
                            String[] parts = path.split("/");
                            if (parts.length >= 4) {
                                // Chuyển đổi đường dẫn từ /home sang /data nếu cần
                                String newPath = path;
                                if (path.startsWith("/home/")) {
                                    newPath = "/data/" + String.join("/", Arrays.copyOfRange(parts, 2, parts.length));
                                }
                                dirInfo.put("path", newPath);
                                dirInfo.put("serverUser", parts[2]); // lấy tên user của server
                                dirInfo.put("directory", parts[parts.length - 1]); // lấy tên thư mục cuối
                                directories.add(dirInfo);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return directories;
    }

    // Hàm cập nhật thư mục trong CSDL
    protected void updateUserDirectories(String taiKhoan, String newPath) {
        JDBC jdbc = new JDBC();
        try (Connection conn = jdbc.connect()) {
            // Lấy danh sách thư mục hiện tại
            String sql = "SELECT thu_muc FROM nguoi_dung WHERE tai_khoan = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, taiKhoan);
            ResultSet rs = stmt.executeQuery();

            String currentPaths = "";
            if (rs.next()) {
                currentPaths = rs.getString("thu_muc");
                if (currentPaths == null) {
                    currentPaths = "";
                }
            }

            // Kiểm tra và thêm đường dẫn mới nếu chưa tồn tại
            if (!currentPaths.contains(newPath)) {
                String updatedPaths = currentPaths.isEmpty() ? newPath : currentPaths + " " + newPath;

                // Cập nhật vào CSDL
                sql = "UPDATE nguoi_dung SET thu_muc = ? WHERE tai_khoan = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, updatedPaths);
                stmt.setString(2, taiKhoan);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
        }
    }

    private void deleteItem(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        List<InforUser> users = (List<InforUser>) session.getAttribute("users");

        // Lấy thông tin cần thiết từ request
        String name = request.getParameter("name");
        String currentPath = request.getParameter("currentPath");
        String serverHost = request.getParameter("server");

        System.out.println("Dang xoa file: " + name + " o duong dan: " + currentPath + " tren server: " + serverHost);

        // Kiểm tra thông tin đầu vào
        if (name == null || currentPath == null || serverHost == null) {
            request.setAttribute("error", "Thiếu thông tin cần thiết để xóa!");
            request.setAttribute("path", request.getAttribute("path"));
            listFiles(request, response, users);
            return;
        }

        // Tìm server tương ứng
        InforUser targetServer = null;
        for (InforUser user : users) {
            if (user.getHost().equals(serverHost)) {
                targetServer = user;
                break;
            }
        }

        if (targetServer == null) {
            request.setAttribute("error", "Không tìm thấy máy chủ!");
            request.setAttribute("path", request.getAttribute("path"));
            listFiles(request, response, users);
            return;
        }

        try {
            // Kết nối SSH đến server
            Session sshSession = InforUser.connect(
                    targetServer.getHost(),
                    targetServer.getPort(),
                    targetServer.getUser(),
                    targetServer.getPassword());

            try {
                // Sử dụng tên file gốc thay vì tên đã chuẩn hóa
                String fullPath = currentPath + "/" + name;
                System.out.println("Duong dan day du: " + fullPath);

                // Kiểm tra xem item có tồn tại không
                String checkCommand = String.format("test -e \"%s\" && echo 'exists'", fullPath);
                String checkResult = executeSSHCommand(sshSession, checkCommand);
                System.out.println("Dang thuc hien lenh kiem tra xem item co ton tai khong: " + checkCommand);
                System.out.println("Ket qua kiem tra xem item co ton tai khong: " + checkResult);

                if (!checkResult.contains("exists")) {
                    System.out.println("Khong tim thay file voi ten: " + name);
                    request.setAttribute("error", "Không tìm thấy file/thư mục: " + name);

                    // Cập nhật đường dẫn và hiển thị danh sách file
                    NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");
                    if (nguoiDung != null) {
                        String basePath = "/data/" + targetServer.getUser() + "/" + nguoiDung.getTaiKhoan();
                        if (currentPath.equals(basePath)) {
                            request.removeAttribute("path");
                            session.removeAttribute("currentPath");
                        } else {
                            request.setAttribute("path", currentPath);
                            session.setAttribute("currentPath", currentPath);
                        }
                    }

                    listFiles(request, response, users);
                    return;
                }

                // Kiểm tra loại (file hay thư mục)
                String typeCommand = String.format("test -d \"%s\" && echo 'directory' || echo 'file'", fullPath);
                String itemType = executeSSHCommand(sshSession, typeCommand);
                System.out.println("Loai: " + itemType);

                //Lấy kích thước item trước khi xóa
                String duCommand = String.format("du -sb '%s' | awk '{print $1}'", fullPath);
                String sizeResult = executeSSHCommand(sshSession, duCommand);
                long itemSize = Long.parseLong(sizeResult);
                System.out.println("Size: " + itemSize);

                // Thực hiện xóa
                String deleteCommand;
                if (itemType.trim().equals("directory")) {
                    deleteCommand = String.format("rm -rf \"%s\"", fullPath);
                } else {
                    deleteCommand = String.format("rm \"%s\"", fullPath);
                }

                // Thực thi lệnh xóa
                System.out.println("Dang thuc hien lenh xoa: " + deleteCommand);
                executeCommand(sshSession, deleteCommand);

                // Kiểm tra xem đã xóa thành công chưa
                String verifyCommand = String.format("test -e \"%s\" && echo 'exists'", fullPath);
                String verifyResult = executeSSHCommand(sshSession, verifyCommand);
                System.out.println(
                        "Kiem tra xoa thanh cong: " + (verifyResult.contains("exists") ? "Chua xoa" : "Da xoa"));

                if (verifyResult.contains("exists")) {
                    String errorMsg = String.format("Không thể xóa %s '%s'",
                            (itemType.trim().equals("directory") ? "thư mục" : "tập tin"),
                            name);
                    request.setAttribute("error", errorMsg);
                } else {
                    String successMsg = String.format("Đã xóa thành công %s '%s'",
                            (itemType.trim().equals("directory") ? "thư mục" : "tập tin"),
                            name);
                    request.setAttribute("success", successMsg);
                    NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");
                    deletePathOnServer(fullPath, nguoiDung.getTaiKhoan(), itemSize);
                }
            } finally {
                if (sshSession != null && sshSession.isConnected()) {
                    sshSession.disconnect();
                }
            }

            // Kiểm tra xem có đang ở thư mục tổng không
            NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");
            if (nguoiDung != null) {
                String basePath = "/data/" + targetServer.getUser() + "/" + nguoiDung.getTaiKhoan();
                if (currentPath.equals(basePath)) {
                    // Nếu đang ở thư mục tổng, hiển thị danh sách từ tất cả server
                    request.removeAttribute("path");
                    session.removeAttribute("currentPath");
                } else {
                    // Nếu đang ở đường dẫn con, hiển thị danh sách của đường dẫn hiện tại
                    request.setAttribute("path", currentPath);
                    session.setAttribute("currentPath", currentPath);
                }
            }

            listFiles(request, response, users);
        } catch (Exception e) {
            System.out.println("LOI khi xoa file: " + e.getMessage());
            String errorMsg = String.format("Lỗi khi xóa '%s': %s", name, e.getMessage());
            request.setAttribute("error", errorMsg);

            // Xử lý hiển thị danh sách khi có lỗi
            NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");
            if (nguoiDung != null && targetServer != null) {
                String basePath = "/data/" + targetServer.getUser() + "/" + nguoiDung.getTaiKhoan();
                if (currentPath.equals(basePath)) {
                    request.removeAttribute("path");
                    session.removeAttribute("currentPath");
                } else {
                    request.setAttribute("path", currentPath);
                    session.setAttribute("currentPath", currentPath);
                }
            }

            listFiles(request, response, users);
        }
    }

    private void deleteMultipleItems(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        List<InforUser> users = (List<InforUser>) session.getAttribute("users");

        // Lấy đường dẫn hiện tại từ request hoặc session
        String currentPath = request.getParameter("currentPath");
        if (currentPath == null) {
            currentPath = (String) session.getAttribute("currentPath");
        }

        // Lấy danh sách items từ request
        String itemsJson = request.getParameter("items");
        if (itemsJson == null || itemsJson.isEmpty()) {
            request.setAttribute("error", "Không có mục nào được chọn để xóa!");
            request.setAttribute("path", currentPath);
            listFiles(request, response, users);
            return;
        }

        try {
            // Parse JSON string thành List<Map>
            Gson gson = new Gson();
            List<Map<String, String>> items = gson.fromJson(itemsJson, List.class);

            // Nhóm các items theo server
            Map<String, List<Map<String, String>>> itemsByServer = new HashMap<>();
            for (Map<String, String> item : items) {
                String serverHost = item.get("server");
                itemsByServer.computeIfAbsent(serverHost, k -> new ArrayList<>()).add(item);
            }

            int totalSuccessCount = 0;
            int totalFailCount = 0;
            StringBuilder errorMessages = new StringBuilder();

            // Xử lý từng server
            for (Map.Entry<String, List<Map<String, String>>> entry : itemsByServer.entrySet()) {
                String serverHost = entry.getKey();
                List<Map<String, String>> serverItems = entry.getValue();

                // Tìm server tương ứng
                InforUser targetServer = null;
                for (InforUser user : users) {
                    if (user.getHost().equals(serverHost)) {
                        targetServer = user;
                        break;
                    }
                }

                if (targetServer == null) {
                    errorMessages.append("Không tìm thấy máy chủ: ").append(serverHost).append("\n");
                    totalFailCount += serverItems.size();
                    continue;
                }

                // Kết nối SSH đến server
                Session sshSession = InforUser.connect(
                        targetServer.getHost(),
                        targetServer.getPort(),
                        targetServer.getUser(),
                        targetServer.getPassword());

                try {
                    int serverSuccessCount = 0;
                    int serverFailCount = 0;

                    // Xóa từng item trên server này
                    for (Map<String, String> item : serverItems) {
                        String fullPath = item.get("path") + "/" + item.get("name");
                        boolean isDirectory = "true".equals(item.get("directory"));

                        // Kiểm tra xem item có tồn tại không
                        String checkCommand = String.format("test -e '%s' && echo 'exists'", fullPath);
                        String checkResult = executeSSHCommand(sshSession, checkCommand);

                        if (!checkResult.contains("exists")) {
                            serverFailCount++;
                            continue;
                        }
                        //Lấy kích thước item trước khi xóa
                        String duCommand = String.format("du -sb '%s' | awk '{print $1}'", fullPath);
                        String sizeResult = executeSSHCommand(sshSession, duCommand);
                        long itemSize = Long.parseLong(sizeResult);
                        System.out.println("Size: " + itemSize);

                        // Thực hiện xóa
                        String deleteCommand;
                        if (isDirectory) {
                            deleteCommand = String.format("rm -rf '%s'", fullPath);
                        } else {
                            deleteCommand = String.format("rm '%s'", fullPath);
                        }

                        // Thực thi lệnh xóa
                        executeCommand(sshSession, deleteCommand);

                        // Kiểm tra xem đã xóa thành công chưa
                        String verifyCommand = String.format("test -e '%s' && echo 'exists'", fullPath);
                        String verifyResult = executeSSHCommand(sshSession, verifyCommand);

                        if (!verifyResult.contains("exists")) {
                            serverSuccessCount++;
                            NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");
                            deletePathOnServer(fullPath, nguoiDung.getTaiKhoan(), itemSize);
                        } else {
                            serverFailCount++;
                        }
                    }

                    totalSuccessCount += serverSuccessCount;
                    totalFailCount += serverFailCount;

                    if (serverFailCount > 0) {
                        // errorMessages.append("Lỗi khi xóa ").append(serverFailCount)
                        // .append(" mục trên máy chủ ").append(serverHost).append("\n");
                        errorMessages.append("Lỗi khi xóa ").append(serverFailCount).append(" mục").append("\n");
                    }

                } finally {
                    if (sshSession != null && sshSession.isConnected()) {
                        sshSession.disconnect();
                    }
                }
            }

            // Hiển thị thông báo kết quả
            if (totalFailCount == 0) {
                request.setAttribute("success", "Đã xóa thành công " + totalSuccessCount + " mục");
            } else {
                String warningMessage = "Đã xóa " + totalSuccessCount + " mục, " + totalFailCount + " mục thất bại";
                if (errorMessages.length() > 0) {
                    warningMessage += "\n" + errorMessages.toString();
                }
                request.setAttribute("warning", warningMessage);
            }

            // request.setAttribute("path", currentPath);
            // listFiles(request, response, users);
            // Nếu đang ở thư mục tổng (không có currentPath), hiển thị danh sách từ tất cả
            // các server
            if (currentPath == null || currentPath.trim().isEmpty()) {
                request.removeAttribute("path");
                session.removeAttribute("currentPath");
            } else {
                // Nếu đang ở đường dẫn cụ thể, hiển thị danh sách từ server đó
                request.setAttribute("path", currentPath);
                session.setAttribute("currentPath", currentPath);
            }

            listFiles(request, response, users); // 25/03/2025

        } catch (Exception e) {
            request.setAttribute("error", "Lỗi khi xóa nhiều mục: " + e.getMessage());
            request.setAttribute("path", currentPath);
            listFiles(request, response, users);
        }
    }

    private void downloadItem(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        List<InforUser> users = (List<InforUser>) session.getAttribute("users");

        // Lấy thông tin cần thiết từ request
        String name = request.getParameter("name");
        String currentPath = request.getParameter("currentPath");
        String serverHost = request.getParameter("server");
        String isDirectory = request.getParameter("isDirectory");

        // Kiểm tra thông tin đầu vào
        if (name == null || currentPath == null || serverHost == null) {
            request.setAttribute("error", "Thiếu thông tin cần thiết để tải xuống!");
            request.setAttribute("path", currentPath);
            listFiles(request, response, users);
            return;
        }

        // Tìm server tương ứng
        InforUser targetServer = null;
        for (InforUser user : users) {
            if (user.getHost().equals(serverHost)) {
                targetServer = user;
                break;
            }
        }

        if (targetServer == null) {
            request.setAttribute("error", "Không tìm thấy máy chủ!");
            request.setAttribute("path", currentPath);
            listFiles(request, response, users);
            return;
        }

        try {
            // Kết nối SSH đến server
            Session sshSession = InforUser.connect(
                    targetServer.getHost(),
                    targetServer.getPort(),
                    targetServer.getUser(),
                    targetServer.getPassword());

            try {
                // Xây dựng đường dẫn đầy đủ
                String fullPath = currentPath + "/" + name;

                // Kiểm tra xem item có tồn tại không
                String checkCommand = String.format("test -e '%s' && echo 'exists'", fullPath);
                String checkResult = executeSSHCommand(sshSession, checkCommand);
                System.out.println("Dang thuc hien lenh kiem tra xem item co ton tai khong: " + checkCommand);
                System.out.println("Ket qua kiem tra xem item co ton tai khong: " + checkResult);
                if (!checkResult.contains("exists")) {
                    request.setAttribute("error", "Không tìm thấy file/thư mục: " + name);
                    request.setAttribute("path", currentPath);
                    listFiles(request, response, users);
                    return;
                }

                // Nếu là thư mục, nén lại trước khi tải
                if ("true".equals(isDirectory)) {
                    String tempZipName = name + ".zip";
                    String zipCommand = String.format("cd '%s' && zip -r '%s' '%s'",
                            currentPath, tempZipName, name);
                    executeCommand(sshSession, zipCommand);
                    fullPath = currentPath + "/" + tempZipName;
                    name = tempZipName;
                }

                // Thiết lập response headers cho việc download
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");

                // Tạo kênh SFTP để tải file
                ChannelSftp channelSftp = (ChannelSftp) sshSession.openChannel("sftp");
                channelSftp.connect();

                try {
                    // Lấy input stream từ file trên server
                    InputStream inputStream = channelSftp.get(fullPath);
                    OutputStream outputStream = response.getOutputStream();

                    // Copy dữ liệu từ input stream sang output stream
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    // Đóng các streams
                    inputStream.close();
                    outputStream.flush();
                    outputStream.close();

                    // Nếu là thư mục đã nén, xóa file zip tạm
                    if ("true".equals(isDirectory)) {
                        String cleanupCommand = String.format("rm '%s'", fullPath);
                        executeCommand(sshSession, cleanupCommand);
                    }

                } finally {
                    channelSftp.disconnect();
                }

            } finally {
                if (sshSession != null && sshSession.isConnected()) {
                    sshSession.disconnect();
                }
            }

        } catch (Exception e) {
            request.setAttribute("error", "Lỗi khi tải xuống: " + e.getMessage());
            // Xử lý hiển thị danh sách khi có lỗi
            NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");
            if (nguoiDung != null) {
                String basePath = "/data/" + targetServer.getUser() + "/" + nguoiDung.getTaiKhoan();
                if (currentPath.equals(basePath)) {
                    request.removeAttribute("path");
                    session.removeAttribute("currentPath");
                } else {
                    request.setAttribute("path", currentPath);
                    session.setAttribute("currentPath", currentPath);
                }
            }
            listFiles(request, response, users);
        }
    }

    private void uploadFile(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        List<InforUser> users = (List<InforUser>) session.getAttribute("users");
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");

        System.out.println("=== BAT DAU XU LY UPLOAD FILE ===");
        System.out.println("Nguoi dung: " + (nguoiDung != null ? nguoiDung.getTaiKhoan() : "khong tim thay"));

        if (nguoiDung == null) {
            System.out.println("LOI: Khong tim thay thong tin nguoi dung");
            request.setAttribute("error", "Không tìm thấy thông tin người dùng.");
            request.getRequestDispatcher("storage.jsp").forward(request, response);
            return;
        }

        // Tính tổng dung lượng của các file sẽ upload
        long totalUploadSize = 0;
        for (Part part : request.getParts()) {
            if ("files".equals(part.getName()) && part.getSize() > 0) {
                totalUploadSize += part.getSize();
            }
        }
        System.out.println("Tong dung luong file se upload: " + formatSize(totalUploadSize));

        // Kiểm tra giới hạn dung lượng
        if (!checkStorageLimit(session, totalUploadSize)) {
            System.out.println("LOI: Vuot qua dung luong cho phep");
            request.setAttribute("error",
                    "Đã vượt quá dung lượng cho phép! Vui lòng xóa bớt dữ liệu hoặc liên hệ admin.");
            request.setAttribute("path", request.getParameter("currentPath"));
            listFiles(request, response, users);
            return;
        }

        String currentPath = request.getParameter("currentPath");
        String targetServer = request.getParameter("server");
        System.out.println("Duong dan hien tai: " + currentPath);
        System.out.println("Server muc tieu: " + targetServer);

        // Tìm server đích
        InforUser targetInforUser = null;
        if (targetServer != null && !targetServer.isEmpty()) {
            for (InforUser user : users) {
                if (user.getHost().equals(targetServer)) {
                    targetInforUser = user;
                    break;
                }
            }
        }

        if (targetInforUser == null) {
            System.out.println("Khong tim thay server muc tieu, dang tim server phu hop...");
            targetInforUser = (InforUser) session.getAttribute("storage_server");
            if (targetInforUser == null) {
                targetInforUser = selectStorageServer(users);
                if (targetInforUser != null) {
                    session.setAttribute("storage_server", targetInforUser);
                    System.out.println("Da chon server: " + targetInforUser.getHost());
                } else {
                    System.out.println("LOI: Khong tim thay server phu hop");
                    request.setAttribute("error", "Không tìm thấy máy chủ phù hợp!");
                    request.setAttribute("path", currentPath);
                    listFiles(request, response, users);
                    return;
                }
            }
        }

        // Kiểm tra dung lượng trống của server, không đủ tìm server mới phù hợp
        String targetPath = (currentPath != null && !currentPath.trim().isEmpty())
                ? currentPath
                : "/data/" + targetInforUser.getUser() + "/" + nguoiDung.getTaiKhoan();
        System.out.println("Duong dan muc tieu: " + targetPath);

        if (!checkServerSpace(targetInforUser, targetPath, totalUploadSize)) {
            System.out.println("Server hien tai khong du dung luong, dang tim server khac...");
            // Tìm server có dung lượng trống phù hợp nhất
            InforUser bestServer = null;
            long bestSpaceDiff = Long.MAX_VALUE;

            for (InforUser server : users) {
                if (!server.getHost().equals(targetInforUser.getHost())) {
                    try {
                        Session sshSession = InforUser.connect(server.getHost(), server.getPort(),
                                server.getUser(), server.getPassword());

                        // Kiểm tra dung lượng trống của server
                        String command = String.format("df -B1 '/data/%s/%s' | tail -1 | awk '{print $4}'",
                                server.getUser(), nguoiDung.getTaiKhoan());
                        String result = executeSSHCommand(sshSession, command);
                        long availableSpace = Long.parseLong(result.trim());

                        sshSession.disconnect();

                        System.out.println(
                                "Server " + server.getHost() + " co dung luong trong: " + formatSize(availableSpace));

                        // Nếu server có đủ dung lượng
                        if (availableSpace >= totalUploadSize) {
                            // Tính chênh lệch giữa dung lượng trống và dung lượng cần thiết
                            long spaceDiff = availableSpace - totalUploadSize;

                            // Nếu chênh lệch nhỏ hơn, cập nhật server tốt nhất
                            if (spaceDiff < bestSpaceDiff) {
                                bestSpaceDiff = spaceDiff;
                                bestServer = server;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(
                                "LOI khi kiem tra dung luong server " + server.getHost() + ": " + e.getMessage());
                    }
                }
            }

            if (bestServer != null) {
                targetInforUser = bestServer;
                // Cập nhật đường dẫn mục tiêu cho server mới
                targetPath = targetPath.replaceFirst("/data/[^/]+/", "/data/" + bestServer.getUser() + "/");
                System.out.println("Da chon server " + bestServer.getHost() + " voi dung luong trong: "
                        + formatSize(bestSpaceDiff + totalUploadSize));
                System.out.println("Duong dan muc tieu moi: " + targetPath);
            } else {
                System.out.println("LOI: Khong co server nao co du dung luong trong");
                request.setAttribute("error", "Không có server nào có đủ dung lượng trống để upload file này!");
                request.setAttribute("path", currentPath);
                listFiles(request, response, users);
                return;
            }
        }

        // Tiếp tục với server đã chọn
        try {
            long totalSizeSuccess = 0;
            // Lấy danh sách file từ request
            List<Part> fileParts = request.getParts().stream()
                    .filter(part -> "files".equals(part.getName()) && part.getSize() > 0)
                    .collect(Collectors.toList());

            if (fileParts.isEmpty()) {
                System.out.println("LOI: Khong co file nao duoc chon");
                request.setAttribute("error", "Không có file nào được chọn!");
                request.setAttribute("path", currentPath);
                listFiles(request, response, users);
                return;
            }

            System.out.println("So luong file se upload: " + fileParts.size());

            // Kết nối SFTP một lần duy nhất
            Session sshSession = InforUser.connect(
                    targetInforUser.getHost(),
                    targetInforUser.getPort(),
                    targetInforUser.getUser(),
                    targetInforUser.getPassword());

            ChannelSftp channelSftp = (ChannelSftp) sshSession.openChannel("sftp");
            channelSftp.connect();

            try {
                // Tạo thư mục đích nếu chưa tồn tại
                String mkdirCommand = String.format("mkdir -p '%s'", targetPath);
                executeCommand(sshSession, mkdirCommand);
                System.out.println("Da tao thu muc dich: " + targetPath);

                String fileName = null;
                int successCount = 0;
                int failCount = 0;
                StringBuilder errorMessages = new StringBuilder();

                // Upload từng file
                for (Part filePart : fileParts) {
                    fileName = filePart.getSubmittedFileName();
                    if (fileName == null || fileName.isEmpty()) {
                        failCount++;
                        errorMessages.append("Ten file khong hop le\n");
                        continue;
                    }

                    System.out.println("Dang xu ly file: " + fileName);

                    // Chuẩn hóa tên file để xử lý ký tự Unicode
                    String safeFileName = sanitizeFileName(fileName);
                    System.out.println("Ten file sau khi chuan hoa: " + safeFileName);

                    // Kiểm tra xem file đã tồn tại trên bất kỳ server nào chưa sau path_base /data/<tên server>/<tên người dùng>
                    String basePath = "/data/";
                    boolean fileExists = false;
                    String existingServer = "";

                    for (InforUser user : users) {
                        try {
                            Session checkSession = InforUser.connect(user.getHost(), user.getPort(),
                                    user.getUser(), user.getPassword());

                            String checkPath = (currentPath != null && !currentPath.isEmpty())
                                    ? currentPath + "/" + safeFileName
                                    : basePath + user.getUser() + "/" + nguoiDung.getTaiKhoan() + "/" + safeFileName;

                            // Kiem tra tren CSDL
                            if (checkPath.equals(basePath + user.getUser() + "/" + nguoiDung.getTaiKhoan() + "/" + safeFileName)) {
                                boolean checkPathRelative = checkFileNameExistsInDB(safeFileName, nguoiDung.getTaiKhoan());
                                if (checkPathRelative) {
                                    System.out.println("Ket qua kiem tra " + checkPathRelative);
                                    System.out.println("Tap tin " + safeFileName + " da ton tai");
                                    fileExists = true;
                                    existingServer = "CSDL path_on_server";
                                    checkSession.disconnect();
                                    break;
                                }
                            }

                            String checkCommand = String.format("test -f '%s' && echo 'exists'", checkPath);
                            String checkResult = executeSSHCommand(checkSession, checkCommand);

                            if (checkResult.contains("exists")) {
                                fileExists = true;
                                existingServer = user.getHost();
                                checkSession.disconnect();
                                break;
                            }

                            checkSession.disconnect();
                        } catch (Exception e) {
                            System.out.println("LOI khi kiem tra server " + user.getHost() + ": " + e.getMessage());
                        }
                    }

                    if (fileExists) {
                        failCount++;
                        errorMessages.append("Tap tin '").append(safeFileName).append("' da ton tai").append("\n");
                        System.out.println("LOI: File " + safeFileName + " da ton tai tren server " + existingServer);
                        continue;
                    }

                    try {
                        // Upload file trực tiếp từ input stream
                        channelSftp.cd(targetPath);
                        channelSftp.put(filePart.getInputStream(), safeFileName);

                        // Đặt quyền cho file
                        String chmodCommand = String.format("chmod 644 '%s/%s'", targetPath, safeFileName);
                        executeCommand(sshSession, chmodCommand);

                        successCount++;
                        totalSizeSuccess += filePart.getSize();
                        // Lưu đường dẫn vào CSDL
                        String fullPath = targetPath + "/" + safeFileName;
                        savePathOnServer(fullPath, targetInforUser.getUser(), nguoiDung.getTaiKhoan(), false, filePart.getSize());

                        System.out.println("Thanh cong: Da upload file " + safeFileName);
                    } catch (JSchException | SftpException | IOException e) {
                        failCount++;
                        errorMessages.append("Loi khi tai len file '").append(safeFileName).append("': ")
                                .append(e.getMessage()).append("\n");
                        System.out.println("LOI khi upload file " + safeFileName + ": " + e.getMessage());
                    }
                }

                // Hiển thị thông báo kết quả
                if (failCount == 0) {
                    System.out.println("=== KET QUA: Da tai len thanh cong " + successCount + " file ===");
                    request.setAttribute("success", "Đã tải lên thành công " + successCount + " file");
                } else {
                    String warningMessage = "Đã tải lên " + successCount + " file, " + failCount + " file thất bại";
                    if (errorMessages.length() > 0) {
                        warningMessage += "\n" + errorMessages.toString();
                    }
                    System.out.println("=== KET QUA: " + warningMessage + " ===");
                    request.setAttribute("warning", warningMessage);
                }

            } finally {
                channelSftp.disconnect();
                sshSession.disconnect();
            }

            if (!targetInforUser.getHost().equals(targetServer)) {
                System.out.println("Di chuyen du lieu tu server cu sang server moi...");
                InforUser originalServer = null;
                for (InforUser user : users) {
                    if (user.getHost().equals(targetServer)) {
                        originalServer = user;
                        break;
                    }
                }

                if (originalServer != null) {
                    Session originalSshSession = InforUser.connect(
                            originalServer.getHost(),
                            originalServer.getPort(),
                            originalServer.getUser(),
                            originalServer.getPassword());

                    try {
                        // Di chuyển dữ liệu từ server cũ sang server mới
                        moveDataToNewServer(originalSshSession, currentPath, targetInforUser.getHost(), targetPath,
                                request);
                        System.out.println("Da di chuyen du lieu tu server cu sang server moi");

                        // Cập nhật session với server mới
                        session.setAttribute("storage_server", targetInforUser);

                        // Cập nhật đường dẫn hiện tại với đường dẫn mới trên server mới
                        String newPath = targetPath;
                        request.setAttribute("path", newPath);
                        session.setAttribute("currentPath", newPath);
                        System.out.println("Da cap nhat duong dan moi: " + newPath);

                        // Lưu đường dẫn vào CSDL
                        renamePathOnServer(currentPath, newPath, nguoiDung.getTaiKhoan());
                        savePathOnServer(newPath, targetInforUser.getUser(), nguoiDung.getTaiKhoan(), true, 0);

                        // Hiển thị danh sách thư mục tại đường dẫn mới
                        listFiles(request, response, users);
                        return;
                    } finally {
                        originalSshSession.disconnect();
                    }
                }
            }

            // Cập nhật session với server hiện tại
            session.setAttribute("storage_server", targetInforUser);

            // Cập nhật đường dẫn hiện tại
            if (currentPath != null && !currentPath.trim().isEmpty()) {
                request.setAttribute("path", currentPath);
                session.setAttribute("currentPath", currentPath);
            } else {
                request.removeAttribute("path");
                session.removeAttribute("currentPath");
            }

            listFiles(request, response, users);

        } catch (Exception e) {
            System.out.println("=== LOI: " + e.getMessage() + " ===");
            request.setAttribute("error", "Lỗi khi tải lên file: " + e.getMessage());
            request.setAttribute("path", currentPath);
            listFiles(request, response, users);
        }
    }

    protected void uploadFolder(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        List<InforUser> users = (List<InforUser>) session.getAttribute("users");
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");

        System.out.println("=== BAT DAU XU LY UPLOAD THU MUC ===");
        System.out.println("Nguoi dung: " + (nguoiDung != null ? nguoiDung.getTaiKhoan() : "khong tim thay"));

        if (nguoiDung == null) {
            System.out.println("LOI: Khong tim thay thong tin nguoi dung trong session");
            request.setAttribute("error", "Không tìm thấy thông tin người dùng trong session.");
            request.getRequestDispatcher("storage.jsp").forward(request, response);
            return;
        }

        // Tính tổng dung lượng của thư mục sẽ upload
        long totalUploadSize = 0;
        long totalSizeSuccess = 0;
        for (Part part : request.getParts()) {
            if ("folder".equals(part.getName()) && part.getSize() > 0) {
                totalUploadSize += part.getSize();
            }
        }
        System.out.println("Tong dung luong thu muc se upload: " + formatSize(totalUploadSize));

        // Kiểm tra giới hạn dung lượng người dùng
        if (!checkStorageLimit(session, totalUploadSize)) {
            System.out.println("LOI: Vuot qua dung luong cho phep");
            request.setAttribute("error",
                    "Đã vượt quá dung lượng cho phép! Vui lòng xóa bớt dữ liệu hoặc liên hệ admin.");
            request.setAttribute("path", request.getParameter("currentPath"));
            listFiles(request, response, users);
            return;
        }

        String currentPath = request.getParameter("currentPath");
        String targetServer = request.getParameter("server");
        if (currentPath == null) {
            targetServer = selectStorageServer(users).getHost();
//            currentPath = "/data/" + targetServer +"/"+nguoiDung.getTaiKhoan();
        }
        System.out.println("Duong dan hien tai: " + currentPath);
        System.out.println("Server muc tieu: " + targetServer);

        // Tìm máy chủ đích
        InforUser targetInforUser = null;
        if (targetServer != null && !targetServer.isEmpty()) {
            for (InforUser user : users) {
                if (user.getHost().equals(targetServer)) {
                    targetInforUser = user;
                    break;
                }
            }
        }

        if (targetInforUser == null) {
            System.out.println("Khong tim thay server muc tieu, dang tim server phu hop...");
            targetInforUser = selectStorageServer(users);
            if (targetInforUser == null) {
                System.out.println("LOI: Khong tim thay server phu hop");
                request.setAttribute("error", "Không tìm thấy máy chủ phù hợp!");
                request.setAttribute("path", currentPath);
                listFiles(request, response, users);
                return;
            }
            System.out.println("Da chon server: " + targetInforUser.getHost());
        }

        // Kiểm tra dung lượng trống của server
        String targetPath = (currentPath != null && !currentPath.trim().isEmpty())
                ? currentPath
                : "/data/" + targetInforUser.getUser() + "/" + nguoiDung.getTaiKhoan();
        System.out.println("Duong dan muc tieu: " + targetPath);

        if (!checkServerSpace(targetInforUser, targetPath, totalUploadSize)) {
            System.out.println("Server hien tai khong du dung luong, dang tim server khac...");
            // Tìm server có dung lượng trống phù hợp nhất
            InforUser bestServer = null;
            long bestSpaceDiff = Long.MAX_VALUE;

            for (InforUser server : users) {
                if (!server.getHost().equals(targetInforUser.getHost())) {
                    try {
                        Session sshSession = InforUser.connect(server.getHost(), server.getPort(),
                                server.getUser(), server.getPassword());

                        // Kiểm tra dung lượng trống của server
                        String command = String.format("df -B1 '/data/%s/%s' | tail -1 | awk '{print $4}'",
                                server.getUser(), nguoiDung.getTaiKhoan());
                        String result = executeSSHCommand(sshSession, command);
                        long availableSpace = Long.parseLong(result.trim());

                        sshSession.disconnect();

                        System.out.println(
                                "Server " + server.getHost() + " co dung luong trong: " + formatSize(availableSpace));

                        // Nếu server có đủ dung lượng
                        if (availableSpace >= totalUploadSize) {
                            // Tính chênh lệch giữa dung lượng trống và dung lượng cần thiết
                            long spaceDiff = availableSpace - totalUploadSize;

                            // Nếu chênh lệch nhỏ hơn, cập nhật server tốt nhất
                            if (spaceDiff < bestSpaceDiff) {
                                bestSpaceDiff = spaceDiff;
                                bestServer = server;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(
                                "LOI khi kiem tra dung luong server " + server.getHost() + ": " + e.getMessage());
                    }
                }
            }

            if (bestServer != null) {
                targetInforUser = bestServer;
                // Cập nhật đường dẫn mục tiêu cho server mới
                targetPath = targetPath.replaceFirst("/data/[^/]+/", "/data/" + bestServer.getUser() + "/");
                System.out.println("Da chon server " + bestServer.getHost() + " voi dung luong trong: "
                        + formatSize(bestSpaceDiff + totalUploadSize));
                System.out.println("Duong dan muc tieu moi: " + targetPath);
            } else {
                System.out.println("LOI: Khong co server nao co du dung luong trong");
                request.setAttribute("error", "Không có server nào có đủ dung lượng trống để upload thư mục này!");
                request.setAttribute("path", currentPath);
                listFiles(request, response, users);
                return;
            }
        }

        // Tiếp tục xử lý upload với server đã chọn
        try {
            // Lấy danh sách file từ request
            List<Part> fileParts = request.getParts().stream()
                    .filter(part -> "folder".equals(part.getName()) && part.getSize() > 0)
                    .collect(Collectors.toList());

            if (fileParts.isEmpty()) {
                System.out.println("LOI: Khong co thu muc nao duoc chon");
                request.setAttribute("error", "Không có thư mục nào được chọn!");
                request.setAttribute("path", currentPath);
                listFiles(request, response, users);
                return;
            }

            // Xác định tên thư mục từ file đầu tiên
            String firstFileName = fileParts.get(0).getSubmittedFileName();
            String folderName = firstFileName.split("/")[0];

            // Chuẩn hóa tên thư mục để xử lý ký tự Unicode
            String safeFolderName = sanitizeFileName(folderName);
            System.out.println("Ten thu muc se upload: " + safeFolderName);
            System.out.println("So luong file trong thu muc: " + fileParts.size());

            // Kiểm tra xem thư mục đã tồn tại trên bất kỳ server nào chưa
            String basePath = "/data/";
            boolean folderExists = false;
            String existingServer = "";

            for (InforUser user : users) {
                try {
                    Session checkSession = InforUser.connect(user.getHost(), user.getPort(),
                            user.getUser(), user.getPassword());

                    String checkPath = (currentPath != null && !currentPath.isEmpty())
                            ? currentPath + "/" + safeFolderName
                            : basePath + user.getUser() + "/" + nguoiDung.getTaiKhoan() + "/" + safeFolderName;

                    // Kiem tra tren CSDL
                    if (checkPath.equals(basePath + user.getUser() + "/" + nguoiDung.getTaiKhoan() + "/" + safeFolderName)) {
                        boolean checkPathRelative = checkFileNameExistsInDB(safeFolderName, nguoiDung.getTaiKhoan());
                        if (checkPathRelative) {
                            System.out.println("Ket qua kiem tra " + checkPathRelative);
                            System.out.println("Thu muc " + safeFolderName + " da ton tai");
                            folderExists = true;
                            existingServer = "CSDL path_on_server";
                            checkSession.disconnect();
                            break;
                        }
                    }

                    String checkCommand = String.format("test -d '%s' && echo 'exists'", checkPath);
                    String checkResult = executeSSHCommand(checkSession, checkCommand);

                    if (checkResult.contains("exists")) {
                        folderExists = true;
                        existingServer = user.getHost();
                        checkSession.disconnect();
                        break;
                    }

                    checkSession.disconnect();
                } catch (Exception e) {
                    System.out.println("LOI khi kiem tra server " + user.getHost() + ": " + e.getMessage());
                }
            }

            if (folderExists) {
                System.out.println("LOI: Thu muc '" + safeFolderName + "' da ton tai tren may chu " + existingServer);
                request.setAttribute("error", "Thư mục '" + safeFolderName + "' đã tồn tại");
                request.setAttribute("path", currentPath);
                listFiles(request, response, users);
                return;
            }

            // Kết nối SFTP một lần duy nhất
            Session sshSession = InforUser.connect(
                    targetInforUser.getHost(),
                    targetInforUser.getPort(),
                    targetInforUser.getUser(),
                    targetInforUser.getPassword());

            ChannelSftp channelSftp = (ChannelSftp) sshSession.openChannel("sftp");
            channelSftp.connect();

            try {
                // Tạo thư mục đích
                String targetFolderPath = targetPath + "/" + safeFolderName;
                String mkdirCommand = String.format("mkdir -p '%s'", targetFolderPath);
                executeCommand(sshSession, mkdirCommand);
                System.out.println("Da tao thu muc dich: " + targetFolderPath);

                int successCount = 0;
                int failCount = 0;
                StringBuilder errorMessages = new StringBuilder();

                // Upload từng file trong thư mục
                for (Part part : fileParts) {
                    String fileName = part.getSubmittedFileName();
                    if (fileName == null || fileName.isEmpty()) {
                        continue;
                    }

                    System.out.println("Dang xu ly file: " + fileName);

                    // Xử lý đường dẫn tương đối và chuẩn hóa tên file
                    String relativePath = fileName.substring(folderName.length());
                    if (relativePath.startsWith("/")) {
                        relativePath = relativePath.substring(1);
                    }

                    // Chuẩn hóa đường dẫn tương đối
                    String[] pathParts = relativePath.split("/");
                    StringBuilder normalizedPath = new StringBuilder();

                    for (int i = 0; i < pathParts.length; i++) {
                        String partString = pathParts[i];
                        if (partString.isEmpty()) {
                            continue;
                        }

                        // Chuẩn hóa từng phần của đường dẫn
                        String normalizedPart = sanitizeFileName(partString);

                        if (i > 0) {
                            normalizedPath.append("/");
                        }
                        normalizedPath.append(normalizedPart);
                    }

                    String safeRelativePath = normalizedPath.toString();
                    System.out.println("Duong dan tuong doi sau khi chuan hoa: " + safeRelativePath);

                    String fullFilePath = targetFolderPath + "/" + safeRelativePath;
                    String parentDir = fullFilePath.substring(0, Math.max(fullFilePath.lastIndexOf('/'), 0));
                    if (parentDir.isEmpty()) {
                        parentDir = targetFolderPath;
                    }

                    try {
                        // Tạo thư mục cha nếu chưa tồn tại
                        String mkdirSubCommand = String.format("mkdir -p '%s'", parentDir);
                        executeCommand(sshSession, mkdirSubCommand);
                        // Upload file
                        try (InputStream inputStream = part.getInputStream()) {
                            channelSftp.put(inputStream, fullFilePath);
                        }

                        // Đặt quyền file
                        String chmodCommand = String.format("chmod 644 '%s'", fullFilePath);
                        executeCommand(sshSession, chmodCommand);

                        successCount++;
                        totalSizeSuccess += part.getSize();

                        System.out.println("Thanh cong: Da upload file " + fileName);
                    } catch (JSchException | SftpException | IOException e) {
                        failCount++;
                        errorMessages.append("Loi khi tai len file '").append(fileName).append("': ")
                                .append(e.getMessage()).append("\n");
                        System.out.println("LOI khi upload file " + fileName + ": " + e.getMessage());
                    }
                }

                // Đặt quyền cho thư mục và các thư mục con
                String chmodDirCommand = String.format("chmod -R 755 '%s'", targetFolderPath);
                executeCommand(sshSession, chmodDirCommand);
                System.out.println("Da cap nhat quyen cho thu muc va cac thu muc con");

                // Nếu đang upload lên server khác
                if (!targetInforUser.getHost().equals(targetServer)) {
                    System.out.println("Di chuyen du lieu den server khac...");
                    InforUser originalServer = null;
                    for (InforUser user : users) {
                        if (user.getHost().equals(targetServer)) {
                            originalServer = user;
                            break;
                        }
                    }

                    if (originalServer != null) {
                        Session originalSshSession = InforUser.connect(
                                originalServer.getHost(),
                                originalServer.getPort(),
                                originalServer.getUser(),
                                originalServer.getPassword());

                        try {
                            // Di chuyển dữ liệu từ server gốc sang server mới
                            String originalPath = currentPath + "/" + safeFolderName;
                            moveDataToNewServer(originalSshSession, originalPath, targetInforUser.getHost(),
                                    targetFolderPath, request);
                            System.out.println("Da di chuyen thu muc: " + safeFolderName);

                            // Cập nhật session với server mới
                            session.setAttribute("storage_server", targetInforUser);

                            // Cập nhật đường dẫn hiện tại với đường dẫn mới trên server mới
                            String newPath = targetFolderPath;
                            request.setAttribute("path", newPath);
                            session.setAttribute("currentPath", newPath);
                            System.out.println("Da cap nhat duong dan moi: " + newPath);

                            // Lưu đường dẫn vào CSDL
                            renamePathOnServer(originalPath, newPath, nguoiDung.getTaiKhoan());
                            savePathOnServer(newPath, targetInforUser.getUser(), nguoiDung.getTaiKhoan(), true, totalSizeSuccess);

                            // Hiển thị danh sách thư mục tại đường dẫn mới
                            listFiles(request, response, users);
                            return;
                        } finally {
                            originalSshSession.disconnect();
                        }
                    }
                }

                // Lưu đường dẫn vào CSDL
                savePathOnServer(targetFolderPath, targetInforUser.getUser(), nguoiDung.getTaiKhoan(), true, totalSizeSuccess);
                // Hiển thị thông báo kết quả
                if (failCount == 0) {
                    System.out.println("=== KET QUA: Da tai len thanh cong " + successCount + " file trong thu muc "
                            + safeFolderName + " ===");
                    request.setAttribute("success",
                            "Đã tải lên thành công " + successCount + " file trong thư mục " + safeFolderName);
                } else {
                    String warningMessage = "Đã tải lên " + successCount + " file, " + failCount + " file thất bại\n"
                            + errorMessages;
                    System.out.println("=== KET QUA: " + warningMessage + " ===");
                    request.setAttribute("warning", warningMessage);
                }

            } finally {
                channelSftp.disconnect();
                sshSession.disconnect();
            }

            // Cập nhật session với server hiện tại
            session.setAttribute("storage_server", targetInforUser);

            // Nếu đang ở thư mục tổng (không có currentPath), hiển thị danh sách từ tất cả
            // các server
            if (currentPath == null || currentPath.trim().isEmpty()) {
                request.removeAttribute("path");
                session.removeAttribute("currentPath");
            } else {
                // Nếu đang ở đường dẫn cụ thể, hiển thị danh sách từ server đó
                request.setAttribute("path", currentPath);
                session.setAttribute("currentPath", currentPath);
            }
            listFiles(request, response, users);

        } catch (Exception e) {
            System.out.println("=== LOI: " + e.getMessage() + " ===");
            request.setAttribute("error", "Lỗi khi tải lên thư mục: " + e.getMessage());
            request.setAttribute("path", currentPath);
            listFiles(request, response, users);
        }
    }

    private boolean checkStorageLimit(HttpSession session, long additionalSize) {
        Map<String, Object> storageInfo = (Map<String, Object>) session.getAttribute("storageInfo");

        if (storageInfo != null) {
            double totalUsedMB = (double) storageInfo.get("totalUsed");
            double storageLimitMB = (double) storageInfo.get("storageLimit");
            double additionalSizeMB = additionalSize / (1024.0 * 1024.0);
            System.out.println("=== BAT DAU KIEM TRA DUNG LUONG ===");
            System.out
                    .println("Dung luong nguoi dung dang su dung: " + Math.round(totalUsedMB * 100.0) / 100.0 + " MB");
            System.out.println("Tong dung luong sau khi them: "
                    + Math.round((totalUsedMB + additionalSizeMB) * 100.0) / 100.0 + " MB");
            System.out.println("Dung luong gioi han: " + Math.round(storageLimitMB * 100.0) / 100.0 + " MB");
            System.out.println("=== KET QUA: "
                    + ((totalUsedMB + additionalSizeMB) <= storageLimitMB ? "Du dung luong" : "Vuot qua dung luong")
                    + " ===");
            return (totalUsedMB + additionalSizeMB) <= storageLimitMB;

        }

        return false;
    }

    private boolean checkServerSpace(InforUser server, String path, long requiredSize) {
        try {
            Session sshSession = InforUser.connect(server.getHost(), server.getPort(),
                    server.getUser(), server.getPassword());

            // Kiểm tra dung lượng trống của server
            String command = String.format("df -B1 '%s' | tail -1 | awk '{print $4}'", path);
            String result = executeSSHCommand(sshSession, command);
            long availableSpace = Long.parseLong(result.trim());

            sshSession.disconnect();
            return availableSpace >= requiredSize;
        } catch (Exception e) {
            System.err.println("Lỗi kiểm tra dung lượng server " + server.getHost() + ": " + e.getMessage());
            return false;
        }
    }

    private void moveDataToNewServer(Session sourceSession, String sourcePath, String targetServer, String targetPath,
            HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        List<InforUser> users = (List<InforUser>) session.getAttribute("users");

        System.out.println("=== BAT DAU DI CHUYEN DU LIEU ===");
        System.out.println("Duong dan nguon: " + sourcePath);
        System.out.println("Server dich: " + targetServer);
        System.out.println("Duong dan dich: " + targetPath);

        // Tìm thông tin người dùng của server đích
        InforUser targetInforUser = null;
        for (InforUser user : users) {
            if (user.getHost().equals(targetServer)) {
                targetInforUser = user;
                break;
            }
        }

        if (targetInforUser == null) {
            String errorMsg = "Khong tim thay thong tin nguoi dung cua server dich";
            System.out.println("LOI: " + errorMsg);
            throw new Exception(errorMsg);
        }

        // Chuẩn hóa đường dẫn nguồn và đích
        sourcePath = sourcePath.replaceAll("//+", "/");
        targetPath = targetPath.replaceAll("//+", "/");

        // Kết nối đến server đích
        Session targetSession = null;
        try {
            System.out.println("Dang ket noi den server dich...");
            targetSession = InforUser.connect(targetServer, targetInforUser.getPort(), targetInforUser.getUser(),
                    targetInforUser.getPassword());
            System.out.println("Ket noi den server dich thanh cong");

            // Tạo thư mục cha trên server đích
            String parentPath = targetPath.substring(0, targetPath.lastIndexOf('/'));
            System.out.println("Dang tao thu muc cha: " + parentPath);
            String mkdirCommand = String.format("mkdir -p '%s'", parentPath);
            executeCommand(targetSession, mkdirCommand);
            System.out.println("Da tao thu muc cha thanh cong");

            // Tạo thư mục đích nếu chưa tồn tại
            System.out.println("Dang tao thu muc dich: " + targetPath);
            mkdirCommand = String.format("mkdir -p '%s'", targetPath);
            executeCommand(targetSession, mkdirCommand);
            System.out.println("Da tao thu muc dich thanh cong");

            // Kiểm tra xem thư mục nguồn có tồn tại không
            System.out.println("Dang kiem tra thu muc nguon...");
            String checkSourceCommand = String.format("test -d '%s' && echo 'exists'", sourcePath);
            String checkSourceResult = executeSSHCommand(sourceSession, checkSourceCommand);
            if (!checkSourceResult.contains("exists")) {
                System.out.println("Thu muc nguon chua ton tai, dang tao...");
                mkdirCommand = String.format("mkdir -p '%s'", sourcePath);
                executeCommand(sourceSession, mkdirCommand);
                System.out.println("Da tao thu muc nguon thanh cong");
            } else {
                System.out.println("Thu muc nguon da ton tai");
            }

            // Tách chuỗi theo dấu "/"
            String[] partsSrc = sourcePath.split("/");
            String[] partsTarget = targetPath.split("/");
            // Kiểm tra xem có đủ phần tử không
            if (partsSrc.length >= 5 && partsTarget.length >= 5) { // Vì phần tử đầu tiên là rỗng do dấu "/" đầu
                sourcePath = "/" + String.join("/", partsSrc[1], partsSrc[2], partsSrc[3], partsSrc[4]);
                targetPath = "/" + String.join("/", partsTarget[1], partsTarget[2], partsTarget[3], partsTarget[4]);
                System.out.println("Duong dan nguon sau khi chuan hoa: " + sourcePath);
                System.out.println("Duong dan dich sau khi chuan hoa: " + targetPath);
            }
            // Sử dụng scp để sao chép dữ liệu
            // String scpCommand = String.format(
            // "scp -o StrictHostKeyChecking=no -r '%s/' '%s@%s:%s/'",
            // sourcePath,
            // targetInforUser.getUser(),
            // targetServer,
            // targetPath);
            // System.out.println("Lenh scp: " + scpCommand);

            // Thêm thông tin về kích thước dữ liệu
            String sizeCommand = String.format("du -sh '%s'", sourcePath);
            String sizeResult = executeSSHCommand(sourceSession, sizeCommand);
            System.out.println("Kich thuoc du lieu can copy: " + sizeResult);

            // Sử dụng SFTP để copy dữ liệu
            System.out.println("Bat dau copy du lieu...");

            // Tạo kênh SFTP cho server nguồn
            ChannelSftp sourceChannel = (ChannelSftp) sourceSession.openChannel("sftp");
            sourceChannel.connect();
            System.out.println("Da ket noi SFTP den server nguon");

            // Tạo kênh SFTP cho server đích
            ChannelSftp targetChannel = (ChannelSftp) targetSession.openChannel("sftp");
            targetChannel.connect();
            System.out.println("Da ket noi SFTP den server dich");

            try {
                // Copy đệ quy thư mục
                System.out.println("Bat dau copy thu muc...");
                copyDirectory(sourceChannel, targetChannel, sourcePath, targetPath);
                System.out.println("Copy thu muc hoan tat");

                System.out.println("Copy hoan tat tu " + sourcePath + " den " + targetPath);

                // Kiểm tra xem thư mục đích đã được tạo thành công
                System.out.println("Dang kiem tra thu muc dich...");
                String checkCommand = String.format("test -d '%s' && echo 'exists'", targetPath);
                String checkResult = executeSSHCommand(targetSession, checkCommand);
                if (!checkResult.contains("exists")) {
                    String errorMsg = "Khong the xac nhan thu muc dich da duoc tao";
                    System.out.println("LOI: " + errorMsg);
                    throw new Exception(errorMsg);
                }
                System.out.println("Xac nhan thu muc dich ton tai");

                // Xóa dữ liệu gốc trên server cũ
                System.out.println("Dang xoa du lieu goc...");
                String rmCommand = String.format("rm -rf '%s'", sourcePath);
                executeCommand(sourceSession, rmCommand);
                System.out.println("Da xoa du lieu goc thanh cong");

                // Cập nhật quyền truy cập cho thư mục đích
                System.out.println("Dang cap nhat quyen truy cap...");
                String chmodCommand = String.format("chmod -R 755 '%s'", targetPath);
                executeCommand(targetSession, chmodCommand);
                System.out.println("Da cap nhat quyen truy cap thanh cong");

                System.out.println("=== KET QUA: Di chuyen du lieu thanh cong ===");

            } catch (Exception e) {
                System.out.println("LOI khi copy du lieu: " + e.getMessage());
                throw e;
            } finally {
                if (sourceChannel != null && sourceChannel.isConnected()) {
                    sourceChannel.disconnect();
                    System.out.println("Da dong ket noi SFTP den server nguon");
                }
                if (targetChannel != null && targetChannel.isConnected()) {
                    targetChannel.disconnect();
                    System.out.println("Da dong ket noi SFTP den server dich");
                }
            }

        } catch (Exception e) {
            System.out.println("LOI khi di chuyen du lieu: " + e.getMessage());
            throw e;
        } finally {
            if (targetSession != null && targetSession.isConnected()) {
                targetSession.disconnect();
                System.out.println("Da dong ket noi den server dich");
            }
        }
    }

    private void copyDirectory(ChannelSftp sourceChannel, ChannelSftp targetChannel, String sourcePath,
            String targetPath) throws Exception {
        System.out.println("Dang copy thu muc: " + sourcePath + " -> " + targetPath);

        // Liệt kê tất cả các file và thư mục trong thư mục nguồn
        Vector<ChannelSftp.LsEntry> entries = sourceChannel.ls(sourcePath);

        for (ChannelSftp.LsEntry entry : entries) {
            if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) {
                continue;
            }

            String sourceFile = sourcePath + "/" + entry.getFilename();
            String targetFile = targetPath + "/" + entry.getFilename();

            if (entry.getAttrs().isDir()) {
                System.out.println("Dang xu ly thu muc con: " + entry.getFilename());
                // Nếu là thư mục, tạo thư mục mới và copy đệ quy
                try {
                    // Kiểm tra xem thư mục đích đã tồn tại chưa
                    try {
                        targetChannel.stat(targetFile);
                        System.out.println("Thu muc dich da ton tai: " + targetFile);
                    } catch (SftpException e) {
                        // Nếu thư mục chưa tồn tại, tạo mới
                        targetChannel.mkdir(targetFile);
                        System.out.println("Da tao thu muc con: " + targetFile);
                    }

                    // Copy nội dung thư mục
                    copyDirectory(sourceChannel, targetChannel, sourceFile, targetFile);
                } catch (Exception e) {
                    System.out.println("LOI khi xu ly thu muc con " + targetFile + ": " + e.getMessage());
                    throw e;
                }
            } else {
                System.out.println("Dang copy file: " + entry.getFilename());
                // Nếu là file, copy trực tiếp
                try {
                    // Kiểm tra xem file đích đã tồn tại chưa
                    try {
                        targetChannel.stat(targetFile);
                        System.out.println("File dich da ton tai, dang xoa: " + targetFile);
                        targetChannel.rm(targetFile);
                    } catch (SftpException e) {
                        // File chưa tồn tại, tiếp tục copy
                    }

                    // Copy file
                    try (InputStream inputStream = sourceChannel.get(sourceFile); OutputStream outputStream = targetChannel.put(targetFile)) {

                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        System.out.println("Da copy file thanh cong: " + entry.getFilename());
                    }
                } catch (IOException e) {
                    System.out.println("LOI khi copy file " + entry.getFilename() + ": " + e.getMessage());
                    throw e;
                }
            }
        }
    }

    private void renameItem(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        List<InforUser> users = (List<InforUser>) session.getAttribute("users");
        NguoiDung nguoiDung = (NguoiDung) session.getAttribute("nguoi_dung");
        // Lấy thông tin cần thiết từ request
        String oldName = request.getParameter("oldName");
        String newName = request.getParameter("newName").trim();
        String currentPath = request.getParameter("currentPath");
        String serverHost = request.getParameter("server");

        System.out.println("=== BAT DAU DOI TEN ===");
        System.out.println("Ten cu: " + oldName);
        System.out.println("Ten moi: " + newName);
        System.out.println("Duong dan hien tai: " + currentPath);
        System.out.println("Server: " + serverHost);

        // Kiểm tra thông tin đầu vào
        if (oldName == null || newName == null || currentPath == null || serverHost == null) {
            System.out.println("LOI: Thieu thong tin can thiet");
            request.setAttribute("error", "Thiếu thông tin cần thiết để đổi tên!");
            request.setAttribute("path", request.getAttribute("currentPath"));
            listFiles(request, response, users);
            return;
        }

        // Tìm server tương ứng
        InforUser targetServer = null;
        for (InforUser user : users) {
            if (user.getHost().equals(serverHost)) {
                targetServer = user;
                break;
            }
        }

        if (targetServer == null) {
            System.out.println("LOI: Khong tim thay may chu");
            request.setAttribute("error", "Không tìm thấy máy chủ!");
            request.setAttribute("path", request.getAttribute("currentPath"));
            listFiles(request, response, users);
            return;
        }

        try {
            // Kết nối SSH đến server
            Session sshSession = InforUser.connect(
                    targetServer.getHost(),
                    targetServer.getPort(),
                    targetServer.getUser(),
                    targetServer.getPassword());

            try {
                // Xây dựng đường dẫn đầy đủ
                String oldPath = currentPath + "/" + oldName;
                String newPath = currentPath + "/" + newName;

                // Kiểm tra xem item cũ có tồn tại không
                String checkCommand = String.format("test -e '%s' && echo 'exists'", oldPath);
                String checkResult = executeSSHCommand(sshSession, checkCommand);
                System.out.println("Ket qua kiem tra xem item co ton tai khong: " + checkResult);

                if (!checkResult.contains("exists")) {
                    System.out.println("LOI: Khong tim thay file/thu muc: " + oldName);
                    request.setAttribute("error", "Không tìm thấy file/thư mục: " + oldName);
                    String basePath = "/data/" + targetServer.getUser() + "/" + nguoiDung.getTaiKhoan();
                    if (currentPath.equals(basePath)) {
                        request.removeAttribute("path");
                        session.removeAttribute("currentPath");
                    } else {
                        request.setAttribute("path", currentPath);
                        session.setAttribute("currentPath", currentPath);
                    }
                    listFiles(request, response, users);
                    return;
                }

                // Kiểm tra xem tên mới đã tồn tại trên bất kỳ server nào chưa
                boolean nameExists = false;
                String checkPath = newPath;

                // Chỉ kiểm tra trên tất cả server nếu currentPath là null hoặc rỗng
                if (request.getAttribute("currentPath") == null
                        || request.getAttribute("currentPath").toString().trim().isEmpty()) {
                    System.out.println("Kiem tra ten moi tren tat ca cac server...");
                    for (InforUser user : users) {
                        Session checkSession = InforUser.connect(
                                user.getHost(),
                                user.getPort(),
                                user.getUser(),
                                user.getPassword());

                        try {
                            // Nếu currentPath là null hoặc rỗng, sử dụng đường dẫn mặc định
                            String serverPath = "/data/" + user.getUser() + "/" + nguoiDung.getTaiKhoan() + "/"
                                    + newName;
                            String checkNewNameCommand = String.format("test -e '%s' && echo 'exists'", serverPath);
                            String checkNewNameResult = executeSSHCommand(checkSession, checkNewNameCommand);

                            if (checkNewNameResult.contains("exists")) {
                                nameExists = true;
                                System.out.println(
                                        "LOI: Ten moi '" + newName + "' da ton tai tren server " + user.getHost());
                                break;
                            }
                        } finally {
                            if (checkSession != null && checkSession.isConnected()) {
                                checkSession.disconnect();
                            }
                        }
                    }
                } else {
                    // Nếu có currentPath, chỉ kiểm tra trên server hiện tại
                    System.out.println("Kiem tra ten moi tren server hien tai...");
                    String checkNewNameCommand = String.format("test -e '%s' && echo 'exists'", checkPath);
                    String checkNewNameResult = executeSSHCommand(sshSession, checkNewNameCommand);
                    nameExists = checkNewNameResult.contains("exists");
                }

                if (nameExists) {
                    System.out.println("LOI: Ten moi '" + newName + "' da ton tai");
                    request.setAttribute("error", "Tên mới '" + newName + "' đã tồn tại");
                    String basePath = "/data/" + targetServer.getUser() + "/" + nguoiDung.getTaiKhoan();
                    if (currentPath.equals(basePath)) {
                        request.removeAttribute("path");
                        session.removeAttribute("currentPath");
                    } else {
                        request.setAttribute("path", currentPath);
                        session.setAttribute("currentPath", currentPath);
                    }
                    listFiles(request, response, users);
                    return;
                }

                // Thực hiện đổi tên
                String mvCommand = String.format("mv '%s' '%s'", oldPath, newPath);
                executeSSHCommand(sshSession, mvCommand);

                System.out.println("=== KET QUA: Da doi ten thanh cong ===");
                request.setAttribute("success", "Đã đổi tên thành công từ '" + oldName + "' thành '" + newName + "'");
                renamePathOnServer(oldPath, newPath, nguoiDung.getTaiKhoan());
            } finally {
                if (sshSession != null && sshSession.isConnected()) {
                    sshSession.disconnect();
                }
            }

            // Cập nhật session và hiển thị danh sách mới
            if (nguoiDung != null) {
                String basePath = "/data/" + targetServer.getUser() + "/" + nguoiDung.getTaiKhoan();
                if (currentPath.equals(basePath)) {
                    // Nếu đang ở thư mục tổng, hiển thị danh sách từ tất cả server
                    request.removeAttribute("path");
                    session.removeAttribute("currentPath");
                } else {
                    // Nếu đang ở đường dẫn con, hiển thị danh sách của đường dẫn hiện tại
                    request.setAttribute("path", currentPath);
                    session.setAttribute("currentPath", currentPath);
                }
            }
            listFiles(request, response, users);

        } catch (Exception e) {
            System.out.println("=== LOI: " + e.getMessage() + " ===");
            request.setAttribute("error", "Lỗi khi đổi tên: " + e.getMessage());

            // Xử lý hiển thị danh sách khi có lỗi
            if (nguoiDung != null) {
                String basePath = "/data/" + targetServer.getUser() + "/" + nguoiDung.getTaiKhoan();
                if (currentPath.equals(basePath)) {
                    request.removeAttribute("path");
                    session.removeAttribute("currentPath");
                } else {
                    request.setAttribute("path", currentPath);
                    session.setAttribute("currentPath", currentPath);
                }
            }

            listFiles(request, response, users);
        }
    }

    //Start Xử lý chia sẻ NFS
    private void shareNFS(HttpServletRequest request, HttpServletResponse response, List<InforUser> users)
            throws ServletException, IOException, Exception {
        Session sourceSession = null;
        Session targetSession = null;
        try {
            String folderPath = request.getParameter("folderPath");
            String folderName = request.getParameter("folderName");
            String serverHost = request.getParameter("serverHost");
            String targetUser = request.getParameter("targetUser");
            String permissions = request.getParameter("permissions");

            // Log thông tin đầu vào
            System.out.println("=== THONG TIN CHIA SE NFS ===");
            System.out.println("Folder Path: " + folderPath);
            System.out.println("Folder Name: " + folderName);
            System.out.println("Server Host: " + serverHost);
            System.out.println("Target User: " + targetUser);
            System.out.println("Permissions: " + permissions);

            // Tìm server nguồn
            InforUser sourceServer = users.stream()
                    .filter(u -> u.getHost().equals(serverHost))
                    .findFirst()
                    .orElseThrow(() -> new Exception("Không tìm thấy thông tin server nguồn"));

            System.out.println("Source Server Info:");
            System.out.println("- Host: " + sourceServer.getHost());
            System.out.println("- User: " + sourceServer.getUser());
            System.out.println("- Port: " + sourceServer.getPort());

            // Kết nối đến server nguồn
            sourceSession = InforUser.connect(
                    sourceServer.getHost(),
                    sourceServer.getPort(),
                    sourceServer.getUser(),
                    sourceServer.getPassword());

            InforUser targetServer = selectStorageServer(users);

            // 1. Cấu hình /etc/exports trên server nguồn
            // String exportLine = String.format("%s %s(%s)",
            // folderPath + "/" + folderName, targetUser, permissions);
            String exportLine = String.format("%s %s(%s)",
                    folderPath + "/" + folderName, targetServer, permissions);

            System.out.println("Export Line: " + exportLine);

            // Thêm entry vào /etc/exports
            String addExportCmd = String.format(
                    "echo '%s' | sudo -S bash -c 'echo \"%s\" >> /etc/exports'",
                    sourceServer.getPassword(), exportLine);
            System.out.println("Adding to /etc/exports: " + addExportCmd);
            executeSSHCommand(sourceSession, addExportCmd);

            // Reload exports
            String reloadExportsCmd = String.format(
                    "echo '%s' | sudo -S exportfs -ra",
                    sourceServer.getPassword());
            System.out.println("Reloading exports: " + reloadExportsCmd);
            executeSSHCommand(sourceSession, reloadExportsCmd);

            // 2. Cấu hình mount point trên server đích
            String targetMountPoint = String.format("/data/%s/%s",
                    targetUser, folderName);

            System.out.println("Target Mount Point: " + targetMountPoint);
            System.out.println("Source Path: " + folderPath + "/" + folderName);

            configureMountPoint(targetUser, targetMountPoint,
                    serverHost, folderPath + "/" + folderName, permissions, targetServer);

            System.out.println("=== CHIA SE NFS THANH CONG ===");
            request.setAttribute("success", "Chia sẻ thư mục thành công!");

        } catch (Exception e) {
            System.out.println("=== LOI KHI CHIA SE NFS: " + e.getMessage() + " ===");
            request.setAttribute("error", "Lỗi khi chia sẻ thư mục: " + e.getMessage());
        } finally {
            if (sourceSession != null) {
                sourceSession.disconnect();
            }
            if (targetSession != null) {
                targetSession.disconnect();
            }
        }
        listFiles(request, response, users);

    }

    private void configureMountPoint(String targetUser, String mountPoint,
            String sourceHost, String sourcePath, String permissions, InforUser targetServer)
            throws Exception {

        System.out.println("=== CAU HINH MOUNT POINT ===");
        System.out.println("Target User: " + targetUser);
        System.out.println("Mount Point: " + mountPoint); // /data/<tên người dùng đích>/<tên thư mục chia sẽ>
        System.out.println("Source Host: " + sourceHost); // server nguồn
        System.out.println("Source Path: " + sourcePath); // đường dẫn đích
        System.out.println("Permissions: " + permissions); // quyền

        // Tìm server của người nhận
        // InforUser targetServer = users.stream()
        // .filter(u -> u.getUser().equals(targetUser))
        // .findFirst()
        // .orElseThrow(() -> new Exception("Không tìm thấy thông tin server của người
        // dùng đích"));
        System.out.println("Target Server Info:");
        System.out.println("- Host: " + targetServer.getHost());
        System.out.println("- User: " + targetServer.getUser());
        System.out.println("- Port: " + targetServer.getPort());

        Session targetSession = null;
        try {
            // Kết nối đến server đích
            targetSession = InforUser.connect(
                    targetServer.getHost(),
                    targetServer.getPort(),
                    targetServer.getUser(),
                    targetServer.getPassword());

            // Tạo thư mục mount point
            String mkdirCmd = String.format(
                    "echo '%s' | sudo -S mkdir -p %s",
                    targetServer.getPassword(), mountPoint);
            System.out.println("Creating mount point: " + mkdirCmd);
            executeSSHCommand(targetSession, mkdirCmd);

            // Cấp quyền cho thư mục
            String chownCmd = String.format(
                    "echo '%s' | sudo -S chown %s:%s %s",
                    targetServer.getPassword(), targetUser, targetUser, mountPoint);
            System.out.println("Setting permissions: " + chownCmd);
            executeSSHCommand(targetSession, chownCmd);

            // Thêm vào /etc/fstab với đường dẫn đúng
            String fstabEntry = String.format("%s:%s %s nfs rw,sync,hard,intr 0 0",
                    sourceHost, sourcePath, mountPoint);

            System.out.println("Fstab Entry: " + fstabEntry);

            // Thêm entry vào /etc/fstab
            String addFstabCmd = String.format(
                    "echo '%s' | sudo -S sh -c 'echo \"%s\" >> /etc/fstab'",
                    targetServer.getPassword(), fstabEntry);
            System.out.println("Adding to /etc/fstab: " + addFstabCmd);
            executeSSHCommand(targetSession, addFstabCmd);

            // Mount thư mục
            String mountCmd = String.format(
                    "echo '%s' | sudo -S mount -a",
                    targetServer.getPassword());
            System.out.println("Mounting: " + mountCmd);
            executeSSHCommand(targetSession, mountCmd);

            // Thêm lệnh remount để áp dụng thay đổi
            String remountCmd = String.format(
                    "echo '%s' | sudo -S mount -o remount %s",
                    targetServer.getPassword(), mountPoint);
            System.out.println("Remounting: " + remountCmd);
            executeSSHCommand(targetSession, remountCmd);

            System.out.println("=== CAU HINH MOUNT POINT THANH CONG ===");

        } finally {
            if (targetSession != null) {
                targetSession.disconnect();
            }
        }
    }

    private void checkUserExists(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, ClassNotFoundException {
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String targetUser = request.getParameter("targetUser");
        String currentUser = ((NguoiDung) request.getSession().getAttribute("nguoi_dung")).getTaiKhoan();

        try {
            if (targetUser.equals(currentUser)) {
                out.write("SELF");
                return;
            }
            JDBC jdbc = new JDBC();
            Connection conn = jdbc.connect();
            String sql = "SELECT tai_khoan FROM nguoi_dung WHERE tai_khoan = ? AND role = 'client'";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, targetUser);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        out.write("EXISTS");
                    } else {
                        out.write("NOT_FOUND");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Loi kiem tra nguoi dung: " + e.getMessage());
            out.write("ERROR");
        }
    }
    //End Xử lý chia sẻ NFS

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        try {
            // Bảng thay thế các ký tự tiếng Việt
            Map<String, String> vietnameseMap = new HashMap<>();
            vietnameseMap.put("ư", "u");
            vietnameseMap.put("ơ", "o");
            vietnameseMap.put("ă", "a");
            vietnameseMap.put("â", "a");
            vietnameseMap.put("ê", "e");
            vietnameseMap.put("ô", "o");
            vietnameseMap.put("ố", "o");
            vietnameseMap.put("ồ", "o");
            vietnameseMap.put("ổ", "o");
            vietnameseMap.put("ộ", "o");
            vietnameseMap.put("ớ", "o");
            vietnameseMap.put("ờ", "o");
            vietnameseMap.put("ở", "o");
            vietnameseMap.put("ỡ", "o");
            vietnameseMap.put("ợ", "o");
            vietnameseMap.put("ứ", "u");
            vietnameseMap.put("ừ", "u");
            vietnameseMap.put("ử", "u");
            vietnameseMap.put("ữ", "u");
            vietnameseMap.put("ự", "u");
            vietnameseMap.put("á", "a");
            vietnameseMap.put("à", "a");
            vietnameseMap.put("ả", "a");
            vietnameseMap.put("ã", "a");
            vietnameseMap.put("ạ", "a");
            vietnameseMap.put("ắ", "a");
            vietnameseMap.put("ằ", "a");
            vietnameseMap.put("ẳ", "a");
            vietnameseMap.put("ẵ", "a");
            vietnameseMap.put("ặ", "a");
            vietnameseMap.put("ấ", "a");
            vietnameseMap.put("ầ", "a");
            vietnameseMap.put("ẩ", "a");
            vietnameseMap.put("ẫ", "a");
            vietnameseMap.put("ậ", "a");
            vietnameseMap.put("é", "e");
            vietnameseMap.put("è", "e");
            vietnameseMap.put("ẻ", "e");
            vietnameseMap.put("ẽ", "e");
            vietnameseMap.put("ẹ", "e");
            vietnameseMap.put("ế", "e");
            vietnameseMap.put("ề", "e");
            vietnameseMap.put("ể", "e");
            vietnameseMap.put("ễ", "e");
            vietnameseMap.put("ệ", "e");
            vietnameseMap.put("í", "i");
            vietnameseMap.put("ì", "i");
            vietnameseMap.put("ỉ", "i");
            vietnameseMap.put("ĩ", "i");
            vietnameseMap.put("ị", "i");
            vietnameseMap.put("ó", "o");
            vietnameseMap.put("ò", "o");
            vietnameseMap.put("ỏ", "o");
            vietnameseMap.put("õ", "o");
            vietnameseMap.put("ọ", "o");
            vietnameseMap.put("ú", "u");
            vietnameseMap.put("ù", "u");
            vietnameseMap.put("ủ", "u");
            vietnameseMap.put("ũ", "u");
            vietnameseMap.put("ụ", "u");
            vietnameseMap.put("ý", "y");
            vietnameseMap.put("ỳ", "y");
            vietnameseMap.put("ỷ", "y");
            vietnameseMap.put("ỹ", "y");
            vietnameseMap.put("ỵ", "y");
            vietnameseMap.put("đ", "d"); 
            // Hoa
            vietnameseMap.put("Ư", "U");
            vietnameseMap.put("Ơ", "O");
            vietnameseMap.put("Ă", "A");
            vietnameseMap.put("Â", "A");
            vietnameseMap.put("Ê", "E");
            vietnameseMap.put("Ô", "O");
            vietnameseMap.put("Ố", "O");
            vietnameseMap.put("Ồ", "O");
            vietnameseMap.put("Ổ", "O");
            vietnameseMap.put("Ộ", "O");
            vietnameseMap.put("Ớ", "O");
            vietnameseMap.put("Ờ", "O");
            vietnameseMap.put("Ở", "O");
            vietnameseMap.put("Ỡ", "O");
            vietnameseMap.put("Ợ", "O");
            vietnameseMap.put("Ứ", "U");
            vietnameseMap.put("Ừ", "U");
            vietnameseMap.put("Ử", "U");
            vietnameseMap.put("Ữ", "U");
            vietnameseMap.put("Ự", "U");
            vietnameseMap.put("Á", "A");
            vietnameseMap.put("À", "A");
            vietnameseMap.put("Ả", "A");
            vietnameseMap.put("Ã", "A");
            vietnameseMap.put("Ạ", "A");
            vietnameseMap.put("Ắ", "A");
            vietnameseMap.put("Ằ", "A");
            vietnameseMap.put("Ẳ", "A");
            vietnameseMap.put("Ẵ", "A");
            vietnameseMap.put("Ặ", "A");
            vietnameseMap.put("Ấ", "A");
            vietnameseMap.put("Ầ", "A");
            vietnameseMap.put("Ẩ", "A");
            vietnameseMap.put("Ẫ", "A");
            vietnameseMap.put("Ậ", "A");
            vietnameseMap.put("É", "E");
            vietnameseMap.put("È", "E");
            vietnameseMap.put("Ẻ", "E");
            vietnameseMap.put("Ẽ", "E");
            vietnameseMap.put("Ẹ", "E");
            vietnameseMap.put("Ế", "E");
            vietnameseMap.put("Ề", "E");
            vietnameseMap.put("Ể", "E");
            vietnameseMap.put("Ễ", "E");
            vietnameseMap.put("Ệ", "E");
            vietnameseMap.put("Í", "I");
            vietnameseMap.put("Ì", "I");
            vietnameseMap.put("Ỉ", "I");
            vietnameseMap.put("Ĩ", "I");
            vietnameseMap.put("Ị", "I");
            vietnameseMap.put("Ó", "O");
            vietnameseMap.put("Ò", "O");
            vietnameseMap.put("Ỏ", "O");
            vietnameseMap.put("Õ", "O");
            vietnameseMap.put("Ọ", "O");
            vietnameseMap.put("Ú", "U");
            vietnameseMap.put("Ù", "U");
            vietnameseMap.put("Ủ", "U");
            vietnameseMap.put("Ũ", "U");
            vietnameseMap.put("Ụ", "U");
            vietnameseMap.put("Ý", "Y");
            vietnameseMap.put("Ỳ", "Y");
            vietnameseMap.put("Ỷ", "Y");
            vietnameseMap.put("Ỹ", "Y");
            vietnameseMap.put("Ỵ", "Y");
            vietnameseMap.put("Đ", "D");

            StringBuilder result = new StringBuilder(fileName);

            // Thay thế ký tự tiếng Việt có dấu
            for (Map.Entry<String, String> entry : vietnameseMap.entrySet()) {
                int index = result.indexOf(entry.getKey());
                while (index != -1) {
                    result.replace(index, index + entry.getKey().length(), entry.getValue());
                    index = result.indexOf(entry.getKey(), index + entry.getValue().length());
                }
            }

            // Thay thế dấu gạch ngang dài (en dash, em dash) bằng dấu gạch ngang thông
            // thường (hyphen)
            result = new StringBuilder(result.toString().replace("‐", "-").replace("–", "-").replace("—", "-"));

            // Loại bỏ tất cả các ký tự không hợp lệ
            String sanitized = result.toString().replaceAll("[\\\\/:*?\"<>|]", "_");

            // Thay thế nhiều khoảng trắng liên tiếp thành một khoảng trắng
            sanitized = sanitized.replaceAll("\\s+", " ");

            return sanitized;
        } catch (Exception e) {
            System.out.println("Lỗi khi chuẩn hóa tên file: " + e.getMessage());
            // Trả về tên gốc nếu có lỗi
            return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        }
    }

    //CSDL path_on_server
    private void savePathOnServer(String fullPath, String serverUser, String userAccount, boolean isDirectory, long size) {
        // Tách đường dẫn để lấy path_relative
        System.out.println("THUC HIEN LUU DUONG DAN VAO CSDL");
        System.out.println("fullPath: " + fullPath);
        System.out.println("serverUser: " + serverUser);
        System.out.println("userAccount: " + userAccount);
        System.out.println("isDirectory: " + isDirectory);
        System.out.println("size: " + size);

        String basePath = "/data/" + serverUser + "/" + userAccount + "/";
        String pathRelative = fullPath.replace(basePath, "");

        System.out.println("basePath: " + basePath);
        System.out.println("pathRelative: " + pathRelative);

        JDBC jdbc = new JDBC();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = jdbc.connect();

            // Kiểm tra xem path đã tồn tại chính xác trong DB chưa
            String checkExactSql = "SELECT * FROM path_on_server WHERE tai_khoan = ? AND path_relative = ?";
            stmt = conn.prepareStatement(checkExactSql);
            stmt.setString(1, userAccount);
            stmt.setString(2, pathRelative.split("/")[0]);
//            stmt.setString(3, basePath);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // Nếu đã tồn tại chính xác thì cập nhật kích thước
                long currentSize = rs.getLong("size");
                System.out.println("Duong dan da ton tai, cap nhat kich thuoc tu " + currentSize + " thanh " + (currentSize + size));

                String updateSql = "UPDATE path_on_server SET size = size + ?, isDirectory = ? WHERE tai_khoan = ? AND path_relative = ?";
                stmt = conn.prepareStatement(updateSql);
                stmt.setLong(1, size); //kich thuoc upload thanh cong
                stmt.setInt(2, isDirectory ? 1 : 0);
                stmt.setString(3, userAccount);
                stmt.setString(4, pathRelative.split("/")[0]);
//                stmt.setString(5, basePath);
                stmt.executeUpdate();

                return; // Hoàn thành cập nhật, không cần kiểm tra tiếp
            }

            // Kiểm tra xem pathRelative có phải là thư mục con của path nào đã tồn tại
            // LIKE CONCAT(path_relative, '/%') để tìm kiếm đường dẫn cha
            String checkParentSql = "SELECT * FROM path_on_server WHERE tai_khoan = ? AND ? LIKE CONCAT(path_relative, '/%')";
            stmt = conn.prepareStatement(checkParentSql);
            stmt.setString(1, userAccount);
//            stmt.setString(2, basePath);
            stmt.setString(2, pathRelative);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // Tìm thấy path cha, chỉ cập nhật kích thước của cha
                String parentPath = rs.getString("path_relative");
                long currentSize = rs.getLong("size");
                System.out.println("Duong dan " + pathRelative + " la con cua " + parentPath + ", cap nhat kich thuoc thu muc cha tu "
                        + currentSize + " thanh " + (currentSize + size));

                String updateParentSql = "UPDATE path_on_server SET size = size + ? WHERE tai_khoan = ? AND path_relative = ?";
                stmt = conn.prepareStatement(updateParentSql);
                stmt.setLong(1, size);
                stmt.setString(2, userAccount);
                stmt.setString(3, parentPath);
//                stmt.setString(4, basePath);
                stmt.executeUpdate();

                // Không thêm bản ghi mới cho đường dẫn con
                System.out.println("Khong them ban ghi moi cho duong dan con " + pathRelative);
                return;
            }

            // Kiểm tra xem pathRelative có phải là thư mục cha của các path đã tồn tại
            String checkChildSql = "SELECT * FROM path_on_server WHERE tai_khoan = ? AND path_relative LIKE CONCAT(?, '/%')";
            stmt = conn.prepareStatement(checkChildSql);
            stmt.setString(1, userAccount);
//            stmt.setString(2, basePath);
            stmt.setString(2, pathRelative);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // Có các path con, vẫn tạo bản ghi mới
                System.out.println("Duong dan " + pathRelative + " la cha cua duong dan hien tai, tao ban ghi moi");
            }

            // Nếu chưa tồn tại thì thêm mới
            String insertSql = "INSERT INTO path_on_server (tai_khoan, path_base, path_relative, isDirectory, size) VALUES (?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(insertSql);
            stmt.setString(1, userAccount);
            stmt.setString(2, basePath);
            stmt.setString(3, pathRelative);
            stmt.setInt(4, isDirectory ? 1 : 0);
            stmt.setLong(5, size);
            stmt.executeUpdate();

        } catch (Exception e) {
            System.out.println("Loi khi luu vao CSDL path_on_server: " + e.getMessage());
            e.printStackTrace();
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
            } catch (Exception e) {
                System.out.println("Lỗi khi đóng kết nối CSDL: " + e.getMessage());
            }
        }
    }

    private void deletePathOnServer(String fullPath, String userAccount, long actualSize) {

        System.out.println("THUC HIEN XOA DUONG DAN TRONG CSDL");
        System.out.println("fullPath: " + fullPath);
        System.out.println("userAccount: " + userAccount);
        System.out.println("actualSize: " + actualSize);

        String[] parts = fullPath.split("/");
        String pathRelative = "";

        if (parts.length >= 5) {
            // Đảm bảo lấy path_relative chính xác
            String serverUser = parts[2];
            String basePath = "/data/" + serverUser + "/" + userAccount + "/";
            pathRelative = fullPath.replace(basePath, "");

            System.out.println("serverUser: " + serverUser);
            System.out.println("basePath: " + basePath);
            System.out.println("pathRelative: " + pathRelative);

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                JDBC jdbc = new JDBC();
                conn = jdbc.connect();

                // Xóa bản ghi trong CSDL
                String checkSql = "SELECT * FROM path_on_server WHERE tai_khoan = ? AND path_relative = ? AND path_base = ?";
                stmt = conn.prepareStatement(checkSql);
                stmt.setString(1, userAccount);
                stmt.setString(2, pathRelative);
                stmt.setString(3, basePath);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    String deleteSql = "DELETE FROM path_on_server WHERE tai_khoan = ? AND path_relative = ? AND path_base = ?";
                    stmt = conn.prepareStatement(deleteSql);
                    stmt.setString(1, userAccount);
                    stmt.setString(2, pathRelative);
                    stmt.setString(3, basePath);
                    stmt.executeUpdate();

                    System.out.println("Xoa duong dan thanh cong: " + pathRelative);
                    return;
                }
                // Nếu đã lấy được kích thước thực tế và lớn hơn 0, kiểm tra đường dẫn cha
                if (actualSize > 0) {
                    // Kiểm tra xem pathRelative có phải là thư mục con của path nào đã tồn tại
                    String checkParentSql = "SELECT * FROM path_on_server WHERE tai_khoan = ? AND path_base = ? AND ? LIKE CONCAT(path_relative, '/%')";
                    stmt = conn.prepareStatement(checkParentSql);
                    stmt.setString(1, userAccount);
                    stmt.setString(2, basePath);
                    stmt.setString(3, pathRelative);
                    rs = stmt.executeQuery();

                    if (rs.next()) {
                        // Tìm thấy path cha, cập nhật kích thước giảm đi
                        String parentPath = rs.getString("path_relative");
                        long currentSize = rs.getLong("size");
                        System.out.println("Duong dan " + pathRelative + " la con cua " + parentPath + ", cap nhat kich thuoc thu muc cha giam tu "
                                + currentSize + " thanh " + (currentSize - actualSize));

                        // Cập nhật kích thước của thư mục cha
                        String updateParentSql = "UPDATE path_on_server SET size = size - ? WHERE tai_khoan = ? AND path_relative = ? AND path_base = ?";
                        stmt = conn.prepareStatement(updateParentSql);
                        stmt.setLong(1, actualSize);
                        stmt.setString(2, userAccount);
                        stmt.setString(3, parentPath);
                        stmt.setString(4, basePath);
                        stmt.executeUpdate();
                    }
                }

            } catch (ClassNotFoundException | SQLException e) {
                LOGGER.log(Level.SEVERE, "Loi khi xoa duong dan trong CSDL", e);
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
    }

    private void renamePathOnServer(String oldPath, String newPath, String userAccount) {
        System.out.println("THUC HIEN DOI TEN DUONG DAN TRONG CSDL");
        System.out.println("oldPath: " + oldPath);
        System.out.println("newPath: " + newPath);
        System.out.println("userAccount: " + userAccount);

        // Phân tích đường dẫn cũ
        String[] oldParts = oldPath.split("/");
        String oldServerUser = "", oldBasePath = "", oldPathRelative = "";

        if (oldParts.length >= 5) {
            oldServerUser = oldParts[2];
            oldBasePath = "/data/" + oldServerUser + "/" + userAccount + "/";
            oldPathRelative = oldParts[4]; // Lấy phần tử đầu tiên sau base
        } else {
            System.out.println("Duong dan cu khong hop le: " + oldPath);
            return;
        }

        // Phân tích đường dẫn mới
        String[] newParts = newPath.split("/");
        String newServerUser = "", newBasePath = "", newPathRelative = "";

        if (newParts.length >= 5) {
            newServerUser = newParts[2];
            newBasePath = "/data/" + newServerUser + "/" + userAccount + "/";
            newPathRelative = newParts[4];
        } else {
            System.out.println("Duong dan moi khong hop le: " + newPath);
            return;
        }

        boolean serverChanged = !oldServerUser.equals(newServerUser);
        System.out.println("Trang thai thay doi server: " + serverChanged);

        try (Connection conn = new JDBC().connect()) {
            // Kiểm tra xem bản ghi cũ có tồn tại không
            String checkSql = "SELECT * FROM path_on_server WHERE tai_khoan = ? AND path_relative = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, userAccount);
                checkStmt.setString(2, oldPathRelative);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    String updateSql;
                    if (serverChanged) {
                        // Cập nhật cả path_relative và path_base nếu khác server
                        updateSql = "UPDATE path_on_server SET path_relative = ?, path_base = ? WHERE tai_khoan = ? AND path_relative = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, newPathRelative);
                            updateStmt.setString(2, newBasePath);
                            updateStmt.setString(3, userAccount);
                            updateStmt.setString(4, oldPathRelative);
                            int updated = updateStmt.executeUpdate();
                            System.out.println("Cap nhat path_base + path_relative: " + updated + " dòng");
                        }
                    } else {
                        // Chỉ cập nhật path_relative
                        updateSql = "UPDATE path_on_server SET path_relative = ? WHERE tai_khoan = ? AND path_relative = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, newPathRelative);
                            updateStmt.setString(2, userAccount);
                            updateStmt.setString(3, oldPathRelative);
                            int updated = updateStmt.executeUpdate();
                            System.out.println("Cap nhat path_relative: " + updated + " dòng");
                        }
                    }
                } else {
                    System.out.println("Khong tim thay ban ghi path_relative = " + oldPathRelative + " trong CSDL.");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Loi khi cap nhat duong dan trong CSDL", e);
        }
    }

//    private void renamePathOnServer(String oldPath, String newPath, String userAccount) {
//        System.out.println("THUC HIEN DOI TEN DUONG DAN TRONG CSDL");
//        System.out.println("oldPath: " + oldPath);
//        System.out.println("newPath: " + newPath);
//        System.out.println("userAccount: " + userAccount);
//
//        // Phân tích đường dẫn cũ
//        String[] oldParts = oldPath.split("/");
//        String oldServerUser = "";
//        String oldPathRelative = "";
//        String oldBasePath = "";
//
//        if (oldParts.length >= 5) {
//            oldServerUser = oldParts[2];
//            oldBasePath = "/data/" + oldServerUser + "/" + userAccount + "/";
//            oldPathRelative = oldPath.replace(oldBasePath, "");
//        } else {
//            System.out.println("Duong dan cu khong hop le: " + oldPath);
//            return;
//        }
//
//        // Phân tích đường dẫn mới
//        String[] newParts = newPath.split("/");
//        String newServerUser = "";
//        String newPathRelative = "";
//        String newBasePath = "";
//
//        if (newParts.length >= 5) {
//            newServerUser = newParts[2];
//            newBasePath = "/data/" + newServerUser + "/" + userAccount + "/";
//            newPathRelative = newPath.replace(newBasePath, "");
//        } else {
//            System.out.println("Duong dan moi khong hop le: " + newPath);
//            return;
//        }
//
//        System.out.println("oldServerUser: " + oldServerUser);
//        System.out.println("oldBasePath: " + oldBasePath);
//        System.out.println("oldPathRelative: " + oldPathRelative);
//
//        System.out.println("newServerUser: " + newServerUser);
//        System.out.println("newBasePath: " + newBasePath);
//        System.out.println("newPathRelative: " + newPathRelative);
//
//        boolean serverChanged = !oldServerUser.equals(newServerUser);
//        System.out.println("Trang thai thay doi server: " + serverChanged);
//
//        Connection conn = null;
//        PreparedStatement stmt = null;
//        ResultSet rs = null;
//
//        try {
//            JDBC jdbc = new JDBC();
//            conn = jdbc.connect();
//
//            // Tìm tất cả bản ghi liên quan đến đường dẫn cũ
//            String checkSql = "SELECT * FROM path_on_server WHERE tai_khoan = ? AND path_relative LIKE ?";
//            stmt = conn.prepareStatement(checkSql);
//            stmt.setString(1, userAccount);
//            stmt.setString(2, oldPathRelative.split("/")[0]);
////            stmt.setString(3, oldBasePath);
//            rs = stmt.executeQuery();
//
//            if (rs.next()) {
//                String updateSql;
//                if (serverChanged) {
//                    // Nếu thay đổi server, cập nhật cả path_base và path_relative
//                    updateSql = "UPDATE path_on_server SET path_relative = ?, path_base = ? WHERE tai_khoan = ? AND path_relative LIKE ?";
//                    stmt = conn.prepareStatement(updateSql);
//                    stmt.setString(1, newPathRelative.split("/")[0]);
//                    stmt.setString(2, newBasePath);
//                    stmt.setString(3, userAccount);
//                    stmt.setString(4, oldPathRelative.split("/")[0]);
////                    stmt.setString(5, oldBasePath);
//                } else {
//                    // Nếu chỉ đổi tên không đổi server
//                    updateSql = "UPDATE path_on_server SET path_relative = ? WHERE tai_khoan = ? AND path_relative = ?";
//                    stmt = conn.prepareStatement(updateSql);
//                    stmt.setString(1, newPathRelative.split("/")[0]);
//                    stmt.setString(2, userAccount);
//                    stmt.setString(3, oldPathRelative.split("/")[0]);
//                }
//
//                int updatedRows = stmt.executeUpdate();
//                System.out.println("Cap nhat thanh cong duong dan: " + updatedRows + " bản ghi");
//            } else {
//                System.out.println("Khong tim thay duong dan can doi ten trong CSDL");
//            }
//        } catch (Exception e) {
//            LOGGER.log(Level.SEVERE, "Loi khi cap nhat duong dan trong CSDL", e);
//            e.printStackTrace();
//        } finally {
//            try {
//                if (rs != null) {
//                    rs.close();
//                }
//                if (stmt != null) {
//                    stmt.close();
//                }
//                if (conn != null) {
//                    conn.close();
//                }
//            } catch (SQLException e) {
//                LOGGER.log(Level.SEVERE, "Loi khi dong ket noi", e);
//            }
//        }
//    }
    private boolean checkFileNameExistsInDB(String fileName, String userAccount) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            JDBC jdbc = new JDBC();
            conn = jdbc.connect();
            // Lấy danh sách tất cả path_relative của người dùng
            String sql = "SELECT path_relative FROM path_on_server WHERE tai_khoan = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, userAccount);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String pathRelative = rs.getString("path_relative");
                if (fileName.equals(pathRelative)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Loi kiem tra ten file/folder trong CSDL: " + e.getMessage());
        }
        return false;
    }
    //CSDL path_on_server
}
