package controller;

import DAO.DriverDAO;
import DAO.PassengerDAO;
import DAO.ReportDAO;
import Model.Driver;
import Model.Passenger;
import Model.Person;
import Model.Report;
import Model.ReportType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.EmailSender;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportAppController {

    @FXML private ComboBox<ReportType> typeComboBox;
    @FXML private TextArea descriptionTextArea;
    @FXML private Button submitButton;
    @FXML private Button backButton;
    @FXML private Button clearButton;
    @FXML private Label typeErrorLabel;
    @FXML private Label descriptionErrorLabel;
    @FXML private Label charCountLabel;
    @FXML private Label messageLabel;

    private Person currentUser;
    private long userId = -1L;
    private final ReportDAO reportDAO;

    public ReportAppController() {
        this.reportDAO = new ReportDAO();
    }

    @FXML
    public void initialize() {
        // Populate ComboBox with report types
        typeComboBox.getItems().addAll(ReportType.values());

        // Add character counter for description
        if (descriptionTextArea != null && charCountLabel != null) {
            descriptionTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
                int length = newValue != null ? newValue.length() : 0;
                charCountLabel.setText(length + " / 300");

                // Limit to 300 characters
                if (length > 300) {
                    descriptionTextArea.setText(newValue.substring(0, 300));
                }
            });
        }

        System.out.println("[ReportApp] Initialized");
    }

    public void setUser(Person user) {
        this.currentUser = user;

        // Retrieve and store user ID immediately when user is set
        if (user != null) {
            try {
                if (user instanceof Passenger) {
                    PassengerDAO passengerDAO = new PassengerDAO();
                    this.userId = passengerDAO.getIdByEmail(user.getEmail());
                    System.out.println("[ReportApp] Passenger ID retrieved: " + this.userId);
                } else if (user instanceof Driver) {
                    DriverDAO driverDAO = new DriverDAO();
                    Long driverId = driverDAO.getDriverIdByEmail(user.getEmail());
                    if (driverId != null) {
                        this.userId = driverId;
                        System.out.println("[ReportApp] Driver ID retrieved: " + this.userId);
                    }
                }
            } catch (Exception e) {
                System.err.println("[ReportApp] Error retrieving user ID: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onSubmit() {
        System.out.println("[ReportApp] Submit clicked");

        ReportType type = typeComboBox.getValue();
        String description = descriptionTextArea.getText();

        // Reset error messages
        hideErrors();
        hideMessage();

        // Validate inputs
        if (type == null || description == null || description.trim().isEmpty()) {
            if (type == null) {
                showError(typeErrorLabel, "Please select a category");
            }
            if (description == null || description.trim().isEmpty()) {
                showError(descriptionErrorLabel, "Please provide a description");
            }
            return;
        }

        // Validate user ID was retrieved
        if (userId <= 0) {
            showMessage("Error: Could not retrieve user information", "red");
            return;
        }

        try {
            // Create new report with the stored user ID
            Report report = new Report(userId, type, description.trim());

            // Save to database
            long reportId = reportDAO.save(report);

            if (reportId > 0) {
                System.out.println("Report Type: " + type);
                System.out.println("Description: " + description);
                System.out.println("User ID: " + userId);
                System.out.println("Report ID: " + reportId);

                // Send email notification to company
                sendAppReportEmail(reportId, type, description.trim());

                showMessage("✓ Report submitted successfully! (ID: " + reportId + ")", "green");

                // Clear form after successful submission
                clearForm();
            } else {
                showMessage("Failed to submit report. Please try again.", "red");
            }
        } catch (Exception e) {
            System.err.println("Error submitting report: " + e.getMessage());
            e.printStackTrace();
            showMessage("Error submitting report: " + e.getMessage(), "red");
        }
    }

    @FXML
    private void onClear() {
        System.out.println("[ReportApp] Clear clicked");
        clearForm();
    }

    private void clearForm() {
        typeComboBox.setValue(null);
        descriptionTextArea.clear();
        hideErrors();
        hideMessage();
    }

    private void showError(Label label, String message) {
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void hideErrors() {
        if (typeErrorLabel != null) {
            typeErrorLabel.setVisible(false);
            typeErrorLabel.setManaged(false);
        }
        if (descriptionErrorLabel != null) {
            descriptionErrorLabel.setVisible(false);
            descriptionErrorLabel.setManaged(false);
        }
    }

    private void showMessage(String message, String color) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-text-fill: " + color + ";");
            messageLabel.setVisible(true);
            messageLabel.setManaged(true);
        }
    }

    private void hideMessage() {
        if (messageLabel != null) {
            messageLabel.setVisible(false);
            messageLabel.setManaged(false);
        }
    }

    @FXML
    private void onBack() {
        try {
            // Determine which settings page to navigate to based on user type
            boolean isDriver = currentUser instanceof Driver;
            String fxmlPath = isDriver ? "/view/DriverSettings.fxml" : "/view/ProfileSettings.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), 390, 750);

            // Pass the current user to the appropriate settings controller
            if (isDriver) {
                DriverSettingsController controller = loader.getController();
                if (currentUser != null) {
                    controller.setUser(currentUser);
                }
            } else {
                ProfileSettingsController controller = loader.getController();
                if (currentUser != null) {
                    controller.setUser(currentUser);
                }
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to navigate back to Settings: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void sendAppReportEmail(long reportId, ReportType reportType, String description) {
        try {
            // Company email address
            String companyEmail = "minigorides.official@gmail.com";

            // Get user information
            String userName = currentUser != null ? currentUser.getName() : "Unknown User";
            String userEmail = currentUser != null ? currentUser.getEmail() : "Unknown Email";
            String userRole = currentUser instanceof Driver ? "Driver" : "Passenger";

            // Get current timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Generate email body
            String emailBody = generateAppReportEmailBody(reportId, reportType.toString(), description,
                                                          userName, userEmail, userRole, timestamp);

            // Send email using EmailSender utility
            boolean emailSent = EmailSender.sendSimpleEmail(companyEmail, "App Report – MiniGO", emailBody);

            if (emailSent) {
                System.out.println("[ReportApp]  Email sent successfully to company");
            } else {
                System.out.println("[ReportApp]  Email sending failed (report still saved to database)");
            }

        } catch (Exception e) {
            System.err.println("[ReportApp] Error sending email: " + e.getMessage());
            e.printStackTrace();
            // Don't show error to user - report is already saved to database
        }
    }

    /**
     * Generate HTML email body for app report
     */
    private String generateAppReportEmailBody(long reportId, String reportType, String description,
                                             String userName, String userEmail, String userRole, String timestamp) {
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; color: #333; }\n" +
            "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
            "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
            "                 color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }\n" +
            "        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }\n" +
            "        .info-box { background: white; padding: 15px; margin: 15px 0; border-left: 4px solid #667eea; }\n" +
            "        .label { font-weight: bold; color: #666; }\n" +
            "        .value { color: #333; margin-bottom: 10px; }\n" +
            "        .description-box { background: #FFF8E1; padding: 15px; margin: 15px 0; border-radius: 5px;\n" +
            "                          border: 1px solid #FFE082; }\n" +
            "        .footer { text-align: center; margin-top: 30px; color: #999; font-size: 12px; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1>📱 App Report – MiniGO</h1>\n" +
            "            <p>Report ID: #" + reportId + "</p>\n" +
            "        </div>\n" +
            "        <div class=\"content\">\n" +
            "            <h3 style=\"color: #667eea;\">📋 Report Details</h3>\n" +
            "            <div class=\"info-box\">\n" +
            "                <div class=\"value\">\n" +
            "                    <span class=\"label\">Report ID:</span> " + reportId + "\n" +
            "                </div>\n" +
            "                <div class=\"value\">\n" +
            "                    <span class=\"label\">Report Type:</span> " + reportType + "\n" +
            "                </div>\n" +
            "                <div class=\"value\">\n" +
            "                    <span class=\"label\">Timestamp:</span> " + timestamp + "\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            \n" +
            "            <h3 style=\"color: #667eea;\">👤 Reporter Information</h3>\n" +
            "            <div class=\"info-box\">\n" +
            "                <div class=\"value\">\n" +
            "                    <span class=\"label\">Name:</span> " + userName + "\n" +
            "                </div>\n" +
            "                <div class=\"value\">\n" +
            "                    <span class=\"label\">Email:</span> " + userEmail + "\n" +
            "                </div>\n" +
            "                <div class=\"value\">\n" +
            "                    <span class=\"label\">Role:</span> " + userRole + "\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            \n" +
            "            <h3 style=\"color: #667eea;\">💬 Description</h3>\n" +
            "            <div class=\"description-box\">\n" +
            "                <p style=\"margin: 0; white-space: pre-wrap;\">" + escapeHtml(description) + "</p>\n" +
            "            </div>\n" +
            "            \n" +
            "            <hr style=\"border: none; border-top: 1px solid #ddd; margin: 30px 0;\">\n" +
            "            \n" +
            "            <p style=\"color: #666; font-size: 14px;\">\n" +
            "                This is an automated report from the MiniGO application. Please review and take appropriate action.\n" +
            "            </p>\n" +
            "        </div>\n" +
            "        <div class=\"footer\">\n" +
            "            <p>© 2025 MiniGO Egypt. All rights reserved.</p>\n" +
            "            <p>This is an automated message from MiniGO App Reporting System.</p>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>";
    }

    /**
     * Escape HTML special characters to prevent injection
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}

