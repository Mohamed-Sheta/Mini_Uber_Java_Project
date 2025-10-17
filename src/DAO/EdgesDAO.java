package DAO;

import Model.Edge;
import Model.Location;
import utils.connection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EdgesDAO {
    public void insertEdge(Edge edge) throws SQLException {
        String sql = "INSERT INTO Edge (from_location_id, to_location_id, distance, estimated_time) VALUES (?, ?, ?, ?)";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            LocationDAO locationDAO = new LocationDAO();
            int fromId = locationDAO.getLocationIdByName(
                    edge.getFrom().getName(), edge.getFrom().getLatitude(), edge.getFrom().getLongitude(), conn);
            int toId = locationDAO.getLocationIdByName(
                    edge.getTo().getName(), edge.getTo().getLatitude(), edge.getTo().getLongitude(), conn);

            stmt.setInt(1, fromId);
            stmt.setInt(2, toId);
            stmt.setDouble(3, edge.getDistance());
            stmt.setInt(4, edge.getEstimated_time());

            stmt.executeUpdate();
            System.out.println("✅ Edge inserted successfully with from_id=" + fromId + " and to_id=" + toId);
        } catch (SQLException e) {
            System.err.println("❌ Error inserting edge: " + e.getMessage());
            throw e;
        }
    }

    public List<Edge> getAllEdges() throws SQLException {
        List<Edge> edges = new ArrayList<>();
        String sql = "SELECT e.*, l1.name AS from_name, l1.latitude AS from_lat, l1.longitude AS from_lon, " +
                "l2.name AS to_name, l2.latitude AS to_lat, l2.longitude AS to_lon " +
                "FROM Edge e " +
                "JOIN Location l1 ON e.from_location_id = l1.location_id " +
                "JOIN Location l2 ON e.to_location_id = l2.location_id";

        try (Connection conn = connection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Location from = new Location(
                        rs.getString("from_name"),
                        rs.getDouble("from_lat"),
                        rs.getDouble("from_lon")
                );
                Location to = new Location(
                        rs.getString("to_name"),
                        rs.getDouble("to_lat"),
                        rs.getDouble("to_lon")
                );
                Edge edge = new Edge(from, to, rs.getDouble("distance"), rs.getInt("estimated_time"));
                edges.add(edge);
            }
            System.out.println("✅ Retrieved " + edges.size() + " edges");
        } catch (SQLException e) {
            System.err.println("❌ Error fetching edges: " + e.getMessage());
            throw e;
        }
        return edges;
    }
}