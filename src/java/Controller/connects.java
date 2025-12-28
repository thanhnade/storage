
package Controller;

import DataBase.JDBC;
import Model.InforUser;
import com.jcraft.jsch.*;
import com.jcraft.jsch.Session;
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author This PC
 */
@WebServlet(name = "connects", urlPatterns = {"/connects"})
public class connects extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String choose = request.getParameter("choose");
        String message = "";
        String errmessage = "";
        try {
            if (choose.equals("clear")) {
                JDBC connectJDBC = new JDBC();
                Connection conn = (Connection) connectJDBC.connect();
                PreparedStatement pstm = conn.prepareStatement("DELETE FROM `tai_khoan` WHERE Disabled=1");
                int result = pstm.executeUpdate();
                if (result > 0) {
                    errmessage = "Xóa các máy đã từng kết nối thành công ";
                } else {
                    errmessage = "Không có máy để xóa ";
                }

                request.setAttribute("errmessage", errmessage);
                request.getRequestDispatcher("/trangchu.jsp").forward(request, response);
            } else if (choose.equals("trangchu")) {
                HttpSession ss = request.getSession();
                ss.removeAttribute("services");
                ss.removeAttribute("outputLines");
                ss.removeAttribute("exitStatus");
                System.out.println("Da xoa session services, outputLines, exitStatus");
                request.getRequestDispatcher("./trangchu.jsp").forward(request, response);
            }
        } catch (Exception e) {
            errmessage = "Lỗi" + e.toString();
            request.setAttribute("errmessage", errmessage);
            request.getRequestDispatcher("/trangchu.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String choose = request.getParameter("choose");
        String[] selected = request.getParameterValues("selected");
        String vitri = request.getParameter("vitri");
        System.out.println("Chon: " + choose + " // Select: " + selected);
        if (selected != null) {
            for (String host : selected) {
                // Lấy giá trị của user tương ứng với host
                String user = request.getParameter("user_" + host);
                String password = request.getParameter("password_" + host);
                String ports = request.getParameter("port_" + host);
                System.out.println("Infor: " + host + "/" + ports + "/" + user + "/" + password);
                int port = Integer.parseInt(ports);

                if (choose.equals("delete")) {
                    try {
                        JDBC connectJDBC = new JDBC();
                        Connection conn = (Connection) connectJDBC.connect();
                        PreparedStatement pstm = conn.prepareStatement("DELETE FROM `tai_khoan` WHERE host=? and user=?");
                        pstm.setString(1, host);
                        pstm.setString(2, user);
                        pstm.executeUpdate();
                        String message = "Xóa máy IP: " + host + " thành công ";
                        request.setAttribute("message", message);
                    } catch (ClassNotFoundException | SQLException e) {
                        String message = "Xóa máy IP: " + host + " thất bại";
                        request.setAttribute("message", message);
                    }
                    request.getRequestDispatcher("/Connect.jsp").forward(request, response);
                } else if (choose.equals("connects")) {
                    if (password.isEmpty()) {
                        String errmessage = "Vui lòng nhập mật khẩu host: " + host + "";
                        request.setAttribute("errmessage", errmessage);
                        if (vitri.equals("trangChu")) {
                            request.getRequestDispatcher("/trangchu.jsp").forward(request, response);
                        }
                        request.getRequestDispatcher("/Connect.jsp").forward(request, response);

                    } else {
                        try {
                            HttpSession ss = request.getSession();
                            //Tao phien session
                            Session session = InforUser.connect(host, port, user, password);
                            System.out.println("Da ket noi den " + user +"@"+ host);
//                            users.add(new InforUser(host, port, user, password, 1, 0));

                            JDBC connectJDBC = new JDBC();
                            Connection conn = (Connection) connectJDBC.connect();
                            //log_history
                            PreparedStatement logfile = conn.prepareStatement("INSERT INTO `log_history`(`host`, `port`, `user`, `Enabled`, `Disabled`) VALUES (?,?,?,1,0)");
                            logfile.setString(1, host);
                            logfile.setInt(2, port);
                            logfile.setString(3, user);
                            logfile.executeUpdate();
                            //tai_khoan
//                            PreparedStatement pstm = conn.prepareStatement("UPDATE `tai_khoan` SET `isEnabled`='1', `Disabled`='0', `port`=?, `user`=?, `password`=? WHERE host=?");
                            PreparedStatement pstm = conn.prepareStatement("UPDATE `tai_khoan` SET `isEnabled`='1', `Disabled`='0', `port`=?, `user`=? WHERE host=?");
                            pstm.setInt(1, port);
                            pstm.setString(2, user);
                            pstm.setString(3, host);
                            pstm.executeUpdate();
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

                            session.disconnect();

                        } catch (JSchException e) {
                            try {
                                System.out.println("Ket noi toi: " + host + " that bai: " + e.getMessage());
                                JDBC connectJDBC = new JDBC();
                                Connection conn = (Connection) connectJDBC.connect();
                                PreparedStatement pstm = conn.prepareStatement("UPDATE `tai_khoan` SET `isEnabled`='0', `Disabled`='1', `port`=? WHERE host=?");
                                pstm.setInt(1, port);
                                pstm.setString(2, host);
                                pstm.executeUpdate();
                                String errmessage = "Kết nối tới: " + host + " thất bại ";
                                request.setAttribute("errmessage", errmessage);
                                if (vitri.equals("trangChu")) {
                                    request.getRequestDispatcher("/trangchu.jsp").forward(request, response);
                                }
                                HttpSession ss = request.getSession();
                                ss.removeAttribute("users");
                                request.getRequestDispatcher("/Connect.jsp").forward(request, response);
                            } catch (ClassNotFoundException | SQLException ex) {
                                java.util.logging.Logger.getLogger(connects.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } catch (ClassNotFoundException | SQLException ex) {
                            java.util.logging.Logger.getLogger(connects.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                }
            }
            request.getRequestDispatcher("/trangchu.jsp").forward(request, response);
        } else {
            String errmessage = "Vui lòng chọn máy cần thực hiện ";
            request.setAttribute("errmessage", errmessage);
            if (vitri.equals("trangChu")) {
                request.getRequestDispatcher("/trangchu.jsp").forward(request, response);
            }
            request.getRequestDispatcher("/Connect.jsp").forward(request, response);
        }
    }
}
