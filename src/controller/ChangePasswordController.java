package controller;

import Model.Person;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import javafx.util.Duration;
import utils.DBConnection;
import utils.UserSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class ChangePasswordController {

    @FXML private Button backButton;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button changePasswordButton;
    @FXML private Label messageLabel;

    private Person currentUser;
    private long userId = -1;
    private boolean isDriver = false;

    /**
     * Set the current user
     */
    public void setUser(Person user) {
        this.currentUser = user;
        this.isDriver = (user instanceof Model.Driver);

        // Get user ID from database
        this.userId = getUserIdFromDatabase(user.getEmail(), isDriver);
    }

    /**
     * Get user ID from database by email
     */
    private long getUserIdFromDatabase(String email, boolean isDriver) {
        String tableName = isDriver ? "drivers" : "passengers";
        String sql = "SELECT id FROM " + tableName + " WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error getting user ID: " + e.getMessage());
        }

        return -1;
    }

    /**
     * Handle change password action
     */
    @FXML
    public void onChangePassword() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate inputs
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("All fields are required", true);
            return;
        }

        if (newPassword.length() < 6) {
            showMessage("Password must be at least 6 characters", true);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showMessage("New passwords do not match", true);
            return;
        }

        // Update password
        if (updatePassword(currentPassword, newPassword)) {
            showMessage("✓ Password changed successfully!", false);

            // Navigate back to Profile after 1.5 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> navigateToProfile());
            pause.play();
        } else {
            showMessage("❌ Current password is incorrect", true);
        }
    }

    /**
     * Update password in database
     */
    private boolean updatePassword(String currentPassword, String newPassword) {
        try {
            // Verify current password
            String hashedCurrent = hashPassword(currentPassword);
            if (!currentUser.getPassword().equals(hashedCurrent)) {
                System.out.println("[ChangePassword] Current password verification failed");
                return false;
            }

            String hashedNew = hashPassword(newPassword);
            String tableName = isDriver ? "drivers" : "passengers";
            String sql = "UPDATE " + tableName + " SET password = ? WHERE id = ?";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, hashedNew);
                ps.setLong(2, userId);
                int rowsUpdated = ps.executeUpdate();

                // Update the user object's password and UserSession
                if (rowsUpdated > 0) {
                    currentUser.setPassword(hashedNew);

                    // Update UserSession to reflect the new password
                    UserSession.getInstance().updateCurrentUser(currentUser);

                    System.out.println("[ChangePassword] ✅ Password updated successfully in database and session");
                    return true;
                }
                System.err.println("[ChangePassword] ❌ No rows updated");
                return false;
            }
        } catch (Exception e) {
            System.err.println("Error updating password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Hash password using SHA-256
     * NOTE: Must match the hashing method in LoginController exactly
     */
    private static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Show message to user
     */
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        if (isError) {
            messageLabel.setStyle("-fx-text-fill: #F85149; -fx-font-size: 13px; -fx-font-weight: 600;");
        } else {
            messageLabel.setStyle("-fx-text-fill: #3FB950; -fx-font-size: 13px; -fx-font-weight: 600;");
        }
    }

    /**
     * Navigate back to Profile Settings screen
     */
    @FXML
    public void onBack() {
        navigateToProfileSettings();
    }

    /**
     * Navigate to Profile Settings screen (Driver or Passenger)
     */
    private void navigateToProfileSettings() {
        try {
            if (isDriver) {
                // Navigate to DriverSettings
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverSettings.fxml"));
                Scene scene = new Scene(loader.load(), 390, 750);

                DriverSettingsController controller = loader.getController();
                if (currentUser != null) {
                    controller.setUser(currentUser);
                    controller.refreshBalance(); // Ensure latest balance is shown
                }

                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            } else {
                // Navigate to ProfileSettings (Passenger)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProfileSettings.fxml"));
                Scene scene = new Scene(loader.load(), 390, 750);

                ProfileSettingsController controller = loader.getController();
                if (currentUser != null) {
                    controller.setUser(currentUser);
                }

                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            }
        } catch (IOException e) {
            System.err.println("Failed to navigate to Settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigate to Profile screen (used after successful password change)
     */
    private void navigateToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Profile.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            ProfileController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
                controller.refreshProfile();
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to Profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

