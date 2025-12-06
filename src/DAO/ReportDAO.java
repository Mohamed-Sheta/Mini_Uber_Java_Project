package DAO;

import Model.Report;
import Model.ReportType;
import utils.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    /**
     * Save a new report to the database
     * @param report The report to save
     * @return The generated report ID, or -1 if failed
     */
    public long save(Report report) {
        final String sql = "INSERT INTO reports(user_id, description, type, created_at) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, report.getUserId());
            ps.setString(2, report.getDescription());
            ps.setString(3, report.getType().name());
            ps.setTimestamp(4, Timestamp.valueOf(report.getCreatedAt()));

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        report.setId(id);
                        System.out.println("✓ Report saved successfully with ID: " + id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving report: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Get all reports for a specific user
     * @param userId The user ID
     * @return List of reports for the user
     */
    public List<Report> getReportsByUser(long userId) {
        final String sql = "SELECT id, user_id, description, type, created_at FROM reports WHERE user_id = ? ORDER BY created_at DESC";
        List<Report> reports = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Report report = new Report(
                            rs.getLong("id"),
                            rs.getLong("user_id"),
                            ReportType.valueOf(rs.getString("type")),
                            rs.getString("description"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                    reports.add(report);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting reports by user: " + e.getMessage());
            e.printStackTrace();
        }
        return reports;
    }

    /**
     * Get all reports from all users
     * @return List of all reports
     */
    public List<Report> getAllReports() {
        final String sql = "SELECT id, user_id, description, type, created_at FROM reports ORDER BY created_at DESC";
        List<Report> reports = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Report report = new Report(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        ReportType.valueOf(rs.getString("type")),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                reports.add(report);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all reports: " + e.getMessage());
            e.printStackTrace();
        }
        return reports;
    }

    /**
     * Delete a report by ID
     * @param id The report ID to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean delete(long id) {
        final String sql = "DELETE FROM reports WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✓ Report deleted successfully (ID: " + id + ")");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting report: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get a report by ID
     * @param id The report ID
     * @return The report or null if not found
     */
    public Report getReportById(long id) {
        final String sql = "SELECT id, user_id, description, type, created_at FROM reports WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Report(
                            rs.getLong("id"),
                            rs.getLong("user_id"),
                            ReportType.valueOf(rs.getString("type")),
                            rs.getString("description"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting report by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}

