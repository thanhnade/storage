package Controller;

import Model.InforUser;
import Model.NFSShare;
import com.jcraft.jsch.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import java.io.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.RequestDispatcher;
import java.util.List;
import java.util.ArrayList;

@WebServlet(name = "NFSService", urlPatterns = {"/nfs"})
public class NFSService extends HttpServlet {

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

            // Kiểm tra NFS đã được cài đặt chưa
            if (!isNFSInstalled(session)) {
                installNFS(session, password);
            }

            String result = "";
            switch (action) {
                case "start":
                    result = startNFS(session, password);
                    request.setAttribute("message", "Đã khởi động dịch vụ NFS thành công");
                    break;

                case "stop":
                    result = stopNFS(session, password);
                    request.setAttribute("message", "Đã dừng dịch vụ NFS thành công");
                    break;

                case "restart":
                    result = restartNFS(session, password);
                    request.setAttribute("message", "Đã khởi động lại dịch vụ NFS thành công");
                    break;

                case "status":
                    result = checkNFSStatus(session, password);
                    if (result.contains("active (running)")) {
                        request.setAttribute("message", "Dịch vụ NFS đang hoạt động");
                    } else if (result.contains("inactive (dead)")) {
                        request.setAttribute("message", "Dịch vụ NFS đang dừng");
                    } else {
                        request.setAttribute("message", "Trạng thái NFS: " + result);
                    }
                    break;

                case "list":
                    result = listShares(session, password);
                    System.out.println("list: \n" + result);
                    request.setAttribute("nfsShares", parseShares(result));
                    request.setAttribute("message", "Đã cập nhật danh sách chia sẻ");
                    break;

                case "listUserDirectories":
                    result = listUserDirectories(session, password);
                    request.setAttribute("userDirectories", result.split("\n"));
                    request.setAttribute("message", "Đã cập nhật danh sách thư mục người dùng");
                    break;

                default:
                    request.setAttribute("errmessage", "Hành động không hợp lệ");
                    break;
            }

