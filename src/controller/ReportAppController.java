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

import java.io.IOException;

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

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

