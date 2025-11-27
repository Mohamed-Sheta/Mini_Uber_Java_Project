package controller;

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
            // Delete user using existing DAO method
            boolean success = deleteUserFromDatabase();

            if (success) {
                // Navigate to RoleSelection after successful deletion
                navigateToRoleSelection();
            } else {
                showError("Failed to delete account. Please try again.");
            }
        } catch (Exception e) {
            showError("Error deleting account: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Delete user from database
     */
    private boolean deleteUserFromDatabase() throws SQLException {
        String tableName = isDriver ? "drivers" : "passengers";
        String sql = "DELETE FROM " + tableName + " WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, currentUser.getEmail());
            int rowsDeleted = ps.executeUpdate();
            return rowsDeleted > 0;
        }
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