            // Luôn cập nhật danh sách chia sẻ sau mỗi hành động
            String shares = listShares(session, password);
            request.setAttribute("nfsShares", parseShares(shares));

        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi: " + e.getMessage());
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }

        // Forward về trang JSP
        RequestDispatcher dispatcher = request.getRequestDispatcher("./nfs.jsp");
        dispatcher.forward(request, response);
    }

    // Kiểm tra NFS đã được cài đặt chưa
    public static boolean isNFSInstalled(Session session) throws JSchException, IOException, InterruptedException {
        String result = executeCommand(session, "dpkg -l | grep nfs-kernel-server");
        return !result.trim().isEmpty();
    }

    // Kiểm tra trạng thái dịch vụ NFS
    public static String checkNFSStatus(Session session, String password) throws JSchException, IOException, InterruptedException {
        String command = String.format("echo '%s' | sudo -S systemctl status nfs-kernel-server", password);
        return executeCommand(session, command);
    }

    // Cài đặt NFS Server
    public static String installNFS(Session session, String password) throws JSchException, IOException, InterruptedException {
        StringBuilder result = new StringBuilder();

        // Cập nhật package list
        String updateCmd = String.format("echo '%s' | sudo -S apt-get update", password);
        result.append(executeCommand(session, updateCmd));

        // Cài đặt NFS server
        String installCmd = String.format("echo '%s' | sudo -S apt-get install -y nfs-kernel-server", password);
        result.append(executeCommand(session, installCmd));

        return result.toString();
    }

    // Cấu hình thư mục chia sẻ
    public static String configureShare(Session session, String password, String directory, String clientIP, String permissions)
            throws JSchException, IOException, InterruptedException {
        StringBuilder result = new StringBuilder();

        // Tạo thư mục nếu chưa tồn tại
        String mkdirCmd = String.format("echo '%s' | sudo -S mkdir -p %s", password, directory);
        result.append(executeCommand(session, mkdirCmd));

        // Cấu hình quyền truy cập
        String chownCmd = String.format("echo '%s' | sudo -S chown nobody:nogroup %s", password, directory);
        result.append(executeCommand(session, chownCmd));

        String chmodCmd = String.format("echo '%s' | sudo -S chmod 777 %s", password, directory);
        result.append(executeCommand(session, chmodCmd));

        // Kiểm tra xem thư mục đã được chia sẻ chưa
        String checkExports = executeCommand(session, "cat /etc/exports");
        if (!checkExports.contains(directory)) {
            // Thêm cấu hình vào /etc/exports
            String exportLine = String.format("%s %s(%s)", directory, clientIP, permissions);
            String exportCmd = String.format("echo '%s' | sudo -S bash -c 'echo \"%s\" >> /etc/exports'",
                    password, exportLine);
            result.append(executeCommand(session, exportCmd));
        }

        // Áp dụng cấu hình
        String exportfsCmd = String.format("echo '%s' | sudo -S exportfs -ra", password);
        result.append(executeCommand(session, exportfsCmd));

        return result.toString();
    }

    // Xóa thư mục chia sẻ
    public static String removeShare(Session session, String password, String directory)
            throws JSchException, IOException, InterruptedException {
        StringBuilder result = new StringBuilder();

        // Xóa cấu hình khỏi /etc/exports
        String sedCmd = String.format("echo '%s' | sudo -S sed -i '\\|%s|d' /etc/exports",
                password, directory.replace("/", "\\/"));
        result.append(executeCommand(session, sedCmd));

        // Áp dụng thay đổi
        String exportfsCmd = String.format("echo '%s' | sudo -S exportfs -ra", password);
        result.append(executeCommand(session, exportfsCmd));

        return result.toString();
    }

    // Start dịch vụ NFS
    public static String startNFS(Session session, String password) throws JSchException, IOException, InterruptedException {
        String command = String.format("echo '%s' | sudo -S systemctl start nfs-kernel-server", password);
        return executeCommand(session, command);
    }

    // Stop dịch vụ NFS
    public static String stopNFS(Session session, String password) throws JSchException, IOException, InterruptedException {
        String command = String.format("echo '%s' | sudo -S systemctl stop nfs-kernel-server", password);
        return executeCommand(session, command);
    }

    // Restart dịch vụ NFS
    public static String restartNFS(Session session, String password) throws JSchException, IOException, InterruptedException {
        String command = String.format("echo '%s' | sudo -S systemctl restart nfs-kernel-server", password);
        return executeCommand(session, command);
    }

    // Liệt kê các thư mục đang chia sẻ
    public static String listShares(Session session, String password) throws JSchException, IOException, InterruptedException {
        return executeCommand(session, "echo " + password + " | sudo -S exportfs -v");
    }

    // Kiểm tra mount points
    public static String checkMounts(Session session) throws JSchException, IOException, InterruptedException {
        return executeCommand(session, "showmount -e localhost");
    }

    // Kiểm tra các client đang kết nối
    public static String checkClients(Session session, String password) throws JSchException, IOException, InterruptedException {
        String command = String.format("echo '%s' | sudo -S showmount -a localhost", password);
        return executeCommand(session, command);
    }

    // Liệt kê các thư mục trong /home/user
    public static String listUserDirectories(Session session, String password) throws JSchException, IOException, InterruptedException {
        return executeCommand(session, "echo " + password + " | sudo -S ls -d /home/user/*");
    }

    private static String executeCommand(Session session, String command)
            throws JSchException, IOException, InterruptedException {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        StringBuilder output = new StringBuilder();
        InputStream in = channel.getInputStream();
        InputStream err = channel.getErrStream();

        channel.connect();

        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) {
                    break;
                }
                output.append(new String(tmp, 0, i));
            }
            while (err.available() > 0) {
                int i = err.read(tmp, 0, 1024);
                if (i < 0) {
                    break;
                }
                output.append(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                if (channel.getExitStatus() != 0) {
                    output.append("Exit status: ").append(channel.getExitStatus());
                }
                break;
            }
            Thread.sleep(100);
        }

        channel.disconnect();
        return output.toString();
    }

    // Thêm phương thức để parse kết quả từ exportfs thành danh sách chia sẻ
    private List<NFSShare> parseShares(String exportfsOutput) {
        List<NFSShare> shares = new ArrayList<>();
        String[] lines = exportfsOutput.split("\n");

        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                NFSShare share = new NFSShare();
                // Parse mỗi dòng output từ exportfs
                // Ví dụ format: /path/to/share client.ip(rw,sync)
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    share.setPath(parts[0]);

                    // Parse phần client và permissions
                    String clientPart = parts[1];
                    int openParen = clientPart.indexOf('(');
                    if (openParen > 0) {
                        share.setClients(clientPart.substring(0, openParen));

                        // Parse permissions và options
                        String options = clientPart.substring(openParen + 1, clientPart.length() - 1);
                        if (options.contains("rw")) {
                            share.setPermissions("rw");
                        } else {
                            share.setPermissions("ro");
                        }
                        share.setOptions(options);
                    }
                    shares.add(share);
                }
            }
        }
        return shares;
    }
}
