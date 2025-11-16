package Controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import Model.Location;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Insets;


public class MapController {

    @FXML private WebView webView;

    @FXML private ComboBox<Location> startCombo;
    @FXML private ComboBox<Location> endCombo;

    @FXML private Button findBtn;
    @FXML private Button clearBtn;
    @FXML private Button toggleBtn;

    @FXML private VBox bottomPanel;

    @FXML private ImageView settingsBtn;
    @FXML private ImageView profileBtn;

    @FXML private StackPane rootContainer;

    private WebEngine engine;
    private boolean jsReady = false;

    private boolean panelVisible = true;
    private final double panelHeight = 260;
    private final double visibleStrip = 45;

    private Label errorToast = null;


    @FXML
    public void initialize() {

        engine = webView.getEngine();
        engine.load(getClass().getResource("/map.html").toExternalForm());

        engine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
            if (newDoc != null) {

                Platform.runLater(() -> {
                    try {
                        Object ready = engine.executeScript("window.mapReady");

                        jsReady = (ready instanceof Boolean && (Boolean) ready);

                        System.out.println("JS READY = " + jsReady);

                    } catch (Exception ex) {
                        System.out.println("JS INIT ERROR: " + ex.getMessage());
                    }
                });
            }
        });

        loadLocations();
        setupButtons();
    }


    private void loadLocations() {
        startCombo.getItems().addAll(
                new Location("Nasr City", 30.0561, 31.3300),
                new Location("Maadi", 29.9603, 31.2596),
                new Location("Giza", 30.0131, 31.2089),
                new Location("New Cairo", 30.0305, 31.4913)
        );

        endCombo.getItems().addAll(startCombo.getItems());
    }


    private void setupButtons() {

        findBtn.setOnAction(e -> requestRoute());
        clearBtn.setOnAction(e -> clearRoute());
        toggleBtn.setOnAction(e -> togglePanel());

        profileBtn.setOnMouseClicked(e -> {
            System.out.println("Open profile screen (Passenger)");
        });

        settingsBtn.setOnMouseClicked(e -> {
            System.out.println("Open Settings screen");
        });
    }


    private void requestRoute() {

        if (!jsReady) {
            System.out.println("❌ JS NOT READY — WAIT!");
            return;
        }

        Location A = startCombo.getValue();
        Location B = endCombo.getValue();

        if (A == null || B == null) {
            showError("⚠ Please select both origin and destination");
            return;
        }

        // Validate that origin and destination are different
        if (isSameLocation(A, B)) {
            showError("❌ Origin and destination cannot be the same");
            return;
        }

        String js = String.format(
                "window.drawRoute(%f,%f,%f,%f)",
                A.getLatitude(), A.getLongitude(),
                B.getLatitude(), B.getLongitude()
        );

        Platform.runLater(() -> {
            try {
                engine.executeScript(js);
                showSuccess("✓ Ride requested successfully!");
            } catch (Exception ex) {
                System.out.println("JS Route Error: " + ex.getMessage());
            }
        });
    }


    private void clearRoute() {
        if (!jsReady) return;

        Platform.runLater(() -> {
            engine.executeScript("window.clearRoute()");
        });
    }


    private void togglePanel() {

        panelVisible = !panelVisible;

        double targetY = panelVisible ? 0 : (panelHeight - visibleStrip);

        TranslateTransition tt =
                new TranslateTransition(Duration.millis(350), bottomPanel);

        tt.setToY(targetY);
        tt.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
        tt.play();

        toggleBtn.setText(panelVisible ? "Hide" : "Show");


        Platform.runLater(() ->
            engine.executeScript("window.dispatchEvent(new Event('resize'))")
        );
    }


    // ==================== HELPER FUNCTIONS ====================

    /**
     * Check if two locations are the same
     */
    private boolean isSameLocation(Location a, Location b) {
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * Show a modern toast/snackbar error message
     * Dismissible on click - position adapts to panel state
     */
    private void showError(String message) {
        // Remove existing error toast if any
        if (errorToast != null && errorToast.getParent() != null) {
            ((StackPane) errorToast.getParent()).getChildren().remove(errorToast);
        }

        // Create error toast
        errorToast = new Label(message);
        errorToast.setStyle(
            "-fx-background-color: #E74C3C; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 14 22; " +
            "-fx-background-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4); " +
            "-fx-cursor: hand; " +
            "-fx-border-color: rgba(255,255,255,0.3); " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 12;"
        );
        errorToast.setMaxWidth(360);
        errorToast.setWrapText(true);

        // Position at top of page
        StackPane.setAlignment(errorToast, Pos.TOP_CENTER);
        StackPane.setMargin(errorToast, new Insets(70, 0, 0, 0));

        // Add to root container
        if (rootContainer != null) {
            rootContainer.getChildren().add(errorToast);

            // Fade in + slide up animation
            errorToast.setOpacity(0);
            errorToast.setTranslateY(20);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(350), errorToast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slideUp = new TranslateTransition(Duration.millis(350), errorToast);
            slideUp.setFromY(20);
            slideUp.setToY(0);
            slideUp.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

            fadeIn.play();
            slideUp.play();

            // Click to dismiss with feedback
            final Label toastRef = errorToast;
            errorToast.setOnMouseClicked(e -> dismissError(toastRef));

            // Auto-dismiss after 4 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(4000);
                    Platform.runLater(() -> dismissError(toastRef));
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }).start();

            // Hover effect for better interactivity
            errorToast.setOnMouseEntered(e -> {
                errorToast.setStyle(
                    "-fx-background-color: #C0392B; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 14px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 14 22; " +
                    "-fx-background-radius: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 5); " +
                    "-fx-cursor: hand; " +
                    "-fx-border-color: rgba(255,255,255,0.5); " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 12;"
                );
            });

            errorToast.setOnMouseExited(e -> {
                errorToast.setStyle(
                    "-fx-background-color: #E74C3C; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 14px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 14 22; " +
                    "-fx-background-radius: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4); " +
                    "-fx-cursor: hand; " +
                    "-fx-border-color: rgba(255,255,255,0.3); " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 12;"
                );
            });
        }
    }

    /**
     * Show a modern toast/snackbar success message
     * Dismissible on click - position adapts to panel state
     */
    private void showSuccess(String message) {
        // Remove existing error toast if any
        if (errorToast != null && errorToast.getParent() != null) {
            ((StackPane) errorToast.getParent()).getChildren().remove(errorToast);
        }

        // Create success toast
        errorToast = new Label(message);
        errorToast.setStyle(
            "-fx-background-color: #27AE60; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 14 22; " +
            "-fx-background-radius: 12; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4); " +
            "-fx-cursor: hand; " +
            "-fx-border-color: rgba(255,255,255,0.3); " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 12;"
        );
        errorToast.setMaxWidth(360);
        errorToast.setWrapText(true);

        // Position at top of page
        StackPane.setAlignment(errorToast, Pos.TOP_CENTER);
        StackPane.setMargin(errorToast, new Insets(70, 0, 0, 0));

        // Add to root container
        if (rootContainer != null) {
            rootContainer.getChildren().add(errorToast);

            // Fade in + slide up animation
            errorToast.setOpacity(0);
            errorToast.setTranslateY(20);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(350), errorToast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slideUp = new TranslateTransition(Duration.millis(350), errorToast);
            slideUp.setFromY(20);
            slideUp.setToY(0);
            slideUp.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

            fadeIn.play();
            slideUp.play();

            // Click to dismiss with feedback
            final Label toastRef = errorToast;
            errorToast.setOnMouseClicked(e -> dismissError(toastRef));

            // Auto-dismiss after 3 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> dismissError(toastRef));
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }).start();

            // Hover effect for better interactivity
            errorToast.setOnMouseEntered(e -> {
                errorToast.setStyle(
                    "-fx-background-color: #229954; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 14px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 14 22; " +
                    "-fx-background-radius: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 5); " +
                    "-fx-cursor: hand; " +
                    "-fx-border-color: rgba(255,255,255,0.5); " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 12;"
                );
            });

            errorToast.setOnMouseExited(e -> {
                errorToast.setStyle(
                    "-fx-background-color: #27AE60; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 14px; " +
                    "-fx-font-weight: bold; " +
                    "-fx-padding: 14 22; " +
                    "-fx-background-radius: 12; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 12, 0, 0, 4); " +
                    "-fx-cursor: hand; " +
                    "-fx-border-color: rgba(255,255,255,0.3); " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 12;"
                );
            });
        }
    }

    /**
     * Dismiss the error toast with fade and scale animation
     */
    private void dismissError(Label toast) {
        if (toast != null && toast.getParent() != null) {
            // Scale down animation
            javafx.animation.ScaleTransition scaleOut = new javafx.animation.ScaleTransition(Duration.millis(250), toast);
            scaleOut.setFromX(1.0);
            scaleOut.setFromY(1.0);
            scaleOut.setToX(0.8);
            scaleOut.setToY(0.8);

            // Fade out animation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(250), toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            fadeOut.setOnFinished(e -> {
                if (toast.getParent() instanceof StackPane) {
                    ((StackPane) toast.getParent()).getChildren().remove(toast);
                }
            });

            scaleOut.play();
            fadeOut.play();
        }
    }
}
