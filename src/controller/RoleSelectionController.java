package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoleSelectionController {

    private static final Logger LOGGER = Logger.getLogger(RoleSelectionController.class.getName());

    @FXML
    private Button passengerButton;

    @FXML
    private Button driverButton;

    @FXML
    private void handlePassengerSelection() {
        LOGGER.info("User selected: Passenger");
        navigateToLogin("passenger");
    }

    @FXML
    private void handleDriverSelection() {
        LOGGER.info("User selected: Driver");
        navigateToLogin("driver");
    }

    private void navigateToLogin(String role) {
        try {
            // Load the login page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Login.fxml"));
            Parent root = loader.load();

            // Get the controller and set the role
            LoginController loginController = loader.getController();
            loginController.setSelectedRole(role);

            LOGGER.info("Navigating to Login page for role: " + role);

            // Get current stage and switch scene
            Stage stage = (Stage) passengerButton.getScene().getWindow();
            Scene scene = new Scene(root, 320, 600);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading Login page", e);
        }
    }

    @FXML
    private void initialize() {
        LOGGER.info("RoleSelectionController initialized");
    }
}

