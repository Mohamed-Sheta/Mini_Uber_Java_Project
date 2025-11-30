package controller;

import Model.Driver;
import Model.Person;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utils.DBConnection;

import java.io.IOException;
import java.sql.*;

public class RideHistoryController {

    @FXML private Button backButton;
    @FXML private Label totalRidesLabel;
    @FXML private ScrollPane ridesScrollPane;
    @FXML private VBox ridesContainer;

    private Person currentUser;
    private long userId = -1;
    private boolean isDriver = false;

    /**
     * Set the current user and load their ride history
     */
    public void setUser(Person user) {
        this.currentUser = user;
        this.isDriver = (user instanceof Driver);

        // Get user ID from database
        this.userId = getUserIdFromDatabase(user.getEmail(), isDriver);

        // Load rides
        loadRideHistory();
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
        }

        return -1;
    }

    /**
     * Load ride history from database
     * Each ride is stored as ONE record containing all information
     */
    private void loadRideHistory() {
        if (userId == -1) {
            return;
        }

        // Get total rides count
        int totalRides = getTotalRides();
        totalRidesLabel.setText("Total Rides: " + totalRides);

        // Clear existing items
        ridesContainer.getChildren().clear();

        String idColumn = isDriver ? "driver_id" : "passenger_id";
        // Simplified query - no GROUP BY needed since each ride is a single record
        String sql = "SELECT rh.id, rh.request_id, rh.ride_cost, rh.completed_at, rh.payment_method, " +
                     "lo.name as origin, ld.name as destination, rr.distance_km, rr.status " +
                     "FROM ride_history rh " +
                     "JOIN ride_requests rr ON rh.request_id = rr.id " +
                     "JOIN locations lo ON rr.origin_id = lo.id " +
                     "JOIN locations ld ON rr.destination_id = ld.id " +
                     "WHERE rh." + idColumn + " = ? " +
                     "ORDER BY rh.completed_at DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    VBox rideCard = createRideCard(
                        rs.getLong("request_id"),
                        rs.getDouble("ride_cost"),
                        rs.getString("origin"),
                        rs.getString("destination"),
                        rs.getDouble("distance_km"),
                        rs.getString("payment_method"),
                        rs.getTimestamp("completed_at"),
                        rs.getString("status")
                    );
                    ridesContainer.getChildren().add(rideCard);
                }

                if (count == 0) {
                    Label noDataLabel = new Label("No completed rides yet");
                    noDataLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #999;");
                    ridesContainer.getChildren().add(noDataLabel);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading rides history: " + e.getMessage());
            e.printStackTrace();
            Label errorLabel = new Label("Error loading ride history");
            errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ff5252;");
            ridesContainer.getChildren().add(errorLabel);
        }
    }

    /**
     * Get total number of rides
     * Each ride is stored as ONE record in ride_history
     */
    private int getTotalRides() {
        if (userId == -1) {
            return 0;
        }

        String columnName = isDriver ? "driver_id" : "passenger_id";
        // Use COUNT(*) since each ride is a single record
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
        }

        return 0;
    }

    /**
     * Create a styled ride card
     */
    private VBox createRideCard(long rideId, double cost, String origin, String destination,
                                 double distance, String paymentMethod, Timestamp completedAt, String status) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color: #1A2333; " +
            "-fx-border-color: #30363D; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 12; " +
            "-fx-background-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 1);"
        );

        // Header: Ride ID and Status
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label rideIdLabel = new Label("Ride #" + rideId);
        rideIdLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #E6EDF3;");

        Label statusLabel = new Label(status);
        statusLabel.setStyle(
            "-fx-background-color: #238636; " +
            "-fx-text-fill: white; " +
            "-fx-padding: 4 10; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-font-size: 11px; " +
            "-fx-font-weight: bold;"
        );

        headerBox.getChildren().addAll(rideIdLabel, statusLabel);

        // Route
        Label routeLabel = new Label(origin + " → " + destination);
        routeLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #58A6FF;");

        // Distance
        Label distanceLabel = new Label(String.format("📍 Distance: %.2f km", distance));
        distanceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8B949E;");

        // Cost and Payment
        HBox costBox = new HBox(10);
        costBox.setAlignment(Pos.CENTER_LEFT);

        Label costLabel = new Label(String.format("💰 Cost: %.2f EGP", cost));
        costLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3FB950;");

        Label paymentLabel = new Label("(" + paymentMethod + ")");
        paymentLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6E7681;");

        costBox.getChildren().addAll(costLabel, paymentLabel);

        // Date
        Label dateLabel = new Label("🕒 " + completedAt.toString().substring(0, 16));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6E7681;");

        card.getChildren().addAll(headerBox, routeLabel, distanceLabel, costBox, dateLabel);
        return card;
    }

    /**
     * Navigate back to Profile screen
     */
    @FXML
    public void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Profile.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            ProfileController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
                controller.refreshProfile();
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate back to Profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

