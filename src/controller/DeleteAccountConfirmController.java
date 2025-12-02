package controller;

import DAO.*;
import Model.Driver;
import Model.Person;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import utils.DBConnection;
import utils.UserSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteAccountConfirmController {

    @FXML private Button cancelButton;
    @FXML private Button deleteButton;
    @FXML private Label messageLabel;

    private Person currentUser;
    private boolean isDriver = false;

    /**
     * Set the current user
     */
    public void setUser(Person user) {
        this.currentUser = user;
        this.isDriver = (user instanceof Driver);
    }

    /**
     * Handle Cancel button - return to ProfileSettings
     */
    @FXML
    public void onCancel() {
        navigateToProfileSettings();
    }

    /**
     * Handle Delete button - delete account and navigate to RoleSelection
     */
    @FXML
    public void onDelete() {
        try {
            System.out.println("[DeleteAccount] Starting account deletion for: " + currentUser.getEmail());

            // ALWAYS use manual cascade deletion to properly handle foreign key constraints
            // This ensures related records (problem_reports, ride_history, etc.) are deleted first
            boolean success = deleteWithManualCascade();

            if (success) {
                System.out.println("[DeleteAccount] ✅ Account deleted successfully");
                // Clear user session
                UserSession.getInstance().clearSession();
                // Navigate to RoleSelection after successful deletion
                navigateToRoleSelection();
            } else {
                System.err.println("[DeleteAccount] ❌ Account deletion failed");
                showError("Failed to delete account. Please try again.");
            }
        } catch (SQLException e) {
            System.err.println("[DeleteAccount] ❌ SQL Error during deletion: " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage() != null && e.getMessage().contains("foreign key constraint")) {
                showError("Unable to delete account due to database constraints. Please contact support.");
            } else {
                showError("Failed to delete account. Please try again.");
            }
        } catch (Exception e) {
            System.err.println("[DeleteAccount] ❌ Unexpected error:");
            e.printStackTrace();
            showError("An unexpected error occurred. Please try again.");
        }
    }

    /**
     * Delete user from database (primary method)
     */
    private boolean deleteUserFromDatabase() throws SQLException {
        String tableName = isDriver ? "drivers" : "passengers";
        String sql = "DELETE FROM " + tableName + " WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, currentUser.getEmail());
            int rowsDeleted = ps.executeUpdate();

            if (rowsDeleted > 0) {
                System.out.println("[DeleteAccount] Deleted " + rowsDeleted + " user record(s)");
            }

            return rowsDeleted > 0;
        }
    }

    /**
     * Delete user with manual cascade - delete related records first using DAOs and SQL
     * This handles all foreign key constraints properly
     */
    private boolean deleteWithManualCascade() throws SQLException {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false); // Start transaction

            String email = currentUser.getEmail();
            String userTable = isDriver ? "drivers" : "passengers";

            // Get user ID
            long userId = getUserId(con, email, userTable);
            if (userId == -1) {
                System.err.println("[DeleteAccount] User ID not found");
                con.rollback();
                return false;
            }

            System.out.println("[DeleteAccount] User ID: " + userId + " (Table: " + userTable + ")");

            // Initialize DAOs
            ProblemReportDAO problemReportDAO = new ProblemReportDAO();

            // Step 1: Delete problem reports using DAO
            if (!isDriver) {
                // Delete reports filed by the passenger
                int deleted = problemReportDAO.deleteReportsByPassenger(con, userId);
                System.out.println("[DeleteAccount] Deleted " + deleted + " problem report(s) filed by passenger");
            } else {
                // Delete reports about the driver
                int deleted = problemReportDAO.deleteReportsByDriver(con, userId);
                System.out.println("[DeleteAccount] Deleted " + deleted + " problem report(s) about driver");
            }

            // Step 2: Delete ride history using SQL (no DAO method exists)
            String historyColumn = isDriver ? "driver_id" : "passenger_id";
            String deleteHistorySql = "DELETE FROM ride_history WHERE " + historyColumn + " = ?";
            int historyDeleted = 0;
            try (PreparedStatement ps = con.prepareStatement(deleteHistorySql)) {
                ps.setLong(1, userId);
                historyDeleted = ps.executeUpdate();
                System.out.println("[DeleteAccount] Deleted " + historyDeleted + " ride history record(s)");
            }

            // Step 3: Handle ride requests using SQL
            int requestsAffected = 0;
            if (isDriver) {
                // For drivers: set driver_id to NULL (preserve request history)
                String updateRequestsSql = "UPDATE ride_requests SET driver_id = NULL WHERE driver_id = ?";
                try (PreparedStatement ps = con.prepareStatement(updateRequestsSql)) {
                    ps.setLong(1, userId);
                    requestsAffected = ps.executeUpdate();
                    System.out.println("[DeleteAccount] Cleared driver from " + requestsAffected + " ride request(s)");
                }
            } else {
                // For passengers: delete all their requests
                String deleteRequestsSql = "DELETE FROM ride_requests WHERE passenger_id = ?";
                try (PreparedStatement ps = con.prepareStatement(deleteRequestsSql)) {
                    ps.setLong(1, userId);
                    requestsAffected = ps.executeUpdate();
                    System.out.println("[DeleteAccount] Deleted " + requestsAffected + " ride request(s)");
                }
            }

            // Step 4: Finally, delete the user using SQL (not DAO to avoid connection conflict)
            String deleteUserSql = "DELETE FROM " + userTable + " WHERE id = ?";
            int userDeleted = 0;
            try (PreparedStatement ps = con.prepareStatement(deleteUserSql)) {
                ps.setLong(1, userId);
                userDeleted = ps.executeUpdate();
                System.out.println("[DeleteAccount] Deleted user account: " + (userDeleted > 0 ? "SUCCESS" : "FAILED"));
            }

            if (userDeleted == 0) {
                System.err.println("[DeleteAccount] Failed to delete user record");
                con.rollback();
                return false;
            }

            // Commit transaction
            con.commit();
            System.out.println("[DeleteAccount] ✅ Transaction committed successfully");
            return true;

        } catch (SQLException e) {
            System.err.println("[DeleteAccount] Error during manual cascade deletion:");
            e.printStackTrace();
            if (con != null) {
                try {
                    con.rollback();
                    System.out.println("[DeleteAccount] Transaction rolled back");
                } catch (SQLException rollbackEx) {
                    System.err.println("[DeleteAccount] Rollback failed:");
                    rollbackEx.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    System.err.println("[DeleteAccount] Error closing connection:");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get user ID from database
     */
    private long getUserId(Connection con, String email, String tableName) throws SQLException {
        String sql = "SELECT id FROM " + tableName + " WHERE email = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        }
        return -1;
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.setStyle("-fx-text-fill: #F85149; -fx-font-size: 13px; -fx-font-weight: 600;");
    }

    /**
     * Navigate back to ProfileSettings (Passenger) or DriverSettings (Driver)
     */
    private void navigateToProfileSettings() {
        try {
            if (isDriver) {
                // Navigate to DriverSettings
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverSettings.fxml"));
                Scene scene = new Scene(loader.load(), 390, 750);

                DriverSettingsController controller = loader.getController();
                if (currentUser != null) {
                    // ✅ CRITICAL: Fetch fresh driver data from database before opening Settings
                    // DO NOT pass the old driver object as it may have stale balance
                    try {
                        DriverDAO driverDAO = new DriverDAO();
                        Long driverId = driverDAO.getDriverIdByEmail(currentUser.getEmail());
                        if (driverId != null) {
                            Driver freshDriver = driverDAO.getDriverById(driverId);
                            if (freshDriver != null) {
                                controller.setUser(freshDriver);
                                System.out.println("[DeleteAccount] ✅ Opened DriverSettings with fresh driver data from database");
                            } else {
                                // Fallback: use old object if fetch fails
                                controller.setUser(currentUser);
                                controller.refreshBalance();
                                System.out.println("[DeleteAccount] ⚠️ Using old driver object, refreshing balance");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[DeleteAccount] Error fetching fresh driver data: " + e.getMessage());
                        controller.setUser(currentUser);
                        controller.refreshBalance();
                    }
                }

                Stage stage = (Stage) cancelButton.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            } else {
                // Navigate to PassengerSettings (ProfileSettings)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProfileSettings.fxml"));
                Scene scene = new Scene(loader.load(), 390, 750);

                ProfileSettingsController controller = loader.getController();
                if (currentUser != null) {
                    controller.setUser(currentUser);
                }

                Stage stage = (Stage) cancelButton.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            }
        } catch (IOException e) {
            System.err.println("Failed to navigate back to Settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigate to RoleSelection screen
     */
    private void navigateToRoleSelection() {
        try {
            // Clear the user session (logout after account deletion)
            UserSession.getInstance().clearSession();
            System.out.println("[DeleteAccount] Session cleared, navigating to Role Selection");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RoleSelection.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            Stage stage = (Stage) deleteButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to RoleSelection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

