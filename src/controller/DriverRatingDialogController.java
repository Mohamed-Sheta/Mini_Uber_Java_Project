package controller;

import Model.Driver;
import Model.Passenger;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class DriverRatingDialogController {

    @FXML private StackPane root;
    @FXML private Label driverNameLabel;
    @FXML private Label star1;
    @FXML private Label star2;
    @FXML private Label star3;
    @FXML private Label star4;
    @FXML private Label star5;
    @FXML private Label ratingTextLabel;
    @FXML private Button submitButton;

    private int selectedRating = 0;
    private Driver driver;
    private Passenger passenger;
    private Runnable onRatingSubmittedCallback;

    @FXML
    public void initialize() {
        // Load CSS stylesheet using the correct resource path
        if (driverNameLabel != null && driverNameLabel.getScene() != null) {
            String cssPath = getClass().getResource("/driver-dialog.css").toExternalForm();
            driverNameLabel.getScene().getStylesheets().add(cssPath);
        }
    }

    public void setDriverInfo(Driver driver, Passenger passenger) {
        this.driver = driver;
        this.passenger = passenger;
        if (driver != null) {
            driverNameLabel.setText(driver.getName());
        }

        // Try to load CSS here as well in case Scene is available now
        loadCssIfNeeded();
    }

    private void loadCssIfNeeded() {
        if (driverNameLabel != null && driverNameLabel.getScene() != null) {
            String cssPath = getClass().getResource("/driver-dialog.css").toExternalForm();
            if (!driverNameLabel.getScene().getStylesheets().contains(cssPath)) {
                driverNameLabel.getScene().getStylesheets().add(cssPath);
                System.out.println("[Rating] CSS stylesheet loaded: " + cssPath);
            }
        }
    }

    public void setOnRatingSubmittedCallback(Runnable callback) {
        this.onRatingSubmittedCallback = callback;
    }

    @FXML
    public void onStar1Click() {
        setRating(1);
    }

    @FXML
    public void onStar2Click() {
        setRating(2);
    }

    @FXML
    public void onStar3Click() {
        setRating(3);
    }

    @FXML
    public void onStar4Click() {
        setRating(4);
    }

    @FXML
    public void onStar5Click() {
        setRating(5);
    }

    private void setRating(int rating) {
        selectedRating = rating;
        updateStarDisplay();

        String[] ratingTexts = {
            "Select your rating",
            "1 Star - Poor",
            "2 Stars - Fair",
            "3 Stars - Good",
            "4 Stars - Very Good",
            "5 Stars - Excellent"
        };
        ratingTextLabel.setText(ratingTexts[rating]);
    }

    private void updateStarDisplay() {
        Label[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < stars.length; i++) {
            if (i < selectedRating) {
                stars[i].getStyleClass().clear();
                stars[i].getStyleClass().add("star-filled");
            } else {
                stars[i].getStyleClass().clear();
                stars[i].getStyleClass().add("star-empty");
            }
        }
    }

    @FXML
    public void onSubmit() {
        if (selectedRating > 0) {
            // Use existing Passenger.RateDriver() method
            if (passenger != null) {
                passenger.RateDriver(selectedRating);
                System.out.println("[Rating] Passenger rated driver: " + selectedRating + " stars");
            }

            // Trigger callback BEFORE closing (to capture rating)
            if (onRatingSubmittedCallback != null) {
                System.out.println("[Rating] Executing callback to capture rating");
                onRatingSubmittedCallback.run();
            }

            // Then close dialog
            Stage stage = (Stage) submitButton.getScene().getWindow();
            stage.close();
            System.out.println("[Rating] Dialog closed");

        } else {
            ratingTextLabel.setText("Please select a rating first");
            ratingTextLabel.setStyle("-fx-text-fill: #E74C3C;");
        }
    }

    @FXML
    public void onSkip() {
        // Skip rating - set to 0
        selectedRating = 0;
        System.out.println("[Rating] Passenger skipped rating");

        // Trigger callback BEFORE closing
        if (onRatingSubmittedCallback != null) {
            System.out.println("[Rating] Executing callback (skipped)");
            onRatingSubmittedCallback.run();
        }

        // Then close dialog
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.close();
        System.out.println("[Rating] Dialog closed (skipped)");
    }

    private void closeDialog() {
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.close();
    }

    public int getSelectedRating() {
        return selectedRating;
    }
}

