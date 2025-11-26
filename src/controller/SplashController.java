package controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SplashController implements Initializable {

    @FXML
    private ImageView logoImage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            // Load logo image from resources with smooth rendering
            Image logo = new Image(getClass().getResourceAsStream("/Logo-removebg-preview.png"),
                                   160, 0, true, true);
            logoImage.setImage(logo);

            // Ensure smooth rendering
            logoImage.setSmooth(true);
            logoImage.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Failed to load logo: " + e.getMessage());
        }

        // Create fade-in animation for the logo (0.4 seconds)
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), logoImage);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        // Create a 1-second delay before navigating to role selection
        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(e -> navigateToRoleSelection());
        delay.play();
    }

    private void navigateToRoleSelection() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/RoleSelection.fxml"));
            Scene scene = new Scene(loader.load(), 390, 750);


            Stage stage = (Stage) logoImage.getScene().getWindow();

            // Set application icon
            stage.getIcons().clear();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/Logo-removebg-preview.png")));

            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load RoleSelection screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

