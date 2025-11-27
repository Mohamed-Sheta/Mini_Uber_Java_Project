package controller;

import Model.*;
import DAO.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.DBConnection;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportProblemController {

    @FXML private Button backButton;
    @FXML private ComboBox<String> rideComboBox;
    @FXML private ComboBox<ProblemType> problemTypeComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private Button submitButton;
    @FXML private Label messageLabel;

    private Person currentUser;
    private List<Long> rideRequestIds = new ArrayList<>();

    @FXML
    public void initialize() {
        // Populate problem types from enum
        problemTypeComboBox.getItems().addAll(ProblemType.values());

        // Set custom cell factory to display user-friendly names
        problemTypeComboBox.setCellFactory(lv -> new javafx.scene.control.ListCell<ProblemType>() {
            @Override
            protected void updateItem(ProblemType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatProblemType(item));
                }
            }
        });

        // Set button cell to also display formatted name
        problemTypeComboBox.setButtonCell(new javafx.scene.control.ListCell<ProblemType>() {
            @Override
            protected void updateItem(ProblemType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatProblemType(item));
                }
            }
        });
    }

    /**
     * Format ProblemType enum to user-friendly display text
     */
    private String formatProblemType(ProblemType type) {
        switch (type) {
            case DRIVER_BEHAVIOR: return "Driver Behavior";
            case DRIVER_LATE: return "Driver Late";
            case RECKLESS_DRIVING: return "Reckless Driving";
            case VEHICLE_CLEANLINESS: return "Vehicle Cleanliness";
            case TECHNICAL_ISSUE: return "Technical Issue";
            case FARE_DISPUTE: return "Fare Dispute";
            case ACCOUNT_ISSUE: return "Account Issue";
            default: return type.toString();
        }
    }

    /**
     * Set the current user and load their completed rides
     */
    public void setUser(Person user) {
        this.currentUser = user;
        loadCompletedRides();
    }

    /**
     * Load completed rides for the current user
     * NOTE: Since we now store TWO records per ride, we use DISTINCT and GROUP BY
     * to avoid showing duplicate rides in the dropdown
     */
    private void loadCompletedRides() {
        if (currentUser == null) {
            System.err.println("[ReportProblem] Current user is null");
            return;
        }

        try {
            long userId = getUserIdFromDatabase();
            if (userId == -1) {
                showMessage("Error: User not found", true);
                System.err.println("[ReportProblem] User ID not found for email: " + currentUser.getEmail());
                return;
            }

            System.out.println("[ReportProblem] Loading rides for user ID: " + userId);

            // Get completed rides from ride_history table
            // Use GROUP BY request_id to get only one entry per ride
            String sql = "SELECT rh.request_id, MAX(rh.completed_at) as completed_at, " +
                        "l1.name as origin, l2.name as destination, MAX(rh.ride_cost) as ride_cost " +
                        "FROM ride_history rh " +
                        "JOIN ride_requests rr ON rh.request_id = rr.id " +
                        "JOIN locations l1 ON rr.origin_id = l1.id " +
                        "JOIN locations l2 ON rr.destination_id = l2.id " +
                        "WHERE rh.passenger_id = ? " +
                        "GROUP BY rh.request_id, l1.name, l2.name " +
                        "ORDER BY MAX(rh.completed_at) DESC";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setLong(1, userId);
                ResultSet rs = ps.executeQuery();

                rideComboBox.getItems().clear();
                rideRequestIds.clear();

                int count = 0;
                while (rs.next()) {
                    long requestId = rs.getLong("request_id");
                    String origin = rs.getString("origin");
                    String destination = rs.getString("destination");
                    Timestamp completedAt = rs.getTimestamp("completed_at");
                    double cost = rs.getDouble("ride_cost");

                    String displayText = String.format("Ride #%d | %s → %s | %.2f EGP | %s",
                            requestId, origin, destination, cost,
                            completedAt.toString().substring(0, 16));

                    rideComboBox.getItems().add(displayText);
                    rideRequestIds.add(requestId);
                    count++;
                }

                System.out.println("[ReportProblem] Loaded " + count + " completed rides");

                if (rideComboBox.getItems().isEmpty()) {
                    showMessage("You have no completed rides to report", true);
                    submitButton.setDisable(true);
                } else {
                    submitButton.setDisable(false);
                }
            }
        } catch (SQLException e) {
            showMessage("Error loading rides: " + e.getMessage(), true);
            System.err.println("[ReportProblem] SQL Error loading rides:");
            e.printStackTrace();
        }
    }

    /**
     * Submit the problem report
     */
    @FXML
    public void onSubmit() {
        System.out.println("[ReportProblem] Submit button clicked");

        // Validate ride selection
        int selectedIndex = rideComboBox.getSelectionModel().getSelectedIndex();
        if (selectedIndex == -1) {
            showMessage("❌ Please select a ride", true);
            System.err.println("[ReportProblem] No ride selected");
            return;
        }

        // Validate problem type selection
        ProblemType problemType = problemTypeComboBox.getValue();
        if (problemType == null) {
            showMessage("❌ Please select a problem type", true);
            System.err.println("[ReportProblem] No problem type selected");
            return;
        }

        // Validate description
        String description = descriptionArea.getText().trim();
        if (description.isEmpty()) {
            showMessage("❌ Please enter a description", true);
            System.err.println("[ReportProblem] No description entered");
            return;
        }

        try {
            long requestId = rideRequestIds.get(selectedIndex);
            long passengerId = getUserIdFromDatabase();
            Long driverId = getDriverIdForRequest(requestId);

            System.out.println("[ReportProblem] Submitting report:");
            System.out.println("  Request ID: " + requestId);
            System.out.println("  Passenger ID: " + passengerId);
            System.out.println("  Driver ID: " + driverId);
            System.out.println("  Problem Type: " + problemType);
            System.out.println("  Description: " + description);

            // Insert problem report using existing DAO
            ProblemReportDAO problemReportDAO = new ProblemReportDAO();
            long reportId = problemReportDAO.insertReport(requestId, passengerId, driverId);

            System.out.println("[ReportProblem] Report ID returned: " + reportId);

            if (reportId > 0) {
                // Insert the problem type and description
                insertProblemDetails(reportId, problemType.ordinal() + 1, description);
                System.out.println("[ReportProblem] Problem details inserted successfully");

                showMessage("✓ Report submitted successfully!", false);

                // Clear form
                rideComboBox.getSelectionModel().clearSelection();
                problemTypeComboBox.getSelectionModel().clearSelection();
                descriptionArea.clear();

                // Navigate back after 2 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        javafx.application.Platform.runLater(this::onBack);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                showMessage("❌ Failed to submit report", true);
                System.err.println("[ReportProblem] Report ID is 0 or negative");
            }
        } catch (SQLException e) {
            showMessage("❌ Error: " + e.getMessage(), true);
            System.err.println("[ReportProblem] SQL Error submitting report:");
            e.printStackTrace();
        } catch (Exception e) {
            showMessage("❌ Unexpected error: " + e.getMessage(), true);
            System.err.println("[ReportProblem] Unexpected error:");
            e.printStackTrace();
        }
    }

    /**
     * Insert problem details into problem_report_types table
     */
    private void insertProblemDetails(long reportId, int problemTypeId, String description) throws SQLException {
        String sql = "INSERT INTO problem_report_types (report_id, type_id, details) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, reportId);
            ps.setInt(2, problemTypeId);
            ps.setString(3, description);

            ps.executeUpdate();
        }
    }

    /**
     * Get driver ID for a specific request
     */
    private Long getDriverIdForRequest(long requestId) {
        String sql = "SELECT driver_id FROM ride_history WHERE request_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, requestId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getLong("driver_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get user ID from database
     */
    private long getUserIdFromDatabase() {
        if (currentUser == null) return -1;

        String tableName = (currentUser instanceof Model.Driver) ? "drivers" : "passengers";
        String sql = "SELECT id FROM " + tableName + " WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, currentUser.getEmail());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Navigate back to ProfileSettings
     */
    @FXML
    public void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProfileSettings.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            ProfileSettingsController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate back to ProfileSettings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show message to user
     */
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);

        if (isError) {
            messageLabel.setStyle("-fx-text-fill: #F85149; -fx-font-size: 13px; -fx-font-weight: 600;");
        } else {
            messageLabel.setStyle("-fx-text-fill: #3FB950; -fx-font-size: 13px; -fx-font-weight: 600;");
        }
    }
}

