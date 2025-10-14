package com.mycompany.uper;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public class ProblemReportDAO {
    private int getProblemTypeId(ProblemType type) throws SQLException {
        String sql = "SELECT type_id FROM ProblemType WHERE type_name = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, type.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("type_id");
                } else {
                    throw new SQLException("ProblemType not found: " + type.name());
                }
            }
        }
    }

    public int addProblemReport(int rideHistoryId, String details, Set<ProblemType> types) throws SQLException {
        
        String insertReportSql = "INSERT INTO ProblemReport (ride_history_id, timestamp, details) VALUES (?, NOW(), ?)";
        String insertJunctionSql = "INSERT INTO ProblemReport_Type (report_id, type_id) VALUES (?, ?)";
        
        Connection conn = null;
        int generatedReportId = -1;

        try {
            conn = ConnectionManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(insertReportSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, rideHistoryId);
                stmt.setString(2, details);
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        generatedReportId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Failed to create problem report, no ID obtained.");
                    }
                }
            }

            try (PreparedStatement junctionStmt = conn.prepareStatement(insertJunctionSql)) {
                for (ProblemType type : types) {
                    int typeId = getProblemTypeId(type);
                    junctionStmt.setInt(1, generatedReportId);
                    junctionStmt.setInt(2, typeId);
                    junctionStmt.addBatch();
                }
                junctionStmt.executeBatch();
            }

            conn.commit();
            System.out.println("✅ Problem Report ID " + generatedReportId + " saved with associated types.");
            return generatedReportId;

        } catch (SQLException e) {
            System.err.println("❌ Error adding Problem Report. Rolling back transaction: " + e.getMessage());
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
        }
    }
}