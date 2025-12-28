/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import Model.InforUser;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author This PC
 */
@WebServlet(name = "configSSH", urlPatterns = {"/configSSH"})
public class configSSH extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String control = request.getParameter("control");
        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        int port = Integer.parseInt(ports);
        String useredit = request.getParameter("useredit");
        System.out.println(control);
        try {
            HttpSession ss = request.getSession();
            if (control.equals("status")) {
                String message = statusSSH(host, port, user, password);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
            } else if (control.equals("start") || control.equals("restart")) {
                String message = configSSH(control, host, port, user, password);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
            } else if (control.equals("see")) {
                List<String> seeSSH = seeSSH(control, host, port, user, password);
                ss.setAttribute("seeSSH", seeSSH);
            } else if (control.equals("changePort")) {
                String newports = request.getParameter("newport");
                int newPort = Integer.parseInt(newports);
                String message = changePort(host, port, user, password, newPort);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
                List<String> seeSSH = seeSSH(control, host, port, user, password);
                ss.setAttribute("seeSSH", seeSSH);
            } else if (control.equals("changeOldPort")) {
                String newports = request.getParameter("newport");
                String oldports = request.getParameter("oldport");
                int newPort = Integer.parseInt(newports);
                int oldPort = Integer.parseInt(oldports);
                String message = changeOldPort(host, port, user, password, newPort, oldPort);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
                List<String> seeSSH = seeSSH(control, host, port, user, password);
                ss.setAttribute("seeSSH", seeSSH);
            } else if (control.equals("edit")) {
                //khong duoc xoa 1 = 3
//                String[] lines = request.getParameterValues("line"); 
//                if (lines != null) {
//                    String file = "";
//                    for (String line : lines) {
//                        file += line + "\n";
//                    }
//                    String message = updateFile(host, port, user, password, file);
//                    request.setAttribute("message", message);
//                    List<String> seeSSH = seeSSH(control, host, port, user, password);
//                    ss.setAttribute("seeSSH", seeSSH);
//                } else {
//                    String message = "Không có thay đổi.";
//                    request.setAttribute("message", message);
//                }
                String[] line0 = request.getParameterValues("line0");
                String[] line1 = request.getParameterValues("line1");
                String file = "";
                for (int i = 0; i < line0.length; i++) {
                    file += line0[i].trim() + " " + line1[i].trim() + "\n";
                }
//                String endline = request.getParameter("endline");
//                file = file + endline;
                String message = fileConfig(host, port, user, password, file);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
                List<String> seeSSH = seeSSH(control, host, port, user, password);
                ss.setAttribute("seeSSH", seeSSH);
            } else if (control.equals("deny") || control.equals("allow")) {
                String message = denyallowUser(control, host, port, user, password, useredit);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
                List<String> seeSSH = seeSSH(control, host, port, user, password);
                ss.setAttribute("seeSSH", seeSSH);

            } else {
                String errmessage = "Dịch vụ bảo trì";
                request.setAttribute("errmessage", errmessage);
            }
            request.getRequestDispatcher("./ssh.jsp").forward(request, response);
        } catch (ServletException | IOException | NumberFormatException e) {
        }
    }

    protected String configSSH(String control, String host, int port, String user, String password) {
        String result = "";
        String command = "echo '" + password + "' | sudo -S systemctl" + control + " sshd";

        try {
            Session session = InforUser.connect(host, port, user, password);
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuilder outBuff = new StringBuilder();
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    outBuff.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            result = "Đã " + control + " dịch vụ SSH";

            channel.disconnect();
            session.disconnect();

        } catch (IOException | JSchException ioEx) {
            ioEx.printStackTrace();
            result = "Lỗi: " + ioEx.getMessage();
        }
        return result;
    }

    protected String statusSSH(String host, int port, String user, String password) {
        String result = "";
        String command = "systemctl status sshd";
        try {
            Session session = InforUser.connect(host, port, user, password);
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuilder outBuff = new StringBuilder();
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    outBuff.append(new String(tmp, 0, i));
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
                result = "Trạng thái dịch vụ SSH: " + outBuff.substring(start + 7, index - 10);
            }
            channel.disconnect();
            session.disconnect();

        } catch (IOException | JSchException ioEx) {
            ioEx.printStackTrace();
            result = "Lỗi: " + ioEx.getMessage();
        }
        return result;
    }

    protected List<String> seeSSH(String control, String host, int port, String user, String password) {
        String result = "";
        Session session = null;
        ChannelExec channel = null;
        // Đọc kết quả
        StringBuilder output = new StringBuilder();
        List<String> lines = new ArrayList<>();
        try {
            String cmd = "cat /etc/ssh/sshd_config";
            // Tạo session và kết nối
            session = InforUser.connect(host, port, user, password);
            // Mở channel và thực thi lệnh
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            // Đọc kết quả
            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            String line;
            while ((line = reader.readLine()) != null) {
                // Kiểm tra xem dòng có rỗng hay không và có bắt đầu bằng dấu # không
                if (line.trim().isEmpty() || line.trim().charAt(0) == '#') {
                    continue;
                } else {
//                    output.append(line).append("<br>");
                    lines.add(line);
                }
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
//        return output.toString();
        return lines;

    }

    protected String changePort(String host, int port, String user, String password, int newPort) {
        String result = "";
        Session session = null;
        ChannelExec channel = null;

        try {
            if (newPort < 1 || newPort > 65535) {
                return "Lỗi: Invalid port number. Please enter a number between 1 and 65535.";
            }

            // Kết nối SSH
            session = InforUser.connect(host, port, user, password);

            // Kiểm tra nếu dòng #Port 22 tồn tại
            String checkCommand = "grep -q '^#Port 22' /etc/ssh/sshd_config";
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(checkCommand);
            channel.connect();
            if (channel.getExitStatus() != 0) {
                result = "Lỗi: Không tìm thấy cấu hình cổng mặc định (#Port 22) trong file /etc/ssh/sshd_config.";
                channel.disconnect();
                session.disconnect();
                return result;
            }
            channel.disconnect();

            // Thay đổi cổng
            String updateCommand = "echo " + password + " | sudo -S sed -i 's/^#Port 22/Port " + newPort + "/' /etc/ssh/sshd_config";
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(updateCommand);
            channel.connect();
            if (channel.getExitStatus() != 0) {
                result = "Lỗi: Không thể thay đổi cổng SSH.";
                channel.disconnect();
                session.disconnect();
                return result;
            }
            channel.disconnect();

            // Khởi động lại SSH
            String restartCommand = "echo " + password + " | sudo -S systemctl restart sshd";
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(restartCommand);
            channel.connect();
            if (channel.getExitStatus() != 0) {
                result = "Lỗi: Không thể khởi động lại dịch vụ SSH.";
            } else {
                result = "Đã thay đổi cổng SSH thành " + newPort;
            }

        } catch (JSchException e) {
            result = "Lỗi: " + e.getMessage();
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return result;
    }

    protected String changeOldPort(String host, int port, String user, String password, int newPort, int oldPort) {
        String result = "";

        // Lệnh kiểm tra cổng cũ
        String checkCommand = "grep '^Port " + oldPort + "' /etc/ssh/sshd_config";
        String changeCommand = "echo " + password + " | sudo -S sed -i 's/^Port " + oldPort + "/Port " + newPort + "/' /etc/ssh/sshd_config";
        String restartSSHCommand = "echo " + password + " | sudo -S systemctl restart ssh";
        Session session = null;
        ChannelExec channel = null;

        try {
            session = InforUser.connect(host, port, user, password);

            // Kiểm tra cổng cũ
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(checkCommand);
            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuilder outBuff = new StringBuilder();
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    outBuff.append(new String(tmp, 0, i));
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

            // Nếu cổng cũ tồn tại, thực hiện thay đổi
            if (outBuff.toString().trim().length() > 0) {
                channel.disconnect(); // Ngắt kết nối cũ trước khi thực hiện thay đổi
                channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(changeCommand);
                in = channel.getInputStream();
                channel.connect();

                outBuff.setLength(0); // Clear output buffer

                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i < 0) {
                            break;
                        }
                        outBuff.append(new String(tmp, 0, i));
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

                // Khởi động lại dịch vụ SSH
                channel.disconnect();
                channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(restartSSHCommand);
                in = channel.getInputStream();
                channel.connect();

                outBuff.setLength(0); // Clear output buffer

                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i < 0) {
                            break;
                        }
                        outBuff.append(new String(tmp, 0, i));
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

                result = "Đã thay đổi cổng SSH từ " + oldPort + " thành " + newPort + " và đã khởi động lại dịch vụ SSH.";
            } else {
                result = "Lỗi: Cổng cũ " + oldPort + " không tồn tại trong tệp cấu hình.";
            }

            channel.disconnect();
            session.disconnect();

        } catch (IOException | JSchException ioEx) {
            result = "Lỗi: " + ioEx.getMessage();
        }

        return result;
    }

//    protected String changeOldPort(String host, int port, String user, String password, int newPort, int oldPort) {
//        String result = "";
//
//        // Lệnh kiểm tra cổng cũ
//        String checkCommand = "grep '^Port " + oldPort + "' /etc/ssh/sshd_config";
//        String changeCommand = "echo " + password + " | sudo -S sed -i 's/^Port " + oldPort + "/Port " + newPort + "/' /etc/ssh/sshd_config";
//        Session session = null;
//        ChannelExec channel = null;
//
//        try {
//            session = InforUser.connect(host, port, user, password);
//
//            // Kiểm tra cổng cũ
//            channel = (ChannelExec) session.openChannel("exec");
//            channel.setCommand(checkCommand);
//            InputStream in = channel.getInputStream();
//            channel.connect();
//
//            byte[] tmp = new byte[1024];
//            StringBuilder outBuff = new StringBuilder();
//            while (true) {
//                while (in.available() > 0) {
//                    int i = in.read(tmp, 0, 1024);
//                    if (i < 0) {
//                        break;
//                    }
//                    outBuff.append(new String(tmp, 0, i));
//                }
//                if (channel.isClosed()) {
//                    break;
//                }
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException ee) {
//                    ee.printStackTrace();
//                }
//            }
//
//            // Nếu cổng cũ tồn tại, thực hiện thay đổi
//            if (outBuff.toString().trim().length() > 0) {
//                channel.disconnect(); // Ngắt kết nối cũ trước khi thực hiện thay đổi
//                channel = (ChannelExec) session.openChannel("exec");
//                channel.setCommand(changeCommand);
//                in = channel.getInputStream();
//                channel.connect();
//
//                outBuff.setLength(0); // Clear output buffer
//
//                while (true) {
//                    while (in.available() > 0) {
//                        int i = in.read(tmp, 0, 1024);
//                        if (i < 0) {
//                            break;
//                        }
//                        outBuff.append(new String(tmp, 0, i));
//                    }
//                    if (channel.isClosed()) {
//                        break;
//                    }
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException ee) {
//                        ee.printStackTrace();
//                    }
//                }
//                result = "Đã thay đổi cổng SSH từ " + oldPort + " thành " + newPort;
//            } else {
//                result = "Lỗi: Cổng cũ " + oldPort + " không tồn tại trong tệp cấu hình.";
//            }
//
//            channel.disconnect();
//            session.disconnect();
//
//        } catch (IOException | JSchException ioEx) {
//            result = "Lỗi: " + ioEx.getMessage();
//        }
//
//        return result;
//    }

    protected String denyallowUser(String control, String host, int port, String user, String password, String useredit) {
        String result = "";
        Session session = null;
        ChannelExec channel = null;
        // Đọc kết quả
        StringBuilder outBuff = new StringBuilder();
        try {
            String cmd = "";
            if (control.equals("deny")) {
                cmd = "echo " + password + " | sudo -S sh -c 'grep -q \"^DenyUsers.*\\b" + useredit + "\\b\" /etc/ssh/sshd_config || { grep -q \"^DenyUsers\" /etc/ssh/sshd_config && sed -i \"/^DenyUsers/ s/$/ " + useredit + "/\" /etc/ssh/sshd_config || echo \"DenyUsers " + useredit + "\" >> /etc/ssh/sshd_config; }'";
            } else {
                cmd = "echo " + password + " | sudo -S sed -i '/^DenyUsers/ s/\\b" + useredit + "\\b//g; s/^DenyUsers\\s*$//;/^DenyUsers/s/  */ /g' /etc/ssh/sshd_config";
            }
            System.out.println(cmd);
            // Tạo session và kết nối
            session = InforUser.connect(host, port, user, password);
            // Mở channel và thực thi lệnh
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            channel.connect();

            // Đợi lệnh hoàn thành
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }
            // Restart SSH nếu lệnh trước hoàn thành thành công
            if (channel.getExitStatus() == 0) {
                channel.disconnect();
                channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand("echo " + password + " | sudo -S systemctl restart sshd");
                channel.connect();
            }

            // Đọc kết quả
            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    outBuff.append(line).append("\n");
                }
            }
            result = "Đã " + control + " " + useredit + " vào SSH.";

        } catch (JSchException | IOException e) {
        } catch (InterruptedException ex) {
            Logger.getLogger(configSSH.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();

                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }
        }
        return result;

    }

    protected String fileConfig(String host, int port, String user, String password, String file) {
        String result = "";
        String command1 = "sudo -S cp /etc/ssh/sshd_config /home/" + user + "/" + user + "ssh.txt";
        String command2 = "sudo -S bash -c 'echo \"" + file + "\" > /etc/ssh/sshd_config'";
        String command3 = "sudo -S sshd -t";
        String command4 = "sudo -S cp /home/" + user + "/" + user + "ssh.txt /etc/ssh/sshd_config";
        StringBuilder outBuff = new StringBuilder(); // Lưu kết quả đầu ra
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
                    outBuff.append(line).append("\n");
                }

                // Đọc đầu ra lỗi
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errBuff.append(errorLine).append("\n");
                }
                System.out.println("CMD: " + command + "\n outBuff: " + outBuff + "\n errBuff: " + errBuff);
                // Ngắt kết nối kênh sau khi thực thi xong lệnh
                channelExec.disconnect();

                // Bỏ qua lỗi nhập passs 
                if (errBuff.length() == 23 + user.length()) {
                    errBuff.setLength(0);
                }

                //Kiểm tra lỗi
                if (errBuff.length() > 0) {
                    // Nếu là command3 có lỗi, chạy command4
                    if (command.equals(command3)) {
                        // Chạy command4
                        command = command4;
                        channelExec = (ChannelExec) session.openChannel("exec");
                        channelExec.setCommand(command);
                        out = channelExec.getOutputStream();
                        in = channelExec.getInputStream();
//                        errorStream = channelExec.getErrStream();
                        channelExec.connect();

                        out.write((password + "\n").getBytes());
                        out.flush();

                        reader = new BufferedReader(new InputStreamReader(in));
                        while ((line = reader.readLine()) != null) {
                            outBuff.append(line).append("\n");
                        }

//                        errorReader = new BufferedReader(new InputStreamReader(errorStream));
//                        while ((errorLine = errorReader.readLine()) != null) {
//                            errBuff.append(errorLine).append("\n");
//                        }
                        channelExec.disconnect();

                        // Nếu command4 có lỗi, cập nhật result và thoát
                        if (errBuff.length() > 0) {
                            int index = 23 + user.length();
                            result = "Lỗi: " + errBuff.toString().substring(index);
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            // Đóng phiên SSH
            session.disconnect();

            // Nếu không có lỗi, trả về kết quả đầu ra
            if (result.isEmpty()) {
                result = "Đã cập nhật file cấu hình SSH" + outBuff.toString();
            }
        } catch (IOException | JSchException ioEx) {
            ioEx.printStackTrace();
            result = "Lỗi: " + ioEx.getMessage();
        }
        return result;
    }

//    protected String updateFile(String host, int port, String user, String password, String file) {
//        String result = "";
//        String command = "echo " + password + " | sudo -S bash -c 'echo \"" + file + "\" > /etc/ssh/sshd_config' && sudo systemctl restart ssh";
//        System.out.println(command);
//        try {
//            Session session = InforUser.connect(host, port, user, password);
//            ChannelExec channel = (ChannelExec) session.openChannel("exec");
//            channel.setCommand(command);
//            InputStream in = channel.getInputStream();
//            channel.connect();
//
//            byte[] tmp = new byte[1024];
//            StringBuilder outBuff = new StringBuilder();
//            while (true) {
//                while (in.available() > 0) {
//                    int i = in.read(tmp, 0, 1024);
//                    if (i < 0) {
//                        break;
//                    }
//                    outBuff.append(new String(tmp, 0, i));
//                }
//                if (channel.isClosed()) {
//                    break;
//                }
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            result = "Đã update file thành công";
//            channel.disconnect();
//            session.disconnect();
//
//        } catch (IOException | JSchException ioEx) {
//            ioEx.printStackTrace();
//            result = "Lỗi: " + ioEx.getMessage();
//        }
//        return result;
//    }
}
