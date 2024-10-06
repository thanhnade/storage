/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import Model.InforUser;
import com.jcraft.jsch.*;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.*;

/**
 *
 * @author acer
 */
@WebServlet(name = "editUser", urlPatterns = {"/editUser"})
public class editUser extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        int port = Integer.parseInt(ports);
        String choose = request.getParameter("choose");
        String useredit = request.getParameter("useredit");
        String newuser = request.getParameter("newuser");
        try {
            HttpSession ss = request.getSession();
            InforUser u = new InforUser(host, port, user, password);
            if (ss.getAttribute("user") == null) {
                ss.setAttribute("user", u);
            } else {
                ss.setAttribute("user", u);
            }
            System.out.println("Choose: "+choose + "User: " +useredit);
            if (choose.equals("edit")) {
                request.setAttribute("useredit", useredit);
                response.sendRedirect("./editUser.jsp");
            } else if (choose.equals("delete")) {
                String message = deleteUser(host, port, user, password, useredit);
                request.setAttribute("message", message);
                response.sendRedirect("./users.jsp");
            } else if (choose.equals("add")) {
                String message = userAdd(host, port, user, password, newuser);
                request.setAttribute("message", message);
                response.sendRedirect("./users.jsp");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String deleteUser(String host, int port, String user, String password, String useredit) {
        String result = "";
        String command = "echo '" + password + "' | sudo -S userdel " + useredit;
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
                }
            }
            result = "Đã xóa " + useredit;
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    protected String userAdd(String host, int port, String user, String password, String newuser) {
        String result = "";
        String command = "echo '" + password + "' | sudo -S useradd " + newuser;
        System.out.println(command);
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
                }
            }
            result = "Đã thêm " + newuser;
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
