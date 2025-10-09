import java.sql.*;

public class connection {
    public static void main(String[] args) {
        try {
            Connection x = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/uber",
                    "root",
                    "1234"
            );
            Statement stmt = x.createStatement();

            String insertQuery = "INSERT INTO locations (name, latitude, longitude) VALUES ('Alex', 31.2001, 29.9187)";
            stmt.executeUpdate(insertQuery);

            String updateQuery = "UPDATE locations SET latitude = 31.2100, longitude = 29.9200 WHERE name = 'Alex'";
            stmt.executeUpdate(updateQuery);

            ResultSet resultSet = stmt.executeQuery("SELECT * FROM locations");

            while (resultSet.next()) {
                System.out.println("-----------------------");
                System.out.println(resultSet.getString("name"));
                System.out.println(resultSet.getDouble("latitude"));
                System.out.println(resultSet.getDouble("longitude"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
