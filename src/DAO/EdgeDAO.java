package DAO;
import utils.*;
import Model.Edge;
import Model.Location;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EdgeDAO {

    public static class EdgeRow {
        public final long id;
        public final int fromId;
        public final int toId;
        public final double distanceKm;
        public EdgeRow(long id, int fromId, int toId, double distanceKm) {
            this.id=id; this.fromId=fromId; this.toId=toId; this.distanceKm=distanceKm;
        }
        @Override public String toString(){ return "EdgeRow{id="+id+", from="+fromId+", to="+toId+", d="+distanceKm+"}"; }
    }

    public long insert(Location from, Location to, double distanceKm) throws SQLException {
        final String sql = "INSERT INTO edges(from_id, to_id, distance_km) VALUES (?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, from.getId());
            ps.setInt(2, to.getId());
            ps.setDouble(3, distanceKm);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    public int update(long id, int fromId, int toId, double distanceKm) throws SQLException {
        final String sql = "UPDATE edges SET from_id=?, to_id=?, distance_km=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, fromId);
            ps.setInt(2, toId);
            ps.setDouble(3, distanceKm);
            ps.setLong(4, id);
            return ps.executeUpdate();
        }
    }

    public int delete(long id) throws SQLException {
        final String sql = "DELETE FROM edges WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    public List<EdgeRow> showAll() throws SQLException {
        final String sql = "SELECT id, from_id, to_id, distance_km FROM edges ORDER BY id";
        List<EdgeRow> out = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new EdgeRow(
                        rs.getLong("id"),
                        rs.getInt("from_id"),
                        rs.getInt("to_id"),
                        rs.getDouble("distance_km")));
            }
        }
        return out;
    }
}
