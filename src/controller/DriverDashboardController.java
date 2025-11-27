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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DriverDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private ToggleButton driverModeToggle;
    @FXML private Label todayEarningsLabel;
    @FXML private Label ridesTodayLabel;
    @FXML private VBox noRidesContainer;
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

        if (driverModeToggle == null || noRidesContainer == null) {
            System.err.println("ERROR: UI components are null");
            return;
        }

        try {
            // Setup default state
            driverModeToggle.setSelected(false);
            driverModeToggle.setText("Offline");
            if (rideRequestWidget != null) {
                rideRequestWidget.setVisible(false);
                rideRequestWidget.setManaged(false);
            }
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
            if (acceptRideButton != null) {
                acceptRideButton.setOnAction(event -> acceptRide());
            }

            System.out.println("DriverDashboardController.initialize() completed");
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
            }
        } catch (Exception e) {
            System.err.println("Error loading driver ID: " + e.getMessage());
        }

        // Load today's statistics
        loadTodayStatistics();
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
                        todayEarningsLabel.setText(String.format("$%.2f", rs.getDouble("earnings")));
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
        pollTimer = new Timer(true);
        pollTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForPendingRides();
            }
        }, 0, 3000);
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

            if (pendingRides.isEmpty()) {
                Platform.runLater(this::hideRideRequest);
                return;
            }

            // Get nearest ride
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

                rideRequestWidget.setVisible(true);
                rideRequestWidget.setManaged(true);
                noRidesContainer.setVisible(false);
                noRidesContainer.setManaged(false);
            }
        } catch (SQLException e) {
            System.err.println("Error displaying ride request: " + e.getMessage());
        }
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
        if (currentRideRequest == null) return;

        try {
            RideRequestDAO rideRequestDAO = new RideRequestDAO();
            rideRequestDAO.update(
                    currentRideRequest.id,
                    currentDriverId,
                    Status.Accepted,
                    currentRideRequest.distanceKm,
                    currentRideRequest.estimatedTime,
                    currentRideRequest.estimatedPrice,
                    new Timestamp(System.currentTimeMillis()),
                    false,
                    false
            );

            stopPollingForRides();
            hideRideRequest();
            openDriverMap(currentRideRequest.id);
        } catch (SQLException e) {
            System.err.println("Error accepting ride: " + e.getMessage());
        }
    }

    private void openDriverMap(long rideId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverMap.fxml"));
            javafx.scene.Parent root = loader.load();

            DriverMapController controller = loader.getController();
            if (controller != null) {
                controller.setRideDetails(rideId, currentDriver, currentDriverId);
            }

            Stage stage = (Stage) driverModeToggle.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            System.err.println("Error opening driver map: " + e.getMessage());
        }
    }

    @FXML
    private void onProfile() {
        stopPollingForRides();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Profile.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            ProfileController controller = loader.getController();
            if (currentDriver != null) {
                controller.setUser(currentDriver);
            }

            Stage stage = (Stage) driverModeToggle.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Error opening profile: " + e.getMessage());
        }
    }

    @FXML
    private void onAbout() {
        stopPollingForRides();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/About.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            AboutController controller = loader.getController();
            if (currentDriver != null) {
                controller.setUser(currentDriver);
            }

            Stage stage = (Stage) driverModeToggle.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Error opening about: " + e.getMessage());
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
}

