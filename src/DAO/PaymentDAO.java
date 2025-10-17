package DAO;

import Model.Payment;
import Model.PaymentType;
import utils.connection;

import java.sql.*;

public class PaymentDAO {
    private int getPaymentTypeId(PaymentType type) throws SQLException {
        String sql = "SELECT type_id FROM PaymentType WHERE type_name = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("type_id");
                } else {
                    throw new SQLException("PaymentType not found for: " + type.name());
                }
            }
        }
    }

    public boolean addPayment(Payment payment, int optionId) throws SQLException {
        String sql = "INSERT INTO Payment (amount, payment_type_id, option_id) VALUES (?, ?, ?)";

        int typeId = getPaymentTypeId(payment.getPaymentMethod());

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setDouble(1, payment.getAmount());
            stmt.setInt(2, typeId);
            stmt.setInt(3, optionId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        payment.setPaymentId(generatedKeys.getInt(1));
                        System.out.println("✅ Payment saved with ID: " + payment.getPaymentId());
                        return true;
                    }
                }
            }
            System.out.println("❌ Failed to save payment");
            return false;
        }
    }
}