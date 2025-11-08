package DAO;

import Model.Location;
import Model.Passenger;
import utils.connection;

import java.sql.*;
import java.util.ArrayList;

public class PassengerDAO {

    private final LocationDAO locationDAO = new LocationDAO();

    // إضافة راكب جديد
    public boolean addPassenger(Passenger passenger) throws SQLException {

        Integer locationId = null;
        if (passenger.getCurrentLocation() != null) {
            locationDAO.save(passenger.getCurrentLocation());
            locationId = locationDAO.getIdByName(passenger.getCurrentLocation().getName());
        }

        String sql = "INSERT INTO passengers (user_ssn, name, phone_number, email, wallet_balance, credit_balance, current_location, latest_driver_rating) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, passenger.getUserSSN());
            stmt.setString(2, passenger.getName());
            stmt.setString(3, passenger.getPhoneNumber());
            stmt.setString(4, passenger.getEmail());
            stmt.setDouble(5, passenger.getWalletBalance());
            stmt.setDouble(6, passenger.getCreditBalance());
            stmt.setObject(7, locationId, Types.INTEGER);
            stmt.setInt(8, passenger.getLatestDriverRating());

            return stmt.executeUpdate() > 0;
        }
    }

    // جلب راكب باستخدام الـ SSN
    public Passenger getPassengerBySSN(String ssn) throws SQLException {

        String sql = "SELECT p.*, l.name AS location_name " +
                "FROM passengers p " +
                "LEFT JOIN locations l ON p.current_location = l.id " +
                "WHERE p.user_ssn = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ssn);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {

                    Location loc = null;
                    if (rs.getString("location_name") != null) {
                        loc = new Location(rs.getString("location_name"));
                    }

                    return new Passenger(
                            rs.getString("user_ssn"),
                            rs.getString("name"),
                            rs.getString("phone_number"),
                            rs.getString("email"),
                            rs.getDouble("wallet_balance"),
                            rs.getDouble("credit_balance"),
                            loc,
                            new ArrayList<>() // rideHistory is ignored for now
                    );
                }
            }
        }
        return null;
    }

    // إضافة رصيد للمحفظة
    public boolean addAmount(String ssn, double amount) throws SQLException {
        if (amount <= 0) return false;

        Passenger passenger = getPassengerBySSN(ssn);
        if (passenger == null) return false;

        double newBalance = passenger.getWalletBalance() + amount;

        String sql = "UPDATE passengers SET wallet_balance = ? WHERE user_ssn = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newBalance);
            stmt.setString(2, ssn);

            return stmt.executeUpdate() > 0;
        }
    }
    public Passenger getPassengerById(long id) throws SQLException {
        String sql = "SELECT user_ssn FROM passengers WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return getPassengerBySSN(rs.getString("user_ssn"));
            }
        }
        return null;
    }

    public long getPassengerIdBySSN(String ssn, Connection conn) throws SQLException {
        String sql = "SELECT id FROM passengers WHERE user_ssn = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ssn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Passenger not found: " + ssn);
    }

}
