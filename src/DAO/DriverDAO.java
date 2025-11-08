package DAO;

import Model.Driver;
import Model.Location;
import utils.connection;

import java.sql.*;
import java.util.ArrayList;

public class DriverDAO {

    private final LocationDAO locationDAO = new LocationDAO();

    // إضافة درايفر جديد
    public boolean addDriver(Driver driver) throws SQLException {

        // حفظ أو جلب الـ Location
        Integer locationId = null;
        if (driver.getCurrentLocation() != null) {
            locationDAO.save(driver.getCurrentLocation());
            locationId = locationDAO.getIdByName(driver.getCurrentLocation().getName());
        }

        String sql = "INSERT INTO drivers (user_ssn, name, phone_number, email, wallet_balance, credit_balance, " +
                "current_location, license_plate, car_model, active, latest_passenger_rating) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, driver.getUserSSN());
            stmt.setString(2, driver.getName());
            stmt.setString(3, driver.getPhoneNumber());
            stmt.setString(4, driver.getEmail());
            stmt.setDouble(5, driver.getWalletBalance());
            stmt.setDouble(6, driver.getCreditBalance());
            stmt.setObject(7, locationId, Types.INTEGER);
            stmt.setString(8, driver.getLicensePlate());
            stmt.setString(9, driver.getCarModel());
            stmt.setBoolean(10, driver.isActive());
            stmt.setInt(11, driver.getLatestPassengerRating());

            return stmt.executeUpdate() > 0;
        }
    }

    // الحصول على Driver بالـ SSN
    public Driver getDriverBySSN(String ssn) throws SQLException {

        String sql = "SELECT d.*, l.name AS location_name " +
                "FROM drivers d " +
                "LEFT JOIN locations l ON d.current_location = l.id " +
                "WHERE d.user_ssn = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ssn);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {

                    Location location = null;
                    if (rs.getString("location_name") != null) {
                        location = new Location(rs.getString("location_name"));
                    }

                    return new Driver(
                            rs.getString("license_plate"),
                            rs.getString("car_model"),
                            rs.getBoolean("active"),
                            rs.getString("user_ssn"),
                            rs.getString("name"),
                            rs.getString("phone_number"),
                            rs.getString("email"),
                            rs.getDouble("wallet_balance"),
                            rs.getDouble("credit_balance"),
                            location,
                            new ArrayList<>() // RideHistory not handled yet
                    );
                }
            }
        }

        return null;
    }

    // إضافة رصيد للمحفظة
    public boolean addAmount(String ssn, double amount) throws SQLException {
        if (amount <= 0) return false;

        Driver driver = getDriverBySSN(ssn);
        if (driver == null) return false;

        double newBalance = driver.getWalletBalance() + amount;

        String sql = "UPDATE drivers SET wallet_balance = ? WHERE user_ssn = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newBalance);
            stmt.setString(2, ssn);

            return stmt.executeUpdate() > 0;
        }
    }
    public Driver getDriverById(long id) throws SQLException {
        String sql = "SELECT user_ssn FROM drivers WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return getDriverBySSN(rs.getString("user_ssn"));
            }
        }
        return null;
    }
    public long getDriverIdBySSN(String ssn, Connection conn) throws SQLException {
        String sql = "SELECT id FROM drivers WHERE user_ssn = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ssn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Driver not found: " + ssn);
    }

}
