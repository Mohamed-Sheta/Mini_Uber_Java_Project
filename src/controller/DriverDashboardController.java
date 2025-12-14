package controller;

import DAO.DriverDAO;
import DAO.LocationDAO;
import DAO.RideRequestDAO;
import Model.Driver;
import Model.Status;
import services.Payment;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.animation.TranslateTransition;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import utils.DBConnection;
import utils.UserSession;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DriverDashboardController {

    // Navigation icons
    @FXML private ImageView aboutBtn;
    @FXML private ImageView profileBtn;

    @FXML private Label welcomeLabel;
    @FXML private Label assignedPassengerName;
    @FXML private Pane toggleSwitch;
    @FXML private Pane toggleThumb;
    @FXML private Label statusLabel;
    @FXML private Label todayEarningsLabel;
    @FXML private Label ridesTodayLabel;
    @FXML private VBox noRidesContainer;
    @FXML private VBox rideRequestWidget;
    @FXML private Label pickupLabel;
    @FXML private Label destinationLabel;
    @FXML private Label distanceLabel;
    @FXML private Label fareLabel;
    @FXML private Button acceptRideButton;
    @FXML private Button rejectRideButton;
    @FXML private Label waitingMessageLabel;
    @FXML private Label waitingSubtitleLabel;

    private Driver currentDriver;
    private long currentDriverId;
    private boolean isDriverOnline = false;
    private RideRequestDAO.RideRequestRow currentRideRequest;
    private Timer pollTimer;
    private PauseTransition rideRequestDelayTransition; // 5-second delay before showing ride request

    // Location coordinates for passing to map
    private double driverLatitude;
    private double driverLongitude;
    private double pickupLatitude;
    private double pickupLongitude;

    @FXML
    public void initialize() {
        System.out.println("DriverDashboardController.initialize() called");

        if (toggleSwitch == null || noRidesContainer == null) {
            System.err.println("ERROR: UI components are null");
            return;
        }

        try {
            // Setup navigation icons
            if (aboutBtn != null) {
                aboutBtn.setOnMouseClicked(e -> navigateToAbout());
            }
            if (profileBtn != null) {
                profileBtn.setOnMouseClicked(e -> navigateToProfile());
            }

            // Setup default state (OFF)
            isDriverOnline = false;
            if (statusLabel != null) {
                statusLabel.setText("Offline");
                statusLabel.setStyle("-fx-text-fill: #8B92A8; -fx-font-weight: 500; -fx-font-size: 13px;");
            }
            if (rideRequestWidget != null) {
                rideRequestWidget.setVisible(false);
                rideRequestWidget.setManaged(false);
            }
            noRidesContainer.setVisible(true);
            noRidesContainer.setManaged(true);

            // Set initial offline message
            showOfflineMessage();

            // Setup toggle switch click handler
            if (toggleSwitch != null) {
                toggleSwitch.setOnMouseClicked(e -> toggleDriverStatus());
            }

            // Setup accept ride button
            if (acceptRideButton != null) {
                acceptRideButton.setOnAction(event -> acceptRide());
            }

            // Setup reject ride button
            if (rejectRideButton != null) {
                rejectRideButton.setOnAction(event -> rejectRide());
            }


            System.out.println("DriverDashboardController.initialize() completed");
        } catch (Exception e) {
            System.err.println("ERROR in initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Toggle driver online/offline status with smooth animation
     * Updates the database immediately using existing DAO methods
     */
    private void toggleDriverStatus() {
        isDriverOnline = !isDriverOnline;

        // Update database first
        if (!updateDriverStatusInDatabase(isDriverOnline)) {
            // If database update fails, revert the toggle
            isDriverOnline = !isDriverOnline;
            System.err.println("[DriverDashboard] Failed to update driver status in database");
            return;
        }

        // Update UI after successful database update
        if (isDriverOnline) {
            // Switch to ON state
            animateToggleToOn();
            if (statusLabel != null) {
                statusLabel.setText("Online");
                statusLabel.setStyle("-fx-text-fill: #00D26A; -fx-font-weight: 600; -fx-font-size: 13px;");
            }
            System.out.println("[DriverDashboard] Driver status set to ONLINE (active=true)");

            // Show waiting message immediately
            showWaitingForRidesMessage();

            // FEATURE 2: 5-second delay BEFORE checking for rides when going online
            System.out.println("[DriverDashboard] Going ONLINE - will check for rides in 5 seconds...");
            PauseTransition initialDelay = new PauseTransition(Duration.seconds(5));
            initialDelay.setOnFinished(event -> {
                System.out.println("[DriverDashboard] 5-second initial delay complete - starting ride polling now");
                checkForPendingRidesAndDisplay();
                // After the first check, start continuous polling every 3 seconds
                startPollingForRides();
            });
            initialDelay.play();
        } else {
            // Switch to OFF state
            animateToggleToOff();
            if (statusLabel != null) {
                statusLabel.setText("Offline");
                statusLabel.setStyle("-fx-text-fill: #8B92A8; -fx-font-weight: 500; -fx-font-size: 13px;");
            }
            System.out.println("[DriverDashboard] Driver status set to OFFLINE (active=false)");
            stopPollingForRides();
            showOfflineMessage();
        }
    }

    private boolean updateDriverStatusInDatabase(boolean isActive) {
        try {
            // Use the currentDriver field that was set in setDriver()
            if (currentDriver == null) {
                System.err.println("[DriverDashboard] Cannot update status - driver not set");
                return false;
            }

            // Get driver ID
            if (currentDriverId == 0) {
                System.err.println("[DriverDashboard] Cannot update status - driver ID not set");
                return false;
            }

            // Create updated Driver object with new active status
            // Note: We must preserve all existing values, only change active status
            Driver updatedDriver = new Driver(
                currentDriver.getLicensePlate(),
                currentDriver.getCarModel(),
                isActive, // NEW status value
                currentDriver.getUserSSN(),
                currentDriver.getName(),
                currentDriver.getPhoneNumber(),
                currentDriver.getEmail(),
                currentDriver.getWalletBalance(),
                currentDriver.getCurrentLocation(),
                currentDriver.getRideHistory(),
                currentDriver.getPassword()
            );

            // Update database using existing DAO method
            DriverDAO driverDAO = new DriverDAO();
            String currentLocationName = currentDriver.getCurrentLocation() != null ?
                                        currentDriver.getCurrentLocation().getName() : null;
            int rowsUpdated = driverDAO.update(currentDriverId, updatedDriver, currentLocationName);

            if (rowsUpdated > 0) {
                System.out.println("[DriverDashboard] ✅ Database updated: driver.active = " + isActive);

                // Update the in-memory driver object
                this.currentDriver = updatedDriver;

                // Also update UserSession if it has the driver
                if (UserSession.getInstance().isDriver()) {
                    UserSession.getInstance().updateCurrentUser(updatedDriver);
                }

                return true;
            } else {
                System.err.println("[DriverDashboard] ❌ No rows updated in database");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("[DriverDashboard] ❌ SQL Error updating driver status: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("[DriverDashboard] ❌ Unexpected error updating driver status: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private void animateToggleToOn() {
        if (toggleThumb == null || toggleSwitch == null) return;

        // Animate thumb to right position (new dimensions: 64px width, 26px thumb)
        TranslateTransition transition = new TranslateTransition(Duration.millis(250), toggleThumb);
        transition.setToX(32); // Move 32px to the right (64 - 26 - 6 padding)
        transition.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        transition.play();

        // Change background to bright green (#00D26A) with shadow
        toggleSwitch.setStyle("-fx-background-color: #00D26A; " +
                             "-fx-background-radius: 16; " +
                             "-fx-cursor: hand; " +
                             "-fx-effect: dropshadow(gaussian, rgba(0, 212, 106, 0.3), 6, 0.0, 0, 1);");

        // Keep white thumb with shadow
        toggleThumb.setStyle("-fx-background-color: #FFFFFF; " +
                            "-fx-background-radius: 13; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 6, 0.0, 0, 2);");
    }

    private void animateToggleToOff() {
        if (toggleThumb == null || toggleSwitch == null) return;

        // Animate thumb to left position
        TranslateTransition transition = new TranslateTransition(Duration.millis(250), toggleThumb);
        transition.setToX(0); // Back to original position
        transition.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        transition.play();

        // Change background to dark grey (#2E2E2E) with shadow
        toggleSwitch.setStyle("-fx-background-color: #2E2E2E; " +
                             "-fx-background-radius: 16; " +
                             "-fx-cursor: hand; " +
                             "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 4, 0.0, 0, 1);");

        // Keep white thumb with shadow
        toggleThumb.setStyle("-fx-background-color: #FFFFFF; " +
                            "-fx-background-radius: 13; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 6, 0.0, 0, 2);");
    }

    public void setDriver(Driver driver) {
        setDriver(driver, false); // Default: not coming from ride completion
    }

    public void setDriver(Driver driver, boolean comingFromRideCompletion) {
        System.out.println("setDriver() called with driver: " + (driver != null ? driver.getName() : "null") +
                          ", comingFromRideCompletion: " + comingFromRideCompletion);

        if (driver == null) {
            System.err.println("ERROR: Driver is null");
            return;
        }

        this.currentDriver = driver;

        // Update UserSession with current driver to ensure profile image sync works
        UserSession.getInstance().updateCurrentUser(driver);
        System.out.println("[DriverDashboard] Updated UserSession with driver: " + driver.getEmail());

        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome Back, " + driver.getName());
        }

        // Get driver ID from database and load current status
        try {
            DriverDAO driverDAO = new DriverDAO();
            Long driverId = driverDAO.getDriverIdByEmail(driver.getEmail());
            if (driverId != null) {
                this.currentDriverId = driverId;
                System.out.println("Driver ID loaded: " + driverId);

                // Load driver's current active status from database
                loadDriverStatusFromDatabase(comingFromRideCompletion);
            }
        } catch (Exception e) {
            System.err.println("Error loading driver ID: " + e.getMessage());
        }

        // Load today's statistics
        loadTodayStatistics();
    }

    private void loadDriverStatusFromDatabase(boolean comingFromRideCompletion) {
        try {
            System.out.println("[DriverDashboard] Loading driver status from database...");

            // Query database for current active status
            boolean databaseStatus = false;
            try (Connection con = DBConnection.getConnection()) {
                String query = "SELECT active FROM drivers WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(query)) {
                    ps.setLong(1, currentDriverId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            databaseStatus = rs.getBoolean("active");
                            System.out.println("[DriverDashboard] Database shows driver active = " + databaseStatus);
                        }
                    }
                }
            }

            // Set local status from database
            isDriverOnline = databaseStatus;

            // Update UI based on loaded status (without animation on initial load)
            if (isDriverOnline) {
                // Driver is ONLINE
                if (toggleThumb != null) {
                    toggleThumb.setTranslateX(32); // Right position
                }
                if (toggleSwitch != null) {
                    toggleSwitch.setStyle("-fx-background-color: #00D26A; " +
                                         "-fx-background-radius: 16; " +
                                         "-fx-cursor: hand; " +
                                         "-fx-effect: dropshadow(gaussian, rgba(0, 212, 106, 0.3), 6, 0.0, 0, 1);");
                }
                if (toggleThumb != null) {
                    toggleThumb.setStyle("-fx-background-color: #FFFFFF; " +
                                        "-fx-background-radius: 13; " +
                                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 6, 0.0, 0, 2);");
                }
                if (statusLabel != null) {
                    statusLabel.setText("Online");
                    statusLabel.setStyle("-fx-text-fill: #00D26A; -fx-font-weight: 600; -fx-font-size: 13px;");
                }

                // Show waiting message and start polling
                showWaitingForRidesMessage();

                if (comingFromRideCompletion) {
                    // FEATURE 2: 3-second delay after ride completion before next ride
                    System.out.println("[DriverDashboard] Returning from ride completion - will check for next ride in 3 seconds...");
                    PauseTransition postRideDelay = new PauseTransition(Duration.seconds(3));
                    postRideDelay.setOnFinished(event -> {
                        System.out.println("[DriverDashboard] 3-second post-ride delay complete - checking for next ride now");
                        checkForPendingRidesAndDisplay();
                        // After first check, start continuous polling
                        startPollingForRides();
                    });
                    postRideDelay.play();
                } else {
                    // Normal load: start polling immediately
                    startPollingForRides();
                }

                System.out.println("[DriverDashboard] ✅ Driver status initialized to ONLINE");
            } else {
                // Driver is OFFLINE
                if (toggleThumb != null) {
                    toggleThumb.setTranslateX(0); // Left position
                }
                if (toggleSwitch != null) {
                    toggleSwitch.setStyle("-fx-background-color: #2E2E2E; " +
                                         "-fx-background-radius: 16; " +
                                         "-fx-cursor: hand; " +
                                         "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 4, 0.0, 0, 1);");
                }
                if (toggleThumb != null) {
                    toggleThumb.setStyle("-fx-background-color: #FFFFFF; " +
                                        "-fx-background-radius: 13; " +
                                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 6, 0.0, 0, 2);");
                }
                if (statusLabel != null) {
                    statusLabel.setText("Offline");
                    statusLabel.setStyle("-fx-text-fill: #8B92A8; -fx-font-weight: 600; -fx-font-size: 13px;");
                }

                // Show offline message
                showOfflineMessage();

                System.out.println("[DriverDashboard] ✅ Driver status initialized to OFFLINE");
            }

        } catch (Exception e) {
            System.err.println("[DriverDashboard] Error loading driver status: " + e.getMessage());
            e.printStackTrace();
        }

        // Load profile image
        loadProfileImage();
    }

    private void loadTodayStatistics() {
        if (currentDriverId == 0) {
            if (todayEarningsLabel != null) todayEarningsLabel.setText("$0.00");
            if (ridesTodayLabel != null) ridesTodayLabel.setText("0");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // Query today's earnings
            String earningsQuery = "SELECT COALESCE(SUM(rh.ride_cost + rh.tips), 0) as earnings " +
                    "FROM ride_history rh WHERE rh.driver_id = ? AND DATE(rh.completed_at) = CURDATE()";

            try (PreparedStatement ps = con.prepareStatement(earningsQuery)) {
                ps.setLong(1, currentDriverId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && todayEarningsLabel != null) {
                        double totalEarnings = rs.getDouble("earnings");
                        // Apply company commission to calculate net earnings for the driver
                        double netEarnings = totalEarnings * (1 - Payment.getCompanyCommission());
                        todayEarningsLabel.setText(String.format("$%.2f", netEarnings));
                    }
                }
            }

            // Query today's ride count
            String ridesQuery = "SELECT COUNT(*) as count FROM ride_history " +
                    "WHERE driver_id = ? AND DATE(completed_at) = CURDATE()";

            try (PreparedStatement ps = con.prepareStatement(ridesQuery)) {
                ps.setLong(1, currentDriverId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && ridesTodayLabel != null) {
                        ridesTodayLabel.setText(String.valueOf(rs.getInt("count")));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading statistics: " + e.getMessage());
        }
    }

    private void startPollingForRides() {
        // Stop any existing polling first
        stopPollingForRides();

        pollTimer = new Timer(true);
        pollTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> checkForPendingRidesAndDisplay());
            }
        }, 3000, 3000); // Start after 3 seconds, repeat every 3 seconds

        System.out.println("[DriverDashboard] Continuous polling started (every 3 seconds)");
    }

    private void stopPollingForRides() {
        if (pollTimer != null) {
            pollTimer.cancel();
            pollTimer = null;
            System.out.println("[DriverDashboard] Polling stopped");
        }

        // Also cancel any pending delay transition
        if (rideRequestDelayTransition != null) {
            rideRequestDelayTransition.stop();
            rideRequestDelayTransition = null;
        }
    }

    private void checkForPendingRidesAndDisplay() {
        try {
            RideRequestDAO rideRequestDAO = new RideRequestDAO();
            LocationDAO locationDAO = new LocationDAO();

            List<RideRequestDAO.RideRequestRow> allRequests = rideRequestDAO.showAll();
            List<LocationDAO.LocationRow> locations = locationDAO.showAll();

            // Filter pending rides
            List<RideRequestDAO.RideRequestRow> pendingRides = new ArrayList<>();
            for (RideRequestDAO.RideRequestRow request : allRequests) {
                if (request.status.equals("Pending") && request.driverId == null) {
                    pendingRides.add(request);
                }
            }

            // If no pending rides exist, create a sample ride request using existing DAO
            if (pendingRides.isEmpty()) {
                System.out.println("[DriverDashboard] No pending rides found - creating sample request");

                // Create a sample ride request if we have locations
                if (locations.size() >= 2) {
                    try {
                        // Get a valid passenger_id from database (use first available passenger)
                        long validPassengerId = 1L; // Default fallback
                        try (Connection con = DBConnection.getConnection()) {
                            String query = "SELECT id FROM passengers LIMIT 1";
                            try (PreparedStatement ps = con.prepareStatement(query);
                                 ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    validPassengerId = rs.getLong("id");
                                    System.out.println("[DriverDashboard] Using passenger ID: " + validPassengerId);
                                }
                            }
                        }

                        // Get two different locations for pickup and destination
                        LocationDAO.LocationRow origin = locations.get(0);
                        LocationDAO.LocationRow destination = locations.get(locations.size() > 2 ? locations.size() - 1 : 1);

                        // Calculate sample distance and price
                        double distance = 15.0 + (Math.random() * 10.0); // 15-25 km
                        int estimatedTime = (int)(distance * 1.2); // ~1.2 min per km
                        double estimatedPrice = distance * 5.0; // $5 per km

                        // Insert sample request into database using existing DAO
                        long requestId = rideRequestDAO.insert(
                            validPassengerId, // Use valid passenger_id from database
                            null, // driver_id (null = pending)
                            origin.id,
                            destination.id,
                            Status.Pending,
                            distance,
                            estimatedTime,
                            estimatedPrice,
                            null, // acceptance_time
                            false, // driver_arrived
                            false  // passenger_arrived
                        );

                        System.out.println("[DriverDashboard] ✅ Sample ride request created with ID: " + requestId);

                        // Reload and check again
                        allRequests = rideRequestDAO.showAll();
                        pendingRides.clear();
                        for (RideRequestDAO.RideRequestRow request : allRequests) {
                            if (request.status.equals("Pending") && request.driverId == null) {
                                pendingRides.add(request);
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("[DriverDashboard] Error creating sample request: " + e.getMessage());
                    }
                }
            }

            if (pendingRides.isEmpty()) {
                Platform.runLater(this::hideRideRequest);
                return;
            }

            // Get nearest ride (first pending ride) and display immediately
            currentRideRequest = pendingRides.get(0);
            Platform.runLater(() -> displayRideRequest(currentRideRequest));

        } catch (SQLException e) {
            System.err.println("Error checking for rides: " + e.getMessage());
        }
    }


    private void displayRideRequest(RideRequestDAO.RideRequestRow request) {
        try {
            LocationDAO locationDAO = new LocationDAO();
            List<LocationDAO.LocationRow> locations = locationDAO.showAll();

            LocationDAO.LocationRow pickup = null;
            LocationDAO.LocationRow destination = null;

            for (LocationDAO.LocationRow loc : locations) {
                if (loc.id == request.originId) pickup = loc;
                if (loc.id == request.destinationId) destination = loc;
            }

            if (pickup != null && destination != null && pickupLabel != null) {
                pickupLabel.setText(pickup.name);
                destinationLabel.setText(destination.name);
                distanceLabel.setText(String.format("%.2f km", request.distanceKm));
                fareLabel.setText(String.format("$%.2f", request.estimatedPrice));

                // Fetch and display passenger name
                String passengerName = getPassengerNameById(request.passengerId);
                if (passengerName != null) {
                    setAssignedPassengerName(passengerName);
                }

                rideRequestWidget.setVisible(true);
                rideRequestWidget.setManaged(true);
                noRidesContainer.setVisible(false);
                noRidesContainer.setManaged(false);
            }
        } catch (SQLException e) {
            System.err.println("Error displaying ride request: " + e.getMessage());
        }
    }

    private String getPassengerNameById(long passengerId) {
        try (Connection con = DBConnection.getConnection()) {
            String query = "SELECT name FROM passengers WHERE id = ?";
            try (PreparedStatement ps = con.prepareStatement(query)) {
                ps.setLong(1, passengerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String name = rs.getString("name");
                        System.out.println("[DriverDashboard] Found passenger name: " + name);
                        return name;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DriverDashboard] Error fetching passenger name: " + e.getMessage());
        }
        return null;
    }

    private void hideRideRequest() {
        if (rideRequestWidget != null) {
            rideRequestWidget.setVisible(false);
            rideRequestWidget.setManaged(false);
        }
        if (noRidesContainer != null) {
            noRidesContainer.setVisible(true);
            noRidesContainer.setManaged(true);
        }
        // Hide passenger name when hiding ride request
        hideAssignedPassengerName();
    }

    private void showOfflineMessage() {
        hideRideRequest();
        if (waitingMessageLabel != null) {
            waitingMessageLabel.setText("Status: Offline");
            waitingMessageLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #8B92A8; -fx-letter-spacing: 0.3px;");
        }
        if (waitingSubtitleLabel != null) {
            waitingSubtitleLabel.setText("Make your status Active to receive ride requests");
            waitingSubtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8B92A8; -fx-font-weight: 500; -fx-line-spacing: 2px;");
        }
    }


    private void showWaitingForRidesMessage() {
        hideRideRequest();
        if (waitingMessageLabel != null) {
            waitingMessageLabel.setText("Waiting for Rides...");
            waitingMessageLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #E6EDF3; -fx-letter-spacing: 0.3px;");
        }
        if (waitingSubtitleLabel != null) {
            waitingSubtitleLabel.setText("You're online! Ride requests will appear here");
            waitingSubtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8B92A8; -fx-font-weight: 500; -fx-line-spacing: 2px;");
        }
    }

    private void acceptRide() {
        if (currentRideRequest == null) {
            System.err.println("[DriverDashboard] Cannot accept ride - no current request");
            return;
        }

        System.out.println("[DriverDashboard] Driver accepted ride ID: " + currentRideRequest.id);

        try {
            // Update ride status to Accepted in database
            RideRequestDAO rideRequestDAO = new RideRequestDAO();
            rideRequestDAO.update(
                    currentRideRequest.id,
                    currentDriverId,
                    Status.Accepted,
                    currentRideRequest.distanceKm,
                    currentRideRequest.estimatedTime,
                    currentRideRequest.estimatedPrice,
                    new Timestamp(System.currentTimeMillis()),
                    false, // driver not yet arrived
                    false  // passenger not yet arrived
            );

            System.out.println("[DriverDashboard] ✅ Ride status updated to Accepted");

            // Stop polling for new rides
            stopPollingForRides();

            // Hide ride request widget
            hideRideRequest();

            // Navigate to DriverRideView
            openDriverRideView(currentRideRequest.id);

        } catch (SQLException e) {
            System.err.println("[DriverDashboard] ❌ Error accepting ride: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void rejectRide() {
        if (currentRideRequest == null) {
            System.err.println("[DriverDashboard] Cannot reject ride - no current request");
            return;
        }

        System.out.println("[DriverDashboard] Driver rejected ride ID: " + currentRideRequest.id);

        try {
            // Delete the request from database using existing DAO
            RideRequestDAO rideRequestDAO = new RideRequestDAO();
            rideRequestDAO.delete(currentRideRequest.id);

            System.out.println("[DriverDashboard] ✅ Ride request deleted from database");
        } catch (SQLException e) {
            System.err.println("[DriverDashboard] ❌ Error deleting ride request: " + e.getMessage());
            e.printStackTrace();
        }

        // Hide the ride request widget
        hideRideRequest();

        // Show waiting message again
        showWaitingForRidesMessage();

        // Clear current request
        currentRideRequest = null;

        System.out.println("[DriverDashboard] ✅ Ride request rejected and removed");
    }

    private void openDriverRideView(long rideId) {
        try {
            System.out.println("[DriverDashboard] Opening DriverRideView for ride ID: " + rideId);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverRideView.fxml"));
            javafx.scene.Parent root = loader.load();

            DriverRideViewController controller = loader.getController();
            if (controller != null) {
                controller.setRideDetails(rideId, currentDriver, currentDriverId);
            }

            Stage stage = (Stage) toggleSwitch.getScene().getWindow();
            stage.setScene(new Scene(root, 390, 750));
            stage.show();

            System.out.println("[DriverDashboard] ✅ Successfully opened DriverRideView");
        } catch (Exception e) {
            System.err.println("[DriverDashboard] ❌ Error opening DriverRideView: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void navigateToAbout() {
        System.out.println("[DriverDashboard] Navigating to About page");
        stopPollingForRides(); // Stop polling when navigating away
        try {
            // Try multiple paths to locate About.fxml
            java.net.URL fxmlUrl = getClass().getResource("/view/About.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getClassLoader().getResource("view/About.fxml");
            }
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("/About.fxml");
            }
            if (fxmlUrl == null) {
                System.err.println("ERROR: Could not find About.fxml in any location");
                return;
            }

            System.out.println("Loading About.fxml from: " + fxmlUrl);
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            javafx.scene.Parent root = loader.load();

            // Pass driver data to About controller if it has setUser method
            AboutController controller = loader.getController();
            if (controller != null && currentDriver != null) {
                try {
                    controller.setUser(currentDriver);
                } catch (Exception e) {
                    System.out.println("About controller doesn't require user data");
                }
            }

            Scene scene = new Scene(root, 390, 750);
            Stage stage = (Stage) aboutBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

            System.out.println("Successfully navigated to About page");
        } catch (IOException ex) {
            System.err.println("Failed to load About screen: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void navigateToProfile() {
        System.out.println("[DriverDashboard] Navigating to Driver Profile page");
        stopPollingForRides(); // Stop polling when navigating away
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Profile.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            // Pass driver data to profile controller
            ProfileController controller = loader.getController();
            if (currentDriver != null) {
                controller.setUser(currentDriver);
                // Always refresh profile data when navigating
                controller.refreshProfile();
                System.out.println("Driver data passed to Profile: " + currentDriver.getName());
            }

            Stage stage = (Stage) profileBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

            System.out.println("Successfully navigated to Driver Profile page");
        } catch (IOException ex) {
            System.err.println("Failed to load Profile screen: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onLogout() {
        stopPollingForRides();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RoleSelection.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Error logging out: " + e.getMessage());
        }
    }

    private void loadProfileImage() {
        try {
            System.out.println("[DriverDashboard] Loading profile image for driver: " +
                (currentDriver != null ? currentDriver.getName() : "null"));

            // Load profile image using ProfilePhotoDAO
            String imagePath = null;
            if (currentDriver != null && currentDriverId > 0) {
                // Use ProfilePhotoDAO to get profile image path
                DAO.ProfilePhotoDAO photoDAO = new DAO.ProfilePhotoDAO();
                imagePath = photoDAO.getProfileImagePath(currentDriverId, "driver");

                System.out.println("[DriverDashboard] Image path from database for driver ID " +
                    currentDriverId + ": " + imagePath);

                // Also update UserSession with current driver and image path
                // This keeps UserSession in sync for when Profile screen needs it
                if (imagePath != null) {
                    UserSession.getInstance().setProfileImagePath(imagePath);
                }
            }

            // Try to load the profile image
            if (imagePath != null && !imagePath.isEmpty()) {
                java.io.File imageFile = new java.io.File(imagePath);
                if (imageFile.exists()) {
                    javafx.scene.image.Image profileImage = new javafx.scene.image.Image(imageFile.toURI().toString());
                    if (!profileImage.isError() && profileBtn != null) {
                        profileBtn.setImage(profileImage);
                        System.out.println("[DriverDashboard] ✅ Profile image loaded successfully: " + imagePath);
                        return;
                    } else {
                        System.out.println("[DriverDashboard] ⚠️ Image loading failed or profileBtn is null");
                    }
                } else {
                    System.out.println("[DriverDashboard] ⚠️ Image file not found: " + imagePath);
                }
            } else {
                System.out.println("[DriverDashboard] ℹ️ No custom profile image set, using default");
            }

            // Load default avatar if no custom image or if loading failed
            javafx.scene.image.Image defaultAvatar = new javafx.scene.image.Image(
                getClass().getResourceAsStream("/user_17436294.png")
            );
            if (profileBtn != null && !defaultAvatar.isError()) {
                profileBtn.setImage(defaultAvatar);
                System.out.println("[DriverDashboard] Default avatar loaded");
            }

        } catch (Exception e) {
            System.err.println("[DriverDashboard] Error loading profile image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setAssignedPassengerName(String name) {
        if (assignedPassengerName != null && name != null && !name.isEmpty()) {
            assignedPassengerName.setText("Passenger: " + name);
            assignedPassengerName.setVisible(true);
            assignedPassengerName.setManaged(true);
            System.out.println("[DriverDashboard] Assigned passenger name displayed: " + name);
        }
    }


    private void hideAssignedPassengerName() {
        if (assignedPassengerName != null) {
            assignedPassengerName.setVisible(false);
            assignedPassengerName.setManaged(false);
        }
    }
}

