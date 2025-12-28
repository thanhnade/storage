/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import Model.InforUser;
import Model.NFSShare;
import com.jcraft.jsch.*;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.*;
import java.util.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Part;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author acer
 */
@WebServlet(name = "configNFS", urlPatterns = {"/configNFS"})
@MultipartConfig
public class configNFS extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        int port = Integer.parseInt(ports);

        Session session = null;
        try {
            session = InforUser.connect(host, port, user, password);

            switch (action) {
                case "start":
                    startNFS(session, request);
                    break;
                case "stop":
                    stopNFS(session, request);
                    break;
                case "restart":
                    restartNFS(session, request);
                    break;
                case "status":
                    checkNFSStatus(session, request);
                    break;
                case "listFiles":
                    listFiles(session, request, response);
                    break;
                case "download":
                    downloadFile(session, request, response);
                    break;
                case "delete":
                    deleteFile(session, request, response);
                    break;
                case "share":
                    shareDirectory(session, request);
                    break;
                case "unshare":
                    unshareDirectory(session, request);
                    break;
                case "folder":
                    createFolder(session, request);
                    break;
                case "file":
                    createFile(session, request);
                    break;
                case "upload":
                    uploadFile(session, request);
                    break;
                default:
                    request.setAttribute("errmessage", "Hành động không hợp lệ");
                    break;
            }

            // Luôn cập nhật danh sách chia sẻ sau mỗi thao tác
            listNFSShares(session, request);
            listNFSMounts(session, request);

        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi: " + e.getMessage());
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("nfs.jsp");
        dispatcher.forward(request, response);
    }

    private void startNFS(Session session, HttpServletRequest request) throws JSchException, IOException {
        String password = request.getParameter("password");
        try {
            String command = String.format("echo '%s' | sudo -S systemctl start nfs-kernel-server", password);
            executeCommand(session, command, request);

            // Kiểm tra lại trạng thái sau khi start
            checkNFSStatus(session, request);
        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi khi khởi động NFS: " + e.getMessage());
        }
    }

    private void stopNFS(Session session, HttpServletRequest request) throws JSchException, IOException {
        String password = request.getParameter("password");
        try {
            String command = String.format("echo '%s' | sudo -S systemctl stop nfs-kernel-server", password);
            executeCommand(session, command, request);

            // Kiểm tra lại trạng thái sau khi stop
            checkNFSStatus(session, request);
        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi khi dừng NFS: " + e.getMessage());
        }
    }

    private void restartNFS(Session session, HttpServletRequest request) throws JSchException, IOException {
        String password = request.getParameter("password");
        try {
            String command = String.format("echo '%s' | sudo -S systemctl restart nfs-kernel-server", password);
            executeCommand(session, command, request);

            // Kiểm tra lại trạng thái sau khi restart
            checkNFSStatus(session, request);
        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi khi khởi động lại NFS: " + e.getMessage());
        }
    }

    private void checkNFSStatus(Session session, HttpServletRequest request) throws JSchException, IOException {
        String password = request.getParameter("password");
        try {
            // Sử dụng -S để đọc password từ stdin
            String command = String.format("echo '%s' | sudo -S systemctl status nfs-kernel-server", password);
            String result = executeCommand(session, command, request);

            if (result.contains("active")) {
                request.setAttribute("message", "Dịch vụ NFS đang hoạt động");
                request.setAttribute("nfsStatus", "active");
            } else if (result.contains("inactive")) {
                request.setAttribute("message", "Dịch vụ NFS đang dừng");
                request.setAttribute("nfsStatus", "inactive");
            } else {
                request.setAttribute("message", "Không thể xác định trạng thái NFS");
                request.setAttribute("nfsStatus", "unknown");
            }
        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi kiểm tra trạng thái NFS: " + e.getMessage());
        }
    }

    private void listFiles(Session session, HttpServletRequest request, HttpServletResponse response)
            throws JSchException, IOException {
        String host = request.getParameter("host");
        String user = request.getParameter("user");
        // Lấy đường dẫn hiện tại từ parameter, nếu không có thì dùng thư mục home
        String currentPath = request.getParameter("path");
        if (currentPath == null || currentPath.isEmpty()) {
            currentPath = "/home/" + user;
        }

        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            String command = "ls " + currentPath;
            channel.setCommand(command);

            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            channel.setOutputStream(responseStream);
            channel.connect();

            while (channel.isConnected()) {
                Thread.sleep(100);
            }

            String[] lines = responseStream.toString().split("\n");
            List<Map<String, String>> fileList = new ArrayList<>();

            // Thêm thư mục cha (..) nếu không phải thư mục gốc
            if (!currentPath.equals("/home/" + user)) {
                Map<String, String> parentDir = new HashMap<>();
                parentDir.put("name", "..");
                parentDir.put("directory", "true");
                fileList.add(parentDir);
            }

            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    Map<String, String> file = new HashMap<>();
                    String fileName = line.trim();

                    // Kiểm tra xem có phải thư mục không
                    channel = (ChannelExec) session.openChannel("exec");
                    channel.setCommand("test -d " + currentPath + "/" + fileName + " && echo 'true' || echo 'false'");
                    ByteArrayOutputStream isDir = new ByteArrayOutputStream();
                    channel.setOutputStream(isDir);
                    channel.connect();
                    while (channel.isConnected()) {
                        Thread.sleep(100);
                    }

                    file.put("name", fileName);
                    file.put("directory", isDir.toString().trim());
                    fileList.add(file);

                    channel.disconnect();
                }
            }

            request.setAttribute("fileList", fileList);
            request.setAttribute("currentPath", currentPath);
            request.setAttribute("selectedHost", host);

            channel.disconnect();
        } catch (Exception e) {
            request.setAttribute("errmessage", "Không thể liệt kê thư mục: " + e.getMessage());
        }
    }

    private void listNFSShares(Session session, HttpServletRequest request) throws JSchException, IOException {
        String password = request.getParameter("password");
        List<NFSShare> shares = new ArrayList<>();

        ChannelExec channel = null;
        try {
            String cmd = "echo '" + password + "' | sudo -S exportfs -v | grep -v \"<world>\"";
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);

            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            channel.setOutputStream(responseStream);
            channel.connect();

            while (channel.isConnected()) {
                Thread.sleep(100);
            }

            String output = responseStream.toString().trim();
            if (output.isEmpty()) {
                System.out.println("Khong co thu muc nao dang duoc chia se qua NFS.");
                request.setAttribute("nfsShares", shares);
                return;
            }

            String[] lines = output.split("\n");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Tach path va client dung tab hoac space
                int firstTab = line.indexOf("\t");
                int firstSpace = line.indexOf(" ");

                int splitIndex = (firstTab != -1) ? firstTab : firstSpace;
                if (splitIndex == -1) {
                    System.err.println("Dong export khong hop le: " + line);
                    continue;
                }

                String pathPart = line.substring(0, splitIndex).trim();
                String clientPart = line.substring(splitIndex).trim();

                if (!clientPart.contains("(")) {
                    System.err.println("Dong export khong chua client hop le: " + line);
                    continue;
                }

                int openParen = clientPart.indexOf('(');
                String client = clientPart.substring(0, openParen).trim();
                String options = clientPart.substring(openParen + 1, clientPart.lastIndexOf(')')).trim();

                NFSShare share = new NFSShare();
                share.setPath(pathPart);
                share.setClients(client);
                share.setOptions(options);
                share.setPermissions(options.contains("rw") ? "rw" : "ro");

                shares.add(share);

                System.out.println("Tim thay thu muc: " + pathPart + " duoc chia se cho client: " + client + " voi quyen: " + share.getPermissions());
            }

            request.setAttribute("nfsShares", shares);
        } catch (Exception e) {
            request.setAttribute("errmessage", "Loi khi lay danh sach chia se: " + e.getMessage());
            System.err.println("Loi khi lay danh sach chia se: " + e.getMessage());
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

//    private void listNFSShares(Session session, HttpServletRequest request) throws JSchException, IOException {
//        String password = request.getParameter("password");
//        List<NFSShare> shares = new ArrayList<>();
//
//        try {
//            // Sử dụng exportfs -v để lấy thông tin chi tiết
//            String cmd = "echo '" + password + "' | sudo -S exportfs -v | grep -v \"<world>\""; // Loại bỏ dòng <world>
//            ChannelExec channel = (ChannelExec) session.openChannel("exec");
//            channel.setCommand(cmd);
//
//            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
//            channel.setOutputStream(responseStream);
//            channel.connect();
//
//            while (channel.isConnected()) {
//                Thread.sleep(100);
//            }
//
//            String[] lines = responseStream.toString().split("\n");
//            String currentPath = null;
//
//            for (String line : lines) {
//                line = line.trim();
//                if (line.isEmpty()) {
//                    continue;
//                }
//
//                // Nếu là dòng chứa đường dẫn (bắt đầu bằng /)
//                if (line.startsWith("/")) {
//                    currentPath = line;
//                } // Nếu là dòng chứa client và options
//                else if (currentPath != null && line.contains("(")) {
//                    NFSShare share = new NFSShare();
//                    share.setPath(currentPath);
//
//                    int openParen = line.indexOf('(');
//                    if (openParen > 0) {
//                        String client = line.substring(0, openParen).trim();
//                        String options = line.substring(openParen + 1, line.lastIndexOf(')'));
//
//                        share.setClients(client);
//                        share.setOptions(options);
//                        share.setPermissions(options.contains("rw") ? "rw" : "ro");
//                        shares.add(share);
//                    }
//                }
//            }
//
//            channel.disconnect();
//            request.setAttribute("nfsShares", shares);
//
//        } catch (Exception e) {
//            request.setAttribute("errmessage", "Lỗi khi lấy danh sách chia sẻ: " + e.getMessage());
//        }
//    }
    protected void shareDirectory(Session session, HttpServletRequest request) throws JSchException, IOException {
        String path = request.getParameter("path");
        String password = request.getParameter("password");
        String[] selectedClients = request.getParameterValues("selectedClients");
        String manualClient = request.getParameter("manualClient");
        String permissions = request.getParameter("permissions");
        String[] options = request.getParameterValues("options");

        try {
            if (path == null || path.trim().isEmpty()) {
                request.setAttribute("errmessage", "Duong dan khong duoc de trong");
                return;
            }

            // Kiem tra thu muc ton tai tren server
            String checkDirCmd = String.format("[ -d '%s' ] && echo 'exists'", path);
            System.out.println("Lenh: " + checkDirCmd);
            if (!"exists".equals(executeCommand(session, checkDirCmd, request).trim())) {
                request.setAttribute("errmessage", "Thu muc khong ton tai: " + path);
                return;
            }

            HttpSession ss = request.getSession();
            List<InforUser> users = (List<InforUser>) ss.getAttribute("users");
            String serverHost = request.getParameter("host");

            // Gom tat ca clients (selected + manual)
            Set<String> allClients = new LinkedHashSet<>();
            if (selectedClients != null) {
                Collections.addAll(allClients, selectedClients);
            }
            if (manualClient != null && !manualClient.trim().isEmpty()) {
                String[] manual = manualClient.split("[,\\s]+");
                for (String c : manual) {
                    if (!c.isBlank()) {
                        allClients.add(c.trim());
                    }
                }
            }

            if (allClients.isEmpty()) {
                request.setAttribute("errmessage", "Khong co client nao duoc chon");
                return;
            }

            // Lay noi dung /etc/exports
            String existingExports = executeCommand(session, "cat /etc/exports", request);
            System.out.println("Lenh: " + existingExports);
            // Phan tich noi dung /etc/exports thanh Map
            Map<String, Set<String>> exportMap = new HashMap<>();
            String[] lines = existingExports.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || !line.contains("(")) {
                    continue;
                }
                String pathPart = line.split("\\s+")[0];
                String clientPart = line.substring(pathPart.length()).trim();
                int openParen = clientPart.indexOf('(');
                if (openParen > 0) {
                    String client = clientPart.substring(0, openParen).trim();
                    exportMap.computeIfAbsent(pathPart, k -> new HashSet<>()).add(client);
                }
            }

            // Chi giu lai nhung client chua duoc chia se
            List<String> clientsToAdd = new ArrayList<>();
            for (String client : allClients) {
                boolean alreadyShared = exportMap.getOrDefault(path, new HashSet<>()).contains(client);
                if (!alreadyShared) {
                    clientsToAdd.add(client);
                }
            }

            if (clientsToAdd.isEmpty()) {
                request.setAttribute("errmessage", "Thu muc da duoc chia se voi tat ca client.");
                return;
            }

            // Tao noi dung exports moi
            StringBuilder optionsStr = new StringBuilder(permissions);
            if (options != null) {
                for (String opt : options) {
                    optionsStr.append(",").append(opt);
                }
            } else {
                // Neu khong co lua chon, mac dinh them 3 cai can thiet
                optionsStr.append(",sync,no_root_squash,no_subtree_check");
            }

            StringBuilder exportsContent = new StringBuilder();
            for (String client : clientsToAdd) {
                exportsContent.append(path).append(" ").append(client)
                        .append("(").append(optionsStr).append(")\n");
            }

            // Ghi vao /etc/exports + reload
            String fullCommand = String.format(
                    "echo '%s' | sudo -S bash -c 'echo \"%s\" >> /etc/exports && exportfs -ra'",
                    password, exportsContent.toString().replace("\"", "\\\"")
            );
            System.out.println("Lenh: " + fullCommand);
            executeCommand(session, fullCommand, request);

            // Mount tren cac client moi
            ExecutorService pool = Executors.newFixedThreadPool(4);
            for (String clientHost : clientsToAdd) {
                users.stream().filter(u -> u.getHost().equals(clientHost)).findFirst().ifPresent(clientInfo -> {
                    pool.submit(() -> {
                        try {
                            configureClientMount(clientInfo, serverHost, path, permissions);
                        } catch (Exception e) {
                            System.err.println("Loi mount tren client " + clientInfo.getHost() + ": " + e.getMessage());
                        }
                    });
                });
            }
            pool.shutdown();
            pool.awaitTermination(20, TimeUnit.SECONDS);
            listNFSShares(session, request); // cap nhat lai danh sach share
            request.setAttribute("message", "Da chia se thu muc " + path + " cho " + clientsToAdd.size() + " client");
        } catch (Exception e) {
            request.setAttribute("errmessage", "Loi khi chia se thu muc: " + e.getMessage());
        }
    }

