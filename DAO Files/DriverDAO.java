package com.mycompany.uper;

import java.sql.*;

public class DriverDAO {
    private final LocationDAO locationDAO = new LocationDAO();

    public boolean addDriver(Driver driver) throws SQLException {
        String sql = "INSERT INTO Driver (user_ssn, name, phone_number, email, wallet_balance, credit_balance, account_rating, " +
                     "license_plate, car_model, is_active, current_location_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Save or get the location ID for currentLocation
            Integer locationId = null;
            if (driver.getCurrentLocation() != null) {
                Location savedLocation = locationDAO.saveOrGetLocation(driver.getCurrentLocation());
                locationId = getLocationId(savedLocation);
            }

            stmt.setString(1, driver.getUserSSN());
            stmt.setString(2, driver.getName());
            stmt.setString(3, driver.getPhoneNumber());
            stmt.setString(4, driver.getEmail());
            stmt.setDouble(5, driver.getWalletBalance());
            stmt.setDouble(6, driver.getCreditBalance());
            stmt.setInt(7, driver.getAccountRating());
            stmt.setString(8, driver.getLicensePlate());
            stmt.setString(9, driver.getCarModel());
            stmt.setBoolean(10, driver.isActive());
            stmt.setObject(11, locationId, Types.INTEGER);

            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                System.out.println("✅ Driver " + driver.getName() + " added successfully.");
            }
            return success;
        }
    }

    public Driver getDriverBySSN(String ssn) throws SQLException {
        String sql = "SELECT d.*, l.name, l.latitude, l.longitude " +
                    "FROM Driver d " +
                    "LEFT JOIN Location l ON d.current_location_id = l.location_id " +
                    "WHERE d.user_ssn = ?";
        
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ssn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Location currentLocation = null;
                    if (rs.getString("name") != null) {
                        currentLocation = new Location(
                            rs.getString("name"),
                            rs.getDouble("latitude"),
                            rs.getDouble("longitude")
                        );
                    }

                    return new Driver(
                        rs.getString("license_plate"),
                        rs.getString("car_model"),
                        rs.getBoolean("is_active"),
                        currentLocation,
                        rs.getString("user_ssn"),
                        rs.getString("name"),
                        rs.getString("phone_number"),
                        rs.getString("email"),
                        rs.getDouble("wallet_balance"),
                        rs.getDouble("credit_balance"),
                        rs.getInt("account_rating")
                    );
                }
            }
        }
        System.out.println("❌ No driver found with SSN: " + ssn);
        return null;
    }

    public boolean addAmount(String ssn, double amount) throws SQLException {
        if (amount <= 0) {
            System.err.println("❌ Invalid amount: " + amount + ". Amount must be positive.");
            return false;
        }

        Driver driver = getDriverBySSN(ssn);
        if (driver == null) {
            System.err.println("❌ Driver with SSN " + ssn + " not found.");
            return false;
        }

        double newBalance = driver.getWalletBalance() + amount;
        String sql = "UPDATE Driver SET wallet_balance = ? WHERE user_ssn = ?";
        
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newBalance);
            stmt.setString(2, ssn);

            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                System.out.println("✅ Added $" + amount + " to driver " + ssn + ". New balance: $" + newBalance);
            }
            return success;
        }
    }

    private Integer getLocationId(Location location) throws SQLException {
        String sql = "SELECT location_id FROM Location WHERE name = ? AND latitude = ? AND longitude = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, location.getName());
            stmt.setDouble(2, location.getLatitude());
            stmt.setDouble(3, location.getLongitude());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("location_id");
                }
            }
        }
        throw new SQLException("Location ID not found for: " + location.getName());
    }
}