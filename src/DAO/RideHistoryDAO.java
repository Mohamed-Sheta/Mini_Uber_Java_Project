package DAO;

import Model.Driver;
import Model.Passenger;
import Model.RideHistory;
import utils.connection;

import java.sql.*;

public class RideHistoryDAO {

    private final DriverDAO driverDAO = new DriverDAO();
    private final PassengerDAO passengerDAO = new PassengerDAO();

    private Long getPassengerIdBySSN(String ssn, Connection conn) throws SQLException {
        String sql = "SELECT id FROM passengers WHERE user_ssn = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ssn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }
        throw new SQLException("Passenger not found with SSN: " + ssn);
    }

    private Long getDriverIdBySSN(String ssn, Connection conn) throws SQLException {
        String sql = "SELECT id FROM drivers WHERE user_ssn = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ssn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }
        throw new SQLException("Driver not found with SSN: " + ssn);
    }

    public boolean addRideHistory(RideHistory history, double cost, String paymentMethod,
                                  double tips, double donationAmount, String donationOrg) throws SQLException {

        String sql = "INSERT INTO ride_history (request_id, driver_id, passenger_id, passenger_rating, driver_rating, " +
                "ride_cost, payment_method, tips, donation_amount, donation_organization) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            Long driverId = getDriverIdBySSN(history.getDriver().getUserSSN(), conn);
            Long passengerId = getPassengerIdBySSN(history.getPassenger().getUserSSN(), conn);
            Long requestId = (long) history.getRequest().getRequestId();


            stmt.setLong(1, requestId);
            stmt.setLong(2, driverId);
            stmt.setLong(3, passengerId);
            stmt.setInt(4, history.getPassengerRating());
            stmt.setInt(5, history.getDriverRating());
            stmt.setDouble(6, cost);
            stmt.setString(7, paymentMethod);
            stmt.setDouble(8, tips);
            stmt.setDouble(9, donationAmount);
            stmt.setString(10, donationOrg);

            boolean success = stmt.executeUpdate() > 0;

            if (success) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        history.setHistoryId(keys.getInt(1));
                    }
                }
            }
            return success;
        }
    }

    public RideHistory getRideHistoryById(long id) throws SQLException {

        String sql = "SELECT * FROM ride_history WHERE id = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {

                    long driverId = rs.getLong("driver_id");
                    long passengerId = rs.getLong("passenger_id");

                    Driver driver = driverDAO.getDriverById(driverId);
                    Passenger passenger = passengerDAO.getPassengerById(passengerId);

                    return new RideHistory(
                            driver,
                            passenger,
                            rs.getInt("passenger_rating"),
                            rs.getInt("driver_rating"),
                            null  // مفيش Request لأننا بنجيب History لوحده
                    );
                }
            }
        }
        return null;
    }
}