//    protected void shareDirectory(Session session, HttpServletRequest request) throws JSchException, IOException {
//        String path = request.getParameter("path");
//        String password = request.getParameter("password");
//        String[] selectedClients = request.getParameterValues("selectedClients");
//        String manualClient = request.getParameter("manualClient");
//        String permissions = request.getParameter("permissions");
//        String[] options = request.getParameterValues("options");
//
//        try {
//            // Kiểm tra đường dẫn
//            if (path == null || path.trim().isEmpty()) {
//                request.setAttribute("errmessage", "Đường dẫn không được để trống");
//                return;
//            }
//
//            // Kiểm tra xem thư mục có tồn tại không
//            String checkDirCmd = String.format("[ -d '%s' ] && echo 'exists' || echo 'not exists'", path);
//            String checkResult = executeCommand(session, checkDirCmd, request);
//            if (!"exists".equals(checkResult.trim())) {
//                request.setAttribute("errmessage", "Thư mục không tồn tại: " + path);
//                return;
//            }
//
//            HttpSession ss = request.getSession();
//            List<InforUser> users = (List<InforUser>) ss.getAttribute("users");
//            String serverHost = request.getParameter("host");
//
//            // Tạo chuỗi tùy chọn
//            StringBuilder optionsStr = new StringBuilder(permissions);
//            if (options != null) {
//                for (String option : options) {
//                    optionsStr.append(",").append(option);
//                }
//            }
//
//            // Kiểm tra xem share đã tồn tại chưa
//            String checkExportsCmd = "cat /etc/exports";
//            String existingExports = executeCommand(session, checkExportsCmd, request);
//            
//            // Tạo StringBuilder để gộp tất cả các dòng exports mới
//            StringBuilder newExportLines = new StringBuilder();
//            List<String> clientsToAdd = new ArrayList<>();
//
//            // Xử lý các client được chọn từ danh sách
//            if (selectedClients != null && selectedClients.length > 0) {
//                for (String clientHost : selectedClients) {
//                    // Kiểm tra xem share này đã tồn tại chưa
//                    if (!existingExports.contains(path + " " + clientHost)) {
//                        clientsToAdd.add(clientHost);
//                        newExportLines.append(path).append(" ")
//                                    .append(clientHost).append("(")
//                                    .append(optionsStr.toString()).append(")\n");
//                    }
//                }
//            }
//
//            // Xử lý client thủ công
//            if (manualClient != null && !manualClient.trim().isEmpty()) {
//                String[] manualClients = manualClient.split("[,\\s]+");
//                for (String client : manualClients) {
//                    client = client.trim();
//                    if (!client.isEmpty() && !existingExports.contains(path + " " + client)) {
//                        clientsToAdd.add(client);
//                        newExportLines.append(path).append(" ")
//                                    .append(client).append("(")
//                                    .append(optionsStr.toString()).append(")\n");
//                    }
//                }
//            }
//
//            if (clientsToAdd.isEmpty()) {
//                request.setAttribute("errmessage", "Không có client mới nào để thêm hoặc share đã tồn tại");
//                return;
//            }
//
//            // Thêm các share mới vào /etc/exports
//            String addExportCmd = "echo '" + password + "' | sudo -S bash -c 'echo -e \"" + 
//                                newExportLines.toString().replace("'", "'\\''") + "\" >> /etc/exports'";
//            executeCommand(session, addExportCmd, request);
//
//            // Kiểm tra xem đã thêm thành công chưa
//            String verifyCmd = "cat /etc/exports";
//            String verifyResult = executeCommand(session, verifyCmd, request);
//            
//            boolean allAdded = clientsToAdd.stream()
//                .allMatch(client -> verifyResult.contains(path + " " + client));
//
//            if (!allAdded) {
//                request.setAttribute("errmessage", "Không thể thêm một số client vào /etc/exports");
//                return;
//            }
//
//            // Cập nhật exports
//            String updateCmd = "echo '" + password + "' | sudo -S exportfs -ra";
//            executeCommand(session, updateCmd, request);
//
//            // Kiểm tra xem exports đã được cập nhật chưa
//            String checkExportCmd = "echo '" + password + "' | sudo -S exportfs -v";
//            String checkExportResult = executeCommand(session, checkExportCmd, request);
//
//            // Cấu hình các client
//            List<Thread> clientTasks = new ArrayList<>();
//            for (String clientHost : clientsToAdd) {
//                InforUser clientInfo = users.stream()
//                    .filter(u -> u.getHost().equals(clientHost))
//                    .findFirst()
//                    .orElse(null);
//
//                if (clientInfo != null) {
//                    Thread clientThread = new Thread(() -> {
//                        try {
//                            configureClientMount(clientInfo, serverHost, path, permissions);
//                        } catch (Exception e) {
//                            System.err.println("Lỗi cấu hình client " + clientInfo.getHost() + ": " + e.getMessage());
//                        }
//                    });
//                    clientTasks.add(clientThread);
//                    clientThread.start();
//                }
//            }
//
//            // Chờ tất cả các client được cấu hình xong
//            for (Thread task : clientTasks) {
//                try {
//                    task.join();
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//
//            request.setAttribute("message", "Đã chia sẻ thư mục " + path + " thành công cho " + clientsToAdd.size() + " client");
//
//        } catch (Exception e) {
//            request.setAttribute("errmessage", "Lỗi khi chia sẻ thư mục: " + e.getMessage());
//        }
//    }
    // Phương thức riêng để cấu hình mount trên client
    private void configureClientMount(InforUser clientInfo, String serverHost, String path, String permissions)
            throws JSchException, IOException {
        Session clientSession = null;
        try {
            clientSession = InforUser.connect(
                    clientInfo.getHost(),
                    clientInfo.getPort(),
                    clientInfo.getUser(),
                    clientInfo.getPassword()
            );

            String[] pathParts = path.split("/");
            String folderName = pathParts[pathParts.length - 1];
            String mountPoint = "/home/" + clientInfo.getUser() + "/" + folderName;

            String setupCmd = String.format(
                    "echo '%s' | sudo -S bash -c '"
                    + "mkdir -p %s && "
                    + "chown %s:%s %s && "
                    + "chmod 755 %s && "
                    + "echo \"%s:%s %s nfs %s 0 0\" >> /etc/fstab && "
                    + "mount -a'",
                    clientInfo.getPassword(),
                    mountPoint,
                    clientInfo.getUser(), clientInfo.getUser(), mountPoint,
                    mountPoint,
                    serverHost, path, mountPoint, permissions
            );
            System.out.println("Lenh mount: " + setupCmd);

            executeCommand(clientSession, setupCmd, null);
        } finally {
            if (clientSession != null) {
                clientSession.disconnect();
            }
        }
    }

    private void unshareDirectory(Session session, HttpServletRequest request) throws JSchException, IOException {
        String path = request.getParameter("path");
        String password = request.getParameter("password");
        String clientHost = request.getParameter("clientHost");

        try {
            HttpSession ss = request.getSession();
            List<InforUser> users = (List<InforUser>) ss.getAttribute("users");

            // 1. Kiem tra file /etc/exports co ton tai duong dan va client nay khong
            String checkExportsCmd = String.format("grep -E '^%s\\s+%s\\(' /etc/exports || echo 'NOT_FOUND'", path, clientHost);
            String checkResult = executeCommand(session, "echo '" + password + "' | sudo -S bash -c \"" + checkExportsCmd + "\"", request);

            if (checkResult.trim().equals("NOT_FOUND")) {
                request.setAttribute("errmessage", "Khong tim thay cau hinh chia se thu muc: " + path + " cho client: " + clientHost);
                return;
            }

            // 2. Xoa dung dong export cua path + client do
            String sedDeleteCmd = String.format(
                    "sed -i '/^%s\\s\\+%s(/d' /etc/exports",
                    path.replace("/", "\\/"), clientHost
            );
            String fullSedCmd = "echo '" + password + "' | sudo -S bash -c \"" + sedDeleteCmd + "\"";
            executeCommand(session, fullSedCmd, request);

            // 3. Reload exportfs
            executeCommand(session, "echo '" + password + "' | sudo -S exportfs -ra", request);

            // 4. Tim client de umount
            Optional<InforUser> clientOpt = users.stream()
                    .filter(u -> u.getHost().equals(clientHost))
                    .findFirst();

            if (clientOpt.isPresent()) {
                InforUser clientInfo = clientOpt.get();
                Session clientSession = null;
                try {
                    clientSession = InforUser.connect(
                            clientInfo.getHost(),
                            clientInfo.getPort(),
                            clientInfo.getUser(),
                            clientInfo.getPassword()
                    );

                    String folderName = path.substring(path.lastIndexOf('/') + 1);
                    String mountPoint = "/home/" + clientInfo.getUser() + "/" + folderName;

                    // 5. Umount + xoa fstab + xoa thu muc
//                    String cleanupCmd = String.format(
//                            "echo '%s' | sudo -S bash -c '"
//                            + "if mountpoint -q %s; then umount -f %s; fi; "
//                            + // Chi umount neu mountpoint ton tai
//                            "sed -i \"/%s/d\" /etc/fstab; "
//                            + "rm -rf %s'",
//                            clientInfo.getPassword(),
//                            mountPoint, mountPoint,
//                            mountPoint,
//                            mountPoint
//                    );
                    String cleanupCmd = String.format(
                            "echo '%s' | sudo -S bash -c '"
                            + "if mountpoint -q %s; then umount -lf %s; fi; "
                            + "grep -v \"%s\" /etc/fstab > /tmp/fstab.tmp && mv /tmp/fstab.tmp /etc/fstab; "
                            + "rm -rf %s'",
                            clientInfo.getPassword(),
                            mountPoint, mountPoint,
                            mountPoint,
                            mountPoint
                    );
                    executeCommand(clientSession, cleanupCmd, null);

                    executeCommand(clientSession, cleanupCmd, null);

                    // 6. Kiem tra lai mount
                    String checkMountCmd = String.format(
                            "mount | grep -q '%s' && echo 'still_mounted' || echo 'unmounted'",
                            mountPoint
                    );
                    String checkMountResult = executeCommand(clientSession, checkMountCmd, null);

                    if ("still_mounted".equals(checkMountResult.trim())) {
                        System.err.println("Loi: Umount that bai o client " + clientInfo.getHost() + " cho mountPoint " + mountPoint);
                    } else {
                        System.out.println("Da umount thanh cong tren client " + clientInfo.getHost() + " cho mountPoint " + mountPoint);
                    }

                } catch (Exception e) {
                    System.err.println("Loi khi umount ben client: " + clientHost + " - " + e.getMessage());
                } finally {
                    if (clientSession != null && clientSession.isConnected()) {
                        clientSession.disconnect();
                    }
                }
            } else {
                System.err.println("Khong tim thay client de umount: " + clientHost);
            }

            listNFSShares(session, request); // cap nhat lai danh sach share
            request.setAttribute("message", "Da huy chia se thu muc " + path + " voi client " + clientHost);
        } catch (Exception e) {
            request.setAttribute("errmessage", "Loi khi huy chia se thu muc: " + e.getMessage());
        }
    }

