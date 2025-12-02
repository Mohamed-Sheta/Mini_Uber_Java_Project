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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import utils.DBConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

public class DriverRideViewController {

    @FXML private ImageView aboutBtn;
    @FXML private ImageView profileBtn;
    @FXML private WebView mapView;
    @FXML private Label statusBannerLabel;
    @FXML private Label statusSubtitleLabel;
    @FXML private Label pickupLabel;
    @FXML private Label destinationLabel;
    @FXML private Label distanceLabel;
    @FXML private Label fareLabel;

    // Popup elements
    @FXML private StackPane popupOverlay;
    @FXML private VBox popupContainer;
    @FXML private Label popupTitle;
    @FXML private Label popupMessage;
    @FXML private Button popupButton;

    private WebEngine webEngine;
    private long rideId;
    private Driver currentDriver;
    private long currentDriverId;
    private RideRequestDAO.RideRequestRow rideRequest;
    private LocationDAO.LocationRow pickupLocation;
    private LocationDAO.LocationRow destinationLocation;

    private Timer phaseTimer;
    private int currentPhase = 0; // 0=on way, 1=arrived at pickup, 2=driving to destination, 3=completed

    @FXML
    public void initialize() {
        System.out.println("[DriverRideView] initialize() called");

        try {
            // Setup navigation icons
            if (aboutBtn != null) {
                aboutBtn.setOnMouseClicked(e -> navigateToAbout());
            }
            if (profileBtn != null) {
                profileBtn.setOnMouseClicked(e -> navigateToProfile());
            }

            // Initialize WebEngine with enhanced error checking
            if (mapView != null) {
                System.out.println("[DriverRideView] WebView found, initializing...");

                // Ensure WebView is visible and has proper size
                mapView.setVisible(true);
                mapView.setManaged(true);

                // Get WebEngine
                webEngine = mapView.getEngine();
                System.out.println("[DriverRideView] WebEngine initialized: " + (webEngine != null));

                // Enable JavaScript (required for map)
                webEngine.setJavaScriptEnabled(true);

                // Load map HTML
                loadMapHtml();
            } else {
                System.err.println("[DriverRideView] ❌ ERROR: mapView is NULL!");
            }

            // Setup popup overlay opacity
            if (popupOverlay != null) {
                popupOverlay.setOpacity(0.0); // Start hidden
            }
            if (popupContainer != null) {
                popupContainer.setOpacity(1.0); // Ensure content is fully visible
            }

            // Setup popup button
            if (popupButton != null) {
                popupButton.setOnAction(e -> handlePopupAction());
            }

            System.out.println("[DriverRideView] initialize() completed");
        } catch (Exception e) {
            System.err.println("[DriverRideView] ERROR in initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMapHtml() {
        try {
            System.out.println("[DriverRideView] ========== MAP LOADING START ==========");
            System.out.println("[DriverRideView] Attempting to load /map.html from classpath");

            // Get resource URL
            java.net.URL mapUrl = getClass().getResource("/map.html");

            if (mapUrl == null) {
                System.err.println("[DriverRideView] ❌ CRITICAL: map.html NOT FOUND at /map.html");
                System.err.println("[DriverRideView] Make sure map.html is in the resources root folder");
                return;
            }

            System.out.println("[DriverRideView] ✅ map.html found at: " + mapUrl.toExternalForm());
            System.out.println("[DriverRideView] Loading map into WebEngine...");

            // Load the map using the approach requested by the user
            webEngine.load(mapUrl.toExternalForm());

            // Add document listener to check when map is ready
            webEngine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
                if (newDoc != null) {
                    System.out.println("[DriverRideView] Document loaded, checking window.mapReady...");
                    Platform.runLater(() -> {
                        checkMapReadyWithRetry(0);
                    });
                }
            });

            System.out.println("[DriverRideView] ========== MAP LOADING INITIATED ==========");
        } catch (Exception e) {
            System.err.println("[DriverRideView] ❌ EXCEPTION during map loading: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkMapReadyWithRetry(int attempt) {
        try {
            Object ready = webEngine.executeScript("typeof window !== 'undefined' && typeof window.mapReady !== 'undefined' && window.mapReady");
            boolean jsReady = (ready instanceof Boolean && (Boolean) ready);

            if (jsReady) {
                System.out.println("[DriverRideView] ✅ Map ready = true (attempt " + (attempt + 1) + ")");
            } else {
                System.out.println("[DriverRideView] Map not ready yet (attempt " + (attempt + 1) + "/10)");
                if (attempt < 10) {
                    new Timer(true).schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> checkMapReadyWithRetry(attempt + 1));
                        }
                    }, 500);
                } else {
                    System.err.println("[DriverRideView] ❌ Map failed to become ready after 10 attempts");
                }
            }
        } catch (Exception ex) {
            System.err.println("[DriverRideView] Error checking map ready (attempt " + (attempt + 1) + "): " + ex.getMessage());
            if (attempt < 10) {
                new Timer(true).schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> checkMapReadyWithRetry(attempt + 1));
                    }
                }, 500);
            }
        }
    }

    public void setRideDetails(long rideId, Driver driver, long driverId) {
        System.out.println("[DriverRideView] ========== SET RIDE DETAILS ==========");
        System.out.println("[DriverRideView] Ride ID: " + rideId);
        System.out.println("[DriverRideView] Driver: " + (driver != null ? driver.getName() : "null"));
        System.out.println("[DriverRideView] Driver ID: " + driverId);

        this.rideId = rideId;
        this.currentDriver = driver;
        this.currentDriverId = driverId;

        try {
            loadRideDetails();
            startRidePhases();
        } catch (Exception e) {
            System.err.println("[DriverRideView] ❌ ERROR in setRideDetails: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadRideDetails() {
        try {
            System.out.println("[DriverRideView] Loading ride details for ride ID: " + rideId);

            // Load ride request
            RideRequestDAO rideRequestDAO = new RideRequestDAO();
            for (RideRequestDAO.RideRequestRow request : rideRequestDAO.showAll()) {
                if (request.id == rideId) {
                    this.rideRequest = request;
                    break;
                }
            }

            if (rideRequest == null) {
                System.err.println("[DriverRideView] ERROR: Ride request not found for ID: " + rideId);
                return;
            }

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
                final LocationDAO.LocationRow finalPickup = pickupLocation;
                final LocationDAO.LocationRow finalDestination = destinationLocation;

                Platform.runLater(() -> {
                    pickupLabel.setText(finalPickup.name);
                    destinationLabel.setText(finalDestination.name);
                    distanceLabel.setText(String.format("%.2f km", rideRequest.distanceKm));
                    fareLabel.setText(String.format("$%.2f", rideRequest.estimatedPrice));

                    // Draw route on map after a short delay to ensure map is loaded
                    new Timer(true).schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> drawRouteOnMap(finalPickup, finalDestination));
                        }
                    }, 1500); // Wait 1.5 seconds for map to be ready
                });
            }

        } catch (SQLException e) {
            System.err.println("[DriverRideView] Error loading ride details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawRouteOnMap(LocationDAO.LocationRow pickup, LocationDAO.LocationRow destination) {
        drawRouteOnMapWithRetry(pickup, destination, 0);
    }

    private void drawRouteOnMapWithRetry(LocationDAO.LocationRow pickup, LocationDAO.LocationRow destination, int attemptCount) {
        try {
            if (webEngine == null) {
                System.err.println("[DriverRideView] ❌ Cannot draw route - webEngine is null");
                return;
            }

            System.out.println("[DriverRideView] Drawing route attempt " + (attemptCount + 1) + "/5");

            // Check if map is ready - poll window.mapReady like MapController
            Object mapReady = null;
            try {
                mapReady = webEngine.executeScript("typeof window !== 'undefined' && typeof window.mapReady !== 'undefined' && window.mapReady");
            } catch (Exception scriptEx) {
                System.err.println("[DriverRideView] Error checking mapReady: " + scriptEx.getMessage());
            }

            if (!Boolean.TRUE.equals(mapReady)) {
                if (attemptCount < 5) {  // Max 5 retries
                    System.out.println("[DriverRideView] Map not ready, retry in 1 second...");
                    new Timer(true).schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> drawRouteOnMapWithRetry(pickup, destination, attemptCount + 1));
                        }
                    }, 1000);
                } else {
                    System.err.println("[DriverRideView] ❌ Map failed to become ready after 5 retries");
                }
                return;
            }

            // Map is ready - draw route with function signature: drawRoute(lat1, lng1, lat2, lng2)
            String script = String.format(
                "drawRoute(%f, %f, %f, %f);",
                pickup.latitude, pickup.longitude,
                destination.latitude, destination.longitude
            );

            System.out.println("[DriverRideView] Executing: " + script);

            try {
                webEngine.executeScript(script);
                System.out.println("[DriverRideView] ✅ Route drawn on map from " + pickup.name + " to " + destination.name);
            } catch (Exception jsEx) {
                System.err.println("[DriverRideView] ❌ JS execution error: " + jsEx.getMessage());
                if (attemptCount < 5) {
                    System.out.println("[DriverRideView] Retrying due to JS error...");
                    new Timer(true).schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Platform.runLater(() -> drawRouteOnMapWithRetry(pickup, destination, attemptCount + 1));
                        }
                    }, 1000);
                }
            }

        } catch (Exception e) {
            System.err.println("[DriverRideView] ❌ Error in drawRouteOnMapWithRetry: " + e.getMessage());
            e.printStackTrace();
            if (attemptCount < 5) {
                new Timer(true).schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> drawRouteOnMapWithRetry(pickup, destination, attemptCount + 1));
                    }
                }, 1000);
            }
        }
    }

    /**
     * Start the automatic ride phases
     * Phase 1: On way to pickup (3-5 seconds)
     * Phase 2: Arrived at pickup (show popup)
     * Phase 3: Driving to destination (3-5 seconds after start ride)
     * Phase 4: Ride completed (show popup with rating)
     */
    private void startRidePhases() {
        System.out.println("[DriverRideView] Starting ride phases...");

        currentPhase = 0;

        // Phase 1: On way to pickup (wait 3-5 seconds randomly)
        int delayToPickup = 3000 + (int)(Math.random() * 2000); // 3000-5000ms
        System.out.println("[DriverRideView] Will arrive at pickup in " + (delayToPickup/1000.0) + " seconds");

        phaseTimer = new Timer(true);
        phaseTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> showArrivedAtPickupPopup());
            }
        }, delayToPickup);
    }

    private void showArrivedAtPickupPopup() {
        System.out.println("[DriverRideView] Phase 1 complete - Showing arrived at pickup popup");

        currentPhase = 1;

        // Update banner
        statusBannerLabel.setText("Arrived at Pickup");
        statusBannerLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 600;");
        statusSubtitleLabel.setText("Waiting for passenger...");

        // Show popup
        popupTitle.setText("Arrival Notice");
        popupMessage.setText("Driver has arrived at passenger pickup point");
        popupButton.setText("Start Ride");
        popupOverlay.setOpacity(0.7); // Set overlay opacity
        popupOverlay.setVisible(true);
        popupOverlay.setManaged(true);
    }

    private void handlePopupAction() {
        System.out.println("[DriverRideView] Popup button clicked - Phase: " + currentPhase);

        if (currentPhase == 1) {
            // Continue from pickup → start driving to destination
            hidePopup();
            startDrivingToDestination();
        } else if (currentPhase == 3) {
            // Done from ride completed → navigate to rating
            hidePopup();
            navigateToRating();
        }
    }

    private void hidePopup() {
        popupOverlay.setVisible(false);
        popupOverlay.setManaged(false);
    }

    private void startDrivingToDestination() {
        System.out.println("[DriverRideView] Phase 2 - Driving to destination");

        currentPhase = 2;

        // Update banner
        statusBannerLabel.setText("Driving to Destination");
        statusBannerLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 600;");
        statusSubtitleLabel.setText("Passenger is in the car");

        // Update ride status in DB
        try {
            RideRequestDAO rideRequestDAO = new RideRequestDAO();
            rideRequestDAO.update(
                rideRequest.id,
                currentDriverId,
                Status.Accepted,
                rideRequest.distanceKm,
                rideRequest.estimatedTime,
                rideRequest.estimatedPrice,
                rideRequest.acceptanceTime,
                true, // driver arrived
                true  // passenger arrived (both in car)
            );
            System.out.println("[DriverRideView] ✅ Ride status updated: both arrived");
        } catch (SQLException e) {
            System.err.println("[DriverRideView] Error updating ride status: " + e.getMessage());
        }

        // Wait 3-5 seconds then show ride completed (randomly)
        int delayToDestination = 3000 + (int)(Math.random() * 2000); // 3000-5000ms
        System.out.println("[DriverRideView] Will arrive at destination in " + (delayToDestination/1000.0) + " seconds");

        phaseTimer = new Timer(true);
        phaseTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> showRideCompletedPopup());
            }
        }, delayToDestination);
    }

    private void showRideCompletedPopup() {
        System.out.println("[DriverRideView] Phase 3 complete - Showing ride completed popup");

        currentPhase = 3;

        // Update banner
        statusBannerLabel.setText("Ride Completed!");
        statusBannerLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 600;");
        statusSubtitleLabel.setText("Trip finished successfully");

        // Update ride status to completed
        try {
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
            System.out.println("[DriverRideView] ✅ Ride status updated to Completed");
        } catch (SQLException e) {
            System.err.println("[DriverRideView] Error updating ride status to completed: " + e.getMessage());
        }

        // Show completion popup
        popupTitle.setText("Ride Completed");
        popupMessage.setText("Ride Completed — Please rate the passenger");
        popupButton.setText("Rate Passenger");
        popupOverlay.setOpacity(0.7); // Set overlay opacity
        popupOverlay.setVisible(true);
        popupOverlay.setManaged(true);
    }

    private void navigateToRating() {
        System.out.println("[DriverRideView] Showing embedded rating panel...");

        try {
            // Load rating panel content
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverRatingPassengerDialog.fxml"));
            javafx.scene.Parent ratingRoot = loader.load();

            DriverRatingPassengerDialogController ratingController = loader.getController();

            // Set callback to receive rating
            ratingController.setRatingCallback((passengerRating) -> {
                System.out.println("[DriverRideView] Driver rated passenger: " + passengerRating + " stars");
                finalizeRideWithRating(passengerRating);
            });

            // Show rating panel inside popup overlay (reuse existing popup mechanism)
            popupContainer.getChildren().clear();
            popupContainer.getChildren().add(ratingRoot);
            popupOverlay.setOpacity(0.7); // Set overlay opacity
            popupOverlay.setVisible(true);
            popupOverlay.setManaged(true);

            System.out.println("[DriverRideView] ✅ Rating panel displayed in-app");

        } catch (Exception e) {
            System.err.println("[DriverRideView] Error showing rating panel: " + e.getMessage());
            e.printStackTrace();
            // If rating fails, finalize with no rating
            finalizeRideWithRating(0);
        }
    }

    private void finalizeRideWithRating(int passengerRating) {
        try {
            // Hide the rating panel overlay
            hidePopup();

            System.out.println("[DriverRideView] ===== FINALIZE RIDE START =====");
            System.out.println("[DriverRideView] Ride ID: " + rideRequest.id);
            System.out.println("[DriverRideView] Driver ID: " + currentDriverId);
            System.out.println("[DriverRideView] Passenger ID: " + (rideRequest.passengerId == 0 ? "NULL (auto-generated)" : rideRequest.passengerId));
            System.out.println("[DriverRideView] Passenger Rating: " + passengerRating);
            System.out.println("[DriverRideView] Ride Cost: $" + String.format("%.2f", rideRequest.estimatedPrice));

            // Create ride history entry (SINGLE entry)
            RideHistoryDAO rideHistoryDAO = new RideHistoryDAO();

            // Handle passenger_id = 0 for auto-generated requests (use 1 as placeholder)
            long passengerId = rideRequest.passengerId == 0 ? 1L : rideRequest.passengerId;

            System.out.println("[DriverRideView] Inserting into ride_history...");
            long historyId = rideHistoryDAO.insert(
                    rideRequest.id,
                    currentDriverId,
                    passengerId,
                    passengerRating, // Driver's rating of passenger
                    0, // Driver rating (passenger will rate driver separately)
                    rideRequest.estimatedPrice,
                    PaymentType.wallet,
                    0.0, // tips (already processed by passenger)
                    0.0, // donation (already processed by passenger)
                    "" // donation org
            );

            System.out.println("[DriverRideView] ✅ Ride history inserted, ID: " + historyId);

            // DON'T DELETE the ride_request - it's referenced by ride_history foreign key
            // The ride_request stays in the database with status='Completed' for reference
            // This maintains data integrity and allows historical tracking
            System.out.println("[DriverRideView] ✅ Ride request kept in database (status=Completed) for historical reference");

            // ===== CRITICAL: UPDATE DRIVER BALANCE (CORRECT FLOW) =====
            System.out.println("[DriverRideView] ===== UPDATING DRIVER BALANCE =====");

            // Validate driver ID
            if (currentDriverId <= 0) {
                System.err.println("[DriverRideView] ❌ CRITICAL ERROR: Invalid driver ID: " + currentDriverId);
                System.err.println("[DriverRideView] ❌ Balance update SKIPPED due to invalid driver ID!");
            } else {
                System.out.println("[DriverRideView] ✅ Driver ID validated: " + currentDriverId);

                // Driver gets 92% after 8% company cut
                double rideFare = rideRequest.estimatedPrice;
                double driverEarnings = rideFare * 0.92;

                System.out.println("[DriverRideView] Ride fare: $" + String.format("%.2f", rideFare));
                System.out.println("[DriverRideView] Driver earnings (92%%): $" + String.format("%.2f", driverEarnings));

                DAO.DriverDAO driverDAO = new DAO.DriverDAO();

                // Step 1: Fetch current balance from database
                System.out.println("[DriverRideView] [Step 1] Fetching current balance from database...");
                double currentBalance = driverDAO.getDriverBalance(currentDriverId);

                if (currentBalance < 0) {
                    System.err.println("[DriverRideView] ❌ ERROR: Failed to fetch current balance from database!");
                    System.err.println("[DriverRideView] ❌ getDriverBalance() returned: " + currentBalance);
                    System.err.println("[DriverRideView] ❌ This indicates a database error. Check connection and table structure.");
                    currentBalance = 0; // Fallback to 0 if fetch fails
                } else {
                    System.out.println("[DriverRideView] ✅ Current balance from DB: $" + String.format("%.2f", currentBalance));
                }

                // Step 2: Calculate new balance
                double newBalance = currentBalance + driverEarnings;
                System.out.println("[DriverRideView] [Step 2] Calculated new balance: $" + String.format("%.2f", newBalance));
                System.out.println("[DriverRideView]          Formula: $" + String.format("%.2f", currentBalance) +
                                 " (old) + $" + String.format("%.2f", driverEarnings) + " (earnings) = $" +
                                 String.format("%.2f", newBalance) + " (new)");

                // Step 3: Update database with new balance
                System.out.println("[DriverRideView] [Step 3] Updating database with new balance...");
                boolean balanceUpdated = driverDAO.updateDriverBalance(currentDriverId, newBalance);

                if (balanceUpdated) {
                    System.out.println("[DriverRideView] ✅✅✅ BALANCE UPDATE SUCCESSFUL! ✅✅✅");
                    System.out.println("[DriverRideView] ✅ Balance increased from $" +
                                     String.format("%.2f", currentBalance) + " to $" +
                                     String.format("%.2f", newBalance));
                    System.out.println("[DriverRideView] ✅ Database row updated successfully!");
                } else {
                    System.err.println("[DriverRideView] ❌❌❌ BALANCE UPDATE FAILED! ❌❌❌");
                    System.err.println("[DriverRideView] ❌ updateDriverBalance() returned false");
                    System.err.println("[DriverRideView] ❌ No rows were updated in the database!");
                    System.err.println("[DriverRideView] ❌ Possible causes:");
                    System.err.println("[DriverRideView]    1. Driver ID doesn't exist in database");
                    System.err.println("[DriverRideView]    2. Database connection issue");
                    System.err.println("[DriverRideView]    3. Insufficient permissions");
                    System.err.println("[DriverRideView]    4. Table/column name mismatch");
                }
            }
            System.out.println("[DriverRideView] ===== BALANCE UPDATE COMPLETE =====");

            // Note: rides_count column removed from database - ride count is calculated from ride_history
            // No need to update a separate counter field

            // Create DriverDAO instance for subsequent operations
            DAO.DriverDAO driverDAO = new DAO.DriverDAO();

            // Set driver back to ONLINE mode (active = true)
            System.out.println("[DriverRideView] Setting driver back to ONLINE mode...");
            boolean onlineSuccess = driverDAO.setDriverOnline(currentDriverId);
            if (onlineSuccess) {
                System.out.println("[DriverRideView] ✅ Driver set to ONLINE (active=true)");
            } else {
                System.err.println("[DriverRideView] ⚠️ Failed to set driver to ONLINE");
            }

            // Reload driver data from database to get updated wallet and stats
            System.out.println("[DriverRideView] Reloading driver data from database...");
            Driver updatedDriver = driverDAO.getDriverById(currentDriverId);
            if (updatedDriver != null) {
                this.currentDriver = updatedDriver;
                System.out.println("[DriverRideView] ✅ Driver data reloaded from DB: wallet=" +
                                  String.format("%.2f", updatedDriver.getWalletBalance()));
            } else {
                System.err.println("[DriverRideView] ⚠️ Failed to reload driver data, trying by email...");
                updatedDriver = driverDAO.getByEmail(currentDriver.getEmail());
                if (updatedDriver != null) {
                    this.currentDriver = updatedDriver;
                    System.out.println("[DriverRideView] ✅ Driver data reloaded by email: wallet=" +
                                      String.format("%.2f", updatedDriver.getWalletBalance()));
                }
            }

            System.out.println("[DriverRideView] ===== FINALIZE RIDE COMPLETE =====");

            // Return to dashboard with updated driver data
            Platform.runLater(this::returnToDashboard);

        } catch (SQLException e) {
            System.err.println("[DriverRideView] ❌ SQL ERROR during ride finalization: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(this::returnToDashboard);
        } catch (Exception e) {
            System.err.println("[DriverRideView] ❌ UNEXPECTED ERROR during ride finalization: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(this::returnToDashboard);
        }
    }

    private void returnToDashboard() {
        try {
            System.out.println("[DriverRideView] Returning to Driver Dashboard after ride completion...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverDashboard.fxml"));
            javafx.scene.Parent root = loader.load();

            DriverDashboardController controller = loader.getController();
            // Pass the updated driver data with flag indicating we're coming from ride completion
            // This triggers the 3-second delay before the next ride request
            controller.setDriver(currentDriver, true); // true = coming from ride completion

            Scene scene = new Scene(root, 390, 750);
            Stage stage = (Stage) mapView.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

            System.out.println("[DriverRideView] ✅ Successfully returned to dashboard (with 3-second delay for next ride)");
        } catch (Exception e) {
            System.err.println("[DriverRideView] Error returning to dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToAbout() {
        System.out.println("[DriverRideView] Navigating to About page");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/About.fxml"));
            javafx.scene.Parent root = loader.load();

            AboutController controller = loader.getController();
            if (controller != null && currentDriver != null) {
                controller.setUser(currentDriver);
            }

            Scene scene = new Scene(root, 390, 750);
            Stage stage = (Stage) aboutBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            System.err.println("[DriverRideView] Failed to load About screen: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void navigateToProfile() {
        System.out.println("[DriverRideView] Navigating to Driver Profile page");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Profile.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            ProfileController controller = loader.getController();
            if (currentDriver != null) {
                controller.setUser(currentDriver);
                controller.refreshProfile();
            }

            Stage stage = (Stage) profileBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            System.err.println("[DriverRideView] Failed to load Profile screen: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

