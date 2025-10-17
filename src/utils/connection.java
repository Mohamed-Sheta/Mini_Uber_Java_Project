package utils;

import java.sql.*;

public class connection {
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/uper_db",
                "root",
                "1234"
        );
    }

    public static void main(String[] args) {
        try {
            Connection x = getConnection();
            System.out.println("Connection established successfully.");
            x.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}