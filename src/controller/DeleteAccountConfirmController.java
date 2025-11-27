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

            // Delete user with proper cascade handling
            boolean success = deleteUserFromDatabase();

            if (success) {
                System.out.println("[DeleteAccount] ✅ Account deleted successfully");
                // Navigate to RoleSelection after successful deletion
                navigateToRoleSelection();
            } else {
                System.err.println("[DeleteAccount] ❌ Account deletion failed - no rows affected");
                showError("Failed to delete account. Please try again.");
            }
        } catch (SQLException e) {
            // Handle foreign key constraint violations gracefully
            System.err.println("[DeleteAccount] ❌ SQL Error during deletion:");
            e.printStackTrace();

            if (e.getMessage() != null && e.getMessage().contains("foreign key constraint")) {
                // Try to manually clean up related records before deletion
                System.out.println("[DeleteAccount] Foreign key constraint detected. Attempting manual cleanup...");
                try {
                    boolean cleanupSuccess = deleteWithManualCascade();
                    if (cleanupSuccess) {
                        System.out.println("[DeleteAccount] ✅ Account deleted successfully after manual cleanup");
                        navigateToRoleSelection();
                        return;
                    }
                } catch (Exception cleanupException) {
                    System.err.println("[DeleteAccount] Manual cleanup failed: " + cleanupException.getMessage());
                    cleanupException.printStackTrace();
                }

                showError("Unable to delete account. Please contact support.");
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
     * Delete user with manual cascade - delete related records first using DAOs
     * This is a fallback if ON DELETE CASCADE is not configured in the database
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
            RideHistoryDAO rideHistoryDAO = new RideHistoryDAO();
            RideRequestDAO rideRequestDAO = new RideRequestDAO();

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

            // Step 2: Delete ride history using DAO
            int historyDeleted;
            if (isDriver) {
                historyDeleted = rideHistoryDAO.deleteByDriver(con, userId);
            } else {
                historyDeleted = rideHistoryDAO.deleteByPassenger(con, userId);
            }
            System.out.println("[DeleteAccount] Deleted " + historyDeleted + " ride history record(s)");

            // Step 3: Handle ride requests using DAO
            if (isDriver) {
                // For drivers: set driver_id to NULL (preserve request history)
                int updated = rideRequestDAO.clearDriverFromRequests(con, userId);
                System.out.println("[DeleteAccount] Cleared driver from " + updated + " ride request(s)");
            } else {
                // For passengers: delete all their requests
                int deleted = rideRequestDAO.deleteByPassenger(con, userId);
                System.out.println("[DeleteAccount] Deleted " + deleted + " ride request(s)");
            }

            // Step 4: Finally, delete the user using DAO
            int userDeleted;
            if (isDriver) {
                DriverDAO driverDAO = new DriverDAO();
                userDeleted = driverDAO.delete(userId);
            } else {
                PassengerDAO passengerDAO = new PassengerDAO();
                userDeleted = passengerDAO.delete(userId);
            }

            System.out.println("[DeleteAccount] Deleted user account: " + (userDeleted > 0 ? "SUCCESS" : "FAILED"));

            if (userDeleted == 0) {
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
     * Navigate back to ProfileSettings
     */
    private void navigateToProfileSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProfileSettings.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            ProfileSettingsController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to ProfileSettings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigate to RoleSelection screen
     */
    private void navigateToRoleSelection() {
        try {
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

