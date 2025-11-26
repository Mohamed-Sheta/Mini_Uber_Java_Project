package controller;

import services.Request;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class RideCompleteDialogController {

    @FXML private Label originLabel;
    @FXML private Label destinationLabel;
    @FXML private Label distanceLabel;
    @FXML private Label totalPriceLabel;
    @FXML private Button okButton;

    public void setRideInfo(Request request) {
        if (request != null) {
            originLabel.setText(request.getOrigin().getName());
            destinationLabel.setText(request.getDestination().getName());
            distanceLabel.setText(String.format("%.2f km", request.getDistance()));
            totalPriceLabel.setText(String.format("%.2f EGP", request.getEstimatedPrice()));
        }
    }

    @FXML
    public void onClose() {
        Stage stage = (Stage) okButton.getScene().getWindow();
        stage.close();
    }
}

