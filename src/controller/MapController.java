package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import Model.Location;
import Model.Passenger;
import Model.Driver;
import Model.Person;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import services.*;
import DAO.*;
import Model.*;
import javafx.concurrent.Task;

import java.sql.*;
import java.util.*;

import java.io.IOException;


public class MapController {

    @FXML private WebView webView;

    @FXML private ComboBox<Location> startCombo;
    @FXML private ComboBox<Location> endCombo;

    @FXML private Button findBtn;
    @FXML private Button clearBtn;
    @FXML private Button toggleBtn;

    @FXML private VBox bottomPanel;

    @FXML private ImageView settingsBtn;
    @FXML private ImageView profileBtn;
    @FXML private Button chatBtn;

    @FXML private StackPane rootContainer;

    // Chat panel UI elements
    @FXML private VBox chatPanel;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesContainer;
    @FXML private VBox passengerQuickMsgBox;
    @FXML private Button closeChatBtn;

    private WebEngine engine;
    private boolean jsReady = false;

    private boolean panelVisible = true;
    private final double panelHeight = 280;
    private final double visibleStrip = 50;

    private Label errorToast = null;

    // User management
    private Person currentUser;
    private boolean isDriver = false;

    // Ride workflow management
    private RideManager rideManager;
    private MapGraph mapGraph;
    private Payment paymentProcessor;
    private Request currentRequest;
    private List<Driver> availableDrivers = new ArrayList<>();
    private Map<Passenger, Long> passengerIdMap = new HashMap<>();
    private Map<Driver, Long> driverIdMap = new HashMap<>();
    private DriverDAO driverDAO = new DriverDAO();
    private PassengerDAO passengerDAO = new PassengerDAO();
    private boolean rideFinalized = false; // Prevent duplicate finalization
    private boolean isCancelled = false; // Track if ride has been cancelled
    private boolean isOnboard = false; // Track if passenger is onboard
    private DriverAssignedDialogController activeDialogController = null; // Reference to active dialog
    private Task<Void> activeRideTask = null; // Reference to active ride task


