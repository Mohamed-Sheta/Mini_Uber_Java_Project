package controller;

import DAO.DriverDAO;
import DAO.PassengerDAO;
import Model.Driver;
import Model.Passenger;
import Model.Person;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import utils.DBConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProfileController {

    @FXML
    private Label ratingLabel;

    @FXML
    private Label totalRidesLabel;

    @FXML
    private Label totalSpentLabel;

    @FXML
    private Label spentLabelText;

    @FXML
    private javafx.scene.image.ImageView avatarImageView;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField addressField;

    @FXML
    private VBox driverFieldsContainer;

    @FXML
    private TextField carModelField;

    @FXML
    private TextField licensePlateField;

    @FXML
    private Button saveButton;

    @FXML
    private Button backButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Label messageLabel;

    private Person currentUser;
    private boolean isDriver = false;
    private long userId = -1;

    public void initialize() {
        // Initial setup if needed
    }

    /**
     * Set the current logged-in user (Passenger or Driver)
     * Call this method from LoginController after successful login
     */
    public void setUser(Person user) {
        this.currentUser = user;
        this.isDriver = (user instanceof Driver);

        // Get user ID from database
        this.userId = getUserIdFromDatabase(user.getEmail(), isDriver);

        // Load profile data
        loadProfileData();
    }

    /**
     * Load profile data from the current user object and database
     */
    private void loadProfileData() {
        if (currentUser == null) {
            return;
        }

        // Avatar is now loaded from avatar.png, no need to set initials

        // Set user name and role
        userNameLabel.setText(currentUser.getName());
        userRoleLabel.setText(isDriver ? "Driver" : "Passenger");

        // Set rating
        double rating = currentUser.getAccountRating();
        ratingLabel.setText(String.format("%.1f", rating));

        // Set total rides
        int totalRides = getTotalRides();
        totalRidesLabel.setText(String.valueOf(totalRides));

        // Set total spent/earned
        double totalAmount = getTotalSpentOrEarned();
        if (isDriver) {
            spentLabelText.setText("Earned");
            totalSpentLabel.setText(String.format("$%.0f", totalAmount));
        } else {
            spentLabelText.setText("Spent");
            totalSpentLabel.setText(String.format("$%.0f", totalAmount));
        }

        // Fill form fields
        nameField.setText(currentUser.getName());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhoneNumber());
        addressField.setText(currentUser.getUserSSN());

        // Show driver-specific fields if user is a driver
        if (isDriver) {
            Driver driver = (Driver) currentUser;
            driverFieldsContainer.setVisible(true);
            driverFieldsContainer.setManaged(true);
            carModelField.setText(driver.getCarModel());
            licensePlateField.setText(driver.getLicensePlate());
        }
    }

    /**
     * Get user ID from database by email
     */
    private long getUserIdFromDatabase(String email, boolean isDriver) {
        String tableName = isDriver ? "drivers" : "passengers";
        String sql = "SELECT id FROM " + tableName + " WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

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
     * Get total number of rides from ride_history
     */
    private int getTotalRides() {
        if (userId == -1) {
            return 0;
        }

        String columnName = isDriver ? "driver_id" : "passenger_id";
        String sql = "SELECT COUNT(*) as total FROM ride_history WHERE " + columnName + " = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting total rides: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Get total amount spent (for passengers) or earned (for drivers)
     */
    private double getTotalSpentOrEarned() {
        if (userId == -1) {
            return 0.0;
        }

        String columnName = isDriver ? "driver_id" : "passenger_id";
        String sql = "SELECT SUM(ride_cost) as total FROM ride_history WHERE " + columnName + " = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting total amount: " + e.getMessage());
            e.printStackTrace();
        }

        return 0.0;
    }

    /**
     * Handle Save Changes button click
     */
    @FXML
    public void onSaveChanges() {
        // Add button animation
        playButtonAnimation(saveButton);

        // Hide previous messages
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        // Validate inputs
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty()) {
            showMessage("Name cannot be empty", true);
            return;
        }

        if (phone.isEmpty()) {
            showMessage("Phone number cannot be empty", true);
            return;
        }

        if (!phone.matches("\\d{11}")) {
            showMessage("Phone number must be exactly 11 digits", true);
            return;
        }

        if (address.isEmpty()) {
            showMessage("Home address cannot be empty", true);
            return;
        }

        // Driver-specific validation
        if (isDriver) {
            String carModel = carModelField.getText().trim();
            String licensePlate = licensePlateField.getText().trim();

            if (carModel.isEmpty()) {
                showMessage("Car model cannot be empty", true);
                return;
            }

            if (licensePlate.isEmpty()) {
                showMessage("License plate cannot be empty", true);
                return;
            }
        }

        // Update database
        try {
            if (isDriver) {
                updateDriverInDatabase(name, phone, address);
            } else {
                updatePassengerInDatabase(name, phone, address);
            }

            // Update current user object
            updateUserObject(name, phone, address);

            // Update UI
            userNameLabel.setText(name);

            showMessage("Profile updated successfully!", false);

        } catch (Exception e) {
            System.err.println("Error updating profile: " + e.getMessage());
            e.printStackTrace();
            showMessage("Failed to update profile. Please try again.", true);
        }
    }

    /**
     * Update passenger in database
     */
    private void updatePassengerInDatabase(String name, String phone, String address) throws SQLException {
        Passenger passenger = (Passenger) currentUser;

        // Create updated passenger object
        Passenger updatedPassenger = new Passenger(
                address, // userSSN (using address field as SSN)
                name,
                phone,
                passenger.getEmail(),
                passenger.getPassword() // Keep existing hashed password
        );

        // Preserve existing balances
        updatedPassenger.updateWalletBalance(passenger.getWalletBalance());
        updatedPassenger.updateCreditBalance(passenger.getCreditBalance());

        // Get location name as string
        String locationName = (passenger.getCurrentLocation() != null) ?
                              passenger.getCurrentLocation().getName() : null;

        // Update in database
        PassengerDAO passengerDAO = new PassengerDAO();
        passengerDAO.update(userId, updatedPassenger, locationName);
    }

    /**
     * Update driver in database
     */
    private void updateDriverInDatabase(String name, String phone, String address) throws SQLException {
        Driver driver = (Driver) currentUser;

        String carModel = carModelField.getText().trim();
        String licensePlate = licensePlateField.getText().trim();

        // Create updated driver object with all existing data preserved
        Driver updatedDriver = new Driver(
                licensePlate,
                carModel,
                driver.isActive(),
                address, // userSSN (using address field as SSN)
                name,
                phone,
                driver.getEmail(),
                driver.getWalletBalance(),
                driver.getCreditBalance(),
                driver.getCurrentLocation(),
                driver.getRideHistory(),
                driver.getPassword() // Keep existing hashed password
        );

        // Get location name as string
        String locationName = (driver.getCurrentLocation() != null) ?
                              driver.getCurrentLocation().getName() : null;

        // Update in database using DriverDAO
        DriverDAO driverDAO = new DriverDAO();
        driverDAO.update(userId, updatedDriver, locationName);
    }

    /**
     * Update the current user object with new values
     */
    private void updateUserObject(String name, String phone, String address) {
        // Create new user object with updated data
        if (isDriver) {
            Driver oldDriver = (Driver) currentUser;
            String carModel = carModelField.getText().trim();
            String licensePlate = licensePlateField.getText().trim();

            // Create new Driver with updated information
            currentUser = new Driver(
                licensePlate,
                carModel,
                oldDriver.isActive(),
                address,
                name,
                phone,
                oldDriver.getEmail(),
                oldDriver.getWalletBalance(),
                oldDriver.getCreditBalance(),
                oldDriver.getCurrentLocation(),
                oldDriver.getRideHistory(),
                oldDriver.getPassword()
            );
        } else {
            Passenger oldPassenger = (Passenger) currentUser;

            // Create new Passenger with updated information
            currentUser = new Passenger(
                address,
                name,
                phone,
                oldPassenger.getEmail(),
                oldPassenger.getPassword()
            );

            // Preserve balances
            ((Passenger) currentUser).updateWalletBalance(oldPassenger.getWalletBalance());
            ((Passenger) currentUser).updateCreditBalance(oldPassenger.getCreditBalance());
        }

        // Refresh all UI fields with new data
        nameField.setText(name);
        phoneField.setText(phone);
        addressField.setText(address);
        userNameLabel.setText(name);
        emailField.setText(((Person) currentUser).getEmail());
    }

    /**
     * Show message to user
     */
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        if (isError) {
            messageLabel.setStyle("-fx-text-fill: #F44336;");
        } else {
            messageLabel.setStyle("-fx-text-fill: #4CAF50;");
        }
    }

    /**
     * Play button click animation
     */
    private void playButtonAnimation(Button button) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), button);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(0.95);
        scaleTransition.setToY(0.95);
        scaleTransition.setCycleCount(2);
        scaleTransition.setAutoReverse(true);
        scaleTransition.play();
    }

    /**
     * Handle Back to Map button click
     * Navigate back to MapView with current user data
     */
    @FXML
    public void onBackToMap() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MapView.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            MapController controller = loader.getController();
            // Pass user data back to map
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
     * Handle Settings button click
     * Navigate to AddFunds screen for wallet top-up
     */
    @FXML
    public void onSettingsClick() {
        System.out.println("=== onSettingsClick() called - Navigating to AddFunds ===");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AddFunds.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            AddFundsController controller = loader.getController();
            // Pass user data to AddFunds screen
            if (currentUser != null) {
                controller.setUser(currentUser);
                System.out.println("User data passed to AddFunds controller");
            }

            Stage stage = (Stage) settingsButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

            System.out.println("AddFunds screen loaded successfully");
        } catch (IOException e) {
            System.err.println("Failed to navigate to AddFunds: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

