package DAO;

import Model.Option;
import utils.connection;

import java.sql.*;

public class OptionDAO {
    public int addOption(Option option) throws SQLException {
        String sql = "INSERT INTO `OptionS` (tips, donation_amount, donation_organization, is_tips_enabled, is_donation_enabled, is_rate_enabled) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setFloat(1, option.getTips());
            stmt.setFloat(2, option.getDonationAmount());
            stmt.setString(3, option.getDonationOrganization());

            stmt.setBoolean(4, false);
            stmt.setBoolean(5, false);
            stmt.setBoolean(6, false);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int optionId = generatedKeys.getInt(1);
                        System.out.println("✅ Option record saved with ID: " + optionId);
                        return optionId;
                    }
                }
            }
            return -1;
        }
    }

    public Option getOptionById(int id) throws SQLException {
        String sql = "SELECT * FROM `OptionS` WHERE option_id = ?";

        try (Connection conn = connection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Option option = new Option();
                    option.setTipsAmount(rs.getFloat("tips"));
                    return option;
                }
            }
        }
        return null;
    }
}