package controller;

import Model.Driver;
import Model.Option;
import Model.Passenger;
import Model.PaymentType;
import services.Payment;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


public class TipsDonationDialogController {

    @FXML private Label rideCostLabel;
    @FXML private TextField tipField;
    @FXML private TextField donationField;
    @FXML private Label totalLabel;
    @FXML private Button confirmButton;

    private double baseCost = 0.0;
    private double tipAmount = 0.0;
    private double donationAmount = 0.0;
    private Passenger passenger;
    private Driver driver;
    private Runnable onConfirmCallback;

    public void setRideInfo(double rideCost, Passenger passenger, Driver driver) {
        this.baseCost = rideCost;
        this.passenger = passenger;
        this.driver = driver;

        rideCostLabel.setText(String.format("%.2f EGP", rideCost));
        updateTotal();

        // Add listeners to text fields
        tipField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateTotal();
        });

        donationField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateTotal();
        });
    }

    public void setOnConfirmCallback(Runnable callback) {
        this.onConfirmCallback = callback;
    }

    private void updateTotal() {
        tipAmount = parseAmount(tipField.getText());
        donationAmount = parseAmount(donationField.getText());

        double total = baseCost + tipAmount + donationAmount;
        totalLabel.setText(String.format("%.2f EGP", total));
    }

    private double parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }
        try {
            double value = Double.parseDouble(text.trim());
            return value > 0 ? value : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @FXML
    public void onConfirm() {
        // Capture amounts BEFORE closing dialog
        tipAmount = parseAmount(tipField.getText());
        donationAmount = parseAmount(donationField.getText());

        System.out.println("[TipsDonation] Confirm clicked - Tip: " + tipAmount + ", Donation: " + donationAmount);

        // Trigger callback BEFORE closing dialog
        if (onConfirmCallback != null) {
            System.out.println("[TipsDonation] Executing callback - passing tip=" + tipAmount + ", donation=" + donationAmount);
            onConfirmCallback.run();
            System.out.println("[TipsDonation] Callback completed");
        }

        // Then close dialog
        closeDialog();
        System.out.println("[TipsDonation] Dialog closed");
    }

    @FXML
    public void onSkip() {
        // Reset amounts
        tipAmount = 0.0;
        donationAmount = 0.0;

        System.out.println("[TipsDonation] Skip clicked - no tip/donation");

        // Trigger callback BEFORE closing
        if (onConfirmCallback != null) {
            System.out.println("[TipsDonation] Executing callback (skipped)");
            onConfirmCallback.run();
            System.out.println("[TipsDonation] Callback completed (skipped)");
        }

        // Then close dialog
        closeDialog();
        System.out.println("[TipsDonation] Dialog closed (skipped)");
    }

    private void closeDialog() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        stage.close();
    }

    public double getTipAmount() {
        return tipAmount;
    }

    public double getDonationAmount() {
        return donationAmount;
    }

    public double getTotalAmount() {
        return baseCost + tipAmount + donationAmount;
    }
}

