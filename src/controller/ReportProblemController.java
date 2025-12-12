package controller;

import Model.*;
import DAO.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.DBConnection;
import utils.EmailSender;

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

    public void setUser(Person user) {
        this.currentUser = user;
        loadCompletedRides();
    }

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
            // Simplified query - no GROUP BY needed since each ride is a single record
            String sql = "SELECT rh.request_id, rh.completed_at, rh.ride_cost, " +
                        "l1.name as origin, l2.name as destination " +
                        "FROM ride_history rh " +
                        "JOIN ride_requests rr ON rh.request_id = rr.id " +
                        "JOIN locations l1 ON rr.origin_id = l1.id " +
                        "JOIN locations l2 ON rr.destination_id = l2.id " +
                        "WHERE rh.passenger_id = ? " +
                        "ORDER BY rh.completed_at DESC";

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

    @FXML
    public void onSubmit() {
        System.out.println("[ReportProblem] Submit button clicked");

        // Validate ride selection
        int selectedIndex = rideComboBox.getSelectionModel().getSelectedIndex();
        if (selectedIndex == -1) {
            showMessage(" Please select a ride", true);
            System.err.println("[ReportProblem] No ride selected");
            return;
        }

        // Validate problem type selection
        ProblemType problemType = problemTypeComboBox.getValue();
        if (problemType == null) {
            showMessage(" Please select a problem type", true);
            System.err.println("[ReportProblem] No problem type selected");
            return;
        }

        // Description is now OPTIONAL - use fallback text if empty
        String description = descriptionArea.getText().trim();
        if (description.isEmpty()) {
            description = "No description provided";
            System.out.println("[ReportProblem] No description entered - using fallback text");
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

                // Get ride details for email
                String rideDetails = rideComboBox.getSelectionModel().getSelectedItem();

                // Send email notification with report details
                sendReportEmail(reportId, requestId, problemType, description, rideDetails);

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
                showMessage(" Failed to submit report", true);
                System.err.println("[ReportProblem] Report ID is 0 or negative");
            }
        } catch (SQLException e) {
            showMessage(" Error: " + e.getMessage(), true);
            System.err.println("[ReportProblem] SQL Error submitting report:");
            e.printStackTrace();
        } catch (Exception e) {
            showMessage(" Unexpected error: " + e.getMessage(), true);
            System.err.println("[ReportProblem] Unexpected error:");
            e.printStackTrace();
        }
    }

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

    private String getDriverNameForRequest(long requestId) {
        String sql = "SELECT d.name FROM drivers d " +
                     "JOIN ride_history rh ON d.id = rh.driver_id " +
                     "WHERE rh.request_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, requestId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            System.err.println("[ReportProblem] Error getting driver name: " + e.getMessage());
            e.printStackTrace();
        }
        return "Unknown Driver";
    }

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

    @FXML
    public void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProfileSettings.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            ProfileSettingsController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            // Get the stage using multiple fallback methods
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(scene);
                stage.show();
            } else {
                System.err.println("Failed to get Stage - cannot navigate back");
            }
        } catch (IOException e) {
            System.err.println("Failed to navigate back to ProfileSettings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get the current Stage using multiple fallback methods
     */
    private Stage getStage() {
        // Try method 1: Get stage from backButton
        if (backButton != null && backButton.getScene() != null && backButton.getScene().getWindow() != null) {
            return (Stage) backButton.getScene().getWindow();
        }

        // Try method 2: Get stage from submitButton
        if (submitButton != null && submitButton.getScene() != null && submitButton.getScene().getWindow() != null) {
            return (Stage) submitButton.getScene().getWindow();
        }

        // Try method 3: Get stage from rideComboBox
        if (rideComboBox != null && rideComboBox.getScene() != null && rideComboBox.getScene().getWindow() != null) {
            return (Stage) rideComboBox.getScene().getWindow();
        }

        // Try method 4: Get stage from problemTypeComboBox
        if (problemTypeComboBox != null && problemTypeComboBox.getScene() != null && problemTypeComboBox.getScene().getWindow() != null) {
            return (Stage) problemTypeComboBox.getScene().getWindow();
        }

        // Try method 5: Get stage from descriptionArea
        if (descriptionArea != null && descriptionArea.getScene() != null && descriptionArea.getScene().getWindow() != null) {
            return (Stage) descriptionArea.getScene().getWindow();
        }

        return null;
    }

    /**
     * Send problem report email notification
     */
    private void sendReportEmail(long reportId, long requestId, ProblemType problemType,
                                 String description, String rideDetails) {
        try {
            // Get current timestamp
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            // Format problem type for display
            String formattedProblemType = formatProblemType(problemType);

            // Get passenger name and email
            String passengerName = currentUser.getName();
            String passengerEmail = currentUser.getEmail();

            // Get driver name for the reported ride
            String driverName = getDriverNameForRequest(requestId);

            // Generate email body including driver name
            String emailBody = generateReportEmailBody(
                reportId,
                requestId,
                rideDetails,
                formattedProblemType,
                description,
                passengerName,
                passengerEmail,
                driverName,
                timestamp
            );

            // Email subject
            String subject = "Problem Report #" + reportId + " - " + formattedProblemType;

            // Send to the configured email address (from EmailSender)
            // Note: This sends to the sender email as a notification
            String recipientEmail = "minigorides.official@gmail.com"; // Your email from EmailSender

            System.out.println("[ReportProblem] Sending email notification to: " + recipientEmail);

            // Use a simpler email sending method without attachment
            boolean sent = sendSimpleEmail(recipientEmail, "MiniGO Support", subject, emailBody);

            if (sent) {
                System.out.println("[ReportProblem] ✅ Email notification sent successfully");
            } else {
                System.out.println("[ReportProblem] ⚠️ Email notification could not be sent");
            }

        } catch (Exception e) {
            System.err.println("[ReportProblem] Error sending email: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the report submission if email fails
        }
    }

    /**
     * Send simple email without attachment using EmailSender utility
     */
    private boolean sendSimpleEmail(String recipientEmail, String recipientName,
                                   String subject, String body) {
        try {
            // We'll use reflection or create a simple email sender
            // Since EmailSender.sendInvoiceEmail requires a PDF, we'll create our own sender here

            java.util.Properties props = new java.util.Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            props.put("mail.smtp.ssl.checkserveridentity", "false");

            // Use credentials from EmailSender (same email account)
            String senderEmail = "minigorides.official@gmail.com";
            String senderPassword = "dpuq boji fuuf nyly";

            javax.mail.Authenticator auth = new javax.mail.Authenticator() {
                @Override
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(senderEmail, senderPassword);
                }
            };

            javax.mail.Session session = javax.mail.Session.getInstance(props, auth);
            javax.mail.Message message = new javax.mail.internet.MimeMessage(session);
            message.setFrom(new javax.mail.internet.InternetAddress(senderEmail, "MiniGO Egypt"));
            message.setRecipients(javax.mail.Message.RecipientType.TO,
                                javax.mail.internet.InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setContent(body, "text/html; charset=UTF-8");

            javax.mail.Transport.send(message);
            return true;

        } catch (Exception e) {
            System.err.println("[ReportProblem] Email sending failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generate HTML email body for problem report
     */
    private String generateReportEmailBody(long reportId, long requestId, String rideDetails,
                                          String problemType, String description,
                                          String passengerName, String passengerEmail,
                                          String driverName, Timestamp timestamp) {
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; color: #333; }\n" +
            "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
            "        .header { background: linear-gradient(135deg, #FF6B6B 0%, #FF5252 100%);\n" +
            "                 color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }\n" +
            "        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }\n" +
            "        .info-box { background: white; padding: 15px; margin: 15px 0; border-left: 4px solid #FF6B6B; }\n" +
            "        .label { font-weight: bold; color: #666; }\n" +
            "        .value { color: #333; margin-bottom: 10px; }\n" +
            "        .description-box { background: #FFF8E1; padding: 15px; margin: 15px 0; border-radius: 5px; }\n" +
            "        .footer { text-align: center; margin-top: 30px; color: #999; font-size: 12px; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1>⚠️ Problem Report</h1>\n" +
            "            <p>Report #" + reportId + "</p>\n" +
            "        </div>\n" +
            "        <div class=\"content\">\n" +
            "            <div class=\"info-box\">\n" +
            "                <div class=\"value\">\n" +
            "                    <span class=\"label\">Report ID:</span> " + reportId + "\n" +
            "                </div>\n" +
            "                <div class=\"value\">\n" +
            "                    <span class=\"label\">Request ID:</span> " + requestId + "\n" +
            "                </div>\n" +
            "                <div class=\"value\">\n" +
            "                    <span class=\"label\">Timestamp:</span> " + timestamp.toString() + "\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            \n" +
            "            <h3 style=\"color: #FF6B6B;\">📍 Ride Details</h3>\n" +
            "            <div class=\"info-box\">\n" +
            "                <div class=\"value\">" + rideDetails + "</div>\n" +
            "            </div>\n" +
            "            \n" +
            "            <h3 style=\"color: #FF6B6B;\">👤 Reporter Information</h3>\n" +
            "            <div class=\"info-box\">\n" +
            "                <div class=\"value\">\n" +
            "                    <span class=\"label\">Passenger Name:</span> " + passengerName + "\n" +
            "                </div>\n" +
            "                <div class=\"value\">\n" +
            "                    <span class=\"label\">Passenger Email:</span> " + passengerEmail + "\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            \n" +
            "            <h3 style=\"color: #FF6B6B;\">🚗 Driver Information</h3>\n" +
            "            <div class=\"info-box\">\n" +
            "                <div class=\"value\">\n" +
            "                    <span class=\"label\">Driver Name:</span> " + driverName + "\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            \n" +
            "            <h3 style=\"color: #FF6B6B;\">🔍 Problem Type</h3>\n" +
            "            <div class=\"info-box\">\n" +
            "                <div class=\"value\" style=\"font-size: 16px; font-weight: bold;\">" + problemType + "</div>\n" +
            "            </div>\n" +
            "            \n" +
            "            <h3 style=\"color: #FF6B6B;\">📝 Description</h3>\n" +
            "            <div class=\"description-box\">\n" +
            "                <p style=\"margin: 0; white-space: pre-wrap;\">" +
            (description.equals("No description provided") ?
                "<em style=\"color: #999;\">No description provided</em>" : description) +
            "</p>\n" +
            "            </div>\n" +
            "            \n" +
            "            <hr style=\"border: none; border-top: 1px solid #ddd; margin: 30px 0;\">\n" +
            "            \n" +
            "            <p style=\"color: #666; font-size: 14px;\">\n" +
            "                This report has been automatically recorded in the system.\n" +
            "                Please review and take appropriate action.\n" +
            "            </p>\n" +
            "            \n" +
            "            <div style=\"text-align: center; margin-top: 30px;\">\n" +
            "                <p style=\"color: #FF6B6B; font-weight: bold;\">\n" +
            "                    Immediate attention required ⚠️\n" +
            "                </p>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <div class=\"footer\">\n" +
            "            <p>© 2025 MiniGO Egypt. All rights reserved.</p>\n" +
            "            <p>This is an automated notification from the MiniGO system.</p>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>";
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

