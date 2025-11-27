package controller;

import Model.Driver;
import Model.Passenger;
import Model.Person;
import DAO.PassengerDAO;
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

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class SettingsController {

    @FXML private Button backButton;
    @FXML private Button addFundsButton;
    @FXML private Button changePasswordButton;
    @FXML private Button reportRideButton;
    @FXML private Button deleteAccountButton;
    @FXML private Button logoutButton;

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
     * Navigate back to MapView
     */
    @FXML
    public void onBackToMap() {
        try {
            java.net.URL fxmlUrl = getClass().getResource("/MapView.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getClassLoader().getResource("MapView.fxml");
            }
            if (fxmlUrl == null) {
                System.err.println("ERROR: Could not find MapView.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(loader.load(), 390, 750);

            MapController controller = loader.getController();
            if (currentUser != null) {
                if (isDriver) {
                    controller.setDriver((Driver) currentUser);
                } else {
                    controller.setPassenger((Passenger) currentUser);
                }
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to Map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Navigate to Add Funds screen
     */
    @FXML
    public void onAddFunds() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AddFunds.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            AddFundsController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            Stage stage = (Stage) addFundsButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to Add Funds: " + e.getMessage());
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
     * Navigate to Report Problem screen
     */
    @FXML
    public void onReportRide() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ReportProblem.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            controller.ReportProblemController reportController = loader.getController();
            if (currentUser != null) {
                reportController.setUser(currentUser);
            }

            Stage stage = (Stage) reportRideButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to Report Problem: " + e.getMessage());
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

    /**
     * Update password in database
     */
    private boolean updatePasswordInDatabase(String hashedPassword) throws SQLException {
        String tableName = isDriver ? "drivers" : "passengers";
        String sql = "UPDATE " + tableName + " SET password = ? WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, hashedPassword);
            ps.setString(2, currentUser.getEmail());

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Delete user from database using existing DAO
     */
    private boolean deleteUserFromDatabase() throws SQLException {
        if (isDriver) {
            DriverDAO driverDAO = new DriverDAO();
            // Get driver ID
            long driverId = getDriverIdFromDatabase();
            if (driverId == -1) return false;
            return driverDAO.delete(driverId) > 0;
        } else {
            PassengerDAO passengerDAO = new PassengerDAO();
            // Get passenger ID
            long passengerId = getPassengerIdFromDatabase();
            if (passengerId == -1) return false;
            return passengerDAO.delete(passengerId) > 0;
        }
    }

    /**
     * Get driver ID from database
     */
    private long getDriverIdFromDatabase() {
        String sql = "SELECT id FROM drivers WHERE email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, currentUser.getEmail());
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Get passenger ID from database
     */
    private long getPassengerIdFromDatabase() {
        String sql = "SELECT id FROM passengers WHERE email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, currentUser.getEmail());
            var rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Hash password using SHA-256
     */
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialogPane.getStyleClass().add("dark-dialog");

        alert.showAndWait();
    }
}

