package Controller;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
import DataBase.JDBC;
import Model.InforUser;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author This PC
 */
@WebServlet(urlPatterns = {"/shutdown"})
public class shutdown extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String username = request.getParameter("user");
        String password = request.getParameter("password");
        
        int port = Integer.parseInt(ports);
        Session session = null;
        ChannelExec channelExec = null;

        try {           
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channelExec = (ChannelExec) session.openChannel("exec");
            String command = "echo '" + password + "' | sudo -S poweroff";
            channelExec.setCommand(command);

            // Đọc đầu ra của lệnh
            InputStream in = channelExec.getInputStream();
            channelExec.connect();

            byte[] tmp = new byte[1024];
            StringBuilder output = new StringBuilder();
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    output.append(new String(tmp, 0, i));
                }
                if (channelExec.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                }
            }

            //Logfile
            JDBC connectJDBC = new JDBC();
            Connection conn = (Connection) connectJDBC.connect();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM `tai_khoan` WHERE user=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PreparedStatement pstm = conn.prepareStatement("UPDATE `tai_khoan` SET `isConnecting`='0',`Connected`='1' WHERE user = ? ");
                pstm.setString(1, username);
                pstm.executeUpdate();
                // Phản hồi kết quả
//                String message = "Đã shutdown máy chủ" + host;
//                request.setAttribute("message", message);
//                request.getRequestDispatcher("/Connect.jsp").forward(request, response);
            }
            // Lấy session hiện tại
            HttpSession ss = request.getSession();

            // Lấy danh sách users từ session
            List<InforUser> users = (List<InforUser>) ss.getAttribute("users");

            // Tìm và xóa người dùng tương ứng dựa trên host và username
            if (users != null) {
                users.removeIf(user -> user.getHost().equals(host) && user.getUser().equals(username));
            }

            // Cập nhật danh sách users trong session
            ss.setAttribute("users", users);
            // Redirect về trang chính hoặc trang khác
            response.sendRedirect("./trangchu.jsp");

        } catch (JSchException e) {
            String message = "Không thể kết nối đến máy chủ" + host;
            request.setAttribute("message", message);
            request.getRequestDispatcher("./trangchu.jsp").forward(request, response);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(shutdown.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(shutdown.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (channelExec != null && channelExec.isConnected()) {
                channelExec.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
