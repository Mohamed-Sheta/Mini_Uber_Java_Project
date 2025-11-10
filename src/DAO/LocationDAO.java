package DAO;
import utils.*;
import Model.Location;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocationDAO {

    public static class LocationRow {
        public final int id;
        public final String name;
        public LocationRow(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString(){ return "LocationRow{id=" + id + ", name='" + name + "'}"; }
    }

    public long insert(Location loc) throws SQLException {
        final String sql = "INSERT INTO locations(name) VALUES (?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, loc.getName());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    public int update(int id, String newName) throws SQLException {
        final String sql = "UPDATE locations SET name=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setInt(2, id);
            return ps.executeUpdate();
        }
    }

    public int delete(int id) throws SQLException {
        final String sql = "DELETE FROM locations WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    public List<LocationRow> showAll() throws SQLException {
        final String sql = "SELECT id, name FROM locations ORDER BY id";
        List<LocationRow> out = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new LocationRow(rs.getInt("id"), rs.getString("name")));
            }
        }
        return out;
    }
}