//    private void unshareDirectory(Session session, HttpServletRequest request) throws JSchException, IOException {
//        String path = request.getParameter("path");
//        String password = request.getParameter("password");
//        String clientHost = request.getParameter("clientHost");
//
//        try {
//            HttpSession ss = request.getSession();
//            List<InforUser> users = (List<InforUser>) ss.getAttribute("users");
//
//            // Kiểm tra xem share có tồn tại không
//            String checkCmd = "echo '" + password + "' | sudo -S cat /etc/exports | grep '" + path + "'";
//            String checkResult = executeCommand(session, checkCmd, request);
//
//            if (checkResult.isEmpty()) {
//                request.setAttribute("errmessage", "Không tìm thấy thư mục chia sẻ " + path);
//                return;
//            }
//
//            // Xóa share khỏi /etc/exports trên server - Sửa lại lệnh sed
//            String sedCmd = String.format(
//                    "echo '%s' | sudo -S sed -i '\\|%s[[:space:]]*%s|d' /etc/exports",
//                    password, path, clientHost
//            );
//            executeCommand(session, sedCmd, request);
//
//            // Kiểm tra xem đã xóa thành công chưa
//            String verifyCmd = "echo '" + password + "' | sudo -S cat /etc/exports | grep '" + path + "'";
//            String verifyResult = executeCommand(session, verifyCmd, request);
//
//            if (verifyResult.contains(clientHost)) {
//                request.setAttribute("errmessage", "Không thể xóa cấu hình chia sẻ. Vui lòng thử lại.");
//                return;
//            }
//
//            // Cập nhật exports ngay lập tức
//            String updateCmd = "echo '" + password + "' | sudo -S exportfs -ra";
//            executeCommand(session, updateCmd, request);
//
//            // Tìm thông tin client từ session
//            InforUser clientInfo = users.stream()
//                    .filter(u -> u.getHost().equals(clientHost))
//                    .findFirst()
//                    .orElse(null);
//
//            if (clientInfo != null) {
//                Session clientSession = InforUser.connect(
//                        clientInfo.getHost(),
//                        clientInfo.getPort(),
//                        clientInfo.getUser(),
//                        clientInfo.getPassword()
//                );
//
//                try {
//                    // Lấy tên thư mục cuối cùng từ path
//                    String[] pathParts = path.split("/");
//                    String folderName = pathParts[pathParts.length - 1];
//
//                    // Mount point trong thư mục home của user
//                    String mountPoint = "/home/" + clientInfo.getUser() + "/" + folderName;
//
//                    // Unmount và dọn dẹp
//                    String cleanupCmd = String.format(
//                            "echo '%s' | sudo -S bash -c '"
//                            + "umount -f %s 2>/dev/null; "
//                            + "sed -i '\\|%s|d' /etc/fstab; "
//                            + "rm -rf %s; "
//                            + "echo \"Done\"'",
//                            clientInfo.getPassword(),
//                            mountPoint,
//                            mountPoint,
//                            mountPoint
//                    );
//                    executeCommand(clientSession, cleanupCmd, request);
//
//                } finally {
//                    clientSession.disconnect();
//                }
//            }
//
//            // Cập nhật lại danh sách shares
//            listNFSShares(session, request);
//
//            request.setAttribute("message", "Đã hủy chia sẻ thư mục " + path + " với " + clientHost);
//        } catch (Exception e) {
//            request.setAttribute("errmessage", "Lỗi khi hủy chia sẻ thư mục: " + e.getMessage());
//        }
//    }
    private void downloadFile(Session session, HttpServletRequest request, HttpServletResponse response)
            throws JSchException, IOException {
        String path = request.getParameter("path");
        String fileName = path.substring(path.lastIndexOf('/') + 1);

        try {
            // Kiểm tra file có tồn tại không
            ChannelExec checkChannel = (ChannelExec) session.openChannel("exec");
            checkChannel.setCommand("test -f " + path + " && echo 'exists'");
            ByteArrayOutputStream checkOutput = new ByteArrayOutputStream();
            checkChannel.setOutputStream(checkOutput);
            checkChannel.connect();

            while (checkChannel.isConnected()) {
                Thread.sleep(100);
            }

            if (!checkOutput.toString().contains("exists")) {
                request.setAttribute("errmessage", "File không tồn tại hoặc không phải là file thông thường");
                return;
            }

            checkChannel.disconnect();

            // Thực hiện download
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            // Thiết lập response
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            // Tải file
            InputStream in = sftpChannel.get(path);
            OutputStream out = response.getOutputStream();

            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            in.close();
            out.close();
            sftpChannel.disconnect();

            request.setAttribute("message", "Đã tải xuống " + fileName);
        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi khi tải xuống: " + e.getMessage());
        }
    }

    private void deleteFile(Session session, HttpServletRequest request, HttpServletResponse response)
            throws JSchException, IOException {
        String fileName = request.getParameter("fileName");
        String currentPath = request.getParameter("currentPath");
        String fullPath = currentPath + "/" + fileName;

        try {
            // Kiểm tra xem file/thư mục có tồn tại không
            ChannelExec checkChannel = (ChannelExec) session.openChannel("exec");
            checkChannel.setCommand("test -e " + fullPath + " && echo 'exists'");
            ByteArrayOutputStream checkOutput = new ByteArrayOutputStream();
            checkChannel.setOutputStream(checkOutput);
            checkChannel.connect();

            while (checkChannel.isConnected()) {
                Thread.sleep(100);
            }

            if (!checkOutput.toString().contains("exists")) {
                request.setAttribute("errmessage", "File hoặc thư mục không tồn tại");
                return;
            }

            checkChannel.disconnect();

            // Thực hiện xóa
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("rm -rf " + fullPath);
            channel.connect();

            while (channel.isConnected()) {
                Thread.sleep(100);
            }

            if (channel.getExitStatus() == 0) {
                request.setAttribute("message", "Đã xóa " + fileName);
                // Cập nhật lại danh sách file sau khi xóa
                listFiles(session, request, response);
            } else {
                request.setAttribute("errmessage", "Không thể xóa " + fileName);
            }

            channel.disconnect();
        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi khi xóa: " + e.getMessage());
        }
    }

    private String executeCommand(Session session, String command, HttpServletRequest request) throws JSchException, IOException {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");

            // Thiết lập môi trường không yêu cầu TTY
            channel.setCommand("export DEBIAN_FRONTEND=noninteractive; " + command);

            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

            channel.setOutputStream(responseStream);
            channel.setErrStream(errorStream);

            channel.connect();

            // Chờ cho đến khi lệnh thực thi xong
            while (channel.isConnected()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            String response = responseStream.toString();
            String error = errorStream.toString();

            // Kiểm tra exit status
            if (channel.getExitStatus() != 0) {
                if (!error.isEmpty()) {
                    // Bỏ qua các thông báo sudo không quan trọng
                    if (!error.contains("[sudo] password for") && !error.contains("password for")) {
                        throw new IOException("Lỗi: " + error);
                    }
                }
            }

            return response;

        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    // Thêm phương thức mới để lấy danh sách mount NFS
    protected List<Map<String, String>> listNFSMounts(Session session, HttpServletRequest request) throws JSchException, IOException {
        List<Map<String, String>> mounts = new ArrayList<>();

        try {
            String password = request.getParameter("password");
            String cmd = "echo '" + password + "' | sudo -S df -hT | grep nfs";

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);

            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            channel.setOutputStream(responseStream);
            channel.connect();

            while (channel.isConnected()) {
                Thread.sleep(100);
            }

            String[] lines = responseStream.toString().split("\n");

            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 7) {
                        Map<String, String> mount = new HashMap<>();
                        mount.put("source", parts[0]);          // Server:Path
                        mount.put("type", parts[1]);           // Filesystem type (nfs)
                        mount.put("size", parts[2]);           // Total size
                        mount.put("used", parts[3]);           // Used space
                        mount.put("available", parts[4]);      // Available space
                        mount.put("usePercentage", parts[5]); // Use percentage
                        mount.put("mountPoint", parts[6]);     // Mount point
                        mounts.add(mount);
                    }
                }
            }

            // Lưu vào session
            request.getSession().setAttribute("nfsMounts", mounts);

        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi khi lấy danh sách mount: " + e.getMessage());
        }

        return mounts;
    }

    private void createFolder(Session session, HttpServletRequest request) throws JSchException, IOException {
        String currentPath = request.getParameter("currentPath");
        String name = request.getParameter("name");
        String password = request.getParameter("password");

        try {
            String folderPath = currentPath + "/" + name;
            String cmd = String.format(
                    "echo '%s' | sudo -S mkdir -p '%s' && chmod 755 '%s'",
                    password, folderPath, folderPath
            );
            executeCommand(session, cmd, request);
            request.setAttribute("message", "Đã tạo thư mục " + name);
        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi khi tạo thư mục: " + e.getMessage());
        }
    }

    private void createFile(Session session, HttpServletRequest request) throws JSchException, IOException {
        String currentPath = request.getParameter("currentPath");
        String name = request.getParameter("name");
        String password = request.getParameter("password");

        try {
            String filePath = currentPath + "/" + name;
            String cmd = String.format(
                    "echo '%s' | sudo -S touch '%s' && chmod 644 '%s'",
                    password, filePath, filePath
            );
            executeCommand(session, cmd, request);
            request.setAttribute("message", "Đã tạo tập tin " + name);
        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi khi tạo tập tin: " + e.getMessage());
        }
    }

    private void uploadFile(Session session, HttpServletRequest request) throws JSchException, IOException, ServletException {
        String currentPath = request.getParameter("currentPath");
        String password = request.getParameter("password");

        try {
            for (Part part : request.getParts()) {
                if (part.getSubmittedFileName() != null) {
                    String fileName = part.getSubmittedFileName();
                    String tempPath = "/tmp/" + fileName;
                    String destPath = currentPath + "/" + fileName;

                    // Lưu file tạm
                    part.write(tempPath);

                    // Di chuyển file đến thư mục đích
                    String cmd = String.format(
                            "echo '%s' | sudo -S mv '%s' '%s' && chmod 644 '%s'",
                            password, tempPath, destPath, destPath
                    );
                    executeCommand(session, cmd, request);
                }
            }
            request.setAttribute("message", "Đã tải lên các tập tin thành công");
        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi khi tải lên tập tin: " + e.getMessage());
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
