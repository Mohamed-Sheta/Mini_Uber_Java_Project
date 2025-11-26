package controller;

import Model.Driver;
import services.Request;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
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

    private Driver assignedDriver;
    private Request rideRequest;
    private boolean accepted = false;
    private Runnable onAcceptCallback;

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

    @FXML
    public void onOk() {
        accepted = true;
        closeDialog();

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
    public void onOverlayClick(MouseEvent event) {
        // Close dialog if clicking outside the dialog box
        if (event.getTarget().toString().contains("dialog-overlay")) {
            onRefuse();
        }
    }

    @FXML
    public void onDialogClick(MouseEvent event) {
        // Consume event to prevent overlay click
        event.consume();
    }

    private void closeDialog() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }

    public boolean isAccepted() {
        return accepted;
    }
}

