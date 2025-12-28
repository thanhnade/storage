package Controller;

import Model.InforUser;
import Model.NFSShare;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author This PC
 */
@WebServlet(name = "MachineManager", urlPatterns = {"/MachineManager"})

//    @Override
//    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        try {
//            HttpSession ss = request.getSession();
//            ss.removeAttribute("services");
//            ss.removeAttribute("outputLines");
//            ss.removeAttribute("exitStatus");
//            request.getRequestDispatcher("./trangchu.jsp").forward(request, response);
//        } catch (ServletException | IOException e) {
//        }
//    }
public class MachineManager extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String host = request.getParameter("host");
        String ports = request.getParameter("port");
        String user = request.getParameter("user");
        String password = request.getParameter("password");
        int port = Integer.parseInt(ports);
        String nav = request.getParameter("nav");
        System.out.println("MachineManager: " + nav + "_" + host + "/" + port + "/" + user + "/" + password);

        Session session = null;
        try {
            HttpSession ss = request.getSession();
            InforUser u = new InforUser(host, port, user, password);
            if (ss.getAttribute("user") == null) {
                ss.setAttribute("user", u);
            } else {
                ss.setAttribute("user", u);
            }
            // Lấy tham số nav từ request và set giá trị cho nó
            ss.setAttribute("activeNav", nav);
            if (nav.equals("listService")) {
                request.getRequestDispatcher("./listService.jsp").forward(request, response);
            } else if (nav.equals("ssh")) {
                List<String> useSSH = useSSH(host, port, user, password);
                ss.setAttribute("useSSH", useSSH);
                List<String> seeSSH = seeSSH(host, port, user, password);
                ss.setAttribute("seeSSH", seeSSH);
                String ds = listUser(host, port, user, password);
                ss.setAttribute("ds", ds);
                request.getRequestDispatcher("./ssh.jsp").forward(request, response);
            } else if (nav.equals("ftp")) {
                List<String> seeFTP = seeFTP(host, port, user, password);
                ss.setAttribute("seeFTP", seeFTP);
                request.getRequestDispatcher("./ftp.jsp").forward(request, response);
            } else if (nav.equals("http")) {
                List<String> seeHTTP = seeHTTP(host, port, user, password);
                ss.setAttribute("seeHTTP", seeHTTP);
                request.getRequestDispatcher("./http.jsp").forward(request, response);
            } else if (nav.equals("mainmachine")) {
                request.getRequestDispatcher("./main-machine.jsp").forward(request, response);
            } else if (nav.equals("listuser")) {
                String ds = listUser(host, port, user, password);
                ss.setAttribute("ds", ds);
                request.getRequestDispatcher("./users.jsp").forward(request, response);
            } else if (nav.equals("nfs")) {
                session = InforUser.connect(host, port, user, password);
                listNFSShares(session, request);
                listNFSMounts(session, request);
                request.getRequestDispatcher("./nfs.jsp").forward(request, response);

            }
        } catch (ServletException | IOException e) {
        } catch (JSchException ex) {
            Logger.getLogger(MachineManager.class.getName()).log(Level.SEVERE, null, ex);
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

    protected List<String> seeSSH(String host, int port, String user, String password) {
        String result = "";
        Session session = null;
        ChannelExec channel = null;
        // Đọc kết quả
        StringBuilder output = new StringBuilder();
        List<String> lines = new ArrayList<>();
        try {
            String cmd = "cat /etc/ssh/sshd_config";
            // Tạo session và kết nối
            session = InforUser.connect(host, port, user, password);
            // Mở channel và thực thi lệnh
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            // Đọc kết quả
            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            String line;
            while ((line = reader.readLine()) != null) {
                // Kiểm tra xem dòng có rỗng hay không và có bắt đầu bằng dấu # không
                if (line.trim().isEmpty() || line.trim().charAt(0) == '#') {
                    continue;
                } else {
//                    output.append(line).append("<br>");
                    lines.add(line);
                }
            }

        } catch (JSchException | IOException e) {
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();

                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }
        }
//        return output.toString();
        return lines;

    }

    protected List<String> seeFTP(String host, int port, String user, String password) {
        Session session = null;
        ChannelExec channel = null;
        // Đọc kết quả
        StringBuilder outBuff = new StringBuilder();
        StringBuilder errBuff = new StringBuilder();
        List<String> lines = new ArrayList<>();
        try {
            String cmd = "cat /etc/vsftpd.conf";
            // Tạo session và kết nối
            session = InforUser.connect(host, port, user, password);
            // Mở channel và thực thi lệnh
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            // Đọc kết quả
            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            BufferedReader errreader = new BufferedReader(new InputStreamReader(channel.getErrStream()));
            channel.connect();

            String line;
            while ((line = reader.readLine()) != null) {
                // Kiểm tra xem dòng có rỗng hay không và có bắt đầu bằng dấu # không
                if (line.trim().isEmpty() || line.trim().charAt(0) == '#') {
                    continue;
                } else {
                    outBuff.append(line).append("<br>");
                    lines.add(line);
                }
            }
            while ((line = errreader.readLine()) != null) {
                errBuff.append(line).append("<br>");
                lines.add(line);
            }

        } catch (JSchException | IOException e) {
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();

                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }
        }

        if (errBuff.length() > 0) {
            String message = "Chưa có dịch vụ FTP, vui lòng tải dịch vụ để thực hiện";
            lines.remove("cat: /etc/vsftpd.conf: No such file or directory");
            lines.add(message);
            return lines;
        } else {
//            return outBuff.toString();
//            System.out.println("Khong loi: " + lines);
            return lines;
        }

    }

    protected List<String> seeHTTP(String host, int port, String user, String password) {
        Session session = null;
        ChannelExec channel = null;
        // Đọc kết quả
        StringBuilder output = new StringBuilder();
        StringBuilder errors = new StringBuilder();
        List<String> lines = new ArrayList<>();
        try {
            String cmd = "cat /etc/apache2/apache2.conf";
            // Tạo session và kết nối
            session = InforUser.connect(host, port, user, password);
            // Mở channel và thực thi lệnh
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            // Đọc kết quả
            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            BufferedReader errreader = new BufferedReader(new InputStreamReader(channel.getErrStream()));
            channel.connect();

            String line;
            while ((line = reader.readLine()) != null) {
                // Kiểm tra xem dòng có rỗng hay không và có bắt đầu bằng dấu # không
                if (line.trim().isEmpty() || line.trim().charAt(0) == '#') {
                } else {
//                    output.append(line).append("<br>");
                    lines.add(line);
                }
            }

            while ((line = errreader.readLine()) != null) {
                lines.add(line);
                errors.append(line).append("<br>");
            }

        } catch (JSchException | IOException e) {
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();

                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }
        }
        if (errors.length() > 0) {
            String message = "Chưa có dịch vụ HTTP, vui lòng tải dịch vụ để thực hiện";
            lines.remove("cat: /etc/apache2/apache2.conf: No such file or directory");
            lines.add(message);
            return lines;
        } else {
//            return outBuff.toString();
            return lines;
        }

    }

    protected List<String> useSSH(String host, int port, String user, String password) {
        List<String> lines = new ArrayList<>();
        try {
            String cmd = "who";
            Session ss = InforUser.connect(host, port, user, password);
            ChannelExec channel = (ChannelExec) ss.openChannel("exec");
            channel.setCommand(cmd);
            InputStream in = channel.getInputStream();
            channel.connect();
            // Đọc đầu ra
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
            // Đóng kênh và ngắt kết nối session
            channel.disconnect();
            ss.disconnect();

        } catch (JSchException | IOException e) {
        }
        return lines;
    }

    private void listNFSShares(Session session, HttpServletRequest request) throws JSchException, IOException {
        String password = request.getParameter("password");
        List<NFSShare> shares = new ArrayList<>();

        ChannelExec channel = null;
        try {
            String cmd = "echo '" + password + "' | sudo -S exportfs -v | grep -v \"<world>\"";
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);

            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            channel.setOutputStream(responseStream);
            channel.connect();

            while (channel.isConnected()) {
                Thread.sleep(100);
            }

            String output = responseStream.toString().trim();
            if (output.isEmpty()) {
                System.out.println("Khong co thu muc nao dang duoc chia se qua NFS.");
                request.setAttribute("nfsShares", shares);
                return;
            }

            String[] lines = output.split("\n");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Tach path va client dung tab hoac space
                int firstTab = line.indexOf("\t");
                int firstSpace = line.indexOf(" ");

                int splitIndex = (firstTab != -1) ? firstTab : firstSpace;
                if (splitIndex == -1) {
                    System.err.println("Dong export khong hop le: " + line);
                    continue;
                }

                String pathPart = line.substring(0, splitIndex).trim();
                String clientPart = line.substring(splitIndex).trim();

                if (!clientPart.contains("(")) {
                    System.err.println("Dong export khong chua client hop le: " + line);
                    continue;
                }

                int openParen = clientPart.indexOf('(');
                String client = clientPart.substring(0, openParen).trim();
                String options = clientPart.substring(openParen + 1, clientPart.lastIndexOf(')')).trim();

                NFSShare share = new NFSShare();
                share.setPath(pathPart);
                share.setClients(client);
                share.setOptions(options);
                share.setPermissions(options.contains("rw") ? "rw" : "ro");

                shares.add(share);

                System.out.println("Tim thay thu muc: " + pathPart + " duoc chia se cho client: " + client + " voi quyen: " + share.getPermissions());
            }

            request.setAttribute("nfsShares", shares);
        } catch (Exception e) {
            request.setAttribute("errmessage", "Loi khi lay danh sach chia se: " + e.getMessage());
            System.err.println("Loi khi lay danh sach chia se: " + e.getMessage());
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    protected List<Map<String, String>> listNFSMounts(Session session, HttpServletRequest request) throws JSchException, IOException {
        List<Map<String, String>> mounts = new ArrayList<>();

        try {
            String password = request.getParameter("password");
            String cmd = "echo '" + password + "' | sudo -S df -hT | grep nfs";

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);

            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            channel.setOutputStream(responseStream);
            channel.connect();

            while (channel.isConnected()) {
                Thread.sleep(100);
            }

            String[] lines = responseStream.toString().split("\n");

            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 7) {
                        Map<String, String> mount = new HashMap<>();
                        mount.put("source", parts[0]);          // Server:Path
                        mount.put("type", parts[1]);           // Filesystem type (nfs)
                        mount.put("size", parts[2]);           // Total size
                        mount.put("used", parts[3]);           // Used space
                        mount.put("available", parts[4]);      // Available space
                        mount.put("usePercentage", parts[5]); // Use percentage
                        mount.put("mountPoint", parts[6]);     // Mount point
                        mounts.add(mount);
                    }
                }
            }

            // Lưu vào session
            request.getSession().setAttribute("nfsMounts", mounts);

        } catch (Exception e) {
            request.setAttribute("errmessage", "Lỗi khi lấy danh sách mount: " + e.getMessage());
        }

        return mounts;
    }

    

}
