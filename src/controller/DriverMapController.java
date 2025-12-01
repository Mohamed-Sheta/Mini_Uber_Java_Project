package controller;

import DAO.DriverDAO;
import DAO.LocationDAO;
import DAO.RideRequestDAO;
import Model.Driver;
import Model.RideHistory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

public class DriverMapController {

    @FXML private ImageView aboutBtn;
    @FXML private ImageView profileBtn;
    @FXML private Label statusLabel;
    @FXML private WebView mapWebView;

    // Stats labels
    @FXML private Label todayRidesLabel;
    @FXML private Label earningsLabel;
    @FXML private Label ratingLabel;

    // Control button
    @FXML private Button goOnlineBtn;

    // Ride request overlay
    @FXML private StackPane rideRequestOverlay;
    @FXML private VBox rideRequestContainer;
    @FXML private Label requestPickupLabel;
    @FXML private Label requestDestinationLabel;
    @FXML private Label requestDistanceLabel;
    @FXML private Label requestFareLabel;
    @FXML private Label timerLabel;
    @FXML private Button declineBtn;
    @FXML private Button acceptBtn;

    private WebEngine webEngine;
    private Driver currentDriver;
    private long currentDriverId;
    private boolean isOnline = false;
    private Timer rideCheckTimer;
    private Timer requestTimer;
    private int requestTimeLeft = 15;
    private RideRequestDAO.RideRequestRow currentRideRequest;

    @FXML
    public void initialize() {
        System.out.println("[DriverMap] Initializing Driver Map Controller...");

        try {
            // Setup navigation buttons
            if (aboutBtn != null) {
                aboutBtn.setOnMouseClicked(e -> navigateToAbout());
            }
            if (profileBtn != null) {
                profileBtn.setOnMouseClicked(e -> navigateToProfile());
            }

            // Initialize WebView and load map
            if (mapWebView == null) {
                System.err.println("ERROR: mapWebView is NULL! Check fx:id in FXML.");
                return;
            }

            System.out.println("[DriverMap] WebView found, initializing map...");

            // Configure WebView
            webEngine = mapWebView.getEngine();
            webEngine.setJavaScriptEnabled(true);
            mapWebView.setVisible(true);
            mapWebView.setPrefSize(390, 400);

            // Load map HTML
            loadMap();

            // Setup control button
            if (goOnlineBtn != null) {
                goOnlineBtn.setOnAction(e -> toggleOnlineStatus());
            }

            // Setup ride request buttons
            if (acceptBtn != null) {
                acceptBtn.setOnAction(e -> acceptRideRequest());
            }
            if (declineBtn != null) {
                declineBtn.setOnAction(e -> declineRideRequest());
            }

            // Initialize stats
            updateStats();

            System.out.println("[DriverMap] Initialization complete!");
        } catch (Exception e) {
            System.err.println("[DriverMap] ERROR in initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMap() {
        try {
            // Just load map.html from resources
            java.net.URL url = getClass().getResource("/map.html");

            if (url != null) {
                System.out.println("Loading map.html from: " + url.toExternalForm());
                webEngine.load(url.toExternalForm());
            } else {
                System.err.println("ERROR: map.html NOT FOUND at /map.html");
            }
        } catch (Exception e) {
            System.err.println("ERROR loading map.html: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setDriver(Driver driver, long driverId) {
        this.currentDriver = driver;
        this.currentDriverId = driverId;

        System.out.println("[DriverMap] Driver set: " + driver.getName() + " (ID: " + driverId + ")");

        // Update UI
        updateStats();

        // Center map on driver's location
        if (driver.getCurrentLocation() != null) {
            centerMapOnDriver();
        }
    }

    private void toggleOnlineStatus() {
        isOnline = !isOnline;

        if (isOnline) {
            goOnline();
        } else {
            goOffline();
        }
    }

    private void goOnline() {
        System.out.println("[DriverMap] Driver going online...");

        statusLabel.setText("🟢 Online");
        statusLabel.setStyle("-fx-text-fill: #3FB950; -fx-font-size: 14px; -fx-font-weight: 600;");
        goOnlineBtn.setText("Go Offline");
        goOnlineBtn.setStyle("-fx-background-color: #da3633; -fx-text-fill: #FFFFFF; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: 600;");

        // Update driver status in database
        updateDriverStatus(true);

        // Start checking for ride requests
        startRideRequestPolling();
    }

    private void goOffline() {
        System.out.println("[DriverMap] Driver going offline...");

        statusLabel.setText("⚫ Offline");
        statusLabel.setStyle("-fx-text-fill: #8B92A8; -fx-font-size: 14px; -fx-font-weight: 600;");
        goOnlineBtn.setText("Go Online");
        goOnlineBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: #FFFFFF; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: 600;");

        // Update driver status in database
        updateDriverStatus(false);

        // Stop checking for ride requests
        stopRideRequestPolling();
    }

    private void updateDriverStatus(boolean active) {
        try {
            if (currentDriver != null) {
                // Create updated Driver object with new active status
                Driver updatedDriver = new Driver(
                    currentDriver.getLicensePlate(),
                    currentDriver.getCarModel(),
                    active, // NEW status value
                    currentDriver.getUserSSN(),
                    currentDriver.getName(),
                    currentDriver.getPhoneNumber(),
                    currentDriver.getEmail(),
                    currentDriver.getWalletBalance(),
                    currentDriver.getCreditBalance(),
                    currentDriver.getCurrentLocation(),
                    currentDriver.getRideHistory(),
                    currentDriver.getPassword()
                );

                DriverDAO driverDAO = new DriverDAO();
                driverDAO.update(
                    currentDriverId,
                    updatedDriver,
                    currentDriver.getCurrentLocation() != null ?
                        currentDriver.getCurrentLocation().getName() : "Downtown Cairo"
                );

                // Update the in-memory driver object
                this.currentDriver = updatedDriver;

                System.out.println("[DriverMap] Driver status updated: " + (active ? "ACTIVE" : "INACTIVE"));
            }
        } catch (Exception e) {
            System.err.println("[DriverMap] Error updating driver status: " + e.getMessage());
        }
    }

    private void startRideRequestPolling() {
        // Schedule the route drawing after a short delay to ensure map.html is fully loaded
        new java.util.Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    try {
                        // Check if window.mapReady is true
                        Object mapReady = webEngine.executeScript("typeof window.mapReady !== 'undefined' && window.mapReady");
                        if (Boolean.TRUE.equals(mapReady)) {
                            System.out.println("[DriverMap] Map is ready, can start polling for rides");
                        }
                    } catch (Exception e) {
                        System.err.println("[DriverMap] Error checking map ready state: " + e.getMessage());
                    }
                });
            }
        }, 2000);

