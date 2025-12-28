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
import jakarta.servlet.http.HttpSession;
import java.io.*;

/**
 *
 * @author acer
 */
@WebServlet(name = "editUser", urlPatterns = {"/editUser"})
public class editUser extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        int port = Integer.parseInt(ports);

        String choose = request.getParameter("choose");
        String useredit = request.getParameter("useredit");
        String newuser = request.getParameter("newuser");
        String newpass = request.getParameter("newpass");
        String renewpass = request.getParameter("renewpass");

        String newusername = request.getParameter("newusername");
        String newemail = request.getParameter("newemail");
        try {
            HttpSession ss = request.getSession();
            InforUser u = new InforUser(host, port, user, password);
            if (ss.getAttribute("user") == null) {
                ss.setAttribute("user", u);
            } else {
                ss.setAttribute("user", u);
            }
            if (choose.equals("delete")) {
                String message = deleteUser(host, port, user, password, useredit);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }

            } else if (choose.equals("add")) {
                String message = userAdd(host, port, user, password, newuser, newpass);
                if (message.contains("Lỗi")) {
                    request.setAttribute("errmessage", message);
                } else {
                    request.setAttribute("message", message);
                }

            } else if (choose.equals("doimk")) {
                if (newpass.equals(renewpass)) {
                    String message = newPassword(host, port, user, password, useredit, newpass);
                    if (message.contains("Lỗi")) {
                        request.setAttribute("errmessage", message);
                    } else {
                        request.setAttribute("message", message);
                    }
                } else {
                    String errmessage = "Mật khẩu không khớp";
                    request.setAttribute("errmessage", errmessage);
                }

            } else if (choose.equals("doiinfo")) {
//                if (!newusername.isEmpty()) {
//                    String cmd = "echo " + password + " | sudo -S usermod -l " + newusername + " -d /home/" + newusername + " -m " + useredit + " && echo " + password + " | sudo -S groupdel " + useredit;
//                    String messageu = editinfo(host, port, user, password, useredit, newusername, cmd);
//                    if (messageu.contains("Lỗi")) {
//                        request.setAttribute("errmessage", messageu);
//                    } else {
//                        request.setAttribute("message", messageu);
//                    }
//                }
//                if (!newemail.isEmpty()) {
//                    String cmd = "echo " + password + " | sudo -S chfn -o email=" + newemail + " " + useredit;
//                    String messagee = editinfo(host, port, user, password, useredit, newemail, cmd);
//                    if (messagee.contains("Lỗi")) {
//                        request.setAttribute("errmessage", messagee);
//                    } else {
//                        request.setAttribute("message", messagee);
//                    }
//                }
//                if (newusername.isEmpty() && newemail.isEmpty()) {
//                    String errmessage = "Không có dữ liệu thay đổi";
//                    request.setAttribute("errmessage", errmessage);
//                }
                if (!newusername.isEmpty() || !newemail.isEmpty()) {
                    String message = editinfo(host, port, user, password, useredit, newusername, newemail);
                    if (message.contains("Lỗi")) {
                        request.setAttribute("errmessage", message);
                    } else {
                        request.setAttribute("message", message);
                    }
                } else {
                    request.setAttribute("errmessage", "Không có dữ liệu thay đổi");
                }

            } else {
                String errmessage = "Thao tác chưa đúng";
                request.setAttribute("errmessage", errmessage);
            }
            String ds = listUser(host, port, user, password);
            ss.setAttribute("ds", ds);
            request.getRequestDispatcher("./users.jsp").forward(request, response);
        } catch (IOException e) {
        }
    }

    protected String listUser(String host, int port, String user, String password) {
        StringBuilder out = new StringBuilder();
//        String cmd = "awk -F: '$3 >= 1000 { print $1 }' /etc/passwd";
        String cmd = "awk -F: '$3 >= 1000 { split($5, arr, \"email=\"); print $1, (arr[2] ? arr[2] : \"No_Email\") }' /etc/passwd";
        try {
            Session ss = InforUser.connect(host, port, user, password);

            ChannelExec channel = (ChannelExec) ss.openChannel("exec");
            channel.setCommand(cmd);
            InputStream is = channel.getInputStream();
            channel.connect();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                out.append(new String(buffer, 0, bytesRead)).append("\n");
            }

            // Đóng kênh và ngắt kết nối session
            channel.disconnect();
            ss.disconnect();

        } catch (JSchException | IOException e) {
        }
        return out.toString();
    }

    protected String userAdd(String host, int port, String user, String password, String newuser, String newpass) {
        String result = "";
        String command = "echo " + password + " | sudo -S adduser " + newuser + " --gecos \"\" --disabled-password && echo \"" + newuser + ":" + newpass + "\" | sudo chpasswd";
        System.out.println(command);

        Session session = null;
        ChannelExec channel = null;

        try {
            session = InforUser.connect(host, port, user, password);
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            InputStream in = channel.getInputStream();
            InputStream errorStream = channel.getErrStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuilder outBuff = new StringBuilder();
            StringBuilder errorBuff = new StringBuilder();

            // Đọc đầu ra
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    outBuff.append(line);
                }
            }
            // Đọc đầu ra lỗi
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorLine = errorLine.trim();
                if (!errorLine.isEmpty()) {
                    errorBuff.append(errorLine);
                }
            }

            // Kiểm tra lỗi
            int kt = 21 + user.length();
            if (errorBuff.toString().isEmpty() || errorBuff.toString().length() == kt) {
                result = "Đã thêm người dùng: " + newuser;
            } else {
                result = "Lỗi: " + errorBuff.toString().substring(kt);
            }

            channel.disconnect();
            session.disconnect();
        } catch (JSchException | IOException e) {
            e.printStackTrace(); // Log lỗi
            result = "Exception: " + e.getMessage();
        }
        return result;
    }

    protected String deleteUser(String host, int port, String user, String password, String useredit) {
        String result = "";
        String command = "echo '" + password + "' | sudo -S userdel -rf " + useredit + " && sudo groupdel " + useredit;
        Session session = null;
        ChannelExec channel = null;
        System.out.println(command);
        try {
            session = InforUser.connect(host, port, user, password);
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            InputStream in = channel.getInputStream();
            InputStream errorStream = channel.getErrStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuilder outBuff = new StringBuilder();
            StringBuilder errorBuff = new StringBuilder();

            while (!channel.isClosed()) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    outBuff.append(new String(tmp, 0, i));
                }
                while (errorStream.available() > 0) {
                    int i = errorStream.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    errorBuff.append(new String(tmp, 0, i));
                }
                Thread.sleep(1000);
            }

            // Kiểm tra lỗi
            int kt = 22 + user.length();
            if (errorBuff.toString().isEmpty() || outBuff.toString().length() > kt) {
                System.out.println(errorBuff + "\n" + outBuff.toString());
                result = "Đã xóa người dùng: " + useredit;
            } else {
                result = "Lỗi: " + errorBuff.toString();
            }

            channel.disconnect();
            session.disconnect();
        } catch (JSchException | IOException | InterruptedException e) {
            // Log lỗi
            result = "Exception: " + e.getMessage();
        }
        return result;
    }

    protected String newPassword(String host, int port, String user, String password, String useredit, String newpass) {
        String result = "";
        // Sửa lại câu lệnh command
        String command = "echo " + password + " | sudo -S chpasswd <<< '" + useredit + ":" + newpass + "'";
        
        Session session = null;
        ChannelExec channel = null;

        try {
            session = InforUser.connect(host, port, user, password);
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            // Lấy cả output stream và error stream
            InputStream in = channel.getInputStream();
            InputStream errorStream = channel.getErrStream();
            
            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuilder outBuff = new StringBuilder();
            StringBuilder errorBuff = new StringBuilder();

            // Đọc output và error
            while (!channel.isClosed()) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    outBuff.append(new String(tmp, 0, i));
                }
                while (errorStream.available() > 0) {
                    int i = errorStream.read(tmp, 0, 1024);
                    if (i < 0) break;
                    errorBuff.append(new String(tmp, 0, i));
                }
                
                if (channel.isClosed()) {
                    break;
                }
                Thread.sleep(100);
            }

            // Kiểm tra exit status
            int exitStatus = channel.getExitStatus();
            if (exitStatus == 0) {
                result = "Đã đổi password người dùng: " + useredit;
            } else {
                String error = errorBuff.toString().trim();
                result = "Lỗi: " + (error.isEmpty() ? "Không thể đổi mật khẩu" : error);
            }

        } catch (JSchException | IOException | InterruptedException e) {
            e.printStackTrace();
            result = "Lỗi: " + e.getMessage();
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }

        return result;
    }

    protected String editinfo(String host, int port, String user, String password, String useredit, String newusername, String newemail) {
        String result = "";
        Session session = null;
        ChannelExec channel = null;

        try {
            session = InforUser.connect(host, port, user, password);
            channel = (ChannelExec) session.openChannel("exec");

            // Tạo command để thay đổi cả username và email
            StringBuilder command = new StringBuilder();
            command.append("echo '").append(password).append("' | sudo -S sh -c '");

            // Nếu có thay đổi username
            if (newusername != null && !newusername.isEmpty()) {
                command.append("usermod -l ").append(newusername)
                        .append(" -d /home/").append(newusername)
                        .append(" -m ").append(useredit)
                        .append(" && groupmod -n ").append(newusername)
                        .append(" ").append(useredit);
            }

            // Nếu có thay đổi email
            if (newemail != null && !newemail.isEmpty()) {
                if (newusername != null && !newusername.isEmpty()) {
                    command.append(" && ");
                }
                command.append("chfn -o \"email=").append(newemail).append("\" ")
                        .append(newusername != null && !newusername.isEmpty() ? newusername : useredit);
            }

            command.append("'");

            System.out.println("Executing command: " + command.toString()); // Debug log

            channel.setCommand(command.toString());
            InputStream in = channel.getInputStream();
            InputStream errorStream = channel.getErrStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuilder outBuff = new StringBuilder();
            StringBuilder errorBuff = new StringBuilder();

            while (!channel.isClosed()) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    outBuff.append(new String(tmp, 0, i));
                }
                while (errorStream.available() > 0) {
                    int i = errorStream.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    errorBuff.append(new String(tmp, 0, i));
                }

                if (channel.isClosed()) {
                    break;
                }
                Thread.sleep(100);
            }

            int exitStatus = channel.getExitStatus();
            System.out.println("Exit status: " + exitStatus);
            System.out.println("Output: " + outBuff.toString());
            System.out.println("Error: " + errorBuff.toString());

            if (exitStatus == 0) {
                StringBuilder successMsg = new StringBuilder("Đã thay đổi thành công: ");
                if (newusername != null && !newusername.isEmpty()) {
                    successMsg.append("tên người dùng thành '").append(newusername).append("'");
                }
                if (newemail != null && !newemail.isEmpty()) {
                    if (newusername != null && !newusername.isEmpty()) {
                        successMsg.append(" và ");
                    }
                    successMsg.append("email thành '").append(newemail).append("'");
                }
                result = successMsg.toString();
            } else {
                String error = errorBuff.toString().trim();
                if (error.contains("[sudo] password for")) {
                    error = error.substring(error.indexOf(":") + 1).trim();
                }
                result = "Lỗi: " + (error.isEmpty() ? "Không thể thay đổi thông tin (Exit status: " + exitStatus + ")" : error);
            }

        } catch (Exception e) {
            e.printStackTrace();
            result = "Lỗi: " + e.getMessage();
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }

        return result;
    }

