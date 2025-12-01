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
    @FXML private Label balanceLabel;
    @FXML private Button changePasswordButton;
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
     * Set the current user and update balance display
     */
    public void setUser(Person user) {
        this.currentUser = user;
        this.isDriver = (user instanceof Driver);

        // Reload fresh data from database to get latest wallet balance
        if (isDriver && user != null) {
            try {
                DriverDAO driverDAO = new DriverDAO();

                // Get driver ID
                Long driverId = driverDAO.getDriverIdByEmail(user.getEmail());
                if (driverId != null) {
                    this.currentDriverId = driverId;
                }

                // Reload fresh driver data
                Driver freshDriver = driverDAO.getByEmail(user.getEmail());
                if (freshDriver != null) {
                    this.currentUser = freshDriver;
                    System.out.println("[DriverSettings] Reloaded driver data - wallet: " +
                                      String.format("%.2f", freshDriver.getWalletBalance()));
                }
            } catch (Exception e) {
                System.err.println("[DriverSettings] Error reloading driver data: " + e.getMessage());
            }
        }

        // Update balance display
        updateBalanceDisplay();
    }

    /**
     * Refresh balance from database and update display
     * Call this method to get the latest balance from database
     */
    public void refreshBalance() {
        if (!isDriver || currentUser == null) {
            return;
        }

        try {
            DriverDAO driverDAO = new DriverDAO();

            // Option 1: Get balance directly by ID (faster)
            if (currentDriverId > 0) {
                double freshBalance = driverDAO.getDriverWalletBalance(currentDriverId);
                if (freshBalance >= 0) {
                    currentUser.updateWalletBalance(freshBalance);
                    System.out.println("[DriverSettings] Balance refreshed from DB: " + String.format("%.2f", freshBalance) + " EGP");
                }
            } else {
                // Option 2: Reload entire driver object
                Driver freshDriver = driverDAO.getByEmail(currentUser.getEmail());
                if (freshDriver != null) {
                    this.currentUser = freshDriver;
                    System.out.println("[DriverSettings] Driver data refreshed - wallet: " +
                                      String.format("%.2f", freshDriver.getWalletBalance()));
                }
            }

            // Update UI
            updateBalanceDisplay();

        } catch (Exception e) {
            System.err.println("[DriverSettings] Error refreshing balance: " + e.getMessage());
        }
    }

    /**
     * Update the balance label with current balance
     */
    private void updateBalanceDisplay() {
        if (balanceLabel != null && currentUser != null) {
            double balance = currentUser.getWalletBalance();
            balanceLabel.setText(String.format("%.2f EGP", balance));
            System.out.println("[DriverSettings] Balance label updated: " + String.format("%.2f EGP", balance));
        }
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

