/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import Model.InforUser;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@WebServlet(name = "configFTP", urlPatterns = {"/configFTP"})
@MultipartConfig
public class configFTP extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String control = request.getParameter("control");
        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        int port = Integer.parseInt(ports);

        try {
            HttpSession ss = request.getSession();
            if (control.equals("status")) {
                String message = statusFTP(host, port, user, password);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }

            } else if (control.equals("start") || control.equals("restart") || control.equals("stop")) {
                String message = configFTP(control, host, port, user, password);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }

            } else if (control.equals("see")) {
                List<String> seeFTP = seeFTP(host, port, user, password);
                ss.setAttribute("seeFTP", seeFTP);
            } else if (control.equals("edit")) {
                System.out.println("Control: " + control);
                String[] line0 = request.getParameterValues("line0");
                String[] line1 = request.getParameterValues("line1");
                String file = "";
                for (int i = 0; i < line0.length; i++) {
                    if (!line0[i].isEmpty()) {
                        file += line0[i].trim() + "=" + line1[i].trim() + "\n";
                    }
                }
                String message = fileConfig(host, port, user, password, file);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
                List<String> seeFTP = seeFTP(host, port, user, password);
                ss.setAttribute("seeFTP", seeFTP);

            } else if (control.equals("upfile")) {
                Part filePart = null;
                try {
                    // Nhận file từ request
                    filePart = request.getPart("file");
                    String fileName = filePart.getSubmittedFileName();

                    // Khởi tạo phiên SFTP
                    JSch jsch = new JSch();
                    Session session = jsch.getSession(user, host, port);
                    session.setPassword(password);

                    // Cấu hình Properties cho SSH
                    Properties config = new Properties();
                    config.put("StrictHostKeyChecking", "no");
                    session.setConfig(config);

                    // Kết nối SSH và mở kênh SFTP
                    session.connect();
                    ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
                    channelSftp.connect();

                    // Gửi file qua SFTP
                    try (InputStream fileContent = filePart.getInputStream()) {
                        channelSftp.put(fileContent, "/home/" + user + "/" + fileName);
                    }
                    // Đóng kênh và session
                    channelSftp.disconnect();
                    session.disconnect();
                    String message = "Đã Upload file " + fileName;
                    request.setAttribute("message", message);
                } catch (Exception e) {
                    String errmessage = "Lỗi Upload file: " + e.getMessage();
                    request.setAttribute("errmessage", errmessage);
                }
            } else if (control.equals("downfile")) {
                String remoteFile = request.getParameter("remoteFile");
                String message = downFile(host, port, user, password, remoteFile);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
            } else {
                String errmessage = "Dịch vụ bảo trì";
                request.setAttribute("errmessage", errmessage);
            }
            request.getRequestDispatcher("./ftp.jsp").forward(request, response);
        } catch (ServletException | IOException e) {
        }
    }

    protected String configFTP(String control, String host, int port, String user, String password) {
        String result = "";
        String command = "echo '" + password + "' | sudo -S systemctl " + control + " vsftpd.service";

        try {
            Session session = InforUser.connect(host, port, user, password);
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuilder outBuff = new StringBuilder();
            StringBuilder errBuff = new StringBuilder();
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    outBuff.append(new String(tmp, 0, i));
                }
                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    errBuff.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ee) {
                    ee.printStackTrace();
                }
            }
            if (errBuff.length() == (22 + user.length())) {
                result = "Đã " + control + " dịch vụ FTP";
            } else {
                result = "Lỗi: " + errBuff.toString().substring(22 + user.length());
            }

            channel.disconnect();
            session.disconnect();

        } catch (IOException | JSchException ioEx) {
        }
        return result;
    }

    protected String statusFTP(String host, int port, String user, String password) {
        String result = "";
        String command = "systemctl status vsftpd.service";
        try {
            Session session = InforUser.connect(host, port, user, password);
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuilder outBuff = new StringBuilder();
            StringBuilder errBuff = new StringBuilder();

            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    outBuff.append(new String(tmp, 0, i));
                }
                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    errBuff.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ee) {
                }
            }

            int start = outBuff.indexOf("Active:");
            int index = outBuff.indexOf("since");
            if (index != -1) {
                result = "Trạng thái dịch vụ FTP: " + outBuff.substring(start + 7, index);
            } else {
                result = "Lỗi: " + errBuff.toString();
            }
            channel.disconnect();
            session.disconnect();

        } catch (IOException | JSchException ioEx) {
            result = "Lỗi: " + ioEx.getMessage();
        }
        return result;
    }

    protected List<String> seeFTP(String host, int port, String user, String password) {
        Session session = null;
        ChannelExec channel = null;
        // Đọc kết quả
        StringBuilder outBuff = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        List<String> lines = new ArrayList<>();
        try {
            String cmd = "cat /etc/vsftpd.conf";
            // Tạo session và kết nối
            session = InforUser.connect(host, port, user, password);
            // Mở channel và thực thi lệnh
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            // Đọc kết quả
            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            BufferedReader errreader = new BufferedReader(new InputStreamReader(channel.getErrStream()));
            channel.connect();

            String line;
            while ((line = reader.readLine()) != null) {
                // Kiểm tra xem dòng có rỗng hay không và có bắt đầu bằng dấu # không
                if (line.trim().isEmpty() || line.trim().charAt(0) == '#') {
                    continue;
                } else {
                    outBuff.append(line).append("<br>");
                    lines.add(line);
                }
            }
            while ((line = errreader.readLine()) != null) {
                errors.append(line).append("<br>");
                lines.add(line);
            }

        } catch (JSchException | IOException e) {
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();

                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }
        }

        if (errors.length() > 0) {
            String message = "Chưa có dịch vụ FTP, vui lòng tải dịch vụ để thực hiện";
            lines.remove("cat: /etc/vsftpd.conf: No such file or directory");
            lines.add(message);
            System.out.println("Loi: " + lines);
            return lines;
        } else {
//            return outBuff.toString();
//            System.out.println("Khong loi: " + lines);
            return lines;
        }

    }

    protected String fileConfig(String host, int port, String user, String password, String file) {
        String result = "";
        String command1 = "sudo -S cp /etc/vsftpd.conf /home/" + user + "/" + user + "ftp.txt";
        String command2 = "sudo -S bash -c 'echo \"" + file + "\" > /etc/vsftpd.conf'";
        String command3 = "sudo -S vsftpd /etc/vsftpd.conf";
        String command4 = "sudo -S cp /home/" + user + "/" + user + "ftp.txt /etc/vsftpd.conf";
        StringBuilder outBuff = new StringBuilder(); // Lưu đầu ra
        StringBuilder errBuff = new StringBuilder(); // Lưu lỗi đầu ra

        try {
            // Tạo phiên kết nối SSH
            Session session = InforUser.connect(host, port, user, password);

            // Danh sách các lệnh cần thực thi
            String[] commands = {command1, command2, command3};
            for (String command : commands) {

                // Mở kênh thực thi cho mỗi lệnh
                ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
                channelExec.setCommand(command);

                // Truyền mật khẩu vào sudo qua OutputStream
                OutputStream out = channelExec.getOutputStream();
                InputStream in = channelExec.getInputStream();
                InputStream errorStream = channelExec.getErrStream();
                channelExec.connect();

                // Gửi mật khẩu vào lệnh sudo
                out.write((password + "\n").getBytes());
                out.flush();

                // Đọc đầu ra
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    outBuff.append(line);
                }

                // Đọc đầu ra lỗi
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errBuff.append(errorLine);
                }
                System.out.println("CMD: " + command + "\n outBuff: " + outBuff + "\n errBuff: " + errBuff);
                // Ngắt kết nối kênh sau khi thực thi xong lệnh
                channelExec.disconnect();
                // Bỏ qua lỗi nhập passs 
                if (errBuff.length() == 22 + user.length() || errBuff.toString().contains("Syntax OK")) {
                    errBuff.setLength(0);
                }

                //Kiểm tra lỗi
                if (errBuff.length() > 0) {
                    result = "Lỗi: " + errBuff.toString();
                    // Chạy command4
                    command = command4;
                    channelExec = (ChannelExec) session.openChannel("exec");
                    channelExec.setCommand(command);
                    out = channelExec.getOutputStream();
                    in = channelExec.getInputStream();
                    errorStream = channelExec.getErrStream();
                    channelExec.connect();

                    out.write((password + "\n").getBytes());
                    out.flush();

                    reader = new BufferedReader(new InputStreamReader(in));
                    while ((line = reader.readLine()) != null) {
                        outBuff.append(line).append("\n");
                    }
                    errorReader = new BufferedReader(new InputStreamReader(errorStream));
                    while ((errorLine = errorReader.readLine()) != null) {
                        errBuff.append(errorLine).append("\n");
                    }
                    System.out.println("CMD: " + command + "\n outBuff: " + outBuff + "\n errBuff: " + errBuff);
                    channelExec.disconnect();
                    break;

                }
            }
            // Đóng phiên SSH
            session.disconnect();

            // Nếu không có lỗi, trả về kết quả đầu ra
            if (result.isEmpty()) {
                result = "Đã cập nhật file cấu hình FTP" + outBuff.toString();
            }
        } catch (IOException | JSchException ioEx) {
            ioEx.printStackTrace();
            result = "Lỗi: " + ioEx.getMessage();
        }
        return result;
    }

    protected String downFile(String host, int port, String user, String password, String remoteFile) {
        String result = null;
        String[] list = remoteFile.split("/");
        String localFile = "C:\\Users\\acer\\Downloads\\" + list[list.length - 1];    // đường dẫn lưu file cục bộ
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            // Thiết lập kết nối
            session = jsch.getSession(user, host, port);
            session.setPassword(password);

            // Thiết lập cấu hình
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            // Mở kênh SFTP
//            Channel channel = session.openChannel("sftp");
//            channel.connect();
//            channelSftp = (ChannelSftp) channel;
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // Tải file xuống
            try (OutputStream outputStream = new FileOutputStream(localFile)) {
                channelSftp.get(remoteFile, outputStream);
                result = "File downloaded successfully!";
            }

        } catch (JSchException | SftpException | IOException e) {
            result = "Lỗi downloading file: " + e.getMessage();
        } finally {
            // Đóng kênh và phiên
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
        return result;
    }

}
