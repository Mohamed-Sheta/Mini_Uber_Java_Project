package DAO;

import Model.Location;
import utils.connection;

import java.sql.*;

public class LocationDAO {
    public Location saveOrGetLocation(Location location) throws SQLException {
        Location existingLocation = getLocationByNameLatLon(location.getName(), location.getLatitude(), location.getLongitude());
        if (existingLocation != null) {
            return existingLocation;
        }

        String sql = "INSERT INTO Location (name, latitude, longitude) VALUES (?, ?, ?)";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, location.getName());
            stmt.setDouble(2, location.getLatitude());
            stmt.setDouble(3, location.getLongitude());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    System.out.println(" Location saved with ID: " + generatedKeys.getInt(1));
                    location.setId(generatedKeys.getInt(1));
                    return location;
                } else {
                    throw new SQLException("Failed to create location, no ID obtained.");
                }
            }
        }
    }

    public Location getLocationByNameLatLon(String name, double lat, double lon) throws SQLException {
        String sql = "SELECT name, latitude, longitude FROM Location WHERE name = ? AND latitude = ? AND longitude = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setDouble(2, lat);
            stmt.setDouble(3, lon);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Location(rs.getString("name"), rs.getDouble("latitude"), rs.getDouble("longitude"));
                }
            }
        }
        return null;
    }

    public int getLocationIdByName(String name, double lat, double lon, Connection conn) throws SQLException {
        String sql = "SELECT location_id FROM Location WHERE name = ? AND latitude = ? AND longitude = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setDouble(2, lat);
            stmt.setDouble(3, lon);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("location_id");
                }
            }
        }
        throw new SQLException("Location not found: " + name);
    }

    public int getLocationIdByName(String name, double lat, double lon) throws SQLException {
        try (Connection conn = connection.getConnection()) {
            return getLocationIdByName(name, lat, lon, conn);
        }
    }
}