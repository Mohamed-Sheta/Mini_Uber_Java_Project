package com.mycompany.uper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RideHistoryDAO {
    private final DriverDAO driverDAO = new DriverDAO();
    private final PassengerDAO passengerDAO = new PassengerDAO();

    public boolean addRideHistory(RideHistory rideHistory) throws SQLException {
        String sql = "INSERT INTO RideHistory (driver_ssn, passenger_ssn, passenger_rating, driver_rating) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, rideHistory.getDriver().getUserSSN());
            stmt.setString(2, rideHistory.getPassenger().getUserSSN());
            stmt.setInt(3, rideHistory.getPassengerRating());
            stmt.setInt(4, rideHistory.getDriverRating());
            
            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        rideHistory.setHistoryId(generatedKeys.getInt(1));
                        System.out.println("✅ RideHistory saved with ID: " + rideHistory.getHistoryId());
                    }
                }
            }
            return success;
        }
    }

    public RideHistory getRideHistoryById(int historyId) throws SQLException {
        String sql = "SELECT * FROM RideHistory WHERE history_id = ?";
        
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, historyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Driver driver = driverDAO.getDriverBySSN(rs.getString("driver_ssn"));
                    Passenger passenger = passengerDAO.getPassengerBySSN(rs.getString("passenger_ssn"));
                    
                    if (driver == null || passenger == null) {
                        System.err.println("❌ Driver or Passenger not found for RideHistory ID: " + historyId);
                        return null;
                    }
                    
                    return new RideHistory(
                        rs.getInt("history_id"),
                        driver,
                        passenger,
                        rs.getInt("passenger_rating"),
                        rs.getInt("driver_rating")
                    );
                }
            }
        }
        System.out.println("❌ No RideHistory found with ID: " + historyId);
        return null;
    }

    public boolean updateRatings(int historyId, int passengerRating, int driverRating) throws SQLException {
        String sql = "UPDATE RideHistory SET passenger_rating = ?, driver_rating = ? WHERE history_id = ?";
        
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, passengerRating);
            stmt.setInt(2, driverRating);
            stmt.setInt(3, historyId);
            
            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                System.out.println("✅ Updated ratings for RideHistory ID: " + historyId);
            }
            return success;
        }
    }

    public List<RideHistory> getRideHistoriesByUser(String userSSN) throws SQLException {
        List<RideHistory> histories = new ArrayList<>();
        String sql = "SELECT * FROM RideHistory WHERE driver_ssn = ? OR passenger_ssn = ?";
        
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, userSSN);
            stmt.setString(2, userSSN);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Driver driver = driverDAO.getDriverBySSN(rs.getString("driver_ssn"));
                    Passenger passenger = passengerDAO.getPassengerBySSN(rs.getString("passenger_ssn"));
                    
                    if (driver == null || passenger == null) {
                        System.err.println("❌ Skipping RideHistory ID: " + rs.getInt("history_id") + " due to missing Driver or Passenger.");
                        continue;
                    }
                    
                    RideHistory history = new RideHistory(
                        rs.getInt("history_id"),
                        driver,
                        passenger,
                        rs.getInt("passenger_rating"),
                        rs.getInt("driver_rating")
                    );
                    histories.add(history);
                }
            }
        }
        System.out.println("✅ Retrieved " + histories.size() + " ride histories for user SSN: " + userSSN);
        return histories;
    }
}