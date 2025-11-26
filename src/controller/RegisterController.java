package controller;

import DAO.DriverDAO;
import DAO.PassengerDAO;
import Model.Driver;
import Model.Passenger;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class RegisterController {

    @FXML
    private Label roleLabel;

    @FXML
    private TextField nameField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private VBox driverFieldsBox;

    @FXML
    private TextField licensePlateField;

    @FXML
    private TextField carModelField;

    @FXML
    private Label errorLabel;

    @FXML
    private Label loginLink;

    @FXML
    private Label nameErrorLabel;

    @FXML
    private Label phoneErrorLabel;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private Label confirmPasswordErrorLabel;

    @FXML
    private Label licensePlateErrorLabel;

    @FXML
    private Label carModelErrorLabel;

    @FXML
    private Button registerButton;

    private String selectedRole;

    public void setSelectedRole(String role) {
        this.selectedRole = role;
        updateRoleLabel();

        // Show/hide driver-specific fields
        if (driverFieldsBox != null) {
            boolean isDriver = role.equals("driver");
            driverFieldsBox.setVisible(isDriver);
            driverFieldsBox.setManaged(isDriver);
        }
    }

    private void updateRoleLabel() {
        if (roleLabel != null && selectedRole != null) {
            String roleText = selectedRole.equals("passenger") ? "Passenger Registration" : "Driver Registration";
            roleLabel.setText(roleText);
        }
    }

    @FXML
    public void onRegister(ActionEvent event) {
        // Hide previous errors
        hideError();
        hideFieldErrors();

        // Add button click animation
        playButtonAnimation(registerButton);

        // Get input values
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Field-level validation
        boolean isValid = true;

        // Validate name
        if (name.isEmpty()) {
            showFieldError(nameErrorLabel, "Name is required");
            isValid = false;
        }

        // Validate phone with Egyptian phone number format
        if (phone.isEmpty()) {
            showFieldError(phoneErrorLabel, "Phone number is required");
            isValid = false;
        } else if (!phone.matches("^(010|011|012|015)[0-9]{8}$")) {
            showFieldError(phoneErrorLabel, "Invalid Egyptian phone number (e.g. 01123456789)");
            isValid = false;
        }

        // Validate email
        if (email.isEmpty()) {
            showFieldError(emailErrorLabel, "Email is required");
            isValid = false;
        } else if (!email.contains("@") || !email.contains(".")) {
            showFieldError(emailErrorLabel, "Invalid email format");
            isValid = false;
        }

        // Validate password with reasonable requirements
        if (password.isEmpty()) {
            showFieldError(passwordErrorLabel, "Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            showFieldError(passwordErrorLabel, "Password must be at least 6 characters");
            isValid = false;
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            showFieldError(confirmPasswordErrorLabel, "Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            showFieldError(confirmPasswordErrorLabel, "Passwords do not match");
            isValid = false;
        }

        // Driver-specific validation
        if (selectedRole.equals("driver")) {
            String licensePlate = licensePlateField.getText().trim();
            String carModel = carModelField.getText().trim();

            if (licensePlate.isEmpty()) {
                showFieldError(licensePlateErrorLabel, "License plate is required");
                isValid = false;
            }

            if (carModel.isEmpty()) {
                showFieldError(carModelErrorLabel, "Car model is required");
                isValid = false;
            }
        }

        if (!isValid) {
            return;
        }

        // Don't hash the password here - the Person constructor will handle it

        try {
            boolean success = false;

            if (selectedRole.equals("passenger")) {
                // Create and save passenger
                PassengerDAO passengerDAO = new PassengerDAO();

                // Check if email already exists
                if (passengerDAO.getByEmail(email) != null) {
                    showError("Email already registered. Please login.");
                    return;
                }

                // Generate SSN (you can improve this logic)
                String ssn = generateSSN(email);

                Passenger passenger = new Passenger(ssn, name, phone, email, password);
                success = passengerDAO.save(passenger);

            } else if (selectedRole.equals("driver")) {
                // Create and save driver
                DriverDAO driverDAO = new DriverDAO();

                // Check if email already exists
                if (driverDAO.getByEmail(email) != null) {
                    showError("Email already registered. Please login.");
                    return;
                }

                String licensePlate = licensePlateField.getText().trim();
                String carModel = carModelField.getText().trim();

                // Generate SSN
                String ssn = generateSSN(email);

                Driver driver = new Driver(ssn, name, phone, email, password, licensePlate, carModel);
                success = driverDAO.save(driver);
            }

            if (success) {
                // Navigate back to login with success message
                navigateToLogin(event, "Registration successful. Please log in.");
            } else {
                showError("Registration failed. Please try again.");
            }

        } catch (Exception e) {
            System.err.println("Registration error: " + e.getMessage());
            e.printStackTrace();
            showError("An error occurred during registration.");
        }
    }

    @FXML
    public void onLoginClick(MouseEvent event) {
        navigateToLogin(event, null);
    }

    private void navigateToLogin(Object event, String successMessage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);

            // Pass the selected role back to Login controller
            LoginController controller = loader.getController();
            controller.setSelectedRole(selectedRole);

            // Set success message if provided
            if (successMessage != null) {
                controller.setSuccessMessage(successMessage);
            }

            Node sourceNode;
            if (event instanceof ActionEvent) {
                sourceNode = (Node) ((ActionEvent) event).getSource();
            } else {
                sourceNode = (Node) ((MouseEvent) event).getSource();
            }

            Stage stage = (Stage) sourceNode.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load Login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void hideFieldErrors() {
        hideFieldError(nameErrorLabel);
        hideFieldError(phoneErrorLabel);
        hideFieldError(emailErrorLabel);
        hideFieldError(passwordErrorLabel);
        hideFieldError(confirmPasswordErrorLabel);
        hideFieldError(licensePlateErrorLabel);
        hideFieldError(carModelErrorLabel);
    }

    private void showFieldError(Label label, String message) {
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }


    private void hideFieldError(Label label) {
        if (label != null) {
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private void playButtonAnimation(Button button) {
        if (button != null) {
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(60), button);
            scaleDown.setToX(0.98);
            scaleDown.setToY(0.98);

            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(60), button);
            scaleUp.setToX(1.0);
            scaleUp.setToY(1.0);

            scaleDown.setOnFinished(e -> scaleUp.play());
            scaleDown.play();
        }
    }


    private String generateSSN(String email) {
        // Simple SSN generation based on email hash
        // In production, you'd use a more sophisticated method
        return String.valueOf(Math.abs(email.hashCode()));
    }
}

