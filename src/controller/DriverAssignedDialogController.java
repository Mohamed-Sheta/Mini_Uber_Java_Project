package controller;

import Model.Driver;
import services.Request;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DriverAssignedDialogController {

    @FXML private Label driverNameLabel;
    @FXML private Label carModelLabel;
    @FXML private Label licensePlateLabel;
    @FXML private Label phoneLabel;
    @FXML private Label estimatedTimeLabel;
    @FXML private Label estimatedPriceLabel;
    @FXML private Button okButton;
    @FXML private Button refuseButton;
    @FXML private Button cancelRideButton;
    @FXML private Button closeButton;
    @FXML private HBox initialButtonBox;
    @FXML private HBox cancelButtonBox;
    @FXML private VBox dialogContainer;
    @FXML private VBox contentWrapper;

    private Driver assignedDriver;
    private Request rideRequest;
    private boolean accepted = false;
    private Runnable onAcceptCallback;
    private Runnable onCancelCallback;

    public void setDriverInfo(Driver driver, Request request) {
        this.assignedDriver = driver;
        this.rideRequest = request;

        if (driver != null) {
            driverNameLabel.setText(driver.getName());
            carModelLabel.setText(driver.getCarModel());
            licensePlateLabel.setText(driver.getLicensePlate());
            phoneLabel.setText(driver.getPhoneNumber());
        }

        if (request != null) {
            estimatedTimeLabel.setText(request.getEstimatedTime() + " min");
            estimatedPriceLabel.setText(String.format("%.2f EGP", request.getEstimatedPrice()));
        }
    }

    public void setOnAcceptCallback(Runnable callback) {
        this.onAcceptCallback = callback;
    }

    public void setOnCancelCallback(Runnable callback) {
        this.onCancelCallback = callback;
    }

    @FXML
    public void onOk() {
        accepted = true;

        // Hide initial buttons (Start Ride / Refuse)
        if (initialButtonBox != null) {
            initialButtonBox.setVisible(false);
            initialButtonBox.setManaged(false);
        }

        // Show Cancel Ride button inside the panel
        showCancelButton();

        // Trigger the ride workflow
        if (onAcceptCallback != null) {
            onAcceptCallback.run();
        }
    }

    @FXML
    public void onRefuse() {
        accepted = false;
        closeDialog();
        // Just close - no database operations
        System.out.println("Ride refused by passenger. Dialog closed.");
    }

    @FXML
    public void onCancelRide() {
        // Trigger cancel callback
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
        closeDialog();
    }

    @FXML
    public void onCloseButtonClick() {
        // X button clicked - close dialog without canceling ride
        // This only hides the panel, keeping the ride logic intact
        System.out.println("[DriverAssignedDialogController] X button clicked - closing panel (ride continues)");
        closeDialog();
    }

    @FXML
    public void onOverlayClick(MouseEvent event) {
        // Hit-testing: Check if click coordinates are inside the panel bounds
        if (contentWrapper != null) {
            // Get the bounds of the content wrapper in the scene coordinate system
            javafx.geometry.Bounds panelBounds = contentWrapper.localToScene(contentWrapper.getBoundsInLocal());

            // Get click coordinates in scene coordinate system
            double clickX = event.getSceneX();
            double clickY = event.getSceneY();

            // Check if click is inside panel bounds
            boolean clickedInsidePanel = panelBounds.contains(clickX, clickY);

            if (!clickedInsidePanel) {
                // Clicked outside the panel - close dialog
                System.out.println("[DriverAssignedDialogController] Clicked outside panel at (" +
                                   clickX + ", " + clickY + ") - closing dialog");
                closeDialog();
            } else {
                // Clicked inside the panel - keep open
                System.out.println("[DriverAssignedDialogController] Clicked inside panel at (" +
                                   clickX + ", " + clickY + ") - keeping dialog open");
            }
        }
    }

    @FXML
    public void onPanelClick(MouseEvent event) {
        // No longer needed - hit-testing in onOverlayClick handles this
        // Keeping for backward compatibility
    }

    /**
     * Check if a node is contained within a parent node
     * @deprecated No longer needed with current implementation
     */
    private boolean isNodeInsideParent(Node node, Node parent) {
        if (node == null) {
            return false;
        }
        if (node == parent) {
            return true;
        }
        // Traverse up the parent hierarchy
        Node current = node;
        while (current != null) {
            if (current == parent) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    @FXML
    public void onDialogClick(MouseEvent event) {
        // No longer needed - keeping for backward compatibility
        event.consume();
    }

    private void closeDialog() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void disableCancelButton() {
        if (cancelRideButton != null && cancelButtonBox != null) {
            // Hide the cancel button entirely
            cancelButtonBox.setVisible(false);
            cancelButtonBox.setManaged(false);
            System.out.println("[DriverAssignedDialogController] Cancel button hidden - passenger is onboard");
        }
    }

    public void hideDriverPanel() {
        if (dialogContainer != null) {
            // Hide the entire driver info panel
            dialogContainer.setVisible(false);
            dialogContainer.setManaged(false);
            System.out.println("[DriverAssignedDialogController] Driver panel hidden - ride started");
        }
    }

    public void showCancelButton() {
        if (cancelButtonBox != null) {
            // Show red Cancel Ride button inside the panel
            cancelButtonBox.setVisible(true);
            cancelButtonBox.setManaged(true);
            System.out.println("[DriverAssignedDialogController] Cancel Ride button shown inside panel");
        }
    }
}

