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
import java.io.PrintWriter;
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

/**
 *
 * @author This PC
 */
@WebServlet(name = "configHTTP", urlPatterns = {"/configHTTP"})
public class configHTTP extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String control = request.getParameter("control");
        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        int port = Integer.parseInt(ports);

        String choose = request.getParameter("choose");
        try {
            HttpSession ss = request.getSession();
            if (control.equals("status")) {
                String message = statusHTTP(host, port, user, password);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
            } else if (control.equals("start") || control.equals("restart") || control.equals("stop")) {
                String message = configHTTP(control, host, port, user, password);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
            } else if (control.equals("see")) {
                List<String> seeHTTP = seeHTTP(host, port, user, password);
                ss.setAttribute("seeHTTP", seeHTTP);
            } else if (control.equals("mysql")) {
                String message = configMySql(choose, host, port, user, password);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
            } else if (control.equals("php")) {
                String message = configPHP(choose, host, port, user, password);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
            } else if (control.equals("edit")) {
                String[] line0 = request.getParameterValues("line0");
                String[] line1 = request.getParameterValues("line1");
                String file = "";
                for (int i = 0; i < line0.length; i++) {
                    file += line0[i].trim() + " " + line1[i].trim() + "\n";
                }
                String message = fileConfig(host, port, user, password, file);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }
                List<String> seeHTTP = seeHTTP(host, port, user, password);
                ss.setAttribute("seeHTTP", seeHTTP);
            } else {
                String errmessage = "Dịch vụ bảo trì";
                request.setAttribute("errmessage", errmessage);
            }
            request.getRequestDispatcher("./http.jsp").forward(request, response);
        } catch (Exception e) {
        }
    }

    public String configHTTP(String control, String host, int port, String user, String password) {
        String result = "";
        String command = "echo '" + password + "' | sudo -S systemctl " + control + " apache2";

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
            if (errBuff.length() < 0 || errBuff.length() == (22 + user.length())) {
                result = "Đã " + control + " dịch vụ HTTP";
            } else {
                result = "Lỗi: " + errBuff.toString().substring(22 + user.length());
            }
            channel.disconnect();
            session.disconnect();

        } catch (IOException | JSchException ioEx) {
            result = "Lỗi: " + ioEx.getMessage();
        }
        return result;
    }

    public String statusHTTP(String host, int port, String user, String password) {
        String result = "";
        String command = "systemctl status apache2";
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
                    ee.printStackTrace();
                }
            }
            int start = outBuff.indexOf("Active:");
            int index = outBuff.indexOf("since");
            if (index != -1) {
                result = "Trạng thái dịch vụ HTTP: " + outBuff.substring(start + 7, index);
            } else {
                result = "Lỗi: Không có dịch vụ HTTP trên máy chủ";
            }
            channel.disconnect();
            session.disconnect();

        } catch (IOException | JSchException ioEx) {
            ioEx.printStackTrace();
            result = "Lỗi: " + ioEx.getMessage();
        }
        return result;
    }

    public List<String> seeHTTP(String host, int port, String user, String password) {
        Session session = null;
        ChannelExec channel = null;
        // Đọc kết quả
        StringBuilder output = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        List<String> lines = new ArrayList<>();
        try {
            String cmd = "cat /etc/apache2/apache2.conf";
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
                } else {
//                    output.append(line).append("<br>");
                    lines.add(line);
                }
            }

            while ((line = errreader.readLine()) != null) {
                lines.add(line);
                errors.append(line).append("<br>");
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
            String message = "Chưa có dịch vụ HTTP, vui lòng tải dịch vụ để thực hiện";
            lines.remove("cat: /etc/apache2/apache2.conf: No such file or directory");
            lines.add(message);
            return lines;
        } else {
//            return outBuff.toString();
            return lines;
        }

    }

    public String configPHP(String choose, String host, int port, String user, String password) {
        String result = "";
        String command = "echo '" + password + "' | sudo -S apt " + choose + " libapache2-mod-php -y";
        System.out.println(command);
        try {
            Session session = InforUser.connect(host, port, user, password);
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            // Lấy luồng kết quả của lệnh
            BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            String msg;
            StringBuilder output = new StringBuilder();
            while ((msg = in.readLine()) != null) {
                if (msg.contains("upgraded")) {
                    output.append(msg);
                }
            }

            //Đóng kết nối
            result = "Đã " + choose + " dịch vụ PHP. " + output.toString();
            channel.disconnect();
            session.disconnect();

        } catch (IOException | JSchException ioEx) {
            result = "Lỗi: " + ioEx.getMessage();
        }
        return result;
    }

    public String configMySql(String choose, String host, int port, String user, String password) {
        String result = "";
        String command = "echo '" + password + "' | sudo -S apt " + choose + " php-mysql -y";
        System.out.println(command);
        try {
            Session session = InforUser.connect(host, port, user, password);
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            // Lấy luồng kết quả của lệnh
            BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            String msg;
            StringBuilder output = new StringBuilder();
            while ((msg = in.readLine()) != null) {
                if (msg.contains("upgraded")) {
                    output.append(msg);
                }
            }

            //Đóng kết nối
            result = "Đã " + choose + " dịch vụ MySql. " + output.toString();
            channel.disconnect();
            session.disconnect();

        } catch (IOException | JSchException ioEx) {
            result = "Lỗi: " + ioEx.getMessage();
        }
        return result;
    }

    protected String fileConfig(String host, int port, String user, String password, String file) {
        String result = "";
        String command1 = "sudo -S cp /etc/apache2/apache2.conf /home/" + user + "/" + user + "http.txt";
//        String command2 = "sudo -S bash -c 'echo \"" + file + "\" > /etc/apache2/apache2.conf'";
        String command2 = "sudo -S bash -c 'cat <<EOF > /etc/apache2/apache2.conf\n" + file + "\nEOF'";

        String command3 = "sudo -S apache2ctl configtest";
        String command4 = "sudo -S cp /home/" + user + "/" + user + "http.txt /etc/apache2/apache2.conf";
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
                result = "Đã cập nhật file cấu hình HTTP" + outBuff.toString();
            }
        } catch (IOException | JSchException ioEx) {
            ioEx.printStackTrace();
            result = "Lỗi: " + ioEx.getMessage();
        }
        return result;
    }
}
