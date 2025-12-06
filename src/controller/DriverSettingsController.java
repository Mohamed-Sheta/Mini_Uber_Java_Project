package controller;

import Model.Driver;
import Model.Person;
import DAO.DriverDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import utils.DBConnection;
import utils.UserSession;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class DriverSettingsController {

    @FXML private Button backButton;
    // Note: balanceLabel removed - Account Balance now shown only in Profile screen
    @FXML private Button changePasswordButton;
    @FXML private Button reportAppButton;
    @FXML private Button deleteAccountButton;
    @FXML private Button logoutButton;

    private Person currentUser;
    private boolean isDriver = true;
    private long currentDriverId = -1;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        System.out.println("[DriverSettings] Controller initialized");
    }

    /**
     * Set the current user
     * Note: Balance display removed from Settings - now only in Profile
     */
    public void setUser(Person user) {
        this.currentUser = user;
        this.isDriver = (user instanceof Driver);

        // Get driver ID for future use
        if (isDriver && user != null) {
            try {
                DriverDAO driverDAO = new DriverDAO();
                Long driverId = driverDAO.getDriverIdByEmail(user.getEmail());
                if (driverId != null) {
                    this.currentDriverId = driverId;
                    System.out.println("[DriverSettings] Driver ID: " + driverId);
                }
            } catch (Exception e) {
                System.err.println("[DriverSettings] Error loading driver ID: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Legacy method - no longer needed since balance is not displayed in Settings
     * Kept for backward compatibility
     */
    public void refreshBalance() {
        System.out.println("[DriverSettings] refreshBalance() called but not needed - balance is in Profile screen now");
    }

    /**
     * Navigate back to Driver Dashboard
     */
    @FXML
    public void onBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverDashboard.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            DriverDashboardController controller = loader.getController();
            if (currentUser != null && isDriver) {
                controller.setDriver((Driver) currentUser);
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to Driver Dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Navigate to Change Password screen
     */
    @FXML
    public void onChangePassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ChangePassword.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            ChangePasswordController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            Stage stage = (Stage) changePasswordButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to Change Password: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigate to Delete Account Confirmation screen
     */
    @FXML
    public void onDeleteAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DeleteAccountConfirm.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            DeleteAccountConfirmController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            Stage stage = (Stage) deleteAccountButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to Delete Account Confirmation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Logout and return to role selection
     */
    @FXML
    public void onLogout() {
        navigateToRoleSelection();
    }

    /**
     * Navigate to Report App screen
     */
    @FXML
    public void onReportApp() {
        try {
            System.out.println("[DriverSettings] Opening Report App screen");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ReportApp.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            // Pass the current user to the ReportAppController
            ReportAppController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
                System.out.println("[DriverSettings] User passed to ReportApp: " + currentUser.getName());
            }

            Stage newStage = new Stage();
            newStage.setTitle("Report App Issue");
            newStage.setScene(scene);
            newStage.show();

            System.out.println("[DriverSettings] Report App screen opened successfully");
        } catch (IOException e) {
            System.err.println("Failed to open Report App screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigate to role selection screen
     */
    private void navigateToRoleSelection() {
        try {
            // Clear the user session (logout)
            UserSession.getInstance().clearSession();
            System.out.println("[DriverSettings] Session cleared, navigating to Role Selection");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RoleSelection.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to Role Selection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
