package DAO;
import utils.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProblemReportDAO {

    public static class ProblemReportRow {
        public final long id;
        public final long requestId;
        public final long reporterPassengerId;
        public final Long driverId;
        public final String details;
        public final Timestamp createdAt;
        public ProblemReportRow(long id, long requestId, long reporterPassengerId, Long driverId, String details, Timestamp createdAt) {
            this.id=id; this.requestId=requestId; this.reporterPassengerId=reporterPassengerId; this.driverId=driverId; this.details=details; this.createdAt=createdAt;
        }
        @Override public String toString(){ return "ProblemReportRow{id="+id+", request="+requestId+"}"; }
    }

    public static class ProblemReportTypeRow {
        public final long reportId;
        public final int typeId;
        public ProblemReportTypeRow(long reportId, int typeId){ this.reportId=reportId; this.typeId=typeId; }
        @Override public String toString(){ return "ProblemReportTypeRow{report="+reportId+", type="+typeId+"}"; }
    }

    public long insertReport(long requestId, long reporterPassengerId, Long driverIdNullable, String details) throws SQLException {
        final String sql = "INSERT INTO problem_reports(request_id, reporter_passenger_id, driver_id, details) VALUES (?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, requestId);
            ps.setLong(2, reporterPassengerId);
            if (driverIdNullable == null) ps.setNull(3, Types.BIGINT); else ps.setLong(3, driverIdNullable);
            ps.setString(4, details);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    public int updateReport(long id, String newDetails, Long newDriverIdNullable) throws SQLException {
        final String sql = "UPDATE problem_reports SET details=?, driver_id=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newDetails);
            if (newDriverIdNullable == null) ps.setNull(2, Types.BIGINT); else ps.setLong(2, newDriverIdNullable);
            ps.setLong(3, id);
            return ps.executeUpdate();
        }
    }

    public int deleteReport(long id) throws SQLException {
        final String sql = "DELETE FROM problem_reports WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    public List<ProblemReportRow> showAllReports() throws SQLException {
        final String sql = "SELECT id, request_id, reporter_passenger_id, driver_id, details, created_at FROM problem_reports ORDER BY id";
        List<ProblemReportRow> out = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Long driverId = (Long) rs.getObject("driver_id");
                out.add(new ProblemReportRow(
                        rs.getLong("id"),
                        rs.getLong("request_id"),
                        rs.getLong("reporter_passenger_id"),
                        driverId,
                        rs.getString("details"),
                        rs.getTimestamp("created_at")
                ));
            }
        }
        return out;
    }

    // ---------- Problem Report Types (junction) ----------

    public int insertReportType(long reportId, int typeId) throws SQLException {
        final String sql = "INSERT INTO problem_report_types(report_id, type_id) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, reportId);
            ps.setInt(2, typeId);
            return ps.executeUpdate();
        }
    }

    public int deleteReportType(long reportId, int typeId) throws SQLException {
        final String sql = "DELETE FROM problem_report_types WHERE report_id=? AND type_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, reportId);
            ps.setInt(2, typeId);
            return ps.executeUpdate();
        }
    }

    public List<ProblemReportTypeRow> showAllReportTypes() throws SQLException {
        final String sql = "SELECT report_id, type_id FROM problem_report_types ORDER BY report_id, type_id";
        List<ProblemReportTypeRow> out = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new ProblemReportTypeRow(rs.getLong("report_id"), rs.getInt("type_id")));
            }
        }
        return out;
    }
}

