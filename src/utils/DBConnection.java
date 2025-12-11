package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/minigo";
    private static final String USER = "root";
    private static final String PASS = "1234";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver Loaded Successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: MySQL JDBC Driver not found!");
            e.printStackTrace();
        }
    }

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        Connection con = DriverManager.getConnection(URL, USER, PASS);
        System.out.println("Connected to Database Successfully.");
        return con;
    }

    public static void closeConnection(Connection con) {
        if (con != null) {
            try {
                con.close();
                System.out.println("Connection Closed Successfully.");
            } catch (SQLException e) {
                System.err.println("Failed to close connection: " + e.getMessage());
            }
        }
    }
}