package DAO;
import utils.*;
import Model.PaymentType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RideHistoryDAO {

    public static class RideHistoryRow {
        public final long id;
        public final long requestId, driverId, passengerId;
        public final int passengerRating, driverRating;
        public final double rideCost, tips, donationAmount;
        public final String donationOrganization;
        public final String paymentMethod;
        public final Timestamp completedAt;

        public RideHistoryRow(long id, long requestId, long driverId, long passengerId,
                              int passengerRating, int driverRating,
                              double rideCost, String paymentMethod, double tips,
                              double donationAmount, String donationOrganization,
                              Timestamp completedAt) {
            this.id=id; this.requestId=requestId; this.driverId=driverId; this.passengerId=passengerId;
            this.passengerRating=passengerRating; this.driverRating=driverRating;
            this.rideCost=rideCost; this.paymentMethod=paymentMethod; this.tips=tips;
            this.donationAmount=donationAmount; this.donationOrganization=donationOrganization;
            this.completedAt=completedAt;
        }
        @Override public String toString(){ return "RideHistoryRow{id="+id+", request="+requestId+"}"; }
    }

    /**
     * Insert a new ride history record.
     *
     * CRITICAL - Rating Column Mapping (DO NOT CONFUSE):
     * - passenger_rating = Driver's rating OF the passenger (driver rates passenger)
     *   Used to calculate PASSENGER's average rating in their profile.
     *
     * - driver_rating = Passenger's rating OF the driver (passenger rates driver)
     *   Used to calculate DRIVER's average rating in their profile.
     *
     * @param requestId The ride request ID
     * @param driverId The driver's ID
     * @param passengerId The passenger's ID
     * @param passengerRating Driver's rating of the passenger (0-5, or null/0 if not rated yet)
     * @param driverRating Passenger's rating of the driver (0-5, or null/0 if not rated yet)
     * @param rideCost The total ride cost
     * @param method Payment method used
     * @param tips Tip amount given to driver
     * @param donationAmount Donation amount
     * @param donationOrganization Name of donation organization
     * @return The generated ride history ID, or -1 if failed
     * @throws SQLException if database error occurs
     */
    public long insert(long requestId, long driverId, long passengerId,
                       Integer passengerRating, Integer driverRating,
                       double rideCost, PaymentType method,
                       double tips, double donationAmount, String donationOrganization) throws SQLException {

        final String sql = "INSERT INTO ride_history(request_id, driver_id, passenger_id, passenger_rating, driver_rating, ride_cost, payment_method, tips, donation_amount, donation_organization) " +
                           "VALUES (?,?,?,?,?,?,?,?,?,?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, requestId);
            ps.setLong(2, driverId);
            ps.setLong(3, passengerId);
            ps.setInt(4, passengerRating == null ? 0 : passengerRating);
            ps.setInt(5, driverRating == null ? 0 : driverRating);
            ps.setDouble(6, rideCost);
            ps.setString(7, method.name());
            ps.setDouble(8, tips);
            ps.setDouble(9, donationAmount);
            ps.setString(10, donationOrganization == null ? "" : donationOrganization);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    /**
     * Update an existing ride history record.
     *
     * CRITICAL - Rating Column Mapping:
     * - passenger_rating = Driver's rating OF the passenger
     * - driver_rating = Passenger's rating OF the driver
     *
     * @param id The ride history record ID to update
     * @param passengerRating Driver's rating of the passenger (0-5, or null to keep existing)
     * @param driverRating Passenger's rating of the driver (0-5, or null to keep existing)
     * @param rideCost The total ride cost (or null to keep existing)
     * @param method Payment method (or null to keep existing)
     * @param tips Tip amount (or null to keep existing)
     * @param donationAmount Donation amount (or null to keep existing)
     * @param donationOrganization Donation organization name (or null to keep existing)
     * @return Number of rows updated (should be 1 if successful, 0 if not found)
     * @throws SQLException if database error occurs
     */
    public int update(long id, Integer passengerRating, Integer driverRating,
                      Double rideCost, PaymentType method, Double tips,
                      Double donationAmount, String donationOrganization) throws SQLException {

        final String sql = "UPDATE ride_history SET passenger_rating=?, driver_rating=?, ride_cost=?, payment_method=?, tips=?, donation_amount=?, donation_organization=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, passengerRating == null ? 0 : passengerRating);
            ps.setInt(2, driverRating == null ? 0 : driverRating);
            ps.setDouble(3, rideCost == null ? 0.0 : rideCost);
            ps.setString(4, method == null ? PaymentType.wallet.name() : method.name());
            ps.setDouble(5, tips == null ? 0.0 : tips);
            ps.setDouble(6, donationAmount == null ? 0.0 : donationAmount);
            ps.setString(7, donationOrganization == null ? "" : donationOrganization);
            ps.setLong(8, id);
            return ps.executeUpdate();
        }
    }

    public int delete(long id) throws SQLException {
        final String sql = "DELETE FROM ride_history WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    /**
     * Delete all ride history records for a specific passenger
     */
    public int deleteByPassenger(Connection con, long passengerId) throws SQLException {
        final String sql = "DELETE FROM ride_history WHERE passenger_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, passengerId);
            return ps.executeUpdate();
        }
    }

    /**
     * Delete all ride history records for a specific driver
     */
    public int deleteByDriver(Connection con, long driverId) throws SQLException {
        final String sql = "DELETE FROM ride_history WHERE driver_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            return ps.executeUpdate();
        }
    }

    public List<RideHistoryRow> showAll() throws SQLException {
        final String sql = "SELECT id, request_id, driver_id, passenger_id, passenger_rating, driver_rating, ride_cost, payment_method, tips, donation_amount, donation_organization, completed_at FROM ride_history ORDER BY id";
        List<RideHistoryRow> out = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new RideHistoryRow(
                        rs.getLong("id"),
                        rs.getLong("request_id"),
                        rs.getLong("driver_id"),
                        rs.getLong("passenger_id"),
                        rs.getInt("passenger_rating"),
                        rs.getInt("driver_rating"),
                        rs.getDouble("ride_cost"),
                        rs.getString("payment_method"),
                        rs.getDouble("tips"),
                        rs.getDouble("donation_amount"),
                        rs.getString("donation_organization"),
                        rs.getTimestamp("completed_at")
                ));
            }
        }
        return out;
    }
}
