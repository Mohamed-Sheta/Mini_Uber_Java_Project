package controller;

import Model.*;
import DAO.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class AddFundsController {

    @FXML private Button backButton;
    @FXML private Button addMoneyButton;
    @FXML private Button logoutButton;
    @FXML private TextField amountField;
    @FXML private Label currentBalanceLabel;
    @FXML private Label messageLabel;

    private Person currentUser;
    private PassengerDAO passengerDAO = new PassengerDAO();
    private DriverDAO driverDAO = new DriverDAO();

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
        String amountText = amountField.getText().trim();

        if (amountText.isEmpty()) {
            showError("Please enter an amount");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);

            if (amount <= 0) {
                showError("Amount must be greater than 0");
                return;
            }

            if (amount > 10000) {
                showError("Maximum amount is 10,000 EGP");
                return;
            }

            // Add money to user's wallet
            double newBalance = currentUser.getWalletBalance() + amount;
            currentUser.updateWalletBalance(newBalance);

            // Update database using existing DAO methods
            if (currentUser instanceof Passenger) {
                updatePassengerBalance((Passenger) currentUser, newBalance);
            } else if (currentUser instanceof Driver) {
                updateDriverBalance((Driver) currentUser, newBalance);
            }

            // Update display
            updateBalanceDisplay();
            showSuccess(String.format("Successfully added %.2f EGP to your wallet!", amount));
            amountField.clear();

        } catch (NumberFormatException e) {
            showError("Invalid amount format");
        } catch (SQLException e) {
            showError("Failed to update balance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updatePassengerBalance(Passenger passenger, double newBalance) throws SQLException {
        // Find passenger in database by email
        var passengers = passengerDAO.showAll();
        for (PassengerDAO.PassengerRow row : passengers) {
            if (row.email.equals(passenger.getEmail())) {
                // Update using existing DAO update method (expects Passenger object)
                passengerDAO.update(
                    row.id,
                    passenger, // Pass the Passenger object directly
                    passenger.getCurrentLocation() != null ? passenger.getCurrentLocation().getName() : null
                );
                System.out.println("Passenger wallet updated: " + newBalance + " EGP");
                break;
            }
        }
    }

    private void updateDriverBalance(Driver driver, double newBalance) throws SQLException {
        // Find driver in database by email
        var drivers = driverDAO.showAll();
        for (DriverDAO.DriverRow row : drivers) {
            if (row.email.equals(driver.getEmail())) {
                // Update using existing DAO update method (expects Driver object)
                driverDAO.update(
                    row.id,
                    driver, // Pass the Driver object directly
                    driver.getCurrentLocation() != null ? driver.getCurrentLocation().getName() : null
                );
                System.out.println("Driver wallet updated: " + newBalance + " EGP");
                break;
            }
        }
    }

    @FXML
    public void onBackToProfile() {
        try {
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

