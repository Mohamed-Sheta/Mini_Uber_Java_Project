package com.mycompany.uper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {

    private static final String URL = "jdbc:mysql://localhost:3306/uper_db?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "sheta123";

    // تحميل Driver مرة واحدة فقط
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ MySQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL JDBC Driver not found. Make sure the JAR is added.");
            e.printStackTrace();
        }
    }

    private ConnectionManager() {
        // Private constructor to prevent instantiation
    }

    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("🔌 New database connection established.");
            return conn;
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to the database. Check URL, USER, and PASSWORD.");
            e.printStackTrace();
            throw e;
        }
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                    System.out.println("🔒 Database connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("❌ Error closing the database connection.");
                e.printStackTrace();
            }
        }
    }
}
