package controller;

import Model.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.DBConnection;

import java.io.IOException;
import java.sql.*;

public class AddFundsController {

    @FXML private Button backButton;
    @FXML private Button addMoneyButton;
    @FXML private Button logoutButton;
    @FXML private TextField amountField;
    @FXML private Label currentBalanceLabel;
    @FXML private Label messageLabel;

    private Person currentUser;

    public void setUser(Person user) {
        this.currentUser = user;
        updateBalanceDisplay();
    }

    @FXML
    public void initialize() {
        // Restrict amount field to numbers and decimal point only
        amountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                amountField.setText(oldValue);
            }
        });
    }

    private void updateBalanceDisplay() {
        if (currentUser != null) {
            double balance = currentUser.getWalletBalance();
            currentBalanceLabel.setText(String.format("%.2f EGP", balance));
        }
    }

    @FXML
    public void quickAdd50() {
        amountField.setText("50");
    }

    @FXML
    public void quickAdd100() {
        amountField.setText("100");
    }

    @FXML
    public void quickAdd200() {
        amountField.setText("200");
    }

    @FXML
    public void onAddMoney() {
        // Get the amount from the text field
        String amountText = amountField.getText().trim();

        // Validate input is not empty
        if (amountText.isEmpty()) {
            showError("Please enter a valid amount.");
            return;
        }

        // Validate input is numeric
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            showError("Please enter a valid amount.");
            return;
        }

        // Validate amount is positive
        if (amount <= 0) {
            showError("Amount must be greater than 0");
            return;
        }

        // Validate maximum amount
        if (amount > 10000) {
            showError("Maximum amount is 10,000 EGP");
            return;
        }

        // Get user ID from database
        long userId = getUserIdFromDatabase();
        if (userId == -1) {
            showError("User not found in database");
            return;
        }

        // Update database balance
        try {
            boolean success = updateBalanceInDatabase(userId, amount);

            if (success) {
                // Update the in-memory user object
                double newBalance = currentUser.getWalletBalance() + amount;
                currentUser.updateWalletBalance(newBalance);

                // Refresh the balance display immediately
                updateBalanceDisplay();

                // Show success message
                showSuccess(String.format("Successfully added %.2f EGP to your wallet!", amount));

                // Clear the amount field
                amountField.clear();
            } else {
                showError("Failed to update balance. Please try again.");
            }

        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get user ID from database
     */
    private long getUserIdFromDatabase() {
        if (currentUser == null) {
            return -1;
        }

        String tableName = (currentUser instanceof Model.Driver) ? "drivers" : "passengers";
        String sql = "SELECT id FROM " + tableName + " WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, currentUser.getEmail());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user ID: " + e.getMessage());
            e.printStackTrace();
        }

        return -1;
    }

    /**
     * Update balance in database using direct SQL UPDATE
     * This is the KEY FIX - using executeUpdate() properly
     */
    private boolean updateBalanceInDatabase(long userId, double amount) throws SQLException {
        String tableName = (currentUser instanceof Model.Driver) ? "drivers" : "passengers";
        String sql = "UPDATE " + tableName + " SET wallet_balance = wallet_balance + ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDouble(1, amount);
            ps.setLong(2, userId);

            // Execute the update - THIS IS THE KEY FIX
            int rowsAffected = ps.executeUpdate();

            // Log the result
            System.out.println("Balance update - Rows affected: " + rowsAffected);

            // Return true if at least one row was updated
            return rowsAffected > 0;
        }
    }

    /**
     * Refresh balance from database after update
     */
    private void refreshBalanceFromDatabase(long userId) {
        String tableName = (currentUser instanceof Model.Driver) ? "drivers" : "passengers";
        String sql = "SELECT wallet_balance FROM " + tableName + " WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double newBalance = rs.getDouble("wallet_balance");
                    currentUser.updateWalletBalance(newBalance);
                    currentBalanceLabel.setText(String.format("%.2f EGP", newBalance));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error refreshing balance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void onBackToProfile() {
        try {
            // Refresh balance from database before navigating back
            long userId = getUserIdFromDatabase();
            if (userId != -1) {
                refreshBalanceFromDatabase(userId);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Profile.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            // Pass updated user data back to profile
            ProfileController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to Profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle Logout button click
     * Clears current user session and navigates back to RoleSelection screen
     */
    @FXML
    public void onLogout() {
        System.out.println("=== Logout clicked ===");

        try {
            // Clear current user session
            currentUser = null;
            System.out.println("User session cleared");

            // Load RoleSelection screen using existing navigation pattern
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RoleSelection.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            // Get current stage and set new scene
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

            System.out.println("Navigated to RoleSelection screen successfully");
        } catch (IOException e) {
            System.err.println("Failed to navigate to RoleSelection: " + e.getMessage());
            e.printStackTrace();
            showError("Logout failed. Please try again.");
        }
    }

    private void showError(String message) {
        messageLabel.setText("❌ " + message);
        messageLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
    }

    private void showSuccess(String message) {
        messageLabel.setText("✓ " + message);
        messageLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
    }
}

