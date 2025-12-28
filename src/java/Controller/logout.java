package Controller;

import DataBase.JDBC;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import Model.InforUser;
import java.util.List;

/**
 *
 * @author acer
 */
@WebServlet(name = "logout", urlPatterns = {"/logout"})
public class logout extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Lấy tham số hành động
        String action = request.getParameter("action");

        try {
            // Lấy session hiện tại
            HttpSession session = request.getSession();
            List<InforUser> users = (List<InforUser>) session.getAttribute("users");

            if ("logoutAll".equals(action)) {
                // Đăng xuất tất cả người dùng
                if (users != null && !users.isEmpty()) {
                    JDBC connectJDBC = new JDBC();
                    Connection conn = (Connection) connectJDBC.connect();

                    for (InforUser user : users) {
                        String host = user.getHost();
                        String username = user.getUser();
                        int port = user.getPort();

                        // Ghi log lịch sử
                        PreparedStatement logfile = conn.prepareStatement("INSERT INTO `log_history`(`host`, `port`, `user`, `Enabled`, `Disabled`) VALUES (?,?,?,0,1)");
                        logfile.setString(1, host);
                        logfile.setInt(2, port);
                        logfile.setString(3, username);
                        logfile.executeUpdate();

                        // Cập nhật trạng thái tài khoản
                        PreparedStatement pstm = conn.prepareStatement("UPDATE `tai_khoan` SET `isEnabled`='0',`Disabled`='1' WHERE user = ? AND host = ?");
                        pstm.setString(1, username);
                        pstm.setString(2, host);
                        pstm.executeUpdate();
                    }

                    // Xóa danh sách người dùng khỏi session
                    users.clear();
                    session.setAttribute("users", users);
                }

                // Redirect về trang chính
                response.sendRedirect("./Connect.jsp");
            } else {
                // Logic xử lý đăng xuất từng người dùng như đã có
                String host = request.getParameter("host");
                String ports = request.getParameter("port");
                String username = request.getParameter("user");
                int port = Integer.parseInt(ports);

                if (users != null) {
                    users.removeIf(user -> user.getHost().equals(host));
                    JDBC connectJDBC = new JDBC();
                    Connection conn = (Connection) connectJDBC.connect();

                    PreparedStatement logfile = conn.prepareStatement("INSERT INTO `log_history`(`host`, `port`, `user`, `Enabled`, `Disabled`) VALUES (?,?,?,0,1)");
                    logfile.setString(1, host);
                    logfile.setInt(2, port);
                    logfile.setString(3, username);
                    logfile.executeUpdate();

                    PreparedStatement pstm = conn.prepareStatement("UPDATE `tai_khoan` SET `isEnabled`='0',`Disabled`='1' WHERE user = ? ");
                    pstm.setString(1, username);
                    pstm.executeUpdate();

                    session.setAttribute("users", users);
                }

                response.sendRedirect("./trangchu.jsp");
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(logout.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
