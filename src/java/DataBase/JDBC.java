/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package DataBase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author admin
 */
public class JDBC {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/nlcs?autoReconnect=true&useSSL=false";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    public Connection connect() throws ClassNotFoundException {
        //Tạo đối tượng Connection
        Connection conn = null;
        Class.forName("com.mysql.cj.jdbc.Driver");
        try {
            conn = DriverManager
                    .getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Kết nối thành công DataBase");
        } catch (SQLException e) {
            System.out.println("Kết nối thất bại");
            e.printStackTrace();
        }
        return conn;
    }
}
