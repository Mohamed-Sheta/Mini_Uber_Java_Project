package controller;

import DAO.DriverDAO;
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

    public void setUser(Person user) {
        this.currentUser = user;
        this.isDriver = (user instanceof Driver);
    }

    @FXML
    public void onCancel() {
        navigateToProfileSettings();
    }

    @FXML
    public void onDelete() {
        try {
            System.out.println("[SoftDelete] ====================================");
            System.out.println("[SoftDelete] Initiating soft delete for user: " + currentUser.getEmail());
            System.out.println("[SoftDelete] User type: " + (isDriver ? "Driver" : "Passenger"));

            boolean success = deleteWithManualCascade();

            if (success) {
                System.out.println("[SoftDelete] Soft delete completed - user account removed, historical data preserved");
                // Clear user session
                UserSession.getInstance().clearSession();
                // Navigate to RoleSelection after successful deletion
                navigateToRoleSelection();
            } else {
                System.err.println("[SoftDelete] Soft delete failed - operation returned false");
                showError("Failed to delete account. Please try again.");
            }
        } catch (SQLException e) {
            System.err.println("[SoftDelete] SQL Error during soft delete operation");
            System.err.println("[SoftDelete] Error message: " + e.getMessage());
            System.err.println("[SoftDelete] Error code: " + e.getErrorCode());
            e.printStackTrace();

            if (e.getMessage() != null && e.getMessage().contains("foreign key constraint")) {
                showError("Unable to delete account due to database constraints. Please contact support.");
            } else {
                showError("Failed to delete account. Please try again.");
            }
        } catch (Exception e) {
            System.err.println("[SoftDelete] Unexpected error during soft delete operation:");
            e.printStackTrace();
            showError("An unexpected error occurred. Please try again.");
        }
    }

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
                System.err.println("[SoftDelete] User ID not found for email: " + email);
                con.rollback();
                return false;
            }

            System.out.println("[SoftDelete] ====================================");
            System.out.println("[SoftDelete] Starting SOFT DELETE for User ID: " + userId);
            System.out.println("[SoftDelete] User Type: " + (isDriver ? "DRIVER" : "PASSENGER"));
            System.out.println("[SoftDelete] Strategy: Replace FKs with -1 → Delete User Account");
            System.out.println("[SoftDelete] ====================================");

            // CRITICAL: Disable foreign key checks temporarily to allow setting FK to -1
            // This allows us to set foreign keys to a non-existent ID (-1) without constraint violations
            try (PreparedStatement ps = con.prepareStatement("SET FOREIGN_KEY_CHECKS = 0")) {
                ps.execute();
                System.out.println("[SoftDelete] Foreign key checks temporarily disabled");
            }

            // Track total records affected
            int totalRecordsUpdated = 0;

            // Step 0: Delete profile photo (if exists)
            System.out.println("[SoftDelete] Step 0: Deleting profile photo (if exists)...");
            int photoDeleted = deleteProfilePhoto(con, userId, isDriver ? "driver" : "passenger");
            System.out.println("[SoftDelete]   " + (photoDeleted > 0 ? "✓" : "○") +
                             " Profile photo: " + (photoDeleted > 0 ? photoDeleted + " record(s) deleted" : "none found"));

            if (isDriver) {
                // ===== DRIVER SOFT DELETE =====
                System.out.println("[SoftDelete] Step 1: Replacing foreign key references with -1 for DRIVER...");

                // Step 1: Replace driver_id with -1 in ride_requests (preserve request history)
                int requestsUpdated = updateForeignKeyToPlaceholder(con, "ride_requests", "driver_id", userId);
                totalRecordsUpdated += requestsUpdated;
                System.out.println("[SoftDelete]   ✓ ride_requests.driver_id: " + requestsUpdated + " record(s) set to -1");

                // Step 2: Replace driver_id with -1 in ride_history (preserve ride history for analytics)
                int historyUpdated = updateForeignKeyToPlaceholder(con, "ride_history", "driver_id", userId);
                totalRecordsUpdated += historyUpdated;
                System.out.println("[SoftDelete]   ✓ ride_history.driver_id: " + historyUpdated + " record(s) set to -1");

                // Step 3: Replace driver_id with -1 in problem_reports (preserve reports for system integrity)
                int reportsUpdated = updateForeignKeyToPlaceholder(con, "problem_reports", "driver_id", userId);
                totalRecordsUpdated += reportsUpdated;
                System.out.println("[SoftDelete]   ✓ problem_reports.driver_id: " + reportsUpdated + " record(s) set to -1");

            } else {
                // ===== PASSENGER SOFT DELETE =====
                System.out.println("[SoftDelete] Step 1: Replacing foreign key references with -1 for PASSENGER...");

                // Step 1: Replace passenger_id with -1 in ride_requests (preserve request history)
                int requestsUpdated = updateForeignKeyToPlaceholder(con, "ride_requests", "passenger_id", userId);
                totalRecordsUpdated += requestsUpdated;
                System.out.println("[SoftDelete]   ✓ ride_requests.passenger_id: " + requestsUpdated + " record(s) set to -1");

                // Step 2: Replace passenger_id with -1 in ride_history (preserve ride history for analytics)
                int historyUpdated = updateForeignKeyToPlaceholder(con, "ride_history", "passenger_id", userId);
                totalRecordsUpdated += historyUpdated;
                System.out.println("[SoftDelete]   ✓ ride_history.passenger_id: " + historyUpdated + " record(s) set to -1");

                // Step 3: Replace reporter_passenger_id with -1 in problem_reports (preserve reports for system integrity)
                int reportsUpdated = updateForeignKeyToPlaceholder(con, "problem_reports", "reporter_passenger_id", userId);
                totalRecordsUpdated += reportsUpdated;
                System.out.println("[SoftDelete]   ✓ problem_reports.reporter_passenger_id: " + reportsUpdated + " record(s) set to -1");

                // Step 4: Replace user_id with -1 in reports (app issue reports - preserve for system analytics)
                int appReportsUpdated = updateForeignKeyToPlaceholder(con, "reports", "user_id", userId);
                totalRecordsUpdated += appReportsUpdated;
                System.out.println("[SoftDelete]   ✓ reports.user_id: " + appReportsUpdated + " record(s) set to -1");
            }

            System.out.println("[SoftDelete] Total foreign key references updated: " + totalRecordsUpdated);
            System.out.println("[SoftDelete] All historical data preserved (no rows deleted)");

            // Step 2: Finally, delete ONLY the user account record
            System.out.println("[SoftDelete] Step 2: Deleting user account record...");
            String deleteUserSql = "DELETE FROM " + userTable + " WHERE id = ?";
            int userDeleted;
            try (PreparedStatement ps = con.prepareStatement(deleteUserSql)) {
                ps.setLong(1, userId);
                userDeleted = ps.executeUpdate();
                System.out.println("[SoftDelete]   " + (userDeleted > 0 ? "✓" : "✗") +
                                 " User account deletion from '" + userTable + "': " +
                                 (userDeleted > 0 ? "SUCCESS" : "FAILED"));
            }

            if (userDeleted == 0) {
                System.err.println("[SoftDelete] ERROR: Failed to delete user record - rolling back transaction");
                // Re-enable foreign key checks before rollback
                try (PreparedStatement ps = con.prepareStatement("SET FOREIGN_KEY_CHECKS = 1")) {
                    ps.execute();
                }
                con.rollback();
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement("SET FOREIGN_KEY_CHECKS = 1")) {
                ps.execute();
                System.out.println("[SoftDelete] Foreign key checks re-enabled");
            }

            con.commit();
            System.out.println("[SoftDelete] ====================================");
            System.out.println("[SoftDelete] ✓ SOFT DELETE COMPLETED SUCCESSFULLY");
            System.out.println("[SoftDelete] ✓ Transaction committed");
            System.out.println("[SoftDelete] ✓ " + totalRecordsUpdated + " historical records preserved (FK = -1)");
            System.out.println("[SoftDelete] ✓ User account removed from '" + userTable + "'");
            System.out.println("[SoftDelete] ====================================");
            return true;

        } catch (SQLException e) {
            System.err.println("[SoftDelete] ====================================");
            System.err.println("[SoftDelete] ERROR: SQL exception during soft delete operation");
            System.err.println("[SoftDelete] Error message: " + e.getMessage());
            System.err.println("[SoftDelete] Error code: " + e.getErrorCode());
            System.err.println("[SoftDelete] SQL state: " + e.getSQLState());
            e.printStackTrace();
            if (con != null) {
                try {
                    // Re-enable foreign key checks before rollback
                    try (PreparedStatement ps = con.prepareStatement("SET FOREIGN_KEY_CHECKS = 1")) {
                        ps.execute();
                        System.out.println("[SoftDelete] Foreign key checks re-enabled after error");
                    } catch (SQLException fkEx) {
                        System.err.println("[SoftDelete] WARNING: Failed to re-enable foreign key checks: " + fkEx.getMessage());
                    }
                    con.rollback();
                    System.out.println("[SoftDelete] ✓ Transaction rolled back successfully - no data modified");
                } catch (SQLException rollbackEx) {
                    System.err.println("[SoftDelete] ERROR: Failed to rollback transaction");
                    System.err.println("[SoftDelete] Rollback error: " + rollbackEx.getMessage());
                    rollbackEx.printStackTrace();
                }
            }
            System.err.println("[SoftDelete] ====================================");
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    System.err.println("[SoftDelete] ERROR: Failed to close database connection");
                    System.err.println("[SoftDelete] Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private int updateForeignKeyToPlaceholder(Connection con, String tableName, String columnName, long userId) throws SQLException {
        String sql = "UPDATE " + tableName + " SET " + columnName + " = -1 WHERE " + columnName + " = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate();
        }
    }

    /**
     * Deletes the profile photo entry from the profile_photos table for the given user.
     * @param con Database connection
     * @param userId The user's ID
     * @param userType Either "passenger" or "driver"
     * @return Number of rows deleted (0 or 1)
     * @throws SQLException if database error occurs
     */
    private int deleteProfilePhoto(Connection con, long userId, String userType) throws SQLException {
        String sql = "DELETE FROM profile_photos WHERE user_id = ? AND user_type = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, userType);
            return ps.executeUpdate();
        }
    }


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
    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.setStyle("-fx-text-fill: #F85149; -fx-font-size: 13px; -fx-font-weight: 600;");
    }

    private void navigateToProfileSettings() {
        try {
            if (isDriver) {
                // Navigate to DriverSettings
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverSettings.fxml"));
                Scene scene = new Scene(loader.load(), 390, 750);

                DriverSettingsController controller = loader.getController();
                if (currentUser != null) {
                    // DO NOT pass the old driver object as it may have stale balance
                    try {
                        DriverDAO driverDAO = new DriverDAO();
                        Long driverId = driverDAO.getDriverIdByEmail(currentUser.getEmail());
                        if (driverId != null) {
                            Driver freshDriver = driverDAO.getDriverById(driverId);
                            if (freshDriver != null) {
                                controller.setUser(freshDriver);
                                System.out.println("[SoftDelete] Opened DriverSettings with fresh driver data from database");
                            } else {
                                // Fallback: use old object if fetch fails
                                controller.setUser(currentUser);
                                controller.refreshBalance();
                                System.out.println("[SoftDelete] Using old driver object, refreshing balance");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[SoftDelete] Error fetching fresh driver data: " + e.getMessage());
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
            System.err.println("[SoftDelete] Failed to navigate back to Settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToRoleSelection() {
        try {
            UserSession.getInstance().clearSession();
            System.out.println("[SoftDelete] Session cleared, navigating to Role Selection");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RoleSelection.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            Stage stage = (Stage) deleteButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("[SoftDelete] Failed to navigate to RoleSelection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

