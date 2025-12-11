package DAO;

import utils.DBConnection;

import java.sql.*;

public class
CompanyTransactionDAO {

    /**
     * Add a new transaction to track company revenue
     * @param rideId The ride request ID
     * @param amount The amount earned by the company
     * @param type Transaction type: "COMPLETED" or "CANCELLED_BY_PASSENGER"
     * @return The generated transaction ID, or -1 if failed
     */
    public long addTransaction(long rideId, double amount, String type) {
        final String sql = "INSERT INTO company_transactions(ride_id, amount, transaction_type) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, rideId);
            ps.setDouble(2, amount);
            ps.setString(3, type);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    System.out.println("✓ Company transaction recorded: ID=" + id + ", RideID=" + rideId +
                                     ", Amount=" + amount + " EGP, Type=" + type);
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Failed to add company transaction: " + e.getMessage());
            e.printStackTrace();
        }

        return -1L;
    }

    /**
     * Get total revenue earned by the company
     * @return Total amount from all transactions
     */
    public double getTotal() {
        final String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM company_transactions";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("✗ Failed to get company total: " + e.getMessage());
            e.printStackTrace();
        }

        return 0.0;
    }

    /**
     * Get total revenue by transaction type
     * @param type Transaction type: "COMPLETED" or "CANCELLED_BY_PASSENGER"
     * @return Total amount for the specified type
     */
    public double getTotalByType(String type) {
        final String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM company_transactions WHERE transaction_type = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, type);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("✗ Failed to get company total by type: " + e.getMessage());
            e.printStackTrace();
        }

        return 0.0;
    }
}

