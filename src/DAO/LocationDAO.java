package DAO;

import Model.Location;
import utils.connection;

import java.sql.*;

public class LocationDAO {

    public Location getByName(String name) throws SQLException {
        String sql = "SELECT name FROM locations WHERE name = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Location(rs.getString("name"));
                }
            }
        }
        return null;
    }

    public Location save(Location location) throws SQLException {
        Location existing = getByName(location.getName());
        if (existing != null) {
            return existing;
        }

        String sql = "INSERT INTO locations (name) VALUES (?)";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, location.getName());
            stmt.executeUpdate();
        }
        return location;
    }

    public Location getById(int id) throws SQLException {
        String sql = "SELECT name FROM locations WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Location(rs.getString("name"));
                }
            }
        }
        return null;
    }

    public Integer getIdByName(String name) throws SQLException {
        String sql = "SELECT id FROM locations WHERE name = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

}
