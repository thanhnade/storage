/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import Model.InforUser;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

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
        try {
            if (control.equals("status")) {
                String message = statusSSH(host, port, user, password);
                request.setAttribute("message", message);
                request.getRequestDispatcher("/ssh.jsp").forward(request, response);
            } else if (control.equals("start") || control.equals("restart")) {
                String message = configSSH(control, host, port, user, password);
                request.setAttribute("message", message);
                request.getRequestDispatcher("/ssh.jsp").forward(request, response);
            } else if (control.equals("see")) {
                String see = seeSSH(control, host, port, user, password);
                request.setAttribute("see", see);
                request.getRequestDispatcher("ssh.jsp").forward(request, response);
            } else {
                response.sendRedirect("./ssh.jsp");
            }
        } catch (Exception e) {
        }
    }

    public String configSSH(String control, String host, int port, String user, String password) {
        String result = "";
        String command = "echo '" + password + "' | sudo -S systemctl" + control + " ssh";

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
            result = "Đã " + control + " dịch vụ SSH thành công";
            channel.disconnect();
            session.disconnect();

        } catch (IOException | JSchException ioEx) {
            ioEx.printStackTrace();
            result = "Lỗi: " + ioEx.getMessage();
        }
        return result;
    }

    public String statusSSH(String host, int port, String user, String password) {
        String result = "";
        String command = "systemctl status ssh";
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
                result = "Trạng thái dịch vụ SSH: " + outBuff.substring(start + 7, index - 10);
            }
            channel.disconnect();
            session.disconnect();

        } catch (IOException | JSchException ioEx) {
            ioEx.printStackTrace();
            result = "Error occurred: " + ioEx.getMessage();
        }
        return result;
    }

    public String seeSSH(String control, String host, int port, String user, String password) {
        Session session = null;
        ChannelExec channel = null;
        // Đọc kết quả
        StringBuilder output = new StringBuilder();
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
                    output.append(line).append("<br>");
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
        return output.toString();

    }

}