//    protected String editinfo(String host, int port, String user, String password, String useredit, String newusername, String cmd) {
//        String result = "";
//        String command = cmd;
//        Session session = null;
//        ChannelExec channel = null;
//        System.out.println(command);
//        try {
//            session = InforUser.connect(host, port, user, password);
//            channel = (ChannelExec) session.openChannel("exec");
//            channel.setCommand(command);
//            InputStream in = channel.getInputStream();
//            InputStream errorStream = channel.getErrStream();
//            channel.connect();
//
//            byte[] tmp = new byte[1024];
//            StringBuilder outBuff = new StringBuilder();
//            StringBuilder errorBuff = new StringBuilder();
//
//            while (!channel.isClosed()) {
//                while (in.available() > 0) {
//                    int i = in.read(tmp, 0, 1024);
//                    if (i < 0) {
//                        break;
//                    }
//                    outBuff.append(new String(tmp, 0, i));
//                }
//                while (errorStream.available() > 0) {
//                    int i = errorStream.read(tmp, 0, 1024);
//                    if (i < 0) {
//                        break;
//                    }
//                    errorBuff.append(new String(tmp, 0, i));
//                }
//                Thread.sleep(1000);
//            }
//
//            // Kiểm tra lỗi
//            if (errorBuff.length() > 0) {
//                result = "Lỗi: " + errorBuff.toString();
//            } else {
//                result = "Thay đổi thông tin thành công";
//            }
//
//        } catch (JSchException | IOException | InterruptedException e) {
//            // Log lỗi
//            result = "Exception: " + e.getMessage();
//        } finally {
//            if (channel != null) {
//                channel.disconnect();
//            }
//            if (session != null) {
//                session.disconnect();
//            }
//        }
//
//        return result;
//    }
}