    @FXML
    public void initialize() {

        engine = webView.getEngine();
        engine.load(getClass().getResource("/map.html").toExternalForm());

        engine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
            if (newDoc != null) {

                Platform.runLater(() -> {
                    try {
                        Object ready = engine.executeScript("window.mapReady");

                        jsReady = (ready instanceof Boolean && (Boolean) ready);

                        System.out.println("JS READY = " + jsReady);

                    } catch (Exception ex) {
                        System.out.println("JS INIT ERROR: " + ex.getMessage());
                    }
                });
            }
        });

        loadLocations();
        setupButtons();
        loadProfileImage();
    }


    private void loadLocations() {
        // Load all predefined locations from LocationDAO
        // This ensures passengers can select from all 10 locations
        LocationDAO locationDAO = new LocationDAO();
        List<Location> predefinedLocations = locationDAO.getPredefinedLocations();

        startCombo.getItems().clear();
        startCombo.getItems().addAll(predefinedLocations);

        endCombo.getItems().clear();
        endCombo.getItems().addAll(predefinedLocations);

        System.out.println("Loaded " + predefinedLocations.size() + " locations for passenger selection");
    }


    private void setupButtons() {

        findBtn.setOnAction(e -> requestRoute());
        clearBtn.setOnAction(e -> clearRoute());
        toggleBtn.setOnAction(e -> togglePanel());

        profileBtn.setOnMouseClicked(e -> {
            navigateToProfile();
        });

        settingsBtn.setOnMouseClicked(e -> {
            navigateToAbout();
        });
    }

    // User management methods
    public void setPassenger(Passenger passenger) {
        this.currentUser = passenger;
        this.isDriver = false;
        System.out.println("MapView initialized for Passenger: " + passenger.getName());
        loadProfileImage();
    }

    public void setDriver(Driver driver) {
        this.currentUser = driver;
        this.isDriver = true;
        System.out.println("MapView initialized for Driver: " + driver.getName());

        // Ensure this driver is active in the system
        ensureDriverIsActive(driver);

        loadProfileImage();
    }

    private void ensureDriverIsActive(Driver driver) {
        // Make sure the driver is active and available for requests
        try {
            List<DriverDAO.DriverRow> drivers = driverDAO.showAll();
            for (DriverDAO.DriverRow row : drivers) {
                if (row.email.equals(driver.getEmail())) {
                    // If driver is not active, update them to be active
                    if (!row.active) {
                        // The driver object is already active=true from model
                        // Just update in database using existing DAO method
                        driverDAO.update(
                            row.id,
                            driver,
                            driver.getCurrentLocation() != null ? driver.getCurrentLocation().getName() : "Downtown Cairo"
                        );
                        System.out.println("Driver activated: " + driver.getName());
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error ensuring driver is active: " + e.getMessage());
        }
    }

    private void navigateToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Profile.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            // Pass user data to profile controller
            ProfileController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
                // Always refresh profile data when navigating from map
                // This ensures updated wallet, rides count, and ratings are displayed
                controller.refreshProfile();
            }

            Stage stage = (Stage) profileBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            System.err.println("Failed to load Profile screen: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void navigateToAbout() {
        System.out.println("=== navigateToAbout() called ===");
        try {
            // Try multiple paths to locate About.fxml
            java.net.URL fxmlUrl = getClass().getResource("/view/About.fxml");
            System.out.println("Trying /view/About.fxml: " + fxmlUrl);
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getClassLoader().getResource("view/About.fxml");
                System.out.println("Trying view/About.fxml: " + fxmlUrl);
            }
            if (fxmlUrl == null) {
                // Try from resources root
                fxmlUrl = getClass().getResource("/About.fxml");
                System.out.println("Trying /About.fxml: " + fxmlUrl);
            }
            if (fxmlUrl == null) {
                System.err.println("ERROR: Could not find About.fxml in any location");
                System.err.println("Tried: /view/About.fxml, view/About.fxml, /About.fxml");
                return;
            }

            System.out.println("Loading About.fxml from: " + fxmlUrl);
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            javafx.scene.Parent root = loader.load();
            System.out.println("FXML loaded successfully, root: " + root);

            Scene scene = new Scene(root, 390, 750);
            System.out.println("Scene created: " + scene);

            // Pass user data to About screen
            AboutController controller = loader.getController();
            System.out.println("Controller: " + controller);
            if (currentUser != null) {
                controller.setUser(currentUser);
                System.out.println("User data passed to About controller");
            }

            Stage stage = (Stage) settingsBtn.getScene().getWindow();
            System.out.println("Current stage: " + stage);

            // Force scene switch
            stage.setScene(scene);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();
            stage.toFront();

            System.out.println("About screen loaded successfully - Scene should be visible now!");
        } catch (IOException ex) {
            System.err.println("IOException while loading About screen: " + ex.getMessage());
            ex.printStackTrace();
        } catch (Exception ex) {
            System.err.println("Unexpected error loading About: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    private void requestRoute() {

        if (!jsReady) {
            System.out.println("❌ JS NOT READY — WAIT!");
            return;
        }

        Location A = startCombo.getValue();
        Location B = endCombo.getValue();

        if (A == null || B == null) {
            showError("⚠ Please select both origin and destination");
            return;
        }

        // Validate that origin and destination are different
        if (isSameLocation(A, B)) {
            showError("❌ Origin and destination cannot be the same");
            return;
        }

        // Draw route on map
        String js = String.format(
                "window.drawRoute(%f,%f,%f,%f)",
                A.getLatitude(), A.getLongitude(),
                B.getLatitude(), B.getLongitude()
        );

        Platform.runLater(() -> {
            try {
                engine.executeScript(js);
            } catch (Exception ex) {
                System.out.println("JS Route Error: " + ex.getMessage());
            }
        });

        // If user is a passenger, start ride workflow
        if (!isDriver && currentUser instanceof Passenger) {
            startPassengerRideWorkflow(A, B);
        } else if (isDriver) {
            showSuccess("✓ Route displayed. Driver mode active.");
        } else {
            showSuccess("✓ Route displayed successfully!");
        }
    }


    private void clearRoute() {
        if (!jsReady) return;

        Platform.runLater(() -> {
            engine.executeScript("window.clearRoute()");
        });
    }


    private void togglePanel() {

        panelVisible = !panelVisible;

        double targetY = panelVisible ? 0 : (panelHeight - visibleStrip);

        TranslateTransition tt =
                new TranslateTransition(Duration.millis(350), bottomPanel);

        tt.setToY(targetY);
        tt.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        tt.play();

        toggleBtn.setText(panelVisible ? "Hide" : "Show");


        Platform.runLater(() ->
                engine.executeScript("window.dispatchEvent(new Event('resize'))")
        );
    }


    // ==================== HELPER FUNCTIONS ====================

    /**
     * Check if two locations are the same
     */
    private boolean isSameLocation(Location a, Location b) {
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * Show a modern toast/snackbar error message
     * Dismissible on click - position adapts to panel state
     */
    private void showError(String message) {
        // Remove existing error toast if any
        if (errorToast != null && errorToast.getParent() != null) {
            ((StackPane) errorToast.getParent()).getChildren().remove(errorToast);
        }

        // Create error toast
        errorToast = new Label(message);
        errorToast.setStyle(
                "-fx-background-color: #E74C3C; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 14 22; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4); " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: rgba(255,255,255,0.3); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 12;"
        );
        errorToast.setMaxWidth(360);
        errorToast.setWrapText(true);

        // Position at top of page
        StackPane.setAlignment(errorToast, Pos.TOP_CENTER);
        StackPane.setMargin(errorToast, new Insets(70, 0, 0, 0));

        // Add to root container
        if (rootContainer != null) {
            rootContainer.getChildren().add(errorToast);

            // Fade in + slide up animation
            errorToast.setOpacity(0);
            errorToast.setTranslateY(20);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(350), errorToast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slideUp = new TranslateTransition(Duration.millis(350), errorToast);
            slideUp.setFromY(20);
            slideUp.setToY(0);
            slideUp.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

            fadeIn.play();
            slideUp.play();

            // Click to dismiss with feedback
            final Label toastRef = errorToast;
            errorToast.setOnMouseClicked(e -> dismissError(toastRef));

            // Auto-dismiss after 4 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(4000);
                    Platform.runLater(() -> dismissError(toastRef));
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }).start();

            // Hover effect for better interactivity
            errorToast.setOnMouseEntered(e -> {
                errorToast.setStyle(
                        "-fx-background-color: #C0392B; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 14px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 14 22; " +
                                "-fx-background-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 5); " +
                                "-fx-cursor: hand; " +
                                "-fx-border-color: rgba(255,255,255,0.5); " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 12;"
                );
            });

            errorToast.setOnMouseExited(e -> {
                errorToast.setStyle(
                        "-fx-background-color: #E74C3C; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 14px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 14 22; " +
                                "-fx-background-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4); " +
                                "-fx-cursor: hand; " +
                                "-fx-border-color: rgba(255,255,255,0.3); " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 12;"
                );
            });
        }
    }

    /**
     * Show a modern toast/snackbar success message
     * Dismissible on click - position adapts to panel state
     */
    private void showSuccess(String message) {
        // Remove existing error toast if any
        if (errorToast != null && errorToast.getParent() != null) {
            ((StackPane) errorToast.getParent()).getChildren().remove(errorToast);
        }

        // Create success toast
        errorToast = new Label(message);
        errorToast.setStyle(
                "-fx-background-color: #27AE60; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 14 22; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4); " +
                        "-fx-cursor: hand; " +
                        "-fx-border-color: rgba(255,255,255,0.3); " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 12;"
        );
        errorToast.setMaxWidth(360);
        errorToast.setWrapText(true);

        // Position at top of page
        StackPane.setAlignment(errorToast, Pos.TOP_CENTER);
        StackPane.setMargin(errorToast, new Insets(70, 0, 0, 0));

        // Add to root container
        if (rootContainer != null) {
            rootContainer.getChildren().add(errorToast);

            // Fade in + slide up animation
            errorToast.setOpacity(0);
            errorToast.setTranslateY(20);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(350), errorToast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slideUp = new TranslateTransition(Duration.millis(350), errorToast);
            slideUp.setFromY(20);
            slideUp.setToY(0);
            slideUp.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

            fadeIn.play();
            slideUp.play();

            // Click to dismiss with feedback
            final Label toastRef = errorToast;
            errorToast.setOnMouseClicked(e -> dismissError(toastRef));

            // Auto-dismiss after 3 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> dismissError(toastRef));
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }).start();

            // Hover effect for better interactivity
            errorToast.setOnMouseEntered(e -> {
                errorToast.setStyle(
                        "-fx-background-color: #229954; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 14px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 14 22; " +
                                "-fx-background-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 5); " +
                                "-fx-cursor: hand; " +
                                "-fx-border-color: rgba(255,255,255,0.5); " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 12;"
                );
            });

            errorToast.setOnMouseExited(e -> {
                errorToast.setStyle(
                        "-fx-background-color: #27AE60; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 14px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 14 22; " +
                                "-fx-background-radius: 12; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4); " +
                                "-fx-cursor: hand; " +
                                "-fx-border-color: rgba(255,255,255,0.3); " +
                                "-fx-border-width: 1; " +
                                "-fx-border-radius: 12;"
                );
            });
        }
    }

    /**
     * Dismiss the error toast with fade and scale animation
     */
    private void dismissError(Label toast) {
        if (toast != null && toast.getParent() != null) {
            // Scale down animation
            javafx.animation.ScaleTransition scaleOut = new javafx.animation.ScaleTransition(Duration.millis(250), toast);
            scaleOut.setFromX(1.0);
            scaleOut.setFromY(1.0);
            scaleOut.setToX(0.8);
            scaleOut.setToY(0.8);

            // Fade out animation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(250), toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            fadeOut.setOnFinished(e -> {
                if (toast.getParent() instanceof StackPane) {
                    ((StackPane) toast.getParent()).getChildren().remove(toast);
                }
            });

            scaleOut.play();
            fadeOut.play();
        }
    }

    // ==================== PASSENGER RIDE WORKFLOW ====================

    /**
     * Inner class to hold ride validation results
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private void startPassengerRideWorkflow(Location origin, Location destination) {
        Passenger passenger = (Passenger) currentUser;

        isCancelled = false;
        isOnboard = false;
        System.out.println("[WORKFLOW] Starting new ride - flags reset (rideFinalized, isCancelled, isOnboard)");
        rideFinalized = false;
        System.out.println("[WORKFLOW] Starting new ride - rideFinalized flag reset");

        showSuccess("🔄 Searching for drivers...");

        // Show loading state for 2-4 seconds (longer search delay)
        Task<ValidationResult> requestTask = new Task<ValidationResult>() {
            @Override
            protected ValidationResult call() {
                try {
                    // Initialize MapGraph if not already done
                    if (mapGraph == null) {
                        // Build MapGraph in memory - match database locations
                        mapGraph = new MapGraph();

                        // Load locations from database to get correct IDs
                        LocationDAO locationDAO = new LocationDAO();
                        java.util.List<LocationDAO.LocationRow> dbLocations = locationDAO.showAll();

                        // Create location map with database IDs
                        java.util.Map<String, Location> locationMap = new java.util.HashMap<>();
                        java.util.Map<Integer, Location> locationIdMap = new java.util.HashMap<>();
                        for (LocationDAO.LocationRow row : dbLocations) {
                            Location loc = new Location(row.name, row.latitude, row.longitude);
                            loc.setId(row.id); // Set database ID
                            locationMap.put(row.name, loc);
                            locationIdMap.put(row.id, loc);
                            mapGraph.addLocation(loc);
                        }

                        // Load edges from database using location IDs
                        EdgeDAO edgeDAO = new EdgeDAO();
                        java.util.List<EdgeDAO.EdgeRow> dbEdges = edgeDAO.showAll();
                        for (EdgeDAO.EdgeRow edge : dbEdges) {
                            Location from = locationIdMap.get(edge.fromId);
                            Location to = locationIdMap.get(edge.toId);
                            if (from != null && to != null) {
                                mapGraph.addEdge(from, to, edge.distanceKm);
                            }
                        }

                        System.out.println("[VALIDATION] MapGraph initialized from database with " + mapGraph.adjacency_list.size() + " locations");
                    }

                    // Find matching locations from MapGraph by name (with database IDs)
                    Location graphOrigin = findLocationInGraphByName(origin.getName());
                    Location graphDestination = findLocationInGraphByName(destination.getName());

                    // VALIDATION: Check if locations exist in graph
                    if (graphOrigin == null || graphDestination == null) {
                        String missingLocs = "";
                        if (graphOrigin == null) missingLocs += origin.getName();
                        if (graphDestination == null) {
                            if (!missingLocs.isEmpty()) missingLocs += " and ";
                            missingLocs += destination.getName();
                        }
                        System.out.println("[VALIDATION] ❌ Locations not found in MapGraph: " + missingLocs);
                        return ValidationResult.failure("⚠️ Invalid route. Location(s) not available: " + missingLocs);
                    }

                    System.out.println("[VALIDATION] ✓ Found locations in graph: " + graphOrigin.getName() + " (id=" + graphOrigin.getId() + ") -> " +
                                     graphDestination.getName() + " (id=" + graphDestination.getId() + ")");

                    // USE EXISTING PASSENGER METHOD: request_ride()
                    // This validates the request, checks wallet balance, calculates distance/time/price
                    try {
                        currentRequest = passenger.request_ride(graphOrigin, graphDestination, mapGraph);
                    } catch (Exception e) {
                        // Catch balance/validation exceptions from passenger.request_ride()
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && (errorMsg.toLowerCase().contains("balance") ||
                                               errorMsg.toLowerCase().contains("insufficient") ||
                                               errorMsg.toLowerCase().contains("funds"))) {
                            System.out.println("[VALIDATION] ❌ Insufficient balance");
                            return ValidationResult.failure("💰 Insufficient balance. Please add funds to continue.");
                        }
                        System.out.println("[VALIDATION] ❌ Ride request failed: " + errorMsg);
                        return ValidationResult.failure("⚠️ Unable to create ride request. " + (errorMsg != null ? errorMsg : "Unknown error"));
                    }

                    // VALIDATION: Check if request was created
                    if (currentRequest == null) {
                        System.out.println("[VALIDATION] ❌ Ride request returned null");
                        return ValidationResult.failure("⚠️ Unable to create ride. Please check your balance and route.");
                    }

                    System.out.println("\n[VALIDATION] ✓ Ride Request Submitted Successfully!");
                    System.out.println("Request ID: " + currentRequest.getRequestId());
                    System.out.println("From: " + graphOrigin.getName() + " (DB ID: " + graphOrigin.getId() + ")");
                    System.out.println("To: " + graphDestination.getName() + " (DB ID: " + graphDestination.getId() + ")");
                    System.out.println("Estimated Distance: " + String.format("%.2f", currentRequest.getDistance()) + " km");
                    System.out.println("Estimated Time: " + currentRequest.getEstimatedTime() + " minutes");
                    System.out.println("Estimated Price: $" + String.format("%.2f", currentRequest.getEstimatedPrice()));
                    System.out.println("Status: " + currentRequest.getStatus());

                    // Initialize payment processor with default payment method and no options
                    paymentProcessor = new Payment(currentRequest.getEstimatedPrice(), PaymentType.wallet, null);

                    // Load ACTIVE drivers from database and ensure at least one is available
                    loadAvailableDrivers();

                    // If no drivers, activate one automatically
                    ensureActiveDriversAvailable();

                    System.out.println("Available drivers count: " + availableDrivers.size());
                    for (Driver d : availableDrivers) {
                        System.out.println("  - " + d.getName() + " at " + d.getCurrentLocation().getName());
                    }

                    // Drivers should now be available - create ride manager
                    rideManager = new RideManager(availableDrivers, currentRequest, mapGraph, paymentProcessor);

                    // Set database maps
                    loadPassengerAndDriverIds();
                    rideManager.setDatabaseMaps(passengerIdMap, driverIdMap);

                    // Simulate longer processing time (3.5 seconds for realistic search)
                    Thread.sleep(3500);

                    // Assign driver using EXISTING METHOD: RideManager.createRide() which calls assignNearestDriver()
                    // This will: 1) insert to ride_requests with status=Pending, 2) assign driver, 3) update status=Accepted
                    try {
                        rideManager.createRide();
                    } catch (Exception e) {
                        System.out.println("[VALIDATION] ❌ Driver assignment failed: " + e.getMessage());
                        return ValidationResult.failure("⚠️ No available drivers found. Please try again later.");
                    }

                    // Verify driver was assigned
                    Driver assignedDriver = rideManager.getCurrentDriver();
                    System.out.println("[VALIDATION] Assigned driver: " + (assignedDriver != null ? assignedDriver.getName() : "NULL"));

                    // VALIDATION: Check if driver was assigned
                    if (assignedDriver == null) {
                        System.out.println("[VALIDATION] ❌ Driver assignment returned null");
                        return ValidationResult.failure("⚠️ No drivers available at the moment. Please try again.");
                    }

                    return ValidationResult.success();

                } catch (InterruptedException e) {
                    System.out.println("[VALIDATION] ❌ Task interrupted");
                    Thread.currentThread().interrupt();
                    return ValidationResult.failure("⚠️ Request was cancelled.");
                } catch (Exception e) {
                    // Catch any unexpected exceptions
                    System.err.println("[VALIDATION] ❌ Unexpected error: " + e.getMessage());
                    e.printStackTrace();
                    return ValidationResult.failure("❌ An unexpected error occurred. Please try again.");
                }
            }
        };

        requestTask.setOnSucceeded(e -> {
            ValidationResult result = requestTask.getValue();

            if (!result.isValid()) {
                // Show validation error to user
                showError(result.getErrorMessage());
                System.out.println("[VALIDATION] Request failed with error: " + result.getErrorMessage());
                return;
            }

            Driver assignedDriver = rideManager.getCurrentDriver();
            System.out.println("[VALIDATION] ✓ Success handler - Assigned driver: " + (assignedDriver != null ? assignedDriver.getName() : "NULL"));

            if (assignedDriver != null) {
                // Show chat panel with system message
                showChatPanel();
                showSystemMessage("Driver has been assigned to your ride");

                // AUTO-SEND: Initial pickup message from driver when assigned
                if (currentRequest != null && currentRequest.getDatabaseId() > 0) {
                    ChatMessage autoMsg = new ChatMessage("🚗 I'm on the way to your pickup location", true);
                    utils.ChatStorage.getInstance().addMessage(currentRequest.getDatabaseId(), autoMsg);
                    System.out.println("[AutoChat] Sent: Driver pickup message");
                }

                // AUTOMATICALLY OPEN CHAT WINDOW FOR PASSENGER
                Platform.runLater(() -> {
                    openChatWindowAutomatically();
                });

                showDriverInfo(assignedDriver);
            } else {
                showError("❌ Driver assignment failed. Please try again.");
            }
        });

        requestTask.setOnFailed(e -> {
            // This should rarely happen now since we handle errors in call()
            System.err.println("[VALIDATION] ❌ Task failed unexpectedly");
            showError("❌ Failed to process ride request. Please try again.");
        });

        new Thread(requestTask).start();
    }

    private void loadAvailableDrivers() {
        try {
            List<DriverDAO.DriverRow> driverRows = driverDAO.showAll();
            availableDrivers.clear();

            for (DriverDAO.DriverRow row : driverRows) {
                // Only add active drivers with a location
                if (row.active && row.currentLocation != null && !row.currentLocation.isEmpty()) {
                    // CRITICAL: Find EXACT Location object from MapGraph
                    Location driverLocation = null;
                    for (Location loc : mapGraph.adjacency_list.keySet()) {
                        if (loc.getName().equalsIgnoreCase(row.currentLocation)) {
                            driverLocation = loc;
                            break;
                        }
                    }

                    // Skip if location not found in graph
                    if (driverLocation == null) {
                        System.out.println("Skipping driver " + row.name + " - location '" + row.currentLocation + "' not in MapGraph");
                        continue;
                    }

                    Driver driver = new Driver(
                        row.licensePlate, row.carModel, row.active,
                        row.userSSN, row.name, row.phone, row.email,
                        row.wallet, row.credit,
                        driverLocation,  // Use EXACT Location from MapGraph
                        new ArrayList<>(), row.password
                    );
                    availableDrivers.add(driver);
                    driverIdMap.put(driver, row.id);

                    System.out.println("Loaded active driver: " + driver.getName() + " at " + driverLocation.getName());
                }
            }
            System.out.println("Total active drivers loaded: " + availableDrivers.size());
        } catch (SQLException ex) {
            System.err.println("Error loading drivers: " + ex.getMessage());
        }
    }

    private void ensureActiveDriversAvailable() {
        // If no active drivers found, activate at least one from database
        if (availableDrivers.isEmpty()) {
            System.out.println("No active drivers found. Activating first available driver...");
            try {
                List<DriverDAO.DriverRow> allDrivers = driverDAO.showAll();
                if (!allDrivers.isEmpty()) {
                    // Take the first driver and activate them
                    DriverDAO.DriverRow firstDriver = allDrivers.get(0);

                    // CRITICAL: Use the EXACT Location object from MapGraph's adjacency list
                    // This ensures distance calculations work correctly
                    Location driverLocation = null;

                    // First, try to find "Downtown Cairo" in the graph
                    for (Location loc : mapGraph.adjacency_list.keySet()) {
                        if (loc.getName().equals("Downtown Cairo")) {
                            driverLocation = loc;
                            break;
                        }
                    }

                    // If not found, use the first location from the graph
                    if (driverLocation == null && !mapGraph.adjacency_list.isEmpty()) {
                        driverLocation = mapGraph.adjacency_list.keySet().iterator().next();
                    }

                    if (driverLocation == null) {
                        throw new Exception("MapGraph has no locations!");
                    }

                    System.out.println("Assigning driver to location: " + driverLocation.getName() +
                                     " (lat=" + driverLocation.getLatitude() + ", lon=" + driverLocation.getLongitude() + ")");

                    // Create driver object with active=true and the EXACT location from MapGraph
                    Driver driver = new Driver(
                        firstDriver.licensePlate,
                        firstDriver.carModel,
                        true, // Force active=true
                        firstDriver.userSSN,
                        firstDriver.name,
                        firstDriver.phone,
                        firstDriver.email,
                        firstDriver.wallet,
                        firstDriver.credit,
                        driverLocation,  // Use exact Location object from MapGraph
                        new ArrayList<>(),
                        firstDriver.password
                    );

                    // Update in database to set active=true
                    driverDAO.update(firstDriver.id, driver, driverLocation.getName());

                    // Add to available list
                    availableDrivers.add(driver);
                    driverIdMap.put(driver, firstDriver.id);

                    System.out.println("✓ Driver activated successfully: " + driver.getName() +
                                     " | Location: " + driverLocation.getName() +
                                     " | Car: " + driver.getCarModel());
                } else {
                    System.err.println("ERROR: No drivers exist in database!");
                }
            } catch (Exception ex) {
                System.err.println("Error activating driver: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void loadPassengerAndDriverIds() {
        if (currentUser instanceof Passenger) {
            Passenger passenger = (Passenger) currentUser;
            try {
                // Find passenger ID in database
                List<PassengerDAO.PassengerRow> passengers = passengerDAO.showAll();
                for (PassengerDAO.PassengerRow row : passengers) {
                    if (row.email.equals(passenger.getEmail())) {
                        passengerIdMap.put(passenger, row.id);
                        break;
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Error loading passenger ID: " + ex.getMessage());
            }
        }
    }

    private Location findLocationByName(String name) {
        // Search in combo boxes for matching location
        for (Location loc : startCombo.getItems()) {
            if (loc.getName().equalsIgnoreCase(name)) {
                return loc;
            }
        }
        // Return a default location if not found
        return new Location(name, 30.0, 31.0);
    }

    private Location findLocationInGraph(String name, List<Location> graphLocations) {
        for (Location loc : graphLocations) {
            if (loc.getName().equalsIgnoreCase(name)) {
                return loc;
            }
        }
        return null;
    }

    private Location findLocationInGraphByName(String name) {
        // Search directly in the mapGraph's adjacency list keys
        if (mapGraph != null && mapGraph.adjacency_list != null) {
            for (Location loc : mapGraph.adjacency_list.keySet()) {
                if (loc.getName().equalsIgnoreCase(name)) {
                    return loc;
                }
            }
        }
        return null;
    }

    /**
     * Show in-app modal dialog for insufficient balance
     */
    private void showInsufficientBalanceDialog() {
        // Show red error message under profile bar (consistent with other alerts)
        showError("❌ Insufficient Balance! Please add funds from Settings.");
        System.out.println("[BALANCE] Insufficient balance alert shown to user");
    }

    private void showDriverInfo(Driver driver) {
        Platform.runLater(() -> {
            try {
                // Load custom dialog - IN-APP MODAL
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverAssignedDialog.fxml"));
                StackPane dialogRoot = loader.load();

                // Get controller and set driver info
                DriverAssignedDialogController dialogController = loader.getController();
                dialogController.setDriverInfo(driver, currentRequest);

                // Set ride ID for chat functionality
                if (currentRequest != null) {
                    dialogController.setRideId(currentRequest.getDatabaseId());
                    System.out.println("[MapController] Chat enabled for ride ID: " + currentRequest.getDatabaseId());
                }

                // Store reference to dialog controller
                activeDialogController = dialogController;

                // Create stage as IN-APP MODAL attached to current window
                Stage ownerStage = (Stage) rootContainer.getScene().getWindow();
                Stage dialogStage = new Stage();
                dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                dialogStage.initOwner(ownerStage); // Attach to app window
                dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
                Scene scene = new Scene(dialogRoot);
                scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                dialogStage.setScene(scene);

                // Center dialog within owner window (below profile bar, above bottom panel)
                dialogStage.setOnShown(e -> {
                    dialogStage.setX(ownerStage.getX() + (ownerStage.getWidth() - dialogStage.getWidth()) / 2);
                    dialogStage.setY(ownerStage.getY() + 90); // 90px from top - below profile/settings bar
                });

                // Set callback for when user clicks Accept (Start Ride)
                dialogController.setOnAcceptCallback(() -> {
                    simulateRideCompletion(dialogStage);
                });

                // Set callback for when user clicks Cancel Ride
                dialogController.setOnCancelCallback(() -> {
                    handlePassengerCancellation(dialogStage);
                });

                // Show dialog (non-blocking - will stay open during ride)
                dialogStage.show();

      
            } catch (Exception ex) {
                System.err.println("Error showing driver dialog: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }

    private void simulateRideCompletion(Stage dialogStage) {
        showSuccess("🚗 Driver is on the way...");

        // Send system message to embedded chat
        showSystemMessage("Driver is on the way to your location");

        // AUTO-SEND: Driver is on the way (to ChatStorage for standalone chat window)
        if (currentRequest != null && currentRequest.getDatabaseId() > 0) {
            ChatMessage autoMsg = new ChatMessage("🚗 Driver is on the way to your pickup location", true);
            utils.ChatStorage.getInstance().addMessage(currentRequest.getDatabaseId(), autoMsg);
            System.out.println("[AutoChat] Sent: Driver is on the way");
        }

        // Simulate ride in progress
        Task<Void> rideTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Wait 3 seconds to simulate driver arriving
                Thread.sleep(3000);

                // Check if ride was cancelled
                if (isCancelled) {
                    System.out.println("[WORKFLOW] Cannot continue a cancelled ride.");
                    return null;
                }

                // Show driver arrived message
                Platform.runLater(() -> {
                    if (!isCancelled) {
                        showSuccess("✓ Driver arrived at pickup point.");
                        showSystemMessage("Driver has arrived at your location");

                        // AUTO-SEND: Driver has arrived
                        if (currentRequest != null && currentRequest.getDatabaseId() > 0) {
                            ChatMessage autoMsg = new ChatMessage("✅ I have arrived at your pickup location", true);
                            utils.ChatStorage.getInstance().addMessage(currentRequest.getDatabaseId(), autoMsg);
                            System.out.println("[AutoChat] Sent: Driver has arrived");
                        }
                    }
                });
                rideManager.markDriverArrived();

                // Wait another 2 seconds for passenger to board
                Thread.sleep(2000);

                // Check if ride was cancelled
                if (isCancelled) {
                    System.out.println("[WORKFLOW] Cannot continue a cancelled ride.");
                    return null;
                }

                // Mark passenger as onboard - disable cancellation from this point
                isOnboard = true;
                Platform.runLater(() -> {
                    if (!isCancelled) {
                        showSuccess("✓ Passenger onboard.");
                        // Disable cancel button
                        if (activeDialogController != null) {
                            activeDialogController.disableCancelButton();
                            System.out.println("[WORKFLOW] Cancel button disabled - passenger is onboard");
                        }
                    }
                });

                // Wait 3 seconds to simulate reaching destination
                Thread.sleep(3000);

                // Check if ride was cancelled
                if (isCancelled) {
                    System.out.println("[WORKFLOW] Cannot continue a cancelled ride.");
                    return null;
                }

                rideManager.markPassengerArrived();

                // Complete the ride
                if (!isCancelled) {
                    rideManager.completeRide();
                } else {
                    System.out.println("[WORKFLOW] Cannot complete a cancelled ride.");
                    return null;
                }

                return null;
            }
        };

        // Store reference to active ride task
        activeRideTask = rideTask;

        rideTask.setOnSucceeded(e -> {
            // Only proceed if not cancelled
            if (!isCancelled) {
                // Close the driver dialog
                if (dialogStage != null) {
                    dialogStage.close();
                }
                completeRideWorkflow();
            } else {
                System.out.println("[WORKFLOW] Ride was cancelled - skipping completion workflow");
                if (dialogStage != null) {
                    dialogStage.close();
                }
            }
        });

        rideTask.setOnFailed(e -> {
            showError("❌ Ride completion failed: " + rideTask.getException().getMessage());
            if (dialogStage != null) {
                dialogStage.close();
            }
        });

        new Thread(rideTask).start();
    }

    private void handlePassengerCancellation(Stage dialogStage) {
        // Check if passenger is already onboard
        if (isOnboard) {
            showError("❌ Cannot cancel after being onboard.");
            System.out.println("[CANCELLATION] Cannot cancel after being onboard.");
            return;
        }

        // Check if already cancelled
        if (isCancelled) {
            showError("⚠️ Ride is already cancelled.");
            System.out.println("[CANCELLATION] Ride is already cancelled.");
            return;
        }

        if (currentUser instanceof Passenger && rideManager != null) {
            Passenger passenger = (Passenger) currentUser;

            // Set cancellation flag FIRST to prevent further actions
            isCancelled = true;
            System.out.println("[CANCELLATION] isCancelled flag set to true");

            // Hide the cancel button in the dialog
            if (activeDialogController != null) {
                activeDialogController.disableCancelButton();
                System.out.println("[CANCELLATION] Cancel button hidden in dialog");
            }

            // Call existing cancellation logic (includes validation and driver wallet update)
            passenger.cancelRide(rideManager);

            // Persist balance changes to database
            try {
                // Get passenger and driver IDs
                Long passengerId = passengerIdMap.get(passenger);
                Driver assignedDriver = rideManager.getCurrentDriver();

                if (passengerId != null) {
                    // Sync passenger wallet to database (penalty deducted)
                    passengerDAO.update(
                        passengerId,
                        passenger,
                        passenger.getCurrentLocation() != null ? passenger.getCurrentLocation().getName() : null
                    );
                    System.out.println("[CANCELLATION] ✅ Passenger wallet synced to DB: " + passenger.getWalletBalance() + " EGP");
                }

                // Sync driver wallet to database (half penalty added)
                if (assignedDriver != null) {
                    Long driverId = driverIdMap.get(assignedDriver);
                    if (driverId != null) {
                        driverDAO.update(
                            driverId,
                            assignedDriver,
                            assignedDriver.getCurrentLocation() != null ? assignedDriver.getCurrentLocation().getName() : null
                        );
                        System.out.println("[CANCELLATION] ✅ Driver wallet synced to DB: " + assignedDriver.getWalletBalance() + " EGP (received half of penalty)");
                    }
                }
            } catch (Exception e) {
                System.err.println("[CANCELLATION] ❌ Failed to sync balance updates to database: " + e.getMessage());
                e.printStackTrace();
            }

            // Show cancellation message with penalty
            showError("❌ Passenger cancelled the trip — penalty applied.");

            System.out.println("[CANCELLATION] Passenger cancelled ride. Penalty of 20 EGP applied (10 EGP to driver).");

            // Close the dialog after a brief delay to show the message
            if (dialogStage != null) {
                new Thread(() -> {
                    try {
                        Thread.sleep(1500); // Wait 1.5 seconds to show the cancellation message
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                    Platform.runLater(() -> {
                        dialogStage.close();
                    });
                }).start();
            }
        }
    }

    private void completeRideWorkflow() {
        Platform.runLater(() -> {
            try {
                double finalPrice = currentRequest.getEstimatedPrice();

                // Load custom completion dialog - IN-APP MODAL
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RideCompleteDialog.fxml"));
                StackPane dialogRoot = loader.load();

                // Get controller and set ride info
                RideCompleteDialogController dialogController = loader.getController();
                dialogController.setRideInfo(currentRequest);

                // Create stage for dialog as IN-APP MODAL attached to current window
                Stage ownerStage = (Stage) rootContainer.getScene().getWindow();
                Stage dialogStage = new Stage();
                dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                dialogStage.initOwner(ownerStage); // Attach to app window
                dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
                Scene scene = new Scene(dialogRoot);
                scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                dialogStage.setScene(scene);

                // Center dialog within owner window (below profile bar)
                dialogStage.setOnShown(e -> {
                    dialogStage.setX(ownerStage.getX() + (ownerStage.getWidth() - dialogStage.getWidth()) / 2);
                    dialogStage.setY(ownerStage.getY() + 110); // 110px from top - below profile bar
                });

                // Show dialog and wait
                dialogStage.showAndWait();

                // After "You Have Arrived" dialog closes, show rating dialog
                showDriverRatingDialog();

            } catch (Exception ex) {
                System.err.println("Error showing completion dialog: " + ex.getMessage());
                ex.printStackTrace();
                showDriverRatingDialog(); // Continue flow even if dialog fails
            }
        // Guard check: Don't show rating dialog if ride was cancelled
        if (isCancelled) {
            System.out.println("[MapController] Cannot show rating dialog for cancelled ride.");
            return;
        }

        });
    }

    private void showDriverRatingDialog() {
        System.out.println("[MapController] ===== showDriverRatingDialog() called =====");

        try {
            Driver assignedDriver = rideManager.getCurrentDriver();
            Passenger passenger = (Passenger) currentUser;

            if (assignedDriver == null || passenger == null) {
                System.err.println("[MapController] Cannot show rating dialog: driver or passenger is null");
                showTipsDonationDialog(0); // Skip to tips
                return;
            }

            System.out.println("[MapController] Loading DriverRatingDialog.fxml...");
            // Load rating dialog - IN-APP MODAL
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DriverRatingDialog.fxml"));
            StackPane dialogRoot = loader.load();

            // Get controller and set driver info
            DriverRatingDialogController dialogController = loader.getController();
            dialogController.setDriverInfo(assignedDriver, passenger);

            // Set callback to proceed to tips/donation after rating
            // This callback will execute AFTER showAndWait() returns
            final int[] selectedRating = {0};

            System.out.println("[MapController] Setting rating callback...");
            dialogController.setOnRatingSubmittedCallback(() -> {
                System.out.println("[MapController] ===== RATING CALLBACK TRIGGERED =====");
                selectedRating[0] = dialogController.getSelectedRating();
                System.out.println("[MapController] Rating captured: " + selectedRating[0] + " stars");
            });

            // Create stage as IN-APP MODAL attached to current window
            Stage ownerStage = (Stage) rootContainer.getScene().getWindow();
            Stage dialogStage = new Stage();
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initOwner(ownerStage); // Attach to app window
            dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            Scene scene = new Scene(dialogRoot);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

            // Load CSS stylesheet using the correct resource path
            String cssPath = getClass().getResource("/driver-dialog.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            System.out.println("[MapController] CSS stylesheet loaded: " + cssPath);

            dialogStage.setScene(scene);

            // Center dialog within owner window (below profile bar)
            dialogStage.setOnShown(e -> {
                dialogStage.setX(ownerStage.getX() + (ownerStage.getWidth() - dialogStage.getWidth()) / 2);
                dialogStage.setY(ownerStage.getY() + 130); // 130px from top - below profile bar
            });

            System.out.println("[MapController] Showing rating dialog with showAndWait()...");
            // Show dialog and WAIT for it to close
            dialogStage.showAndWait();
            System.out.println("[MapController] Rating dialog closed (showAndWait returned)");

            // NOW immediately show Tips/Donation dialog (no Platform.runLater needed)
        System.out.println("══════════════════════════════════════════════════════");
            showTipsDonationDialog(selectedRating[0]);
        System.out.println("══════════════════════════════════════════════════════");
        } catch (Exception ex) {
            System.err.println("[MapController] Error showing rating dialog: " + ex.getMessage());
        // Guard check: Don't show tips dialog if ride was cancelled
        if (isCancelled) {
            System.out.println("[TipsDialog] Cannot show tips dialog for cancelled ride.");
            return;
        }

            ex.printStackTrace();
            showTipsDonationDialog(0); // Skip to tips on error
        }
    }

    private void showTipsDonationDialog(int driverRating) {
        System.out.println("\n\n");
        System.out.println("═══════════════════════════════���═══════════════════════════");
        System.out.println("  showTipsDonationDialog() CALLED - Rating: " + driverRating);
        System.out.println("═══════════════════════���═══════════════════════════════════");
        System.out.println("\n");

        try {
            Driver assignedDriver = rideManager.getCurrentDriver();
            Passenger passenger = (Passenger) currentUser;
            double rideCost = currentRequest.getEstimatedPrice();

            System.out.println("[TipsDialog] === DIAGNOSTIC INFO ===");
            System.out.println("[TipsDialog] Driver: " + (assignedDriver != null ? assignedDriver.getName() : "NULL"));
            System.out.println("[TipsDialog] Passenger: " + (passenger != null ? passenger.getName() : "NULL"));
            System.out.println("[TipsDialog] Ride cost: " + rideCost);
            System.out.println("[TipsDialog] rootContainer: " + (rootContainer != null ? "OK" : "NULL"));

            if (assignedDriver == null || passenger == null) {
                System.err.println("[TipsDialog] ❌ Cannot show dialog: driver or passenger is null");
                finalizeRideCompletion(driverRating, 0.0, 0.0);
                return;
            }

            System.out.println("[TipsDialog] Loading FXML...");
            java.net.URL fxmlUrl = getClass().getResource("/view/TipsDonationDialog.fxml");
            System.out.println("[TipsDialog] FXML URL: " + fxmlUrl);

            if (fxmlUrl == null) {
                System.err.println("[TipsDialog] ❌ FXML file not found!");
                finalizeRideCompletion(driverRating, 0.0, 0.0);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            System.out.println("[TipsDialog] FXMLLoader created");

            StackPane dialogRoot = loader.load();
            System.out.println("[TipsDialog] ✅ FXML loaded successfully!");

            TipsDonationDialogController dialogController = loader.getController();
            System.out.println("[TipsDialog] Controller: " + (dialogController != null ? "OK" : "NULL"));

            if (dialogController == null) {
                System.err.println("[TipsDialog] ❌ Controller is null!");
                finalizeRideCompletion(driverRating, 0.0, 0.0);
                return;
            }

            dialogController.setRideInfo(rideCost, passenger, assignedDriver);
            System.out.println("[TipsDialog] ✅ Ride info set");

            // Capture tip/donation amounts when callback is triggered
            final double[] capturedAmounts = {0.0, 0.0};

            dialogController.setOnConfirmCallback(() -> {
                System.out.println("[TipsDialog] ===== CONFIRM CALLBACK TRIGGERED =====");
                capturedAmounts[0] = dialogController.getTipAmount();
                capturedAmounts[1] = dialogController.getDonationAmount();
                System.out.println("[TipsDialog] Captured - Tip: " + capturedAmounts[0] + ", Donation: " + capturedAmounts[1]);
            });
            System.out.println("[TipsDialog] ✅ Callback set");

            Stage ownerStage = (Stage) rootContainer.getScene().getWindow();
            if (ownerStage == null) {
                System.err.println("[TipsDialog] ❌ Owner stage is null!");
                finalizeRideCompletion(driverRating, 0.0, 0.0);
                return;
            }
            System.out.println("[TipsDialog] ✅ Owner stage obtained");

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Tips & Donation");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initOwner(ownerStage);
            dialogStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            Scene scene = new Scene(dialogRoot);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialogStage.setScene(scene);
            System.out.println("[TipsDialog] ✅ Scene created");

            dialogStage.setOnShown(e -> {
                double x = ownerStage.getX() + (ownerStage.getWidth() - dialogStage.getWidth()) / 2;
                double y = ownerStage.getY() + 100;
                dialogStage.setX(x);
                dialogStage.setY(y);
                System.out.println("[TipsDialog] Dialog positioned at x=" + x + ", y=" + y);
            });

            System.out.println("\n═══════════════════════════════���═══════════════════════════");
            System.out.println("  SHOWING TIPS/DONATION DIALOG NOW");
            System.out.println("═══════════════════════���═══════════════════════════════════\n");

            dialogStage.showAndWait();

            System.out.println("\n═══════════════════════════════���═══════════════════════════");
            System.out.println("  TIPS/DONATION DIALOG CLOSED");
            System.out.println("═══════════════════════════════════════════════════════════\n");

            // NOW immediately finalize with captured amounts
            System.out.println("[TipsDialog] Proceeding to finalize with tip=" + capturedAmounts[0] + ", donation=" + capturedAmounts[1]);
            finalizeRideCompletion(driverRating, capturedAmounts[0], capturedAmounts[1]);

        // Guard check: Don't finalize if ride was cancelled
        if (isCancelled) {
            System.out.println("[FINALIZE] Cannot finalize a cancelled ride.");
            return;
        }

        } catch (Exception ex) {
            System.err.println("\n❌❌❌ EXCEPTION in showTipsDonationDialog ❌❌❌");
            System.err.println("Exception type: " + ex.getClass().getName());
            System.err.println("Exception message: " + ex.getMessage());
            System.err.println("Stack trace:");
            ex.printStackTrace();
            System.err.println("❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌❌\n");
            finalizeRideCompletion(driverRating, 0.0, 0.0);
        }
    }

    private void finalizeRideCompletion(int passengerRating, double tipAmount, double donationAmount) {
        // Prevent duplicate finalization - CRITICAL for Issue #2 (ride count)
        if (rideFinalized) {
            System.err.println("[FINALIZE] ⚠️ WARNING: finalizeRideCompletion already called! Ignoring duplicate call.");
            System.err.println("[FINALIZE] This prevents ride_history from being inserted twice.");
            return;
        }

        System.out.println("[FINALIZE] First call - setting rideFinalized flag");
        rideFinalized = true;

        // All dialogs completed - now persist everything to database
        try {
            Driver assignedDriver = rideManager.getCurrentDriver();
            Passenger passenger = (Passenger) currentUser;

            if (assignedDriver == null || passenger == null || currentRequest == null) {
                System.err.println("[FINALIZE] Cannot finalize: missing driver, passenger, or request");
                showSuccess("✅ Ride completed!");
                return;
            }

            double rideCost = currentRequest.getEstimatedPrice();
            double finalTotal = rideCost + tipAmount + donationAmount;

            // Get passenger and driver IDs from maps
            Long passengerId = passengerIdMap.get(passenger);
            Long driverId = driverIdMap.get(assignedDriver);

            if (passengerId == null || driverId == null) {
                System.err.println("[FINALIZE] Cannot finalize: passenger or driver ID not found in maps");
                System.err.println("[FINALIZE] Passenger ID: " + passengerId + ", Driver ID: " + driverId);
                showSuccess("✅ Ride completed!");
                return;
            }

            // Get the ride request ID from RideManager
            long rideRequestId = rideManager.getRideRequestId();

            if (rideRequestId <= 0) {
                System.err.println("[FINALIZE] Cannot finalize: invalid ride request ID: " + rideRequestId);
                showSuccess("✅ Ride completed!");
                return;
            }

            System.out.println("\n[FINALIZE] ===== Finalizing Ride Completion =====");
            System.out.println("[FINALIZE] Request ID: " + rideRequestId);
            System.out.println("[FINALIZE] Passenger: " + passenger.getName() + " (ID: " + passengerId + ")");
            System.out.println("[FINALIZE] Driver: " + assignedDriver.getName() + " (ID: " + driverId + ")");
            System.out.println("[FINALIZE] Rating: " + passengerRating + " stars");
            System.out.println("[FINALIZE] Tip: " + tipAmount + " EGP");
            System.out.println("[FINALIZE] Donation: " + donationAmount + " EGP");
            System.out.println("[FINALIZE] Ride Cost: " + rideCost + " EGP");
            System.out.println("[FINALIZE] Final Total: " + finalTotal + " EGP");

            // STEP 1: Insert ride_history record into database
            // Note: RideManager.completeRide() already inserted a record, but with old values
            // We need to delete it and insert a new one with updated tips/donation/rating
            System.out.println("[FINALIZE] Step 1: Inserting into ride_history...");
            RideHistoryDAO rideHistoryDAO = new RideHistoryDAO();

            try {
                // First, delete any existing ride_history record for this request
                // (RideManager already created one with old values)
                String deleteSql = "DELETE FROM ride_history WHERE request_id = ?";
                try (Connection con = utils.DBConnection.getConnection();
                     PreparedStatement ps = con.prepareStatement(deleteSql)) {
                    ps.setLong(1, rideRequestId);
                    int deleted = ps.executeUpdate();
                    System.out.println("[FINALIZE] Deleted " + deleted + " old ride_history record(s) from RideManager");
                }

                // Insert ONE new record with complete information
                // This single record contains all data for both passenger and driver
                long historyId = rideHistoryDAO.insert(
                    rideRequestId,
                    driverId,
                    passengerId,
                    0,                // passenger_rating = Driver's rating of passenger (will be set by driver separately)
                    passengerRating,  // driver_rating = Passenger's rating of driver (set by passenger now)
                    finalTotal,       // Total cost including tips and donation
                    Model.PaymentType.wallet,
                    tipAmount,
                    donationAmount,
                    donationAmount > 0 ? "MiniGO Foundation" : ""
                );
                System.out.println("[FINALIZE] ✅ ride_history inserted successfully, id=" + historyId);

            } catch (Exception e) {
                System.err.println("[FINALIZE] ❌ Failed to insert ride_history record: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }

            // STEP 2: Keep ride_request with Completed status (do NOT delete)
            // The ride_request record stays in the database with status='Completed'
            // ride_history references it via foreign key
            System.out.println("[FINALIZE] Step 2: ride_request kept with status='Completed' (not deleted)");
            System.out.println("[FINALIZE] ✅ ride_request remains in database with foreign key intact");

            // STEP 3: Sync wallet balances to database
            // Note: RideManager.completeRide() → Payment.updateProcessPayment() already deducted from passenger
            // and added to driver. We just need to save the updated balances to the database.
            System.out.println("[FINALIZE] Step 3: Syncing passenger wallet to database...");
            try {
                double currentPassengerBalance = passenger.getWalletBalance();

                // Update passenger in database with current balance (already deducted by Payment)
                PassengerDAO passengerDAO = new PassengerDAO();
                passengerDAO.update(
                    passengerId,
                    passenger,
                    passenger.getCurrentLocation() != null ? passenger.getCurrentLocation().getName() : null
                );
                System.out.println("[FINALIZE] ✅ Passenger wallet synced to DB: " + currentPassengerBalance + " EGP");
            } catch (Exception e) {
                System.err.println("[FINALIZE] ❌ Failed to sync passenger wallet: " + e.getMessage());
                e.printStackTrace();
            }

            // STEP 4: Sync driver wallet to database
            // Note: Payment.processPayment() already added earnings to driver wallet (amount * 0.92)
            System.out.println("[FINALIZE] Step 4: Syncing driver wallet to database...");
            try {
                double currentDriverBalance = assignedDriver.getWalletBalance();

                // Update driver in database with current balance (already added by Payment)
                DriverDAO driverDAO = new DriverDAO();
                driverDAO.update(
                    driverId,
                    assignedDriver,
                    assignedDriver.getCurrentLocation() != null ? assignedDriver.getCurrentLocation().getName() : null
                );
                System.out.println("[FINALIZE] ✅ Driver wallet synced to DB: " + currentDriverBalance + " EGP");
            } catch (Exception e) {
                System.err.println("[FINALIZE] ❌ Failed to sync driver wallet: " + e.getMessage());
                e.printStackTrace();
            }

            // STEP 5: Increment ride counts for both passenger and driver
            System.out.println("[FINALIZE] Step 5: Updating ride counts...");
            // Update user stats in ProfileController for both passenger and driver
            updateUserStatsAfterRide(passengerId, driverId, finalTotal, rideCost + tipAmount);
            System.out.println("[FINALIZE] ✅ Ride counts and spent/earned updated for passenger and driver");

            // STEP 6: Generate PDF invoice using existing method
            System.out.println("[FINALIZE] Step 6: Generating PDF invoice...");
            try {
                generateInvoice(finalTotal);
                System.out.println("[FINALIZE] ✅ PDF invoice generated successfully");
            } catch (Exception e) {
                System.err.println("[FINALIZE] ❌ Failed to generate PDF invoice: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("[FINALIZE] ========================================\n");

            // Close the chat window automatically when ride is complete
            closeChatWindow();

            // Show final success message
            showSuccess("✅ Ride completed! Thank you for using MiniGO!");

        } catch (Exception ex) {
            System.err.println("[FINALIZE] ❌ CRITICAL ERROR during ride finalization: " + ex.getMessage());
            ex.printStackTrace();
            showError("❌ Error saving ride data. Please contact support.");
        }
    }

    private void generateInvoice(double amount) {
        try {
            Passenger passenger = (Passenger) currentUser;
            String invoiceId = "INV" + System.currentTimeMillis();

            // Generate PDF invoice using InvoiceGenerator utility
            String pdfFilePath = utils.InvoiceGenerator.generateInvoicePdf(invoiceId, passenger.getName(), amount);

            System.out.println("[INVOICE] PDF generated: " + invoiceId);

            // Send invoice via email
            String passengerEmail = passenger.getEmail();
            String passengerName = passenger.getName();

            System.out.println("[INVOICE] Sending invoice to email: " + passengerEmail);

            // Generate email body
            String emailSubject = "Your MiniGO Trip Receipt - " + invoiceId;
            String emailBody = utils.EmailSender.generateInvoiceEmailBody(passengerName, amount, invoiceId);

            // Send email with PDF attachment
            boolean emailSent = utils.EmailSender.sendInvoiceEmail(
                passengerEmail,
                passengerName,
                emailSubject,
                emailBody,
                pdfFilePath
            );

            if (emailSent) {
                System.out.println("[INVOICE] ✅ Invoice email sent successfully to " + passengerEmail);
                // Show success message to user
                Platform.runLater(() -> {
                    showSuccess("📧 Invoice sent to " + passengerEmail);
                });
            } else {
                System.err.println("[INVOICE] ❌ Failed to send invoice email (Email credentials not configured)");
                // Don't show error to user - PDF is already saved locally
                System.out.println("[INVOICE] ℹ️ PDF invoice saved locally: " + pdfFilePath);
                System.out.println("[INVOICE] ℹ️ To enable email: Configure Gmail credentials in EmailSender.java");
            }

        } catch (Exception ex) {
            System.err.println("[INVOICE] Error generating/sending invoice: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Update user stats after ride completion
     * Stats are now calculated dynamically from ride_history - no need to cache
     */
    private void updateUserStatsAfterRide(long passengerId, long driverId, double passengerSpent, double driverEarned) {
        // Stats are calculated on-demand from ride_history table
        // No caching needed - removed user_stats table dependency
        System.out.println("[STATS] Ride completed - stats will be calculated dynamically from ride_history");
    }

    /**
     * Load profile image from UserSession
     * Called on initialization and when returning from Profile screen
     */
    private void loadProfileImage() {
        try {
            String imagePath = utils.UserSession.getInstance().getProfileImagePath();

            if (imagePath != null && !imagePath.isEmpty()) {
                java.io.File imageFile = new java.io.File(imagePath);
                if (imageFile.exists()) {
                    javafx.scene.image.Image profileImage = new javafx.scene.image.Image(imageFile.toURI().toString());
                    if (!profileImage.isError() && profileBtn != null) {
                        profileBtn.setImage(profileImage);
                        System.out.println("[MapController] ✅ Profile image loaded: " + imagePath);
                        return;
                    }
                }
            }

            // Load default avatar if no custom image
            javafx.scene.image.Image defaultAvatar = new javafx.scene.image.Image(
                getClass().getResourceAsStream("/user_17436294.png")
            );
            if (profileBtn != null && !defaultAvatar.isError()) {
                profileBtn.setImage(defaultAvatar);
                System.out.println("[MapController] Default avatar loaded");
            }

        } catch (Exception e) {
            System.err.println("[MapController] Error loading profile image: " + e.getMessage());
        }
    }

    // ==================== CHAT PANEL MANAGEMENT ====================

    /**
     * Show chat panel with slide-in animation
     */
    public void showChatPanel() {
        if (chatPanel != null) {
            chatPanel.setVisible(true);
            chatPanel.setManaged(true);

            // Slide in animation
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), chatPanel);
            slideIn.setFromX(320);
            slideIn.setToX(0);
            slideIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
            slideIn.play();

            System.out.println("[ChatPanel] Showing chat panel");

            // Start polling for messages if ride is active
            if (currentRequest != null) {
                startChatPolling();
            }
        }
    }

    /**
     * Hide chat panel with slide-out animation
     */
    @FXML
    private void onCloseChatPanel() {
        if (chatPanel != null) {
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), chatPanel);
            slideOut.setFromX(0);
            slideOut.setToX(320);
            slideOut.setInterpolator(javafx.animation.Interpolator.EASE_IN);
            slideOut.setOnFinished(e -> {
                chatPanel.setVisible(false);
                chatPanel.setManaged(false);
            });
            slideOut.play();

            System.out.println("[ChatPanel] Hiding chat panel");
            stopChatPolling();
        }
    }

    /**
     * Display a chat message in the UI
     */
    private void displayChatMessage(Model.ChatMessage message) {
        if (chatMessagesContainer == null) return;

        Platform.runLater(() -> {
            // Create message bubble
            Label msgLabel = new Label(message.getText());
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(250);
            msgLabel.setPadding(new Insets(10, 14, 10, 14));

            // Style based on sender
            if (message.isDriver()) {
                // Driver message (green, right-aligned)
                msgLabel.setStyle("-fx-background-color: #238636; -fx-text-fill: white; " +
                                "-fx-background-radius: 12; -fx-font-size: 13px;");
            } else {
                // Passenger message (blue, left-aligned)
                msgLabel.setStyle("-fx-background-color: #1F6FEB; -fx-text-fill: white; " +
                                "-fx-background-radius: 12; -fx-font-size: 13px;");
            }

            // Time label
            Label timeLabel = new Label(message.getTimestamp());
            timeLabel.setStyle("-fx-text-fill: #8B92A8; -fx-font-size: 10px;");

            // Container for message
            VBox msgBox = new VBox(4, msgLabel, timeLabel);
            msgBox.setAlignment(message.isDriver() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            chatMessagesContainer.getChildren().add(msgBox);

            // Auto-scroll to bottom
            Platform.runLater(() -> {
                if (chatScrollPane != null) {
                    chatScrollPane.setVvalue(1.0);
                }
            });
        });
    }

    /**
     * Send passenger quick message
     */
    @FXML
    private void onPassengerQuickMsg1() {
        sendPassengerMessage("Where are you?");
    }

    @FXML
    private void onPassengerQuickMsg2() {
        sendPassengerMessage("How long until you arrive?");
    }

    @FXML
    private void onPassengerQuickMsg3() {
        sendPassengerMessage("I'm waiting at the pickup point.");
    }

    /**
     * Send a message from passenger
     */
    private void sendPassengerMessage(String text) {
        if (currentRequest == null) return;

        long rideId = currentRequest.getDatabaseId();
        Model.ChatMessage message = new Model.ChatMessage(text, false); // false = passenger
        utils.ChatStorage.getInstance().addMessage(rideId, message);
        displayChatMessage(message);

        System.out.println("[ChatPanel] Passenger sent: " + text);
    }

    /**
     * Poll for new messages from driver
     */
    private Timer chatPollingTimer;
    private int lastChatMessageCount = 0;

    private void startChatPolling() {
        if (chatPollingTimer != null) {
            chatPollingTimer.cancel();
        }

        chatPollingTimer = new Timer(true);
        chatPollingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (currentRequest == null) return;

                long rideId = currentRequest.getDatabaseId();
                List<Model.ChatMessage> messages = utils.ChatStorage.getInstance().getMessages(rideId);

                if (messages.size() > lastChatMessageCount) {
                    // New messages arrived
                    for (int i = lastChatMessageCount; i < messages.size(); i++) {
                        displayChatMessage(messages.get(i));
                    }
                    lastChatMessageCount = messages.size();
                }
            }
        }, 1000, 1000); // Check every 1 second

        System.out.println("[ChatPanel] Started polling for messages");
    }

    private void stopChatPolling() {
        if (chatPollingTimer != null) {
            chatPollingTimer.cancel();
            chatPollingTimer = null;
        }
        lastChatMessageCount = 0;
        System.out.println("[ChatPanel] Stopped polling for messages");
    }

    /**
     * Show system message in chat
     */
    private void showSystemMessage(String text) {
        if (chatMessagesContainer == null || currentRequest == null) return;

        Platform.runLater(() -> {
            Label systemLabel = new Label("🔔 " + text);
            systemLabel.setWrapText(true);
            systemLabel.setMaxWidth(280);
            systemLabel.setStyle("-fx-background-color: #2E2E2E; -fx-text-fill: #E6EDF3; " +
                               "-fx-background-radius: 8; -fx-font-size: 12px; -fx-padding: 8 12; " +
                               "-fx-font-style: italic;");
            systemLabel.setAlignment(Pos.CENTER);

            VBox msgBox = new VBox(systemLabel);
            msgBox.setAlignment(Pos.CENTER);

            chatMessagesContainer.getChildren().add(msgBox);

            // Auto-scroll
            Platform.runLater(() -> {
                if (chatScrollPane != null) {
                    chatScrollPane.setVvalue(1.0);
                }
            });
        });
    }

    /**
     * Open standalone chat window
     */
    @FXML
    private void onOpenChat() {
        try {
            // Check if there's an active ride
            if (currentRequest == null) {
                showError("⚠️ No active ride. Start a ride to use chat.");
                return;
            }

            System.out.println("[MapController] Opening chat window for ride ID: " + currentRequest.getDatabaseId());

            // Load ChatView.fxml from view folder
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ChatView.fxml"));
            javafx.scene.Parent root = loader.load();

            // Get controller and set session
            ChatViewController chatController = loader.getController();
            chatController.setChatSession(currentRequest.getDatabaseId(), isDriver);

            // Create new stage for chat
            Stage chatStage = new Stage();
            chatStage.setTitle("💬 Ride Chat");
            chatStage.initModality(javafx.stage.Modality.NONE); // Non-blocking
            chatStage.initOwner((Stage) rootContainer.getScene().getWindow());

            Scene scene = new Scene(root, 360, 700);
            chatStage.setScene(scene);
            chatStage.setResizable(false);

            // Position next to main window
            Stage ownerStage = (Stage) rootContainer.getScene().getWindow();
            chatStage.setX(ownerStage.getX() + ownerStage.getWidth() + 10);
            chatStage.setY(ownerStage.getY());

            chatStage.show();

            System.out.println("[MapController] Chat window opened successfully");
            showSuccess("💬 Chat opened");

        } catch (Exception ex) {
            System.err.println("[MapController] Error opening chat: " + ex.getMessage());
            ex.printStackTrace();
            showError("❌ Failed to open chat window");
        }
    }

    /**
     * Automatically open chat window when ride becomes active
     * Called when driver is assigned to passenger
     */
    private Stage activeChatStage = null; // Keep reference to chat window

    private void openChatWindowAutomatically() {
        // Check if chat window is already open
        if (activeChatStage != null && activeChatStage.isShowing()) {
            System.out.println("[ChatWindow] Chat window already open, bringing to front");
            activeChatStage.toFront();
            return;
        }

        try {
            // Check if there's an active ride
            if (currentRequest == null) {
                System.err.println("[ChatWindow] Cannot open - no active ride");
                return;
            }

            System.out.println("[ChatWindow] Auto-opening chat for ride ID: " + currentRequest.getDatabaseId());

            // Load ChatView.fxml from view folder
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ChatView.fxml"));
            javafx.scene.Parent root = loader.load();

            // Get controller and set session
            ChatViewController chatController = loader.getController();
            chatController.setChatSession(currentRequest.getDatabaseId(), isDriver);

            // Create new stage for chat - styled like the app
            Stage chatStage = new Stage();
            chatStage.setTitle("💬 Ride Chat - MiniGO");
            chatStage.initModality(javafx.stage.Modality.NONE); // Non-blocking floating window
            chatStage.initOwner((Stage) rootContainer.getScene().getWindow());

            Scene scene = new Scene(root, 360, 700);
            chatStage.setScene(scene);
            chatStage.setResizable(false);

            // Position next to main window (on the right side)
            Stage ownerStage = (Stage) rootContainer.getScene().getWindow();
            chatStage.setX(ownerStage.getX() + ownerStage.getWidth() + 10);
            chatStage.setY(ownerStage.getY());

            // Store reference
            activeChatStage = chatStage;

            // Clear reference when closed
            chatStage.setOnHidden(event -> {
                activeChatStage = null;
                System.out.println("[ChatWindow] Chat window closed");
            });

            chatStage.show();

            System.out.println("[ChatWindow] ✅ Chat window opened automatically");

            // Show success notification
            Platform.runLater(() -> showSuccess("💬 Chat opened - Stay connected with your " + (isDriver ? "passenger" : "driver")));

        } catch (Exception ex) {
            System.err.println("[ChatWindow] ❌ Error auto-opening chat: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Close the active chat window (called when ride ends)
     */
    private void closeChatWindow() {
        if (activeChatStage != null && activeChatStage.isShowing()) {
            Platform.runLater(() -> {
                activeChatStage.close();
                activeChatStage = null;
                System.out.println("[ChatWindow] ✅ Chat window closed automatically (ride ended)");
            });
        }
    }
}
