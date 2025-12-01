package controller;

import DAO.DriverDAO;
import DAO.PassengerDAO;
import Model.Driver;
import Model.Passenger;
import Model.Person;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import utils.DBConnection;
import utils.UserSession;

import java.io.IOException;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

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

    @FXML
    private VBox ridesCard;


    private Person currentUser;
    private boolean isDriver = false;
    private long userId = -1;

    public void initialize() {
        // Only make rides card clickable
        if (ridesCard != null) {
            ridesCard.setOnMouseClicked(e -> openRidesDetails());
        }

        // Rating and Spent cards are display-only (no click handlers)

        // Check if user is already in session and load their data
        if (UserSession.getInstance().isLoggedIn()) {
            setUser(UserSession.getInstance().getCurrentUser());
        }
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
     * Refresh profile data from database
     * Call this after wallet updates, ride completion, or any profile changes
     */
    public void refreshProfile() {
        if (currentUser == null || userId == -1) {
            return;
        }

        // Reload user data from database to get latest wallet balance, etc.
        reloadUserFromDatabase();

        // Reload stats and display
        loadProfileData();

        System.out.println("[Profile] Profile data refreshed successfully");
    }

    /**
     * Reload user object from database to get latest data
     */
    private void reloadUserFromDatabase() {
        if (currentUser == null || userId == -1) {
            return;
        }

        try {
            if (isDriver) {
                DriverDAO driverDAO = new DriverDAO();
                List<DriverDAO.DriverRow> drivers = driverDAO.showAll();
                for (DriverDAO.DriverRow row : drivers) {
                    if (row.id == userId) {
                        // Update current user object with latest data
                        Driver driver = (Driver) currentUser;
                        driver.updateWalletBalance(row.wallet);
                        driver.updateCreditBalance(row.credit);
                        System.out.println("[Profile] Driver data reloaded: wallet=" + row.wallet);
                        break;
                    }
                }
            } else {
                PassengerDAO passengerDAO = new PassengerDAO();
                List<PassengerDAO.PassengerRow> passengers = passengerDAO.showAll();
                for (PassengerDAO.PassengerRow row : passengers) {
                    if (row.id == userId) {
                        // Update current user object with latest data
                        Passenger passenger = (Passenger) currentUser;
                        passenger.updateWalletBalance(row.wallet);
                        passenger.updateCreditBalance(row.credit);
                        System.out.println("[Profile] Passenger data reloaded: wallet=" + row.wallet);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Profile] Error reloading user data: " + e.getMessage());
            e.printStackTrace();
        }
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

        // FEATURE 1: Load and display user stats from database
        UserStatsData stats = loadUserStats(userId);
        ratingLabel.setText(String.format("%.1f", stats.rating));
        totalRidesLabel.setText(String.valueOf(stats.rides));

        if (isDriver) {
            spentLabelText.setText("Earned");
            // For drivers, show total earned (with tips and 8% commission deducted)
            double totalEarned = getTotalEarnedWithTips();
            totalSpentLabel.setText(String.format("$%.2f", totalEarned));
        } else {
            spentLabelText.setText("Spent");
            totalSpentLabel.setText(String.format("$%.2f", stats.spent));
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
     * Each ride creates ONE record in ride_history table
     */
    private int getTotalRides() {
        if (userId == -1) {
            System.out.println("[Profile] getTotalRides: Invalid userId, returning 0");
            return 0;
        }

        String columnName = isDriver ? "driver_id" : "passenger_id";
        String sql = "SELECT COUNT(*) as total FROM ride_history WHERE " + columnName + " = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("total");
                    System.out.println("[Profile] Fetched rides=" + count + " for " + (isDriver ? "driver" : "passenger") + " id=" + userId);
                    return count;
                }
            }
        } catch (SQLException e) {
            System.err.println("[Profile] Error getting total rides: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[Profile] No ride data found, returning 0");
        return 0;
    }

    /**
     * Get total amount spent (for passengers) or earned (for drivers)
     * For passengers: returns total ride_cost
     * For drivers: returns ride_cost (this will be used by getTotalEarnedWithTips for full calculation)
     */
    private double getTotalSpentOrEarned() {
        if (userId == -1) {
            System.out.println("[Profile] getTotalSpentOrEarned: Invalid userId, returning 0.0");
            return 0.0;
        }

        String columnName = isDriver ? "driver_id" : "passenger_id";
        String sql = "SELECT SUM(ride_cost) as total FROM ride_history WHERE " + columnName + " = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double total = rs.getDouble("total");
                    System.out.println("[Profile] Fetched spent/earned=" + String.format("%.2f", total) +
                                      " for " + (isDriver ? "driver" : "passenger") + " id=" + userId);
                    return total;
                }
            }
        } catch (SQLException e) {
            System.err.println("[Profile] Error getting total amount: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[Profile] No spending/earning data found, returning 0.0");
        return 0.0;
    }

    /**
     * Get total amount earned by driver including tips
     * Driver receives 92% of ride_cost (8% goes to company) + 100% of tips
     * Formula: SUM((ride_cost * 0.92) + tips)
     */
    private double getTotalEarnedWithTips() {
        if (userId == -1 || !isDriver) {
            System.out.println("[Profile] getTotalEarnedWithTips: Invalid userId or not driver, returning 0.0");
            return 0.0;
        }

        // Calculate (ride_cost * 0.92) + tips for each ride
        String sql = "SELECT SUM((ride_cost * 0.92) + COALESCE(tips, 0)) as total_earned " +
                     "FROM ride_history WHERE driver_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double earned = rs.getDouble("total_earned");
                    System.out.println("[Profile] Fetched earned=" + String.format("%.2f", earned) + " for driver id=" + userId);
                    return earned;
                }
            }
        } catch (SQLException e) {
            System.err.println("[Profile] Error getting total earned with tips: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[Profile] No earning data found, returning 0.0");
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
            if (isDriver) {
                // Navigate to DriverDashboard for drivers
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverDashboard.fxml"));
                Scene scene = new Scene(loader.load(), 390, 750);

                DriverDashboardController controller = loader.getController();
                if (currentUser != null) {
                    controller.setDriver((Driver) currentUser);
                }

                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            } else {
                // Navigate to MapView for passengers
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/MapView.fxml"));
                Scene scene = new Scene(loader.load(), 390, 750);

                MapController controller = loader.getController();
                if (currentUser != null) {
                    controller.setPassenger((Passenger) currentUser);
                }

                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            }
        } catch (IOException e) {
            System.err.println("Failed to navigate back: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle Settings button click
     * Navigate to ProfileSettings full-screen page
     */
    @FXML
    public void onSettingsClick() {
        System.out.println("=== onSettingsClick() called - Navigating to Settings ===");
        try {
            if (isDriver) {
                // Navigate to DriverSettings for drivers
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverSettings.fxml"));
                Scene scene = new Scene(loader.load(), 390, 750);

                DriverSettingsController controller = loader.getController();
                if (currentUser != null) {
                    controller.setUser(currentUser);
                    // Explicitly refresh balance from database to ensure latest value is shown
                    controller.refreshBalance();
                    System.out.println("User data passed to DriverSettings controller with fresh balance");
                }

                Stage stage = (Stage) settingsButton.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
                System.out.println("DriverSettings screen loaded successfully");
            } else {
                // Navigate to ProfileSettings for passengers
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProfileSettings.fxml"));
                Scene scene = new Scene(loader.load(), 390, 750);

                ProfileSettingsController controller = loader.getController();
                if (currentUser != null) {
                    controller.setUser(currentUser);
                    System.out.println("User data passed to ProfileSettings controller");
                }

                Stage stage = (Stage) settingsButton.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
                System.out.println("ProfileSettings screen loaded successfully");
            }
        } catch (IOException e) {
            System.err.println("Failed to navigate to Settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ====================================================================
    // FEATURE 1: USER STATS FUNCTIONS
    // ====================================================================

    /**
     * Inner class to hold user stats data (no external class creation)
     */
    private static class UserStatsData {
        int rides;
        double spent;
        double rating;

        UserStatsData(int rides, double spent, double rating) {
            this.rides = rides;
            this.spent = spent;
            this.rating = rating;
        }
    }

    /**
     * Load user stats from database - calculates dynamically from ride_history
     */
    private UserStatsData loadUserStats(long userId) {
        if (userId == -1) {
            return new UserStatsData(0, 0.0, 0.0);
        }

        // Calculate stats dynamically from ride_history table
        int rides = getTotalRides();
        double spent = getTotalSpentOrEarned();
        double rating = getAverageRating();

        return new UserStatsData(rides, spent, rating);
    }

    /**
     * Get average rating using the existing Person.getAverageRating() method
     * This method reloads ride history from database first to ensure fresh data
     */
    private double getAverageRating() {
        if (currentUser == null) {
            System.out.println("[Profile] getAverageRating: currentUser is null, returning 0.0");
            return 0.0;
        }

        // Reload ride history from database to ensure Person has fresh data
        reloadRideHistoryFromDatabase();

        // Use the existing Person.getAverageRating() method
        double rating = currentUser.getAverageRating();

        System.out.println("[Profile] Fetched rating=" + String.format("%.1f", rating) +
                          " for " + (isDriver ? "driver" : "passenger") +
                          ", rides in history=" + (currentUser.getRideHistory() != null ? currentUser.getRideHistory().size() : 0));

        return rating;
    }

    /**
     * Reload ride history from database and populate currentUser's rideHistory list
     * This ensures Person.getAverageRating() has the latest data
     */
    private void reloadRideHistoryFromDatabase() {
        if (currentUser == null || userId == -1) {
            return;
        }

        try {
            DAO.RideHistoryDAO rideHistoryDAO = new DAO.RideHistoryDAO();
            List<DAO.RideHistoryDAO.RideHistoryRow> rows = rideHistoryDAO.showAll();

            // Filter rows for this user and build RideHistory objects
            currentUser.getRideHistory().clear();

            for (DAO.RideHistoryDAO.RideHistoryRow row : rows) {
                boolean isThisUser = isDriver ? (row.driverId == userId) : (row.passengerId == userId);

                if (isThisUser) {
                    // Create minimal RideHistory object for rating calculation
                    // Person.getAverageRating() only needs passenger/driver rating values
                    Model.RideHistory rideHistory = new Model.RideHistory(
                        null, // driver - not needed for rating calculation
                        null, // passenger - not needed for rating calculation
                        row.passengerRating,
                        row.driverRating,
                        null  // request - not needed for rating calculation
                    );
                    currentUser.getRideHistory().add(rideHistory);
                }
            }

            System.out.println("[Profile] Reloaded " + currentUser.getRideHistory().size() + " ride history records from DB");
        } catch (Exception e) {
            System.err.println("[Profile] Error reloading ride history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update user stats after a completed ride
     * Recalculates dynamically from ride_history - no caching needed
     */
    public void updateUserStats(long userId, double rideCost) {
        if (userId == -1) {
            System.err.println("Invalid user ID for stats update");
            return;
        }

        try {
            // Simply refresh the UI - stats are calculated on-demand
            refreshStatsUI();

            System.out.println("User stats refreshed from ride_history");

        } catch (Exception e) {
            System.err.println("Error updating user stats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Refresh stats UI with latest data from ride_history
     */
    private void refreshStatsUI() {
        UserStatsData stats = loadUserStats(userId);

        // Update UI labels
        ratingLabel.setText(String.format("%.1f", stats.rating));
        totalRidesLabel.setText(String.valueOf(stats.rides));

        if (isDriver) {
            double totalEarned = getTotalEarnedWithTips();
            totalSpentLabel.setText(String.format("$%.2f", totalEarned));
        } else {
            totalSpentLabel.setText(String.format("$%.2f", stats.spent));
        }
    }


    // ====================================================================
    // FEATURE 2: SETTINGS MODAL (NOT MONEY-RELATED)
    // ====================================================================

    /**
     * Open settings modal with proper settings options
     */
    private void openSettingsModal() {
        Stage settingsStage = new Stage();

        // Set window icon FIRST - Settings uses gear icon
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/settings_12280787.png");
            if (iconStream != null) {
                javafx.scene.image.Image icon = new javafx.scene.image.Image(iconStream);
                if (!icon.isError()) {
                    settingsStage.getIcons().setAll(icon);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load icon for Settings: " + e.getMessage());
        }

        settingsStage.initModality(Modality.APPLICATION_MODAL);
        settingsStage.setTitle("⚙ Settings");
        settingsStage.setWidth(350);
        settingsStage.setHeight(500);

        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setStyle("-fx-background-color: #f5f5f5;");

        // Title
        Label titleLabel = new Label("Settings");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // Add Funds Button (Passengers only - not shown for drivers)
        Button addFundsBtn = null;
        if (!isDriver) {
            addFundsBtn = createSettingsButton("Add Funds", "#4CAF50");
            addFundsBtn.setOnAction(e -> {
                settingsStage.close();
                openAddFundsPage();
            });
        }

        // Change Password Section
        Button changePasswordBtn = createSettingsButton("Change Password", "#2196F3");
        changePasswordBtn.setOnAction(e -> openChangePasswordDialog(settingsStage));

        // Notifications Section
        HBox notifBox = new HBox(10);
        notifBox.setAlignment(Pos.CENTER_LEFT);
        Label notifLabel = new Label("Notifications");
        notifLabel.setStyle("-fx-font-size: 14px;");
        CheckBox notifToggle = new CheckBox("Enable");
        notifToggle.setSelected(true);
        notifBox.getChildren().addAll(notifLabel, notifToggle);

        // Report a Ride Button (Passengers only - not shown for drivers)
        Button reportRideBtn = null;
        if (!isDriver) {
            reportRideBtn = createSettingsButton("Report a Ride", "#FF5722");
            reportRideBtn.setOnAction(e -> {
                settingsStage.close();
                openReportPage();
            });
        }

        // Delete Account
        Button deleteAccountBtn = createSettingsButton("Delete Account", "#F44336");
        deleteAccountBtn.setOnAction(e -> handleDeleteAccount(settingsStage));

        // Logout
        Button logoutBtn = createSettingsButton("Logout", "#FF9800");
        logoutBtn.setOnAction(e -> handleLogout(settingsStage));

        // Close button
        Button closeBtn = createSettingsButton("Close", "#757575");
        closeBtn.setOnAction(e -> settingsStage.close());

        // Build layout conditionally based on user type
        mainLayout.getChildren().addAll(titleLabel, new Label(""));

        if (addFundsBtn != null) {
            mainLayout.getChildren().add(addFundsBtn);
        }

        mainLayout.getChildren().addAll(changePasswordBtn, notifBox, new Label(""));

        if (reportRideBtn != null) {
            mainLayout.getChildren().add(reportRideBtn);
        }

        mainLayout.getChildren().addAll(
            deleteAccountBtn,
            logoutBtn,
            new Label(""),
            closeBtn
        );

        Scene scene = new Scene(mainLayout);
        settingsStage.setScene(scene);
        settingsStage.showAndWait();
    }

    /**
     * Create styled button for settings modal
     */
    private Button createSettingsButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(280);
        btn.setPrefHeight(40);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                    "-fx-font-size: 14px; -fx-background-radius: 5px; -fx-cursor: hand;");
        return btn;
    }

    /**
     * Open Report Problem page
     */
    private void openReportPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ReportProblem.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            ReportProblemController reportController = loader.getController();
            if (currentUser != null) {
                reportController.setUser(currentUser);
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to Report Problem: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Open change password dialog
     */
    private void openChangePasswordDialog(Stage parentStage) {
        Stage pwdStage = new Stage();

        // Set window icon FIRST - Change Password uses settings icon
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/settings_12280787.png");
            if (iconStream != null) {
                javafx.scene.image.Image icon = new javafx.scene.image.Image(iconStream);
                if (!icon.isError()) {
                    pwdStage.getIcons().setAll(icon);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load icon for Change Password: " + e.getMessage());
        }

        pwdStage.initModality(Modality.APPLICATION_MODAL);
        pwdStage.setTitle("Change Password");
        pwdStage.setWidth(350);
        pwdStage.setHeight(300);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Change Password");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        PasswordField currentPwdField = new PasswordField();
        currentPwdField.setPromptText("Current Password");
        currentPwdField.setPrefWidth(280);

        PasswordField newPwdField = new PasswordField();
        newPwdField.setPromptText("New Password");
        newPwdField.setPrefWidth(280);

        PasswordField confirmPwdField = new PasswordField();
        confirmPwdField.setPromptText("Confirm New Password");
        confirmPwdField.setPrefWidth(280);

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);

        Button submitBtn = new Button("Change Password");
        submitBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        submitBtn.setOnAction(e -> {
            String current = currentPwdField.getText();
            String newPwd = newPwdField.getText();
            String confirm = confirmPwdField.getText();

            if (current.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
                messageLabel.setText("All fields are required");
                messageLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            if (newPwd.length() < 6) {
                messageLabel.setText("Password must be at least 6 characters");
                messageLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            if (!newPwd.equals(confirm)) {
                messageLabel.setText("Passwords do not match");
                messageLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            // Update password in database
            if (updatePassword(current, newPwd)) {
                messageLabel.setText("Password changed successfully!");
                messageLabel.setStyle("-fx-text-fill: green;");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.seconds(1.5));
                pause.setOnFinished(ev -> pwdStage.close());
                pause.play();
            } else {
                messageLabel.setText("Current password is incorrect");
                messageLabel.setStyle("-fx-text-fill: red;");
            }
        });

        layout.getChildren().addAll(titleLabel, currentPwdField, newPwdField, confirmPwdField, messageLabel, submitBtn);

        Scene scene = new Scene(layout);
        pwdStage.setScene(scene);
        pwdStage.showAndWait();
    }

    /**
     * Update password in database
     */
    private boolean updatePassword(String currentPassword, String newPassword) {
        try {
            // Verify current password
            String hashedCurrent = hashPassword(currentPassword);
            if (!currentUser.getPassword().equals(hashedCurrent)) {
                return false;
            }

            String hashedNew = hashPassword(newPassword);
            String tableName = isDriver ? "drivers" : "passengers";
            String sql = "UPDATE " + tableName + " SET password = ? WHERE id = ?";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, hashedNew);
                ps.setLong(2, userId);
                ps.executeUpdate();
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error updating password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Hash password using SHA-256
     */
    /**
     * Hash password using SHA-256
     * NOTE: Must match the hashing method in LoginController exactly
     */
    private String hashPassword(String password) {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handle delete account
     */
    private void handleDeleteAccount(Stage parentStage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Account");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("This action cannot be undone. All your data will be permanently deleted.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String tableName = isDriver ? "drivers" : "passengers";
                    String sql = "DELETE FROM " + tableName + " WHERE id = ?";

                    try (Connection con = DBConnection.getConnection();
                         PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setLong(1, userId);
                        ps.executeUpdate();
                    }

                    // Note: ride_history and other related records will be handled by DB CASCADE rules

                    parentStage.close();
                    navigateToLogin();
                } catch (Exception e) {
                    System.err.println("Error deleting account: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Handle logout
     */
    private void handleLogout(Stage parentStage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                parentStage.close();
                navigateToLogin();
            }
        });
    }

    /**
     * Navigate to login screen
     */
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) settingsButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("MiniGo - Login");
        } catch (Exception e) {
            System.err.println("Error navigating to login: " + e.getMessage());
        }
    }

    // ====================================================================
    // FEATURE 1: ADD FUNDS PAGE (INLINE - NO FXML)
    // ====================================================================

    /**
     * Open Add Funds page as an inline modal dialog
     * Matches the app's dark theme style
     */
    private void openAddFundsPage() {
        Stage fundsStage = new Stage();

        // Set window icon FIRST - AddFunds uses money/wallet icon
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/safe.png");
            if (iconStream != null) {
                javafx.scene.image.Image icon = new javafx.scene.image.Image(iconStream);
                if (!icon.isError()) {
                    fundsStage.getIcons().setAll(icon);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load icon for Add Funds: " + e.getMessage());
        }

        fundsStage.initModality(Modality.APPLICATION_MODAL);
        fundsStage.setTitle("💰 Add Funds");
        fundsStage.setWidth(400);
        fundsStage.setHeight(600);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle("-fx-background-color: linear-gradient(to bottom, #0A0C10, #0D1117);");

        // Header with settings icon
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 15, 0));

        Label settingsIcon = new Label("⚙");
        settingsIcon.setStyle("-fx-font-size: 24px; -fx-text-fill: #8B949E; -fx-cursor: hand;");
        settingsIcon.setOnMouseClicked(e -> {
            fundsStage.close();
            openSettingsModal();
        });

        Label headerTitle = new Label("Add Funds");
        headerTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #E6EDF3;");

        header.getChildren().addAll(settingsIcon, headerTitle);

        // Current Balance Display
        VBox balanceBox = new VBox(8);
        balanceBox.setAlignment(Pos.CENTER);
        balanceBox.setStyle("-fx-background-color: #1C2333; -fx-background-radius: 12; -fx-padding: 20;");

        Label balanceLabel = new Label("Current Balance");
        balanceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #8B949E;");

        Label balanceAmount = new Label(String.format("%.2f EGP", currentUser.getWalletBalance()));
        balanceAmount.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #10B981;");

        balanceBox.getChildren().addAll(balanceLabel, balanceAmount);

        // Amount Input
        VBox inputBox = new VBox(8);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        Label inputLabel = new Label("Enter Amount (EGP)");
        inputLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #E6EDF3;");

        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount...");
        amountField.setPrefHeight(45);
        amountField.setStyle("-fx-background-color: #161B22; -fx-text-fill: #E6EDF3; " +
                            "-fx-prompt-text-fill: #6E7681; -fx-font-size: 16px; " +
                            "-fx-background-radius: 8; -fx-border-color: #30363D; " +
                            "-fx-border-radius: 8; -fx-padding: 10;");

        // Restrict to numbers and decimal point
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d{0,2})?")) {
                amountField.setText(oldVal);
            }
        });

        inputBox.getChildren().addAll(inputLabel, amountField);

        // Quick Add Buttons
        VBox quickAddBox = new VBox(10);
        quickAddBox.setAlignment(Pos.CENTER);

        Label quickLabel = new Label("Quick Add");
        quickLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8B949E;");

        HBox buttonsRow = new HBox(10);
        buttonsRow.setAlignment(Pos.CENTER);

        Button btn50 = createQuickAddButton("50 EGP", amountField);
        Button btn100 = createQuickAddButton("100 EGP", amountField);
        Button btn200 = createQuickAddButton("200 EGP", amountField);

        buttonsRow.getChildren().addAll(btn50, btn100, btn200);
        quickAddBox.getChildren().addAll(quickLabel, buttonsRow);

        // Message Label
        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(350);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-padding: 10 0 0 0;");

        // Spacer
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Add Money Button
        Button addButton = new Button("Add Money");
        addButton.setPrefHeight(50);
        addButton.setPrefWidth(350);
        addButton.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; " +
                          "-fx-font-size: 16px; -fx-font-weight: bold; " +
                          "-fx-background-radius: 10; -fx-cursor: hand;");

        addButton.setOnAction(e -> {
            String amountText = amountField.getText().trim();

            if (amountText.isEmpty()) {
                messageLabel.setText("❌ Please enter an amount");
                messageLabel.setStyle("-fx-text-fill: #EF4444;");
                return;
            }

            try {
                double amount = Double.parseDouble(amountText);

                if (amount <= 0) {
                    messageLabel.setText("❌ Amount must be greater than 0");
                    messageLabel.setStyle("-fx-text-fill: #EF4444;");
                    return;
                }

                if (amount > 10000) {
                    messageLabel.setText("❌ Maximum amount is 10,000 EGP");
                    messageLabel.setStyle("-fx-text-fill: #EF4444;");
                    return;
                }

                // Add funds to wallet
                double newBalance = currentUser.getWalletBalance() + amount;
                currentUser.updateWalletBalance(newBalance);

                // Update database
                updateWalletInDatabase(newBalance);

                // Update UI
                balanceAmount.setText(String.format("%.2f EGP", newBalance));
                messageLabel.setText("✓ Successfully added " + String.format("%.2f", amount) + " EGP!");
                messageLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                amountField.clear();

                // Refresh Profile page stats
                refreshStatsUI();

                System.out.println("=== Funds Added ===");
                System.out.println("User: " + currentUser.getEmail());
                System.out.println("Amount: " + amount + " EGP");
                System.out.println("New Balance: " + newBalance + " EGP");

            } catch (NumberFormatException ex) {
                messageLabel.setText("❌ Invalid amount format");
                messageLabel.setStyle("-fx-text-fill: #EF4444;");
            }
        });

        // Close Button
        Button closeButton = new Button("Close");
        closeButton.setPrefHeight(45);
        closeButton.setPrefWidth(350);
        closeButton.setStyle("-fx-background-color: #30363D; -fx-text-fill: #E6EDF3; " +
                            "-fx-font-size: 15px; -fx-background-radius: 10; -fx-cursor: hand;");
        closeButton.setOnAction(e -> fundsStage.close());

        layout.getChildren().addAll(
            header,
            balanceBox,
            inputBox,
            quickAddBox,
            messageLabel,
            spacer,
            addButton,
            new Label(""), // spacing
            closeButton
        );

        Scene scene = new Scene(layout);
        fundsStage.setScene(scene);
        fundsStage.showAndWait();
    }

    /**
     * Create quick add button
     */
    private Button createQuickAddButton(String text, TextField targetField) {
        Button btn = new Button(text);
        btn.setPrefWidth(100);
        btn.setPrefHeight(40);
        btn.setStyle("-fx-background-color: #1C2333; -fx-text-fill: #E6EDF3; " +
                    "-fx-font-size: 13px; -fx-background-radius: 8; " +
                    "-fx-border-color: #3B82F6; -fx-border-radius: 8; -fx-cursor: hand;");

        // Extract number from text (e.g., "50 EGP" -> "50")
        String amount = text.split(" ")[0];
        btn.setOnAction(e -> targetField.setText(amount));

        return btn;
    }

    /**
     * Update wallet balance in database
     */
    private void updateWalletInDatabase(double newBalance) {
        String tableName = isDriver ? "drivers" : "passengers";
        String sql = "UPDATE " + tableName + " SET wallet_balance = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDouble(1, newBalance);
            ps.setLong(2, userId);
            ps.executeUpdate();

            System.out.println("Wallet updated in database: " + newBalance + " EGP");

        } catch (SQLException e) {
            System.err.println("Error updating wallet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ====================================================================
    // FEATURE 3: REPORT A RIDE
    // ====================================================================

    /**
     * Handle Report a Ride button (add this button to your FXML under Save Changes)
     */
    @FXML
    public void onReportRide() {
        openReportModal();
    }

    /**
     * Open report ride modal
     */
    private void openReportModal() {
        Stage reportStage = new Stage();

        // Set window icon FIRST - Report uses settings icon
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/settings_12280787.png");
            if (iconStream != null) {
                javafx.scene.image.Image icon = new javafx.scene.image.Image(iconStream);
                if (!icon.isError()) {
                    reportStage.getIcons().setAll(icon);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load icon for Report Ride: " + e.getMessage());
        }

        reportStage.initModality(Modality.APPLICATION_MODAL);
        reportStage.setTitle("Report a Ride");
        reportStage.setWidth(400);
        reportStage.setHeight(400);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle("-fx-background-color: #ffffff;");

        Label titleLabel = new Label("Report a Ride");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label rideIdLabel = new Label("Ride ID / Ride Number:");
        rideIdLabel.setStyle("-fx-font-size: 12px;");

        TextField rideIdField = new TextField();
        rideIdField.setPromptText("Enter ride ID (e.g., 12345)");
        rideIdField.setPrefWidth(350);

        Label descLabel = new Label("Problem Description:");
        descLabel.setStyle("-fx-font-size: 12px;");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Describe the problem you experienced...");
        descArea.setPrefHeight(120);
        descArea.setWrapText(true);

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(350);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button submitBtn = new Button("Submit Report");
        submitBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        submitBtn.setPrefWidth(150);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-size: 14px;");
        cancelBtn.setPrefWidth(150);
        cancelBtn.setOnAction(e -> reportStage.close());

        submitBtn.setOnAction(e -> {
            String rideId = rideIdField.getText().trim();
            String description = descArea.getText().trim();

            if (rideId.isEmpty()) {
                messageLabel.setText("Please enter a ride ID");
                messageLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            if (description.isEmpty() || description.length() < 10) {
                messageLabel.setText("Please provide a detailed description (min 10 characters)");
                messageLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            // Submit report
            long reportId = submitRideReport(userId, rideId, description);

            if (reportId > 0) {
                messageLabel.setText("Report submitted successfully! Report ID: " + reportId);
                messageLabel.setStyle("-fx-text-fill: green;");

                // Clear fields
                rideIdField.clear();
                descArea.clear();

                // Close after 2 seconds
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.seconds(2));
                pause.setOnFinished(ev -> reportStage.close());
                pause.play();
            } else {
                messageLabel.setText("Failed to submit report. Please try again.");
                messageLabel.setStyle("-fx-text-fill: red;");
            }
        });

        buttonBox.getChildren().addAll(submitBtn, cancelBtn);

        layout.getChildren().addAll(
            titleLabel,
            new Label(""),
            rideIdLabel,
            rideIdField,
            descLabel,
            descArea,
            messageLabel,
            buttonBox
        );

        Scene scene = new Scene(layout);
        reportStage.setScene(scene);
        reportStage.showAndWait();
    }

    /**
     * Submit ride report to database
     */
    private long submitRideReport(long userId, String rideId, String description) {
        String sql = "INSERT INTO ride_reports (user_id, is_driver, ride_id, description, status, created_at) " +
                     "VALUES (?, ?, ?, ?, 'PENDING', ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, userId);
            ps.setBoolean(2, isDriver);
            ps.setString(3, rideId);
            ps.setString(4, description);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long reportId = rs.getLong(1);
                    System.out.println("\n=== Ride Report Submitted ===");
                    System.out.println("User ID: " + userId);
                    System.out.println("Is Driver: " + isDriver);
                    System.out.println("Ride ID: " + rideId);
                    System.out.println("Description: " + description);
                    System.out.println("Report ID: " + reportId);
                    System.out.println("============================\n");
                    return reportId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error submitting ride report: " + e.getMessage());
            e.printStackTrace();
        }

        return -1;
    }

    // ====================================================================
    // FEATURE 1: DETAIL PAGES (RATING / RIDES / SPENT)
    // ====================================================================

    /**
     * Open Rating Details Page
     * Shows history of ratings received by the user
     */
    private void openRatingDetails() {
        Stage detailStage = new Stage();

        // Set window icon FIRST - Rating uses avatar/star icon
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/avatar.png");
            if (iconStream != null) {
                javafx.scene.image.Image icon = new javafx.scene.image.Image(iconStream);
                if (!icon.isError()) {
                    detailStage.getIcons().setAll(icon);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load icon for Rating Details: " + e.getMessage());
        }

        detailStage.initModality(Modality.APPLICATION_MODAL);
        detailStage.setTitle("⭐ Rating Details");
        detailStage.setWidth(400);
        detailStage.setHeight(500);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle("-fx-background-color: #ffffff;");

        // Title
        Label titleLabel = new Label("Rating History");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Average Rating Display
        double avgRating = calculateAverageRating();
        Label avgLabel = new Label(String.format("Average Rating: %.1f ★", avgRating));
        avgLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        // ScrollPane for ratings list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(350);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox ratingsContainer = new VBox(10);
        ratingsContainer.setPadding(new Insets(10));
        ratingsContainer.setStyle("-fx-background-color: #f9f9f9;");

        // Load ratings from database
        loadRatingHistory(ratingsContainer);

        scrollPane.setContent(ratingsContainer);

        // Close button
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 150;");
        closeBtn.setOnAction(e -> detailStage.close());

        layout.getChildren().addAll(titleLabel, avgLabel, new Label(""), scrollPane, closeBtn);

        Scene scene = new Scene(layout);
        detailStage.setScene(scene);
        detailStage.show();
    }

    /**
     * Calculate average rating from ride history
     * For Drivers: Show average of passenger_rating (ratings received FROM passengers)
     * For Passengers: Show average of driver_rating (ratings received FROM drivers)
     */
    private double calculateAverageRating() {
        if (userId == -1) return 0.0;

        // FIXED: Drivers should see passenger_rating (ratings they received FROM passengers)
        String ratingColumn = isDriver ? "passenger_rating" : "driver_rating";
        String idColumn = isDriver ? "driver_id" : "passenger_id";
        String sql = "SELECT AVG(" + ratingColumn + ") as avg_rating FROM ride_history WHERE " + idColumn + " = ? AND " + ratingColumn + " > 0";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("avg_rating");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating average rating: " + e.getMessage());
        }

        return 0.0;
    }

    /**
     * Load rating history from database
     * For Drivers: Show passenger_rating (ratings received FROM passengers)
     * For Passengers: Show driver_rating (ratings received FROM drivers)
     */
    private void loadRatingHistory(VBox container) {
        if (userId == -1) {
            container.getChildren().add(new Label("No rating history available"));
            return;
        }

        // FIXED: Drivers should see passenger_rating (ratings they received FROM passengers)
        String ratingColumn = isDriver ? "passenger_rating" : "driver_rating";
        String idColumn = isDriver ? "driver_id" : "passenger_id";
        String sql = "SELECT rh.id, rh." + ratingColumn + " as rating, rh.completed_at, " +
                     "lo.name as origin, ld.name as destination " +
                     "FROM ride_history rh " +
                     "JOIN ride_requests rr ON rh.request_id = rr.id " +
                     "JOIN locations lo ON rr.origin_id = lo.id " +
                     "JOIN locations ld ON rr.destination_id = ld.id " +
                     "WHERE rh." + idColumn + " = ? AND rh." + ratingColumn + " > 0 " +
                     "ORDER BY rh.completed_at DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    VBox ratingBox = createRatingItem(
                        rs.getLong("id"),
                        rs.getInt("rating"),
                        rs.getString("origin"),
                        rs.getString("destination"),
                        rs.getTimestamp("completed_at")
                    );
                    container.getChildren().add(ratingBox);
                }

                if (count == 0) {
                    Label noDataLabel = new Label("No ratings yet");
                    noDataLabel.setStyle("-fx-text-fill: #999;");
                    container.getChildren().add(noDataLabel);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading rating history: " + e.getMessage());
            Label errorLabel = new Label("Error loading ratings");
            errorLabel.setStyle("-fx-text-fill: red;");
            container.getChildren().add(errorLabel);
        }
    }

    /**
     * Create a rating item display
     */
    private VBox createRatingItem(long rideId, int rating, String origin, String destination, Timestamp completedAt) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label rideLabel = new Label("Ride #" + rideId);
        rideLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        String stars = "★".repeat(rating) + "☆".repeat(5 - rating);
        Label ratingLabel = new Label(stars + " (" + rating + "/5)");
        ratingLabel.setStyle("-fx-text-fill: #FFA000; -fx-font-size: 14px;");

        Label routeLabel = new Label(origin + " → " + destination);
        routeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        Label dateLabel = new Label(completedAt.toString().substring(0, 16));
        dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

        box.getChildren().addAll(rideLabel, ratingLabel, routeLabel, dateLabel);
        return box;
    }

    /**
     * Open Rides Details Page
     * Navigate to full-page RideHistory screen
     */
    private void openRidesDetails() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RideHistory.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            RideHistoryController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate to Ride History: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load rides history from database
     * NOTE: Since we now store TWO records per ride, we use GROUP BY request_id
     * to avoid showing duplicate rides
     */
    private void loadRidesHistory(VBox container) {
        if (userId == -1) {
            container.getChildren().add(new Label("No ride history available"));
            return;
        }

        String idColumn = isDriver ? "driver_id" : "passenger_id";
        String sql = "SELECT rh.request_id, MAX(rh.ride_cost) as ride_cost, MAX(rh.completed_at) as completed_at, " +
                     "MAX(rh.payment_method) as payment_method, " +
                     "lo.name as origin, ld.name as destination, rr.distance_km " +
                     "FROM ride_history rh " +
                     "JOIN ride_requests rr ON rh.request_id = rr.id " +
                     "JOIN locations lo ON rr.origin_id = lo.id " +
                     "JOIN locations ld ON rr.destination_id = ld.id " +
                     "WHERE rh." + idColumn + " = ? " +
                     "GROUP BY rh.request_id, lo.name, ld.name, rr.distance_km " +
                     "ORDER BY MAX(rh.completed_at) DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    VBox rideBox = createRideItem(
                        rs.getLong("request_id"),  // Use request_id instead of rh.id
                        rs.getDouble("ride_cost"),
                        rs.getString("origin"),
                        rs.getString("destination"),
                        rs.getDouble("distance_km"),
                        rs.getString("payment_method"),
                        rs.getTimestamp("completed_at")
                    );
                    container.getChildren().add(rideBox);
                }

                if (count == 0) {
                    Label noDataLabel = new Label("No completed rides yet");
                    noDataLabel.setStyle("-fx-text-fill: #999;");
                    container.getChildren().add(noDataLabel);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading rides history: " + e.getMessage());
            Label errorLabel = new Label("Error loading rides");
            errorLabel.setStyle("-fx-text-fill: red;");
            container.getChildren().add(errorLabel);
        }
    }

    /**
     * Create a ride item display
     */
    private VBox createRideItem(long rideId, double cost, String origin, String destination,
                                double distance, String paymentMethod, Timestamp completedAt) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label rideLabel = new Label("Ride #" + rideId);
        rideLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label statusLabel = new Label("Completed");
        statusLabel.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 2 8; -fx-border-radius: 3; -fx-background-radius: 3; -fx-font-size: 10px;");
        headerBox.getChildren().addAll(rideLabel, new Label("  "), statusLabel);

        Label routeLabel = new Label(origin + " → " + destination);
        routeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333; -fx-font-weight: bold;");

        Label distanceLabel = new Label(String.format("Distance: %.2f km", distance));
        distanceLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        HBox costBox = new HBox(10);
        costBox.setAlignment(Pos.CENTER_LEFT);
        Label costLabel = new Label(String.format("Cost: $%.2f", cost));
        costLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        Label paymentLabel = new Label("(" + paymentMethod + ")");
        paymentLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
        costBox.getChildren().addAll(costLabel, paymentLabel);

        Label dateLabel = new Label(completedAt.toString().substring(0, 16));
        dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

        box.getChildren().addAll(headerBox, routeLabel, distanceLabel, costBox, dateLabel);
        return box;
    }

    /**
     * Open Spent Details Page
     * Shows list of all payments and ride costs
     */
    private void openSpentDetails() {
        Stage detailStage = new Stage();

        // Set window icon FIRST - Spending uses money icon
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/safe.png");
            if (iconStream != null) {
                javafx.scene.image.Image icon = new javafx.scene.image.Image(iconStream);
                if (!icon.isError()) {
                    detailStage.getIcons().setAll(icon);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load icon for Spending Details: " + e.getMessage());
        }

        detailStage.initModality(Modality.APPLICATION_MODAL);
        detailStage.setTitle(isDriver ? "💰 Earnings Details" : "💰 Spending Details");
        detailStage.setWidth(400);
        detailStage.setHeight(550);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle("-fx-background-color: #ffffff;");

        // Title
        Label titleLabel = new Label(isDriver ? "Earnings History" : "Spending History");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Total spent/earned
        double totalAmount = getTotalSpentOrEarned();
        Label totalLabel = new Label(String.format("Total %s: $%.2f", isDriver ? "Earned" : "Spent", totalAmount));
        totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        // ScrollPane for payments list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox paymentsContainer = new VBox(10);
        paymentsContainer.setPadding(new Insets(10));
        paymentsContainer.setStyle("-fx-background-color: #f9f9f9;");

        // Load payment history
        loadSpentHistory(paymentsContainer);

        scrollPane.setContent(paymentsContainer);

        // Close button
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white; -fx-font-size: 14px; -fx-pref-width: 150;");
        closeBtn.setOnAction(e -> detailStage.close());

        layout.getChildren().addAll(titleLabel, totalLabel, new Label(""), scrollPane, closeBtn);

        Scene scene = new Scene(layout);
        detailStage.setScene(scene);
        detailStage.show();
    }

    /**
     * Load spending/earnings history from database
     * NOTE: Since we now store TWO records per ride, we use GROUP BY request_id
     * to avoid showing duplicate payment records
     */
    private void loadSpentHistory(VBox container) {
        if (userId == -1) {
            container.getChildren().add(new Label("No payment history available"));
            return;
        }

        String idColumn = isDriver ? "driver_id" : "passenger_id";
        String sql = "SELECT rh.request_id, MAX(rh.ride_cost) as ride_cost, MAX(rh.payment_method) as payment_method, " +
                     "MAX(rh.tips) as tips, MAX(rh.completed_at) as completed_at, " +
                     "lo.name as origin, ld.name as destination " +
                     "FROM ride_history rh " +
                     "JOIN ride_requests rr ON rh.request_id = rr.id " +
                     "JOIN locations lo ON rr.origin_id = lo.id " +
                     "JOIN locations ld ON rr.destination_id = ld.id " +
                     "WHERE rh." + idColumn + " = ? " +
                     "GROUP BY rh.request_id, lo.name, ld.name " +
                     "ORDER BY MAX(rh.completed_at) DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    VBox paymentBox = createPaymentItem(
                        rs.getLong("request_id"),  // Use request_id instead of rh.id
                        rs.getDouble("ride_cost"),
                        rs.getDouble("tips"),
                        rs.getString("payment_method"),
                        rs.getString("origin"),
                        rs.getString("destination"),
                        rs.getTimestamp("completed_at")
                    );
                    container.getChildren().add(paymentBox);
                }

                if (count == 0) {
                    Label noDataLabel = new Label("No payment history yet");
                    noDataLabel.setStyle("-fx-text-fill: #999;");
                    container.getChildren().add(noDataLabel);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading spent history: " + e.getMessage());
            Label errorLabel = new Label("Error loading payment history");
            errorLabel.setStyle("-fx-text-fill: red;");
            container.getChildren().add(errorLabel);
        }
    }

    /**
     * Create a payment item display
     */
    private VBox createPaymentItem(long rideId, double cost, double tips, String paymentMethod,
                                   String origin, String destination, Timestamp completedAt) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label rideLabel = new Label("Ride #" + rideId);
        rideLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Label routeLabel = new Label(origin + " → " + destination);
        routeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        HBox amountBox = new HBox(10);
        amountBox.setAlignment(Pos.CENTER_LEFT);
        Label amountLabel = new Label(String.format("$%.2f", cost));
        amountLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (isDriver ? "#4CAF50" : "#F44336") + "; -fx-font-weight: bold;");

        if (tips > 0 && isDriver) {
            Label tipsLabel = new Label(String.format("(+$%.2f tips)", tips));
            tipsLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #4CAF50;");
            amountBox.getChildren().addAll(amountLabel, tipsLabel);
        } else {
            amountBox.getChildren().add(amountLabel);
        }

        Label methodLabel = new Label("via " + paymentMethod);
        methodLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

        Label dateLabel = new Label(completedAt.toString().substring(0, 16));
        dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

        box.getChildren().addAll(rideLabel, routeLabel, amountBox, methodLabel, dateLabel);
        return box;
    }

    // ====================================================================
    // END OF ProfileController
}