        rideCheckTimer = new Timer(true);
        rideCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForRideRequests();
            }
        }, 3000, 5000); // Check every 5 seconds
    }

    private void stopRideRequestPolling() {
        if (rideCheckTimer != null) {
            rideCheckTimer.cancel();
            rideCheckTimer = null;
        }
    }

    private void checkForRideRequests() {
        try {
            RideRequestDAO rideRequestDAO = new RideRequestDAO();

            for (RideRequestDAO.RideRequestRow request : rideRequestDAO.showAll()) {
                // Find pending requests without a driver
                if (request.driverId == 0 && "PENDING".equals(request.status)) {
                    // Show ride request popup
                    Platform.runLater(() -> showRideRequest(request));
                    break; // Show only one request at a time
                }
            }
        } catch (SQLException e) {
            System.err.println("[DriverMap] Error checking for ride requests: " + e.getMessage());
        }
    }

    private void showRideRequest(RideRequestDAO.RideRequestRow request) {
        try {
            this.currentRideRequest = request;

            // Load location details
            LocationDAO locationDAO = new LocationDAO();
            LocationDAO.LocationRow pickup = null;
            LocationDAO.LocationRow destination = null;

            for (LocationDAO.LocationRow loc : locationDAO.showAll()) {
                if (loc.id == request.originId) {
                    pickup = loc;
                }
                if (loc.id == request.destinationId) {
                    destination = loc;
                }
            }

            if (pickup != null && destination != null) {
                // Update labels
                requestPickupLabel.setText(pickup.name);
                requestDestinationLabel.setText(destination.name);
                requestDistanceLabel.setText(String.format("%.2f km", request.distanceKm));
                requestFareLabel.setText(String.format("$%.2f", request.estimatedPrice));

                // Draw route on map
                final LocationDAO.LocationRow finalPickup = pickup;
                final LocationDAO.LocationRow finalDestination = destination;

                new java.util.Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            try {
                                // Check if map is ready before drawing
                                Object mapReady = webEngine.executeScript(
                                    "typeof window.mapReady !== 'undefined' && window.mapReady"
                                );

                                if (Boolean.TRUE.equals(mapReady)) {
                                    String script = String.format(
                                        "drawRoute(%f, %f, %f, %f);",
                                        finalPickup.longitude, finalPickup.latitude,
                                        finalDestination.longitude, finalDestination.latitude
                                    );
                                    webEngine.executeScript(script);
                                    System.out.println("[DriverMap] Route drawn on map");
                                }
                            } catch (Exception e) {
                                System.err.println("[DriverMap] Error drawing route: " + e.getMessage());
                            }
                        });
                    }
                }, 500);

                // Show overlay
                rideRequestOverlay.setVisible(true);
                rideRequestOverlay.setManaged(true);

                // Start countdown timer
                startRequestTimer();
            }
        } catch (Exception e) {
            System.err.println("[DriverMap] Error showing ride request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startRequestTimer() {
        requestTimeLeft = 15;
        timerLabel.setText(String.valueOf(requestTimeLeft));

        if (requestTimer != null) {
            requestTimer.cancel();
        }

        requestTimer = new Timer(true);
        requestTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    requestTimeLeft--;
                    timerLabel.setText(String.valueOf(requestTimeLeft));

                    if (requestTimeLeft <= 0) {
                        // Auto-decline when timer runs out
                        declineRideRequest();
                    }
                });
            }
        }, 1000, 1000);
    }

    private void acceptRideRequest() {
        System.out.println("[DriverMap] Accepting ride request...");

        stopRequestTimer();
        hideRideRequestOverlay();

        // Navigate to DriverDashboard or DriverRideView
        navigateToDriverDashboard();
    }

    private void declineRideRequest() {
        System.out.println("[DriverMap] Declining ride request...");

        stopRequestTimer();
        hideRideRequestOverlay();

        // Clear map
        try {
            webEngine.executeScript("clearRoute();");
        } catch (Exception e) {
            System.err.println("[DriverMap] Error clearing route: " + e.getMessage());
        }

        currentRideRequest = null;
    }

    private void stopRequestTimer() {
        if (requestTimer != null) {
            requestTimer.cancel();
            requestTimer = null;
        }
    }

    private void hideRideRequestOverlay() {
        rideRequestOverlay.setVisible(false);
        rideRequestOverlay.setManaged(false);
    }

    private void updateStats() {
        if (currentDriver != null) {
            // Update today's rides (placeholder - implement actual logic)
            todayRidesLabel.setText("0");

            // Update earnings (placeholder - implement actual logic)
            earningsLabel.setText("$0.00");

            // Update rating - calculate from ride history
            double averageRating = 5.0; // Default
            if (currentDriver.getRideHistory() != null && !currentDriver.getRideHistory().isEmpty()) {
                int totalRatings = 0;
                int ratingCount = 0;
                for (RideHistory ride : currentDriver.getRideHistory()) {
                    // Assuming RideHistory has a rating field
                    // If not available, keep default 5.0
                }
                if (ratingCount > 0) {
                    averageRating = (double) totalRatings / ratingCount;
                }
            }
            ratingLabel.setText(String.format("⭐ %.1f", averageRating));
        }
    }

    private void centerMapOnDriver() {
        try {
            if (currentDriver.getCurrentLocation() != null) {
                double lon = currentDriver.getCurrentLocation().getLongitude();
                double lat = currentDriver.getCurrentLocation().getLatitude();

                String script = String.format("centerMap(%f, %f, 14);", lon, lat);
                webEngine.executeScript(script);
            }
        } catch (Exception e) {
            System.err.println("[DriverMap] Error centering map: " + e.getMessage());
        }
    }

    private void navigateToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Profile.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            ProfileController controller = loader.getController();
            if (currentDriver != null) {
                controller.setUser(currentDriver);
                controller.refreshProfile();
            }

            // Clean up timers
            if (rideCheckTimer != null) {
                rideCheckTimer.cancel();
            }
            if (requestTimer != null) {
                requestTimer.cancel();
            }

            if (mapWebView != null && mapWebView.getScene() != null && mapWebView.getScene().getWindow() != null) {
                Stage stage = (Stage) mapWebView.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            }
        } catch (IOException e) {
            System.err.println("[DriverMap] Error navigating to profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToAbout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/About.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            // Clean up timers
            if (rideCheckTimer != null) {
                rideCheckTimer.cancel();
            }
            if (requestTimer != null) {
                requestTimer.cancel();
            }

            if (mapWebView != null && mapWebView.getScene() != null && mapWebView.getScene().getWindow() != null) {
                Stage stage = (Stage) mapWebView.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            }
        } catch (IOException e) {
            System.err.println("[DriverMap] Error navigating to about: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToDriverDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverDashboard.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            DriverDashboardController controller = loader.getController();
            if (currentDriver != null) {
                controller.setDriver(currentDriver);
            }

            // Clean up timers
            if (rideCheckTimer != null) {
                rideCheckTimer.cancel();
            }
            if (requestTimer != null) {
                requestTimer.cancel();
            }

            if (mapWebView != null && mapWebView.getScene() != null && mapWebView.getScene().getWindow() != null) {
                Stage stage = (Stage) mapWebView.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            }
        } catch (IOException e) {
            System.err.println("[DriverMap] Error navigating to dashboard: " + e.getMessage());
        }
    }
}

