/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package Controller;

import DataBase.JDBC;
import Model.InforUser;
import com.jcraft.jsch.*;
import com.jcraft.jsch.Session;
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String choose = request.getParameter("choose");
        String[] selected = request.getParameterValues("selected");
        List<InforUser> users = new ArrayList<>();
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
                        String message = "Xóa" + host + " thành công !!!";
                        request.setAttribute("message", message);
                    } catch (ClassNotFoundException | SQLException e) {
                        String message = "Xóa" + host + " thất bại!!!";
                        request.setAttribute("message", message);
                    }
                    request.getRequestDispatcher("/Connect.jsp").forward(request, response);
                } else {
                    if (password.isEmpty()) {
                        String message = "Vui lòng nhập mật khẩu host: " + host + " user:" + user;
                        request.setAttribute("message", message);
                        request.getRequestDispatcher("/Connect.jsp").forward(request, response);
                    } else {
                        try {
                            HttpSession ss = request.getSession();
                            JDBC connectJDBC = new JDBC();
                            Connection conn = (Connection) connectJDBC.connect();
                            PreparedStatement pstm = conn.prepareStatement("UPDATE `tai_khoan` SET `isConnecting`='1', `Connected`='0', `port`=? WHERE host=? and user=?");
                            pstm.setInt(1, port);
                            pstm.setString(2, host);
                            pstm.setString(3, user);
                            pstm.executeUpdate();
                            //Tao phien session
                            Session session = InforUser.connect(host, port, user, password);
                            System.out.println("Ket noi thanh cong:  " + host + "/" + port + "/" + user + "/" + password + "/");
                            users.add(new InforUser(host, port, user, password, 1, 0));
                            if (ss.getAttribute("users") == null) {
                                ss.setAttribute("users", users);
                            } else {
                                ss.setAttribute("users", users);
                            }
                            session.disconnect();

                        } catch (JSchException e) {
                            try {
                                System.out.println("Ket noi toi: " + host + " that bai: " + e.getMessage());
                                JDBC connectJDBC = new JDBC();
                                Connection conn = (Connection) connectJDBC.connect();
                                PreparedStatement pstm = conn.prepareStatement("UPDATE `tai_khoan` SET `isConnecting`='0', `Connected`='1', `port`=? WHERE host=? and user=?");
                                pstm.setInt(1, port);
                                pstm.setString(2, host);
                                pstm.setString(3, user);
                                pstm.executeUpdate();
                                String message = "Kết nối tới: " + host + " thất bại !!!";
                                request.setAttribute("message", message);
                                request.getRequestDispatcher("/Connect.jsp").forward(request, response);
                            } catch (ClassNotFoundException ex) {
                                java.util.logging.Logger.getLogger(connects.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (SQLException ex) {
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
            String message = "Vui lòng chọn máy cần thực hiện ";
            request.setAttribute("message", message);
            request.getRequestDispatcher("/Connect.jsp").forward(request, response);
        }
    }
}
