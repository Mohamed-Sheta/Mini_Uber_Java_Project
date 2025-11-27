package DAO;
import utils.*;
import Model.Status;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RideRequestDAO {

    public static class RideRequestRow {
        public final long id;
        public final long passengerId;
        public final Long driverId; // may be null
        public final int originId, destinationId;
        public final String status;
        public final double distanceKm;
        public final int estimatedTime;
        public final double estimatedPrice;
        public final Timestamp acceptanceTime;
        public final boolean driverArrived;
        public final boolean passengerArrived;

        public RideRequestRow(long id, long passengerId, Long driverId, int originId, int destinationId, String status,
                              double distanceKm, int estimatedTime, double estimatedPrice, Timestamp acceptanceTime,
                              boolean driverArrived, boolean passengerArrived) {
            this.id=id; this.passengerId=passengerId; this.driverId=driverId; this.originId=originId; this.destinationId=destinationId;
            this.status=status; this.distanceKm=distanceKm; this.estimatedTime=estimatedTime; this.estimatedPrice=estimatedPrice;
            this.acceptanceTime=acceptanceTime; this.driverArrived=driverArrived; this.passengerArrived=passengerArrived;
        }
        @Override public String toString(){ return "RideRequestRow{id="+id+", status="+status+"}"; }
    }

    public long insert(long passengerId, Integer driverIdNullable, int originId, int destinationId,
                       Status status, double distanceKm, int estimatedTime, double estimatedPrice,
                       Timestamp acceptanceTimeNullable, boolean driverArrived, boolean passengerArrived) throws SQLException {

        final String sql = "INSERT INTO ride_requests(passenger_id, driver_id, origin_id, destination_id, status, distance_km, estimated_time, estimated_price, acceptance_time, driver_arrived, passenger_arrived) " +
                           "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, passengerId);
            if (driverIdNullable == null) ps.setNull(2, Types.BIGINT); else ps.setLong(2, driverIdNullable);
            ps.setInt(3, originId);
            ps.setInt(4, destinationId);
            ps.setString(5, status.name());
            ps.setDouble(6, distanceKm);
            ps.setInt(7, estimatedTime);
            ps.setDouble(8, estimatedPrice);
            if (acceptanceTimeNullable == null) ps.setNull(9, Types.TIMESTAMP); else ps.setTimestamp(9, acceptanceTimeNullable);
            ps.setBoolean(10, driverArrived);
            ps.setBoolean(11, passengerArrived);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    public int update(long id, Long driverIdNullable, Status status,
                      Double distanceKm, Integer estimatedTime, Double estimatedPrice,
                      Timestamp acceptanceTimeNullable, Boolean driverArrived, Boolean passengerArrived) throws SQLException {

        final String sql = "UPDATE ride_requests SET driver_id=?, status=?, distance_km=?, estimated_time=?, estimated_price=?, acceptance_time=?, driver_arrived=?, passenger_arrived=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (driverIdNullable == null) ps.setNull(1, Types.BIGINT); else ps.setLong(1, driverIdNullable);
            ps.setString(2, status.name());
            ps.setDouble(3, distanceKm);
            ps.setInt(4, estimatedTime);
            ps.setDouble(5, estimatedPrice);
            if (acceptanceTimeNullable == null) ps.setNull(6, Types.TIMESTAMP); else ps.setTimestamp(6, acceptanceTimeNullable);
            ps.setBoolean(7, driverArrived);
            ps.setBoolean(8, passengerArrived);
            ps.setLong(9, id);

            return ps.executeUpdate();
        }
    }

    public int delete(long id) throws SQLException {
        final String sql = "DELETE FROM ride_requests WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    /**
     * Delete all ride requests for a specific passenger
     */
    public int deleteByPassenger(Connection con, long passengerId) throws SQLException {
        final String sql = "DELETE FROM ride_requests WHERE passenger_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, passengerId);
            return ps.executeUpdate();
        }
    }

    /**
     * Set driver_id to NULL for all ride requests assigned to a specific driver
     * (Used when deleting a driver - we want to keep the request history)
     */
    public int clearDriverFromRequests(Connection con, long driverId) throws SQLException {
        final String sql = "UPDATE ride_requests SET driver_id = NULL WHERE driver_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            return ps.executeUpdate();
        }
    }

    public List<RideRequestRow> showAll() throws SQLException {
        final String sql = "SELECT id, passenger_id, driver_id, origin_id, destination_id, status, distance_km, estimated_time, estimated_price, acceptance_time, driver_arrived, passenger_arrived " +
                           "FROM ride_requests ORDER BY id";
        List<RideRequestRow> out = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Long driverId = (Long) rs.getObject("driver_id");
                out.add(new RideRequestRow(
                        rs.getLong("id"),
                        rs.getLong("passenger_id"),
                        driverId,
                        rs.getInt("origin_id"),
                        rs.getInt("destination_id"),
                        rs.getString("status"),
                        rs.getDouble("distance_km"),
                        rs.getInt("estimated_time"),
                        rs.getDouble("estimated_price"),
                        rs.getTimestamp("acceptance_time"),
                        rs.getBoolean("driver_arrived"),
                        rs.getBoolean("passenger_arrived")
                ));
            }
        }
        return out;
    }
}
