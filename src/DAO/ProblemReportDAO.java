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
        public final Timestamp createdAt;

        public ProblemReportRow(long id, long requestId, long reporterPassengerId, Long driverId, Timestamp createdAt) {
            this.id = id;
            this.requestId = requestId;
            this.reporterPassengerId = reporterPassengerId;
            this.driverId = driverId;
            this.createdAt = createdAt;
        }

        @Override
        public String toString() {
            return "ProblemReport{id=" + id + ", request=" + requestId + "}";
        }
    }

    public long insertReport(long requestId, long reporterPassengerId, Long driverIdNullable) throws SQLException {
        final String sql = "INSERT INTO problem_reports(request_id, reporter_passenger_id, driver_id) VALUES (?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, requestId);
            ps.setLong(2, reporterPassengerId);

            if (driverIdNullable == null) ps.setNull(3, Types.BIGINT);
            else ps.setLong(3, driverIdNullable);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    public int updateReport(long id, Long newDriverIdNullable) throws SQLException {
        final String sql = "UPDATE problem_reports SET driver_id=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (newDriverIdNullable == null) ps.setNull(1, Types.BIGINT);
            else ps.setLong(1, newDriverIdNullable);

            ps.setLong(2, id);
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

    /**
     * Delete all problem reports filed by a specific passenger
     * @param passengerId the passenger ID
     * @return number of reports deleted
     */
    public int deleteReportsByPassenger(long passengerId) throws SQLException {
        final String sql = "DELETE FROM problem_reports WHERE reporter_passenger_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, passengerId);
            return ps.executeUpdate();
        }
    }

    /**
     * Delete all problem reports about a specific driver
     * @param driverId the driver ID
     * @return number of reports deleted
     */
    public int deleteReportsByDriver(long driverId) throws SQLException {
        final String sql = "DELETE FROM problem_reports WHERE driver_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            return ps.executeUpdate();
        }
    }

    /**
     * Delete all problem reports filed by a passenger using a specific connection (for transactions)
     */
    public int deleteReportsByPassenger(Connection con, long passengerId) throws SQLException {
        final String sql = "DELETE FROM problem_reports WHERE reporter_passenger_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, passengerId);
            return ps.executeUpdate();
        }
    }

    /**
     * Delete all problem reports about a driver using a specific connection (for transactions)
     */
    public int deleteReportsByDriver(Connection con, long driverId) throws SQLException {
        final String sql = "DELETE FROM problem_reports WHERE driver_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            return ps.executeUpdate();
        }
    }

    public List<ProblemReportRow> showAllReports() throws SQLException {
        final String sql = "SELECT id, request_id, reporter_passenger_id, driver_id, created_at FROM problem_reports ORDER BY id";
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
                        rs.getTimestamp("created_at")
                ));
            }
        }
        return out;
    }
}