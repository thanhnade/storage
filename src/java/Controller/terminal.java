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
import java.io.*;
import java.util.*;

/**
 *
 * @author acer
 */
@WebServlet(name = "terminal", urlPatterns = {"/terminal"})
public class terminal extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String str = request.getParameter("str");
        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        int port = Integer.parseInt(ports);

        StringBuilder output = new StringBuilder(); //luu k.qua dau ra
        List<String> lines = new ArrayList<>(); // Danh sách để lưu trữ từng dòng
        try {
            //tao phien ket noi ssh
            Session session = InforUser.connect(host, port, user, password);

            //mo kenh thuc thi lenh
            ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(str);

            InputStream in = channelExec.getInputStream();
            InputStream errorStream = channelExec.getErrStream(); // Luồng lỗi
            channelExec.connect(); // Kết nối và thực thi lệnh

            // Đọc đầu ra
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim(); // Loại bỏ khoảng trắng ở đầu và cuối dòng
                if (!line.isEmpty()) { // Kiểm tra nếu dòng không rỗng
                    lines.add(line); // Thêm dòng vào danh sách
                }
            }

            // Đọc đầu ra lỗi
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorLine = errorLine.trim(); // Loại bỏ khoảng trắng
                if (!errorLine.isEmpty()) { // Kiểm tra nếu dòng lỗi không rỗng
                    lines.add(errorLine);
                }
            }

            // Thêm trạng thái thoát vào đầu ra
            output.append("Exit-status: ").append(channelExec.getExitStatus());

            // Lưu từng dòng vào request attribute
            request.setAttribute("outputLines", lines);
            request.setAttribute("exitStatus", output.toString()); // Lưu trạng thái thoát

            channelExec.disconnect();
            session.disconnect();

        } catch (JSchException e) {
            output.append("Lỗi: ").append(e.getMessage());
            request.setAttribute("error", output.toString());
        } catch (IOException e) {
            output.append("Lỗi khi đọc đầu ra: ").append(e.getMessage());
            request.setAttribute("error", output.toString());
        }
        request.getRequestDispatcher("/main-machine.jsp").forward(request, response);
    }

}
