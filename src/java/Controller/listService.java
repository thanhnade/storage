/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import Model.InforUser;
import com.jcraft.jsch.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@WebServlet(name = "listService", urlPatterns = {"/listService"})
public class listService extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        int port = Integer.parseInt(ports);

        //Lenh thuc thi 
        String cmd = "dpkg -l | awk '/^ii/ {print $2 \" \" $3}'";
//        String cmd = "systemctl list-units --type=service --state=active --no-pager --no-legend | awk '{print $1, $5,$NF}'";

        // Đọc kết quả
        StringBuilder output = new StringBuilder();
        Session session = null;
        ChannelExec channel = null;

        String action = request.getParameter("action");
        if ("show".equals(action)) {
            try {
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
                    output.append(line).append("\n");
                }

                channel.disconnect();
                session.disconnect();
            } catch (JSchException | IOException e) {
                e.printStackTrace();
            }

            request.setAttribute("listService", output.toString());
            request.getRequestDispatcher("./listService.jsp").forward(request, response);
        } else {
            response.sendRedirect("./listService.jsp");
        }
    }

}
