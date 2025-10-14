package com.mycompany.uper;

import java.sql.*;

public class PassengerDAO {
    private final LocationDAO locationDAO = new LocationDAO();

    public boolean addPassenger(Passenger passenger) throws SQLException {
        String sql = "INSERT INTO Passenger (user_ssn, name, phone_number, email, wallet_balance, credit_balance, account_rating) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, passenger.getUserSSN());
            stmt.setString(2, passenger.getName());
            stmt.setString(3, passenger.getPhoneNumber());
            stmt.setString(4, passenger.getEmail());
            stmt.setDouble(5, passenger.getWalletBalance()); 
            stmt.setDouble(6, passenger.getCreditBalance());
            stmt.setInt(7, 0);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateBalances(String ssn, double newWalletBalance, double newCreditBalance) throws SQLException {
        String sql = "UPDATE Passenger SET wallet_balance = ?, credit_balance = ? WHERE user_ssn = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newWalletBalance);
            stmt.setDouble(2, newCreditBalance);
            stmt.setString(3, ssn);

            return stmt.executeUpdate() > 0;
        }
    }

    public Passenger getPassengerBySSN(String ssn) throws SQLException {
        String sql = "SELECT p.*, l.name AS current_name, l.latitude AS current_lat, l.longitude AS current_lon " +
                    "FROM Passenger p " +
                    "LEFT JOIN Location l ON p.current_location_id = l.location_id " +
                    "WHERE p.user_ssn = ?";
        
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, ssn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Location currentLocation = null;
                    if (rs.getString("current_name") != null) {
                        currentLocation = new Location(
                            rs.getString("current_name"),
                            rs.getDouble("current_lat"),
                            rs.getDouble("current_lon")
                        );
                    }

                    Passenger passenger = new Passenger(
                        currentLocation,
                        rs.getString("user_ssn"),
                        rs.getString("name"),
                        rs.getString("phone_number"),
                        rs.getString("email"),
                        rs.getDouble("wallet_balance"),
                        rs.getDouble("credit_balance"),
                        rs.getInt("account_rating")
                    );
                    System.out.println("✅ Retrieved passenger: " + passenger.getName());
                    return passenger;
                }
            }
        }
        System.out.println("❌ No passenger found with SSN: " + ssn);
        return null;
    }
}