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
import java.io.InputStream;

/**
 *
 * @author This PC
 */
@WebServlet(name = "MachineManager", urlPatterns = {"/MachineManager"})
public class MachineManager extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        int port = Integer.parseInt(ports);
        System.out.println("Infor: " + host + "/" + port + "/" + user + "/" + password);

        try {
            HttpSession ss = request.getSession();
            InforUser u = new InforUser(host, port, user, password);
            String ds = listUser(host, port, user, password);
            ss.setAttribute("ds", ds);
            if (ss.getAttribute("user") == null) {
                ss.setAttribute("user", u);
            } else {
                ss.setAttribute("user", u);
            }
            request.getRequestDispatcher("./main-machine.jsp").forward(request, response);

        } catch (Exception e) {
        }

    }

    public String listUser(String host, int port, String user, String password) {
        StringBuilder out = new StringBuilder();
        String cmd = "awk -F: '$3 >= 1000 { print $1 }' /etc/passwd";
        try {
            Session ss = InforUser.connect(host, port, user, password);

            ChannelExec channel = (ChannelExec) ss.openChannel("exec");
            channel.setCommand(cmd);
            InputStream is = channel.getInputStream();
            channel.connect();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                out.append(new String(buffer, 0, bytesRead)).append("\n");
            }

            // Đóng kênh và ngắt kết nối session
            channel.disconnect();
            ss.disconnect();

        } catch (JSchException | IOException e) {
        }
        return out.toString();
    }

}
