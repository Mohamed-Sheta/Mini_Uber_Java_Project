package DAO;

import Model.Location;
import Model.Passenger;
import utils.connection;

import java.sql.*;

public class PassengerDAO {
    private final LocationDAO locationDAO = new LocationDAO();

    public boolean addPassenger(Passenger passenger) throws SQLException {
        String sql = "INSERT INTO Passenger (user_ssn, name, phone_number, email, wallet_balance, credit_balance, account_rating, " +
                "current_location_id, destination_location_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Save or get the location ID for currentLocation
            Integer currentLocationId = null;
            if (passenger.getCurrentLocation() != null) {
                Location savedCurrent = locationDAO.saveOrGetLocation(passenger.getCurrentLocation());
                currentLocationId = getLocationId(savedCurrent);
            }

            // Save or get the location ID for destination
            Integer destinationLocationId = null;
            if (passenger.getDestination() != null) {
                Location savedDestination = locationDAO.saveOrGetLocation(passenger.getDestination());
                destinationLocationId = getLocationId(savedDestination);
            }

            stmt.setString(1, passenger.getUserSSN());
            stmt.setString(2, passenger.getName());
            stmt.setString(3, passenger.getPhoneNumber());
            stmt.setString(4, passenger.getEmail());
            stmt.setDouble(5, passenger.getWalletBalance());
            stmt.setDouble(6, passenger.getCreditBalance());
            stmt.setDouble(7, passenger.getAverageRating()); // Fixed to use getAccountRating (add getter if needed)
            stmt.setObject(8, currentLocationId, Types.INTEGER);
            stmt.setObject(9, destinationLocationId, Types.INTEGER);

            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                System.out.println(" Passenger " + passenger.getName() + " added successfully.");
            }
            return success;
        }
    }

    public Passenger getPassengerBySSN(String ssn) throws SQLException {
        String sql = "SELECT p.*, l1.name AS curr_name, l1.latitude AS curr_lat, l1.longitude AS curr_lon, " +
                "l2.name AS dest_name, l2.latitude AS dest_lat, l2.longitude AS dest_lon " +
                "FROM Passenger p " +
                "LEFT JOIN Location l1 ON p.current_location_id = l1.location_id " +
                "LEFT JOIN Location l2 ON p.destination_location_id = l2.location_id " +
                "WHERE p.user_ssn = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ssn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Location currentLocation = null;
                    if (rs.getString("curr_name") != null) {
                        currentLocation = new Location(
                                rs.getString("curr_name"),
                                rs.getDouble("curr_lat"),
                                rs.getDouble("curr_lon")
                        );
                    }

                    Location destination = null;
                    if (rs.getString("dest_name") != null) {
                        destination = new Location(
                                rs.getString("dest_name"),
                                rs.getDouble("dest_lat"),
                                rs.getDouble("dest_lon")
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
                    passenger.setDestination(destination);
                    return passenger;
                }
            }
        }
        System.out.println(" No passenger found with SSN: " + ssn);
        return null;
    }

    private Integer getLocationId(Location location) throws SQLException {
        String sql = "SELECT location_id FROM Location WHERE name = ? AND latitude = ? AND longitude = ?";
        try (Connection conn = connection.getConnection();
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