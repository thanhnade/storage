/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import DataBase.JDBC;
import Model.InforUser;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.jcraft.jsch.*;
import com.jcraft.jsch.Session;
import jakarta.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author This PC
 */
@WebServlet(name = "ConnectSSH", urlPatterns = {"/ConnectSSH"})
public class ConnectSSH extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String user = request.getParameter("user");
        String password = request.getParameter("password");

        int port = Integer.parseInt(ports);
        try {
            HttpSession ss = request.getSession();
//            users.add(new InforUser(host, port, user, password, 1, 0));
//            if (ss.getAttribute("users") == null) {
//                ss.setAttribute("users", users);
//            } else {
//                 ss.setAttribute("users", users);
//            }
            Session session = InforUser.connect(host, port, user, password);
            System.out.println("Da ket noi den " + user + "@" + host);
            JDBC connectJDBC = new JDBC();
            Connection conn = (Connection) connectJDBC.connect();
            PreparedStatement logfile = conn.prepareStatement("INSERT INTO `log_history`(`host`, `port`, `user`, `Enabled`, `Disabled`) VALUES (?,?,?,1,0)");
            logfile.setString(1, host);
            logfile.setInt(2, port);
            logfile.setString(3, user);
            logfile.executeUpdate();

//            PreparedStatement ps = conn.prepareStatement("SELECT * FROM `tai_khoan` WHERE user=? and host=? and port=?");
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM `tai_khoan` WHERE host=?");
            ps.setString(1, host);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
//                PreparedStatement pstm = conn.prepareStatement("UPDATE `tai_khoan` SET `isEnabled`='1', `Disabled`='0', `port`=?, `user`=?, `password`=? WHERE host=?");
                PreparedStatement pstm = conn.prepareStatement("UPDATE `tai_khoan` SET `isEnabled`='1', `Disabled`='0', `port`=?, `user`=? WHERE host=?");
                pstm.setInt(1, port);
                pstm.setString(2, user);
//                pstm.setString(3, password);
                pstm.setString(3, host);
                pstm.executeUpdate();
            } else {
//                PreparedStatement stmt = conn.prepareStatement("INSERT INTO tai_khoan (host, port, user, password, isEnabled, Disabled) VALUES (?, ?, ?, ?, 1, 0)");
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO tai_khoan (host, port, user, isEnabled, Disabled) VALUES (?, ?, ?, 1, 0)");
                stmt.setString(1, host);
                stmt.setInt(2, port);
                stmt.setString(3, user);
//                stmt.setString(4, password);
                stmt.executeUpdate();
            }

            List<InforUser> sessionUsers = (List<InforUser>) ss.getAttribute("users");
            if (sessionUsers == null) {
                sessionUsers = new ArrayList<>();
                sessionUsers.add(new InforUser(host, port, user, password, 1, 0));
                ss.setAttribute("users", sessionUsers);
            } else {
                // Kiểm tra nếu người dùng chưa tồn tại trong danh sách thì mới thêm
                boolean exists = false;
                for (InforUser iu : sessionUsers) {
                    if (iu.getHost().equals(host)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    sessionUsers.add(new InforUser(host, port, user, password, 1, 0));
                }
                ss.setAttribute("users", sessionUsers);
            }
            session.disconnect(); // Đóng kết nối sau khi sử dụng

            //thành công
            String message = "Đã kết nối máy chủ " + host;
            request.setAttribute("message", message);
            request.getRequestDispatcher("/trangchu.jsp").forward(request, response);
        } catch (JSchException e) {
            // Kết nối thất bại
            String errmessage = "Kết nối đến " + host + " thất bại ";
            if (request.getParameter("vitri").equals("trangConnect")) {
                request.setAttribute("errmessage", errmessage);
                request.getRequestDispatcher("/Connect.jsp").forward(request, response);
            } else {
                request.setAttribute("errmessage", errmessage);
                request.getRequestDispatcher("/trangchu.jsp").forward(request, response);
            }
        } catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
