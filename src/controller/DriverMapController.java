package controller;

import DAO.LocationDAO;
import DAO.RideRequestDAO;
import DAO.RideHistoryDAO;
import Model.Driver;
import Model.Status;
import Model.PaymentType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DriverMapController {

    @FXML private WebView mapWebView;
    @FXML private Label pickupLabel;
    @FXML private Label destinationLabel;
    @FXML private Label distanceLabel;
    @FXML private Label fareLabel;
    @FXML private Button startTripButton;
    @FXML private Button completeTripButton;

    private WebEngine webEngine;
    private long rideId;
    private Driver currentDriver;
    private long currentDriverId;
    private RideRequestDAO.RideRequestRow rideRequest;
    private LocationDAO.LocationRow pickupLocation;
    private LocationDAO.LocationRow destinationLocation;

    // Coordinates for drawing route
    private double driverLatitude;
    private double driverLongitude;
    private double pickupLatitude;
    private double pickupLongitude;

    @FXML
    public void initialize() {
        System.out.println("DriverMapController.initialize() called");

        if (mapWebView == null) {
            System.err.println("ERROR: mapWebView is NULL! Check fx:id in FXML.");
            return;
        }

        if (startTripButton == null || completeTripButton == null) {
            System.err.println("ERROR: Button controls are null!");
            return;
        }

        try {
            // Initialize WebEngine
            webEngine = mapWebView.getEngine();

            mapWebView.setVisible(true);
            mapWebView.setPrefSize(390, 400);

            // Hide complete trip button initially
            completeTripButton.setVisible(false);
            completeTripButton.setManaged(false);

            // Setup button actions
            startTripButton.setOnAction(e -> startTrip());
            completeTripButton.setOnAction(e -> completeTrip());

            // Load map.html from resources
            loadMapHtml();

            System.out.println("DriverMapController.initialize() completed successfully");
        } catch (Exception e) {
            System.err.println("ERROR in DriverMapController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMapHtml() {
        try {
            // Just load map.html from resources
            java.net.URL url = getClass().getResource("/map.html");
            if (url != null) {
                webEngine.load(url.toExternalForm());
                System.out.println("Loading map.html from: " + url.toExternalForm());
            } else {
                System.err.println("ERROR: map.html NOT FOUND at /map.html");
            }
        } catch (Exception e) {
            System.err.println("ERROR loading map.html: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setRideDetails(long rideId, Driver driver, long driverId) {
        System.out.println("setRideDetails() called - Ride ID: " + rideId + ", Driver: " +
                          (driver != null ? driver.getName() : "null") + ", Driver ID: " + driverId);

        this.rideId = rideId;
        this.currentDriver = driver;
        this.currentDriverId = driverId;

        try {
            loadRideDetails();
        } catch (Exception e) {
            System.err.println("ERROR in setRideDetails: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Method to receive coordinates from DriverDashboardController
    public void setLocationCoordinates(double driverLat, double driverLng, double pickupLat, double pickupLng) {
        System.out.println("setLocationCoordinates() called:");
        System.out.println("  Driver: (" + driverLat + ", " + driverLng + ")");
        System.out.println("  Pickup: (" + pickupLat + ", " + pickupLng + ")");

        this.driverLatitude = driverLat;
        this.driverLongitude = driverLng;
        this.pickupLatitude = pickupLat;
        this.pickupLongitude = pickupLng;

        // Draw route after coordinates are set
        drawRouteOnMap();
    }

    private void loadRideDetails() {
        try {
            System.out.println("Loading ride details for ride ID: " + rideId);

            // Load ride request
            RideRequestDAO rideRequestDAO = new RideRequestDAO();
            for (RideRequestDAO.RideRequestRow request : rideRequestDAO.showAll()) {
                if (request.id == rideId) {
                    this.rideRequest = request;
                    break;
                }
            }

            if (rideRequest == null) {
                System.err.println("ERROR: Ride request not found for ID: " + rideId);
                showAlert("Error", "Ride request not found.");
                return;
            }

            System.out.println("Ride request found: " + rideRequest.id);

            // Load locations
            LocationDAO locationDAO = new LocationDAO();
            for (LocationDAO.LocationRow loc : locationDAO.showAll()) {
                if (loc.id == rideRequest.originId) {
                    pickupLocation = loc;
                }
                if (loc.id == rideRequest.destinationId) {
                    destinationLocation = loc;
                }
            }

            if (pickupLocation != null && destinationLocation != null) {
                System.out.println("Locations loaded - Pickup: " + pickupLocation.name +
                                 ", Destination: " + destinationLocation.name);

                if (pickupLabel != null) {
                    pickupLabel.setText(pickupLocation.name);
                }
                if (destinationLabel != null) {
                    destinationLabel.setText(destinationLocation.name);
                }
                if (distanceLabel != null) {
                    distanceLabel.setText(String.format("%.2f km", rideRequest.distanceKm));
                }
                if (fareLabel != null) {
                    fareLabel.setText(String.format("$%.2f", rideRequest.estimatedPrice));
                }
            } else {
                System.err.println("ERROR: Could not load locations - Pickup: " +
                                 pickupLocation + ", Destination: " + destinationLocation);
            }

        } catch (SQLException e) {
            System.err.println("Error loading ride details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMapWithRoute() {
        if (pickupLocation == null || destinationLocation == null) {
            System.err.println("ERROR: Pickup or destination location is null");
            return;
        }

        // Schedule the route drawing after a short delay to ensure map.html is fully loaded
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Wait 1 second for map to initialize
                Platform.runLater(() -> {
                    try {
                        // Check if window.mapReady is true
                        Object mapReady = webEngine.executeScript("typeof window.mapReady !== 'undefined' && window.mapReady");
                        if (Boolean.TRUE.equals(mapReady)) {
                            String distanceScript = String.format(
                                "window.calculateDistance(%f, %f, %f, %f);",
                                pickupLocation.latitude, pickupLocation.longitude,
                                destinationLocation.latitude, destinationLocation.longitude
                            );
                            Object distanceResult = webEngine.executeScript(distanceScript);
                            double calculatedDistance = 0;

                            if (distanceResult != null) {
                                if (distanceResult instanceof Number) {
                                    calculatedDistance = ((Number) distanceResult).doubleValue();
                                } else if (distanceResult instanceof String) {
                                    try {
                                        calculatedDistance = Double.parseDouble((String) distanceResult);
                                    } catch (NumberFormatException e) {
                                        System.err.println("Could not parse distance: " + distanceResult);
                                    }
                                }
                                System.out.println("Calculated distance: " + calculatedDistance + " km");

                                // Update distance label with calculated value
                                final double finalDistance = calculatedDistance;
                                Platform.runLater(() -> {
                                    if (distanceLabel != null) {
                                        distanceLabel.setText(String.format("%.2f km", finalDistance));
                                    }
                                });
                            }

                            // Draw the route on the map
                            String routeScript = String.format(
                                "window.drawRoute(%f, %f, %f, %f);",
                                pickupLocation.latitude, pickupLocation.longitude,
                                destinationLocation.latitude, destinationLocation.longitude
                            );
                            webEngine.executeScript(routeScript);
                            System.out.println("Route drawn successfully between " +
                                pickupLocation.name + " and " + destinationLocation.name);
                        } else {
                            System.err.println("ERROR: Map not ready after 1 second delay");
                        }
                    } catch (Exception e) {
                        System.err.println("ERROR executing map script: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // New method to draw route between driver location and pickup location
    private void drawRouteOnMap() {
        System.out.println("drawRouteOnMap() called");

        // Wait for map to be ready, then draw route
        new Thread(() -> {
            try {
                Thread.sleep(1500); // Wait 1.5 seconds for map to initialize
                Platform.runLater(() -> {
                    try {
                        // Check if map is ready
                        Object mapReady = webEngine.executeScript(
                            "typeof window.mapReady !== 'undefined' && window.mapReady"
                        );

                        if (Boolean.TRUE.equals(mapReady)) {
                            System.out.println("Map is ready, drawing route...");

                            // Draw route from driver to pickup using JavaScript
                            String routeScript = String.format(
                                "window.drawRoute(%f, %f, %f, %f);",
                                driverLatitude, driverLongitude,
                                pickupLatitude, pickupLongitude
                            );
                            webEngine.executeScript(routeScript);

                            System.out.println("Route drawn successfully:");
                            System.out.println("  From Driver: (" + driverLatitude + ", " + driverLongitude + ")");
                            System.out.println("  To Pickup: (" + pickupLatitude + ", " + pickupLongitude + ")");

                        } else {
                            System.err.println("ERROR: Map not ready after 1.5 second delay");
                        }
                    } catch (Exception e) {
                        System.err.println("ERROR executing map script: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void startTrip() {
        try {
            // Update ride status to in-progress
            RideRequestDAO rideRequestDAO = new RideRequestDAO();
            // We'll keep status as Accepted, just mark that driver arrived
            rideRequestDAO.update(
                    rideRequest.id,
                    currentDriverId,
                    Status.Accepted,
                    rideRequest.distanceKm,
                    rideRequest.estimatedTime,
                    rideRequest.estimatedPrice,
                    rideRequest.acceptanceTime,
                    true, // driver arrived
                    true  // passenger arrived
            );

            startTripButton.setManaged(false);
            completeTripButton.setVisible(true);
            completeTripButton.setManaged(true);

            showAlert("Trip Started", "The trip has been started. Drive safely!");

        } catch (SQLException e) {
            System.err.println("Error starting trip: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to start trip.");
        }
    }

    private void completeTrip() {
        try {
            // Update ride status to Completed
            RideRequestDAO rideRequestDAO = new RideRequestDAO();
            rideRequestDAO.update(
                    rideRequest.id,
                    currentDriverId,
                    Status.Completed,
                    rideRequest.distanceKm,
                    rideRequest.estimatedTime,
                    rideRequest.estimatedPrice,
                    rideRequest.acceptanceTime,
                    true,
                    true
            );

            // Create ride history entry
            RideHistoryDAO rideHistoryDAO = new RideHistoryDAO();
            rideHistoryDAO.insert(
                    rideRequest.id,
                    currentDriverId,
                    rideRequest.passengerId,
                    0, // passenger rating (not set yet)
                    0, // driver rating (not set yet)
                    rideRequest.estimatedPrice,
                    PaymentType.wallet,
                    0.0, // tips
                    0.0, // donation
                    "" // donation org
            );

            // Update driver wallet balance
            try (Connection con = DBConnection.getConnection()) {
                String updateWallet = "UPDATE drivers SET wallet_balance = wallet_balance + ? WHERE id = ?";
                try (PreparedStatement ps = con.prepareStatement(updateWallet)) {
                    ps.setDouble(1, rideRequest.estimatedPrice * 0.8); // Driver gets 80%
                    ps.setLong(2, currentDriverId);
                    ps.executeUpdate();
                }
            }

            showAlert("Trip Completed", "Great job! The fare has been added to your wallet.");

            // Return to dashboard
            returnToDashboard();

        } catch (SQLException e) {
            System.err.println("Error completing trip: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to complete trip.");
        }
    }

    @FXML
    private void onBackToDashboard() {
        returnToDashboard();
    }

    private void returnToDashboard() {
        try {
            System.out.println("Returning to Driver Dashboard...");

            java.net.URL fxmlUrl = getClass().getResource("/view/DriverDashboard.fxml");
            if (fxmlUrl == null) {
                System.err.println("ERROR: Could not find /view/DriverDashboard.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            javafx.scene.Parent root = loader.load();

            if (root == null) {
                System.err.println("ERROR: Loaded root is null");
                return;
            }

            DriverDashboardController controller = loader.getController();
            if (controller == null) {
                System.err.println("ERROR: DriverDashboardController is null");
                return;
            }

            controller.setDriver(currentDriver);

            Scene scene = new Scene(root, 390, 750);

            if (mapWebView != null && mapWebView.getScene() != null && mapWebView.getScene().getWindow() != null) {
                Stage stage = (Stage) mapWebView.getScene().getWindow();
                stage.setScene(scene);
                stage.show();
                System.out.println("Successfully returned to dashboard");
            } else {
                System.err.println("ERROR: Cannot navigate - window not found");
            }

        } catch (Exception e) {
            System.err.println("Error returning to dashboard: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getName());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}

