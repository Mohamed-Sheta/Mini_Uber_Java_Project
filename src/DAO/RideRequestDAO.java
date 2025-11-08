package DAO;

import Model.Location;
import Model.Passenger;
import Model.Driver;
import services.Request;
import utils.connection;

import java.sql.*;

public class RideRequestDAO {

    private final PassengerDAO passengerDAO = new PassengerDAO();
    private final DriverDAO driverDAO = new DriverDAO();
    private final LocationDAO locationDAO = new LocationDAO();

    // ✅ Insert request into DB
    public long saveRequest(Request request) throws SQLException {

        String sql = "INSERT INTO ride_requests (passenger_id, origin_id, destination_id, status, distance_km, estimated_time, estimated_price) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Get Passenger ID
            long passengerId = getPassengerId(request.getPassenger(), conn);

            // Save and resolve locations
            locationDAO.save(request.getOrigin());
            locationDAO.save(request.getDestination());

            long originId = getLocationId(request.getOrigin(), conn);
            long destinationId = getLocationId(request.getDestination(), conn);

            stmt.setLong(1, passengerId);
            stmt.setLong(2, originId);
            stmt.setLong(3, destinationId);
            stmt.setString(4, request.getStatus().name());
            stmt.setDouble(5, request.getDistance());
            stmt.setInt(6, request.getEstimatedTime());
            stmt.setDouble(7, request.getEstimatedPrice());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1); // return DB ID
                }
            }
        }
        throw new SQLException("Failed to insert Ride Request");
    }

    // ✅ Assign driver to request
    public boolean assignDriver(long requestDbId, Driver driver) throws SQLException {

        String sql = "UPDATE ride_requests SET driver_id = ?, status = 'Accepted', acceptance_time = NOW() WHERE id = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            long driverId = getDriverId(driver, conn);

            stmt.setLong(1, driverId);
            stmt.setLong(2, requestDbId);

            return stmt.executeUpdate() > 0;
        }
    }

    // ✅ Update status only
    public boolean updateStatus(long requestDbId, String status) throws SQLException {

        String sql = "UPDATE ride_requests SET status = ? WHERE id = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setLong(2, requestDbId);

            return stmt.executeUpdate() > 0;
        }
    }

    //  -------------------------------------------------
    //  Helper Methods
    //  -------------------------------------------------

    private long getPassengerId(Passenger p, Connection conn) throws SQLException {
        return passengerDAO.getPassengerIdBySSN(p.getUserSSN(), conn);
    }

    private long getDriverId(Driver d, Connection conn) throws SQLException {
        return driverDAO.getDriverIdBySSN(d.getUserSSN(), conn);
    }

    private long getLocationId(Location location, Connection conn) throws SQLException {
        Integer id = locationDAO.getIdByName(location.getName());
        if (id == null) throw new SQLException("Location not found: " + location.getName());
        return id;
    }
}
