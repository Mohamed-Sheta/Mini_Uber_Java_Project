package controller;

import DAO.DriverDAO;
import DAO.LocationDAO;
import DAO.RideRequestDAO;
import Model.Driver;
import Model.Status;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import utils.DBConnection;

import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DriverDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private ToggleButton driverModeToggle;
    @FXML private Label todayEarningsLabel;
    @FXML private Label ridesTodayLabel;
    @FXML private VBox noRidesContainer;

    // Ride request widget components
    @FXML private VBox rideRequestWidget;
    @FXML private Label pickupLabel;
    @FXML private Label destinationLabel;
    @FXML private Label distanceLabel;
    @FXML private Label fareLabel;
    @FXML private Button acceptRideButton;

    private Driver currentDriver;
    private long currentDriverId;
    private RideRequestDAO.RideRequestRow currentRideRequest;
    private Timer pollTimer;

    // Location coordinates for passing to map
    private double driverLatitude;
    private double driverLongitude;
    private double pickupLatitude;
    private double pickupLongitude;

    @FXML
    public void initialize() {
        System.out.println("DriverDashboardController.initialize() called");

        // Add null checks for all UI components
        if (driverModeToggle == null || noRidesContainer == null || welcomeLabel == null ||
            todayEarningsLabel == null || ridesTodayLabel == null || rideRequestWidget == null ||
            pickupLabel == null || destinationLabel == null ||
            distanceLabel == null || fareLabel == null || acceptRideButton == null) {
            System.err.println("ERROR: One or more FXML components are null. Check FXML file IDs.");
            System.err.println("driverModeToggle: " + driverModeToggle);
            System.err.println("noRidesContainer: " + noRidesContainer);
            System.err.println("rideRequestWidget: " + rideRequestWidget);
            System.err.println("acceptRideButton: " + acceptRideButton);
            return;
        }

        try {
            // Setup default state
            driverModeToggle.setSelected(false);
            driverModeToggle.setText("Offline");
            rideRequestWidget.setVisible(false);
            rideRequestWidget.setManaged(false);
            noRidesContainer.setVisible(true);
            noRidesContainer.setManaged(true);

            // Setup toggle listener
            driverModeToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    driverModeToggle.setText("Online");
                    driverModeToggle.setStyle("-fx-background-color: #10B981; -fx-text-fill: white;");
                    startPollingForRides();
                } else {
                    driverModeToggle.setText("Offline");
                    driverModeToggle.setStyle("");
                    stopPollingForRides();
                    hideRideRequest();
                }
            });

            // Setup accept ride button
            acceptRideButton.setOnAction(event -> acceptRide());

            System.out.println("DriverDashboardController.initialize() completed successfully");
        } catch (Exception e) {
            System.err.println("ERROR in initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setDriver(Driver driver) {
        System.out.println("setDriver() called with driver: " + (driver != null ? driver.getName() : "null"));

        if (driver == null) {
            System.err.println("ERROR: Driver is null");
            return;
        }

        this.currentDriver = driver;

        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome Back, " + driver.getName());
        }

        // Get driver ID from database
        try {
            DriverDAO driverDAO = new DriverDAO();
            Long driverId = driverDAO.getDriverIdByEmail(driver.getEmail());
            if (driverId != null) {
                this.currentDriverId = driverId;
                System.out.println("Driver ID loaded: " + driverId);
            } else {
                System.err.println("Driver ID not found for email: " + driver.getEmail());
            }
        } catch (Exception e) {
            System.err.println("Error loading driver ID: " + e.getMessage());
            e.printStackTrace();
        }

        // Load today's statistics
        try {
            loadTodayStatistics();
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadTodayStatistics() {
        if (currentDriverId == 0) {
            System.err.println("Driver ID not set, cannot load statistics");
            if (todayEarningsLabel != null) {
                todayEarningsLabel.setText("$0.00");
            }
            if (ridesTodayLabel != null) {
                ridesTodayLabel.setText("0");
            }
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.err.println("Database connection is null");
                return;
            }

            // Query today's earnings
            String earningsQuery = "SELECT COALESCE(SUM(rh.ride_cost + rh.tips), 0) as earnings " +
                    "FROM ride_history rh " +
                    "WHERE rh.driver_id = ? AND DATE(rh.completed_at) = CURDATE()";

            try (PreparedStatement ps = con.prepareStatement(earningsQuery)) {
                ps.setLong(1, currentDriverId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double earnings = rs.getDouble("earnings");
                        if (todayEarningsLabel != null) {
                            todayEarningsLabel.setText(String.format("$%.2f", earnings));
                        }
                    }
                }
            }

            // Query today's ride count
            String ridesQuery = "SELECT COUNT(*) as count " +
                    "FROM ride_history " +
                    "WHERE driver_id = ? AND DATE(completed_at) = CURDATE()";

            try (PreparedStatement ps = con.prepareStatement(ridesQuery)) {
                ps.setLong(1, currentDriverId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt("count");
                        if (ridesTodayLabel != null) {
                            ridesTodayLabel.setText(String.valueOf(count));
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error loading statistics: " + e.getMessage());
            e.printStackTrace();
            if (todayEarningsLabel != null) {
                todayEarningsLabel.setText("$0.00");
            }
            if (ridesTodayLabel != null) {
                ridesTodayLabel.setText("0");
            }
        }
    }

    private void startPollingForRides() {
        pollTimer = new Timer(true);
        pollTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForPendingRides();
            }
        }, 0, 3000); // Poll every 3 seconds
    }

    private void stopPollingForRides() {
        if (pollTimer != null) {
            pollTimer.cancel();
            pollTimer = null;
        }
    }

    private void checkForPendingRides() {
        try {
            RideRequestDAO rideRequestDAO = new RideRequestDAO();
            List<RideRequestDAO.RideRequestRow> allRequests = rideRequestDAO.showAll();

            // Find first pending ride without a driver
            for (RideRequestDAO.RideRequestRow request : allRequests) {
                if (request.status.equals("Pending") && request.driverId == null) {
                    currentRideRequest = request;
                    Platform.runLater(() -> displayRideRequest(request));
                    return;
                }
            }

            // No pending rides found
            Platform.runLater(this::hideRideRequest);

        } catch (SQLException e) {
            System.err.println("Error checking for rides: " + e.getMessage());
        }
    }

    private void displayRideRequest(RideRequestDAO.RideRequestRow request) {
        try {
            // Get location details
            LocationDAO locationDAO = new LocationDAO();
            List<LocationDAO.LocationRow> locations = locationDAO.showAll();

            LocationDAO.LocationRow pickup = null;
            LocationDAO.LocationRow destination = null;

            for (LocationDAO.LocationRow loc : locations) {
                if (loc.id == request.originId) {
                    pickup = loc;
                }
                if (loc.id == request.destinationId) {
                    destination = loc;
                }
            }

            if (pickup != null && destination != null) {
                // Store coordinates for passing to map
                this.pickupLatitude = pickup.latitude;
                this.pickupLongitude = pickup.longitude;

                // Set driver location (manually for now - you can update these values)
                // For demo, using Cairo coordinates - replace with actual driver location
                this.driverLatitude = 30.0444;
                this.driverLongitude = 31.2357;

                // Show centered popup dialog
                showRideRequestPopup(pickup.name, destination.name, request.distanceKm, request.estimatedPrice);
            }

        } catch (SQLException e) {
            System.err.println("Error displaying ride request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showRideRequestPopup(String pickupName, String destinationName, double distance, double fare) {
        // Create a new stage for the popup
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.UNDECORATED);
        popupStage.setTitle("New Ride Request");

        // Create popup content with dark theme
        VBox popupContent = new VBox(15);
        popupContent.setAlignment(Pos.CENTER);
        popupContent.setPadding(new Insets(25));
        popupContent.setStyle(
            "-fx-background-color: #1C2333;" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: #30363D;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 20, 0, 0, 4);"
        );

        // Title
        Label titleLabel = new Label("🚗 New Ride Request!");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #3B82F6;");

        // Details container
        VBox detailsBox = new VBox(12);
        detailsBox.setAlignment(Pos.CENTER_LEFT);

        // Pickup
        HBox pickupBox = new HBox(10);
        pickupBox.setAlignment(Pos.CENTER_LEFT);
        Label pickupIcon = new Label("📍");
        pickupIcon.setStyle("-fx-font-size: 18px;");
        VBox pickupInfo = new VBox(2);
        Label pickupTitle = new Label("Pickup");
        pickupTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8B949E;");
        Label pickupValue = new Label(pickupName);
        pickupValue.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #E6EDF3;");
        pickupInfo.getChildren().addAll(pickupTitle, pickupValue);
        pickupBox.getChildren().addAll(pickupIcon, pickupInfo);

        // Destination
        HBox destBox = new HBox(10);
        destBox.setAlignment(Pos.CENTER_LEFT);
        Label destIcon = new Label("📌");
        destIcon.setStyle("-fx-font-size: 18px;");
        VBox destInfo = new VBox(2);
        Label destTitle = new Label("Destination");
        destTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8B949E;");
        Label destValue = new Label(destinationName);
        destValue.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #E6EDF3;");
        destInfo.getChildren().addAll(destTitle, destValue);
        destBox.getChildren().addAll(destIcon, destInfo);

        // Distance and Fare
        HBox statsBox = new HBox(20);
        statsBox.setAlignment(Pos.CENTER);

        VBox distanceBox = new VBox(4);
        distanceBox.setAlignment(Pos.CENTER);
        Label distanceTitle = new Label("Distance");
        distanceTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8B949E;");
        Label distanceValue = new Label(String.format("%.2f km", distance));
        distanceValue.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #E6EDF3;");
        distanceBox.getChildren().addAll(distanceTitle, distanceValue);

        VBox fareBox = new VBox(4);
        fareBox.setAlignment(Pos.CENTER);
        Label fareTitle = new Label("Fare");
        fareTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #8B949E;");
        Label fareValue = new Label(String.format("$%.2f", fare));
        fareValue.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #10B981;");
        fareBox.getChildren().addAll(fareTitle, fareValue);

        statsBox.getChildren().addAll(distanceBox, fareBox);

        detailsBox.getChildren().addAll(pickupBox, destBox, statsBox);

        // Accept button
        Button acceptButton = new Button("Accept Ride");
        acceptButton.setStyle(
            "-fx-background-color: linear-gradient(to bottom, #2EA043, #238636);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 12 24 12 24;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(35, 134, 54, 0.3), 8, 0, 0, 2);"
        );
        acceptButton.setPrefWidth(250);
        acceptButton.setOnAction(e -> {
            popupStage.close();
            acceptRide();
        });

        // Decline button
        Button declineButton = new Button("Decline");
        declineButton.setStyle(
            "-fx-background-color: #161B22;" +
            "-fx-border-color: #30363D;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-text-fill: #E6EDF3;" +
            "-fx-font-size: 13px;" +
            "-fx-padding: 10 20 10 20;" +
            "-fx-cursor: hand;"
        );
        declineButton.setPrefWidth(250);
        declineButton.setOnAction(e -> {
            popupStage.close();
            currentRideRequest = null;
        });

        popupContent.getChildren().addAll(titleLabel, detailsBox, acceptButton, declineButton);

        // Create scene with transparent background
        Scene popupScene = new Scene(popupContent, 350, 400);
        popupScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        popupStage.setScene(popupScene);

        // Center the popup on screen
        popupStage.centerOnScreen();

        // Show popup
        popupStage.show();
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
    }

    private void acceptRide() {
        if (currentRideRequest == null) {
            showAlert("No Ride", "No ride request available.");
            return;
        }

        try {
            // Update ride request in database
            RideRequestDAO rideRequestDAO = new RideRequestDAO();
            Timestamp acceptanceTime = new Timestamp(System.currentTimeMillis());

            int updated = rideRequestDAO.update(
                    currentRideRequest.id,
                    currentDriverId,
                    Status.Accepted,
                    currentRideRequest.distanceKm,
                    currentRideRequest.estimatedTime,
                    currentRideRequest.estimatedPrice,
                    acceptanceTime,
                    false,
                    false
            );

            if (updated > 0) {
                System.out.println("Ride accepted successfully!");
                stopPollingForRides();
                hideRideRequest(); // Hide widget before navigation
                openDriverMap(currentRideRequest.id);
            } else {
                showAlert("Error", "Failed to accept ride. Please try again.");
            }

        } catch (SQLException e) {
            System.err.println("Error accepting ride: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "An error occurred while accepting the ride.");
        }
    }

    private void openDriverMap(long rideId) {
        try {
            System.out.println("Opening Driver Map for ride ID: " + rideId);

            // Load FXML using FXMLLoader
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverMap.fxml"));
            javafx.scene.Parent root = loader.load();

            // Get controller and pass data with coordinates
            DriverMapController controller = loader.getController();
            if (controller != null) {
                // Pass ride details
                controller.setRideDetails(rideId, currentDriver, currentDriverId);

                // Pass coordinates for drawing route
                controller.setLocationCoordinates(
                    driverLatitude,
                    driverLongitude,
                    pickupLatitude,
                    pickupLongitude
                );
            }

            // Get stage and switch scene
            Stage stage = (Stage) driverModeToggle.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

            System.out.println("Driver Map opened successfully");

        } catch (Exception e) {
            System.err.println("Error opening driver map: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to open map screen.");
        }
    }

    @FXML
    private void onLogout() {
        stopPollingForRides();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RoleSelection.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            if (welcomeLabel != null && welcomeLabel.getScene() != null && welcomeLabel.getScene().getWindow() != null) {
                Stage stage = (Stage) welcomeLabel.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            } else {
                System.err.println("ERROR: Cannot navigate - window not found");
            }

        } catch (IOException e) {
            System.err.println("Error logging out: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Cleanup when controller is destroyed
    public void cleanup() {
        stopPollingForRides();
    }
}

