/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import DataBase.JDBC;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import Model.InforUser;
import java.util.List;

/**
 *
 * @author This PC
 */
@WebServlet(name = "logout", urlPatterns = {"/logout"})
public class logout extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Lấy thông tin người dùng cần xóa từ request (host và user)
        String host = request.getParameter("host");
        String username = request.getParameter("user");

        try {
            //Logfile
            JDBC connectJDBC = new JDBC();
            Connection conn = (Connection) connectJDBC.connect();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM `tai_khoan` WHERE user=? and host=?");
            ps.setString(1, username);
            ps.setString(2, host);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PreparedStatement pstm = conn.prepareStatement("UPDATE `tai_khoan` SET `isConnecting`='0',`Connected`='1' WHERE user = ? ");
                pstm.setString(1, username);
                pstm.executeUpdate();
                // Phản hồi kết quả
//                String message = "Đã đăng xuất máy chủ";
//                request.setAttribute("message", message);
//                request.getRequestDispatcher("/Connect.jsp").forward(request, response);
            }
            // Lấy session hiện tại
            HttpSession session = request.getSession();

            // Lấy danh sách users từ session
            List<InforUser> users = (List<InforUser>) session.getAttribute("users");

            // Tìm và xóa người dùng tương ứng dựa trên host và username
            if (users != null) {
                users.removeIf(user -> user.getHost().equals(host) && user.getUser().equals(username));
            }else{
                response.sendRedirect("./Connect.jsp");
            }
           
            // Cập nhật danh sách users trong session
            session.setAttribute("users", users);
            // Redirect về trang chính hoặc trang khác
            response.sendRedirect("./trangchu.jsp");

        } catch (ClassNotFoundException ex) {
            Logger.getLogger(logout.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(logout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
