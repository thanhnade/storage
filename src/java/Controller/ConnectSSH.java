/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import DataBase.JDBC;
import Model.InforUser;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.swing.JOptionPane;
import com.jcraft.jsch.*;
import com.jcraft.jsch.Session;
import com.mysql.cj.jdbc.PreparedStatementWrapper;
import jakarta.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

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
        List<InforUser> users = new ArrayList<>();
        try {
            HttpSession ss = request.getSession();
            users.add(new InforUser(host, port, user, password, 1, 0));
            if (ss.getAttribute("users") == null) {
                ss.setAttribute("users", users);
            } else {
                ss.setAttribute("users", users);
            }
            Session session = InforUser.connect(host, port, user, password);
            // Kết nối thành công
            JDBC connectJDBC = new JDBC();
            Connection conn = (Connection) connectJDBC.connect();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM `tai_khoan` WHERE user=? and host=?");
            ps.setString(1, user);
            ps.setString(2, host);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PreparedStatement pstm = conn.prepareStatement("UPDATE `tai_khoan` SET `isConnecting`='1', `Connected`='0' WHERE host=? and user=?");
                pstm.setString(1, host);
                pstm.setString(2, user);
                pstm.executeUpdate();
            } else {
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO tai_khoan (host, port, user, password, isConnecting, Connected) VALUES (?, ?, ?, ?, 1, 0)");
                stmt.setString(1, host);
                stmt.setInt(2, port);
                stmt.setString(3, user);
                stmt.setString(4, password);
                stmt.executeUpdate();
            }
            // Hiển thị thông báo thành công
            response.sendRedirect("./trangchu.jsp");

            session.disconnect(); // Đóng kết nối sau khi sử dụng
        } catch (JSchException e) {
            // Kết nối thất bại
            String message = "Kết nối thất bại !!! Vui lòng kiểm tra lại...";
            request.setAttribute("message", message);
            request.getRequestDispatcher("/Connect.jsp").forward(request, response);
        } catch (ClassNotFoundException | SQLException ex) {
            java.util.logging.Logger.getLogger(ConnectSSH.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
