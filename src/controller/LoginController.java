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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginController {

    @FXML
    private Label roleLabel;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Label successLabel;

    @FXML
    private Label registerLink;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private Button loginButton;

    private String selectedRole;

    public void initialize() {
        // Auto-focus on email field when screen loads
        if (emailField != null) {
            emailField.requestFocus();
        }
    }

    public void setSelectedRole(String role) {
        this.selectedRole = role;
        updateRoleLabel();
    }

    public void setSuccessMessage(String message) {
        if (successLabel != null) {
            successLabel.setText(message);
            successLabel.setVisible(true);
            successLabel.setManaged(true);
        }
    }

    private void updateRoleLabel() {
        if (roleLabel != null && selectedRole != null) {
            String roleText = selectedRole.equals("passenger") ? "Passenger Login" : "Driver Login";
            roleLabel.setText(roleText);
        }
    }

    @FXML
    public void onLogin(ActionEvent event) {
        // Hide previous messages
        hideMessages();
        hideFieldErrors();

        // Add button click animation
        playButtonAnimation(loginButton);

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Field-level validation
        boolean isValid = true;

        if (email.isEmpty()) {
            showFieldError(emailErrorLabel, "Email cannot be empty");
            isValid = false;
        }

        if (password.isEmpty()) {
            showFieldError(passwordErrorLabel, "Password cannot be empty");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Hash the password
        String hashedPassword = hashPassword(password);

        // Check credentials based on role
        boolean loginSuccess = false;
        Object user = null;

        try {
            if (selectedRole.equals("passenger")) {
                PassengerDAO passengerDAO = new PassengerDAO();
                Passenger passenger = passengerDAO.getByEmail(email);

                if (passenger != null && passenger.getPassword().equals(hashedPassword)) {
                    loginSuccess = true;
                    user = passenger;
                }
            } else if (selectedRole.equals("driver")) {
                DriverDAO driverDAO = new DriverDAO();
                Driver driver = driverDAO.getByEmail(email);

                if (driver != null && driver.getPassword().equals(hashedPassword)) {
                    loginSuccess = true;
                    user = driver;
                }
            }

            if (loginSuccess) {
                // Navigate to home screen
                navigateToHome(event, user);
            } else {
                showError("Account not found. Please register.");
            }

        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace();
            showError("An error occurred. Please try again.");
        }
    }

    @FXML
    public void onRegisterClick(MouseEvent event) {
        loadRegisterScreen(event);
    }

    @FXML
    public void onBackToRoleSelection(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RoleSelection.fxml"));
            Scene scene = new Scene(loader.load(), 320, 600);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load Role Selection screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadRegisterScreen(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Register.fxml"));
            Scene scene = new Scene(loader.load(), 320, 600);

            // Pass the selected role to Register controller
            RegisterController controller = loader.getController();
            controller.setSelectedRole(selectedRole);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load Register screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToHome(ActionEvent event, Object user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/HomePage.fxml"));
            Scene scene = new Scene(loader.load(), 320, 600);

            // Pass user data to HomePage controller if needed
            HomePageController controller = loader.getController();
            if (selectedRole.equals("passenger")) {
                controller.setPassenger((Passenger) user);
            } else {
                controller.setDriver((Driver) user);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load Home screen: " + e.getMessage());
            e.printStackTrace();
            showError("Failed to navigate to home screen.");
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideMessages() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
        if (successLabel != null) {
            successLabel.setVisible(false);
            successLabel.setManaged(false);
        }
    }

    private void hideFieldErrors() {
        hideFieldError(emailErrorLabel);
        hideFieldError(passwordErrorLabel);
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

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}

