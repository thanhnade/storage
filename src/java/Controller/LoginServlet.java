package Controller;

import DataBase.JDBC;
import Model.InforUser;
import Model.NguoiDung;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.sql.SQLException;

@WebServlet("/loginServlet")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        JDBC jdbc = new JDBC();
        try (Connection conn = jdbc.connect()) {
            // Kiểm tra tài khoản có tồn tại không
            String checkUserSql = "SELECT * FROM nguoi_dung WHERE tai_khoan = ?";
            try (PreparedStatement checkUserStmt = conn.prepareStatement(checkUserSql)) {
                checkUserStmt.setString(1, username);
                try (ResultSet rs = checkUserStmt.executeQuery()) {
                    if (!rs.next()) {
                        request.setAttribute("error", "Tài khoản không tồn tại!");
                        request.getRequestDispatcher("login.jsp").forward(request, response);
                        return;
                    }

                    // Nếu tài khoản tồn tại, kiểm tra mật khẩu
                    String matKhau = rs.getString("mat_khau");
                    if (!matKhau.equals(password)) {
                        request.setAttribute("error", "Sai mật khẩu!");
                        request.getRequestDispatcher("login.jsp").forward(request, response);
                        return;
                    }

                    // Đăng nhập thành công
                    HttpSession session = request.getSession();
                    NguoiDung nguoiDung = new NguoiDung();
                    nguoiDung.setTaiKhoan(username);
                    nguoiDung.setMatKhau(password);
                    nguoiDung.setThuMuc(rs.getString("thu_muc"));
                    nguoiDung.setRole(rs.getString("role"));
                    
                    // Lưu thông tin người dùng vào session
                    session.setAttribute("nguoi_dung", nguoiDung);

                    //Lấy danh sách máy chủ từ session
                    List<InforUser> users = (List<InforUser>) session.getAttribute("users");
                    if (users == null || users.isEmpty()) {
                        request.setAttribute("error", "Vui lòng kết nối đến máy chủ trước!");
                        request.getRequestDispatcher("Connect.jsp").forward(request, response);
                        return;
                    }

                    response.sendRedirect("./StorageServlet");
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println(e);
            request.setAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }

}
