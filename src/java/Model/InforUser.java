/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Model;

import DataBase.JDBC;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 *
 * @author This PC
 */
public class InforUser {

    private InforUser() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private String host;
    private int port;
    private String user;
    private String password;
    private int isConnecting;
    private int Connected;

    //start
    public static InforUser vd() {
        return new InforUser();
    }
    public ArrayList<InforUser> selectAll() {
        ArrayList<InforUser> kq = new ArrayList<InforUser>();
        try {
            String sql = "SELECT * FROM tai_khoan";
            JDBC connectJDBC = new JDBC();
            Connection conn = (Connection) connectJDBC.connect();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery(sql);
            while (rs.next()) {
                String host = rs.getString("host");
                int port = rs.getInt("port");
                String user = rs.getString("user");
                String password = rs.getString("password");
                int isConnecting = rs.getInt("isConnecting");
                int Connected = rs.getInt("Connected");
                InforUser u = new InforUser(host, port, user, password, isConnecting, Connected);
                kq.add(u);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return kq;
    }
    //end
    
    public static Session connect(String host, int port, String username, String password) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        return session;
    }
    
    
    public InforUser(String host, int port, String user) {
        this.host = host;
        this.port = port;
        this.user = user;
    }

    public InforUser(String host, int port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public InforUser(String host, int port, String user, String password, int isConnecting, int Connected) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.isConnecting = isConnecting;
        this.Connected = Connected;
    }

    

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public int getIsConnecting() {
        return isConnecting;
    }

    public void setIsConnecting(int isConnecting) {
        this.isConnecting = isConnecting;
    }

    public int getConnected() {
        return Connected;
    }

    public void setConnected(int Connected) {
        this.Connected = Connected;
    }

}
