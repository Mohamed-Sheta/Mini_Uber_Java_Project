package DAO;

import Model.ProblemReport;
import Model.ProblemType;
import services.RideManager;
import utils.connection;

import java.sql.*;

public class ProblemReportDAO {

    private int getProblemTypeId(ProblemType type, Connection conn) throws SQLException {
        String sql = "SELECT id FROM problem_types WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        throw new SQLException("ProblemType not found: " + type.name());
    }

    private Long getPassengerIdBySSN(String ssn, Connection conn) throws SQLException {
        String sql = "SELECT id FROM passengers WHERE user_ssn = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ssn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }
        throw new SQLException("Passenger not found with SSN: " + ssn);
    }

    private Long getDriverIdBySSN(String ssn, Connection conn) throws SQLException {
        String sql = "SELECT id FROM drivers WHERE user_ssn = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ssn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }
        return null; // يمكن يكون البلاغ بدون سائق
    }

    public long addProblemReport(ProblemReport report) throws SQLException {

        RideManager manager = report.rideManager;

        String insertReportSql =
                "INSERT INTO problem_reports (request_id, reporter_passenger_id, driver_id, details) VALUES (?, ?, ?, ?)";

        String insertTypesSql =
                "INSERT INTO problem_report_types (report_id, type_id) VALUES (?, ?)";

        Connection conn = connection.getConnection();
        long generatedReportId;

        try {
            conn.setAutoCommit(false);

            long requestId = manager.getRequest().getRequestId();
            long passengerId = getPassengerIdBySSN(manager.getRequest().getPassenger().getUserSSN(), conn);
            Long driverId = manager.getCurrentDriver() != null
                    ? getDriverIdBySSN(manager.getCurrentDriver().getUserSSN(), conn)
                    : null;

            try (PreparedStatement stmt = conn.prepareStatement(insertReportSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, requestId);
                stmt.setLong(2, passengerId);
                if (driverId != null) stmt.setLong(3, driverId);
                else stmt.setNull(3, Types.BIGINT);
                stmt.setString(4, report.getDetails());
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) generatedReportId = keys.getLong(1);
                    else throw new SQLException("Failed to retrieve generated report_id.");
                }
            }

            try (PreparedStatement stmt2 = conn.prepareStatement(insertTypesSql)) {
                for (ProblemType type : report.getTypes()) {
                    stmt2.setLong(1, generatedReportId);
                    stmt2.setInt(2, getProblemTypeId(type, conn));
                    stmt2.addBatch();
                }
                stmt2.executeBatch();
            }

            conn.commit();
            System.out.println("✅ Problem Report Saved Successfully. Report ID: " + generatedReportId);
            return generatedReportId;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
