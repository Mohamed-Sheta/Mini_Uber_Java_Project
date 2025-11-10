package DAO;
import utils.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProblemReportTypeDAO {

    public static class ProblemReportTypeRow {
        public final long reportId;
        public final int typeId;
        public final String details;

        public ProblemReportTypeRow(long reportId, int typeId, String details) {
            this.reportId = reportId;
            this.typeId = typeId;
            this.details = details;
        }

        @Override
        public String toString() {
            return "ProblemReportTypeRow{" +
                    "reportId=" + reportId +
                    ", typeId=" + typeId +
                    ", details='" + details + '\'' +
                    '}';
        }
    }

    public int insert(long reportId, int typeId, String details) throws SQLException {
        final String sql = "INSERT INTO problem_report_types(report_id, type_id, details) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, reportId);
            ps.setInt(2, typeId);
            ps.setString(3, details);
            return ps.executeUpdate();
        }
    }

    public int delete(long reportId, int typeId) throws SQLException {
        final String sql = "DELETE FROM problem_report_types WHERE report_id = ? AND type_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, reportId);
            ps.setInt(2, typeId);
            return ps.executeUpdate();
        }
    }

    public int update(long reportId, int typeId, String newDetails) throws SQLException {
        final String sql = "UPDATE problem_report_types SET details = ? WHERE report_id = ? AND type_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newDetails);
            ps.setLong(2, reportId);
            ps.setInt(3, typeId);
            return ps.executeUpdate();
        }
    }

    public List<ProblemReportTypeRow> showAll() throws SQLException {
        final String sql = "SELECT report_id, type_id, details FROM problem_report_types ORDER BY report_id, type_id";

        List<ProblemReportTypeRow> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new ProblemReportTypeRow(
                        rs.getLong("report_id"),
                        rs.getInt("type_id"),
                        rs.getString("details")
                ));
            }
        }
        return list;
    }

    public void initializeProblemTypes() throws SQLException {
        String sql = "INSERT IGNORE INTO problem_types (id, name) VALUES " +
                "(1,'DRIVER_BEHAVIOR'), " +
                "(2,'DRIVER_LATE'), " +
                "(3,'RECKLESS_DRIVING'), " +
                "(4,'VEHICLE_CLEANLINESS'), " +
                "(5,'TECHNICAL_ISSUE'), " +
                "(6,'FARE_DISPUTE'), " +
                "(7,'ACCOUNT_ISSUE')";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
}
