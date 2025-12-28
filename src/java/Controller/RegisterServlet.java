package Controller;

import DataBase.JDBC;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.sql.SQLException;

@WebServlet("/registerServlet")
public class RegisterServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirm_password");
        
        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", "Mật khẩu xác nhận không khớp!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }
        
        try {
            JDBC jdbc = new JDBC();
            Connection conn = jdbc.connect();
            
            // Kiểm tra tài khoản đã tồn tại chưa
            String checkSql = "SELECT tai_khoan FROM nguoi_dung WHERE tai_khoan = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                request.setAttribute("error", "Tài khoản đã tồn tại. Vui lòng chọn tài khoản khác!");
                conn.close();
                request.getRequestDispatcher("login.jsp").forward(request, response);
                return;
            }
            
            // Thêm tài khoản mới
            String sql = "INSERT INTO nguoi_dung (tai_khoan, mat_khau, role, storage_limit) VALUES (?, ?, 'client', 1073741824)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                request.setAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            } else {
                request.setAttribute("error", "Đăng ký thất bại!");
            }
            
            conn.close();
            request.getRequestDispatcher("login.jsp").forward(request, response);
            
        } catch (ServletException | IOException | ClassNotFoundException | SQLException e) {
            System.out.println(e);
            request.setAttribute("error", "Đã xảy ra lỗi trong quá trình đăng ký. Vui lòng thử lại sau!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }
} 