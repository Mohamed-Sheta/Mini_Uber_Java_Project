package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import java.util.function.Consumer;
public class DriverRatingPassengerDialogController {

    @FXML private Label titleLabel;
    @FXML private HBox starsContainer;
    @FXML private Button submitButton;
    @FXML private Button skipButton;

    private int selectedRating = 0;
    private Consumer<Integer> ratingCallback;

    @FXML
    public void initialize() {
        System.out.println("[DriverRatingPassenger] Dialog initialized");

        if (titleLabel != null) {
            titleLabel.setText("Rate Your Passenger");
        }

        // Setup star buttons
        if (starsContainer != null) {
            setupStarButtons();
        }

        // Setup submit button
        if (submitButton != null) {
            submitButton.setOnAction(e -> submitRating());
        }

        // Setup skip button
        if (skipButton != null) {
            skipButton.setOnAction(e -> skipRating());
        }
    }

    private void setupStarButtons() {
        starsContainer.getChildren().clear();
        starsContainer.setSpacing(8);

        for (int i = 1; i <= 5; i++) {
            final int rating = i;
            Button starBtn = createStarButton(rating);
            starBtn.setOnAction(e -> selectRating(rating));
            starsContainer.getChildren().add(starBtn);
        }
    }

    private Button createStarButton(int rating) {
        Button btn = new Button();
        btn.setPrefSize(50, 50);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-border-width: 0;");

        // Create star SVG
        SVGPath star = new SVGPath();
        star.setContent("M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z");
        star.setFill(Color.web("#30363D")); // Default grey
        star.setScaleX(1.5);
        star.setScaleY(1.5);

        btn.setGraphic(star);
        btn.setUserData(rating);

        return btn;
    }

    private void selectRating(int rating) {
        selectedRating = rating;
        System.out.println("[DriverRatingPassenger] Passenger rated with: " + rating + " stars");

        // Update star colors
        for (int i = 0; i < starsContainer.getChildren().size(); i++) {
            Button starBtn = (Button) starsContainer.getChildren().get(i);
            SVGPath star = (SVGPath) starBtn.getGraphic();

            if ((i + 1) <= rating) {
                star.setFill(Color.web("#FFD700")); // Gold
            } else {
                star.setFill(Color.web("#30363D")); // Grey
            }
        }
    }

    private void submitRating() {
        System.out.println("[DriverRatingPassenger] Submitting rating: " + selectedRating);

        if (ratingCallback != null) {
            ratingCallback.accept(selectedRating);
        }

        closeDialog();
    }

    private void skipRating() {
        System.out.println("[DriverRatingPassenger] Driver skipped rating");

        if (ratingCallback != null) {
            ratingCallback.accept(0);
        }

        closeDialog();
    }

    private void closeDialog() {
        try {
            // Try to close as Stage (for modal dialog mode)
            Stage stage = (Stage) submitButton.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        } catch (Exception e) {
            // If not a Stage, it means we're embedded - parent will handle cleanup
            System.out.println("[DriverRatingPassenger] Embedded mode - parent will handle cleanup");
        }
    }

    public void setRatingCallback(Consumer<Integer> callback) {
        this.ratingCallback = callback;
    }
}

