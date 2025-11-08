package DAO;
import utils.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProblemReportTypeDAO {

    // كلاس بسيط يمثل صف واحد من الجدول
    public static class ProblemReportTypeRow {
        public final long reportId;
        public final int typeId;

        public ProblemReportTypeRow(long reportId, int typeId) {
            this.reportId = reportId;
            this.typeId = typeId;
        }

        @Override
        public String toString() {
            return "ProblemReportTypeRow{report_id=" + reportId + ", type_id=" + typeId + "}";
        }
    }

    // --------------------------------------------------------
    // INSERT
    // --------------------------------------------------------
    public int insert(long reportId, int typeId) throws SQLException {
        final String sql = "INSERT INTO problem_report_types(report_id, type_id) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, reportId);
            ps.setInt(2, typeId);
            return ps.executeUpdate();
        }
    }

    // --------------------------------------------------------
    // DELETE
    // --------------------------------------------------------
    public int delete(long reportId, int typeId) throws SQLException {
        final String sql = "DELETE FROM problem_report_types WHERE report_id = ? AND type_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, reportId);
            ps.setInt(2, typeId);
            return ps.executeUpdate();
        }
    }

    // --------------------------------------------------------
    // UPDATE
    // (نادر الاستخدام في جدول ربط، لكن مضاف لو احتجته)
    // --------------------------------------------------------
    public int update(long oldReportId, int oldTypeId, long newReportId, int newTypeId) throws SQLException {
        final String sql = "UPDATE problem_report_types SET report_id = ?, type_id = ? WHERE report_id = ? AND type_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, newReportId);
            ps.setInt(2, newTypeId);
            ps.setLong(3, oldReportId);
            ps.setInt(4, oldTypeId);
            return ps.executeUpdate();
        }
    }

    // --------------------------------------------------------
    // SHOW ALL
    // --------------------------------------------------------
    public List<ProblemReportTypeRow> showAll() throws SQLException {
        final String sql = "SELECT report_id, type_id FROM problem_report_types ORDER BY report_id, type_id";
        List<ProblemReportTypeRow> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new ProblemReportTypeRow(
                        rs.getLong("report_id"),
                        rs.getInt("type_id")
                ));
            }
        }
        return list;
    }
}