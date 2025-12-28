/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import Model.InforUser;
import com.jcraft.jsch.*;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 *
 * @author acer
 */
@WebServlet(name = "listService", urlPatterns = {"/listService"})
public class listService extends HttpServlet {


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        int port = Integer.parseInt(ports);
        String action = request.getParameter("action");
        String servicename = request.getParameter("servicename");

        Session session = null;
        ChannelExec channel = null;
        List<String[]> services = new ArrayList<>();
        HttpSession ss = request.getSession();
        if ("show".equals(action)) {
            services = showServices(action, host, port, user, password);
            ss.setAttribute("services", services);
            request.getRequestDispatcher("./main-machine.jsp").forward(request, response);
        } else if (action.equals("start") || action.equals("stop")) {
            try {
                if (!servicename.trim().equals("ssh")) {
                    String cmd = "echo " + password + " | sudo -S service " + servicename.trim() + " " + action;
                    System.out.println(cmd);
                    // Tạo session và kết nối
                    session = InforUser.connect(host, port, user, password);
                    // Mở channel và thực thi lệnh
                    channel = (ChannelExec) session.openChannel("exec");
                    channel.setCommand(cmd);
                    // Đọc kết quả
                    BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                    BufferedReader errReader = new BufferedReader(new InputStreamReader(channel.getErrStream()));

                    channel.connect();

                    String line;
                    StringBuilder output = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    String errline;
                    StringBuilder errput = new StringBuilder();
                    while ((errline = errReader.readLine()) != null) {
                        errput.append(errline).append("\n");
                    }

                    if (errput.length() == 23 + user.length()) {
                        errput.setLength(0);
                    }
                    if (errput.length() > 0) {
                        String errmessage = "Lỗi " + action + " dịch vụ " + servicename +": "+ errput.toString().substring(23+ user.length());
                        request.setAttribute("errmessage", errmessage);
                    } else {
                        String message = "Đã " + action + " dịch vụ " + servicename;
                        request.setAttribute("message", message);
                        services = showServices(action, host, port, user, password);
                        ss.setAttribute("services", services);
                    }
                    channel.disconnect();
                    session.disconnect();
                } else {
                    String errmessage = "Dịch vụ " + servicename.trim() + " không được phép.";
                    request.setAttribute("errmessage", errmessage);
                }

            } catch (JSchException | IOException e) {
            }
            request.getRequestDispatcher("./main-machine.jsp").forward(request, response);
        } else if (action.equals("reset")) {
            ss.removeAttribute("services");
        } else if (action.equals("seeConfig")) {
            List<String> seeConfig = seeConfig(host, port, user, password, servicename);
            if (seeConfig == null || seeConfig.isEmpty()) {
                String errmessage = "Không tìm thấy file cấu hình dịch vụ " + servicename;
                request.setAttribute("errmessage", errmessage);
            } else {
                request.setAttribute("seeConfig", seeConfig);
            }
        } else {
            String errmessage = "Dịch vụ đang bảo trì.";
        }
        request.getRequestDispatcher("./main-machine.jsp").forward(request, response);
    }

    protected List<String[]> showServices(String control, String host, int port, String user, String password) {
        Session session = null;
        ChannelExec channel = null;
        List<String[]> services = new ArrayList<>();
        try {
            String cmd = "echo " + password + " | sudo -S service --status-all";
            System.out.println(cmd);
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
                // Mỗi dòng sẽ có dạng: "[ + ] service-name"
                String[] parts = line.trim().split("\\s+", 3); // Tách theo khoảng trắng
                if (parts.length == 3) {
                    String status = parts[1].equals("+") ? "Running" : "Stopped";
                    String serviceName = parts[2];
                    // Lưu thông tin vào mảng String[]: trạng thái, tên dịch vụ
                    services.add(new String[]{status, serviceName.substring(1)});
                }
            }
            channel.disconnect();
            session.disconnect();
        } catch (JSchException | IOException e) {
            e.printStackTrace();
        }
        return services;
    }

    protected List<String> seeConfig(String host, int port, String user, String password, String servicename) {
        String result = "";
        Session session = null;
        ChannelExec channel = null;
        // Đọc kết quả
        StringBuilder output = new StringBuilder();
        List<String> lines = new ArrayList<>();
        try {
            String cmd = "systemctl cat " + servicename + ".service";
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
}
