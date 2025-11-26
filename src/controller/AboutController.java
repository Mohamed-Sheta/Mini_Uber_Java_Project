package controller;

import Model.Driver;
import Model.Passenger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class AboutController {

    @FXML
    private Button backButton;

    private Object currentUser;
    private boolean isDriver = false;

    /**
     * Set the current user (called when navigating from Profile or Map)
     */
    public void setUser(Object user) {
        this.currentUser = user;
        this.isDriver = (user instanceof Driver);
    }

    /**
     * Navigate back to MapView
     */
    @FXML
    public void onBackToMap() {
        try {
            // Get URL first, then create FXMLLoader
            java.net.URL fxmlUrl = getClass().getResource("/MapView.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getClassLoader().getResource("MapView.fxml");
            }
            if (fxmlUrl == null) {
                System.err.println("ERROR: Could not find MapView.fxml");
                return;
            }

            System.out.println("Loading MapView from: " + fxmlUrl);
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(loader.load(), 390, 750);

            MapController controller = loader.getController();
            // Pass user data back to map
            if (currentUser != null) {
                if (isDriver) {
                    controller.setDriver((Driver) currentUser);
                } else {
                    controller.setPassenger((Passenger) currentUser);
                }
            }

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
            System.out.println("MapView loaded successfully");
        } catch (IOException e) {
            System.err.println("Failed to navigate to Map: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

