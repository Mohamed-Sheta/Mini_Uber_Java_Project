package DAO;

import Model.Edge;
import Model.Location;
import utils.connection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EdgesDAO {

    private final LocationDAO locationDAO = new LocationDAO();

    // Insert Edge (يضيف المكانين لو مش موجودين الأول)
    public void insertEdge(Edge edge) throws SQLException {

        // احفظ أو رجّع Location موجود
        Location fromLocation = locationDAO.save(edge.getFrom());
        Location toLocation = locationDAO.save(edge.getTo());

        // هات الـ IDs من DB
        Integer fromId = locationDAO.getIdByName(fromLocation.getName());
        Integer toId = locationDAO.getIdByName(toLocation.getName());

        if (fromId == null || toId == null) {
            throw new SQLException("Location ID not found while inserting edge");
        }

        String sql = "INSERT INTO edges (from_id, to_id, distance_km) VALUES (?, ?, ?)";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fromId);
            stmt.setInt(2, toId);
            stmt.setDouble(3, edge.getDistance());

            stmt.executeUpdate();
            System.out.println("Edge inserted successfully: " + edge);
        }
    }

    // Get all edges
    public List<Edge> getAllEdges() throws SQLException {
        List<Edge> edges = new ArrayList<>();

        String sql = "SELECT e.distance_km, l1.name AS from_name, l2.name AS to_name " +
                "FROM edges e " +
                "JOIN locations l1 ON e.from_id = l1.id " +
                "JOIN locations l2 ON e.to_id = l2.id";

        try (Connection conn = connection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Location from = new Location(rs.getString("from_name"));
                Location to = new Location(rs.getString("to_name"));

                Edge edge = new Edge(from, to, rs.getDouble("distance_km"));
                edges.add(edge);
            }
        }

        return edges;
    }
}
