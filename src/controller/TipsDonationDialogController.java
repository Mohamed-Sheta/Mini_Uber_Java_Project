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
    @FXML private Label errorLabel;
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

        // Add real-time input validation to prevent negative values
        tipField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Filter out negative signs and invalid characters
            if (newVal != null && !newVal.isEmpty()) {
                // Check for negative sign and show error
                if (newVal.contains("-")) {
                    showError("❌ Tip cannot be negative!");
                    tipField.setText(oldVal);
                    return;
                }

                // Remove any negative signs
                String filtered = newVal.replaceAll("-", "");
                // Only allow digits and decimal point
                filtered = filtered.replaceAll("[^0-9.]", "");
                // Prevent multiple decimal points
                if (filtered.indexOf('.') != filtered.lastIndexOf('.')) {
                    filtered = oldVal;
                }
                if (!filtered.equals(newVal)) {
                    tipField.setText(filtered);
                }
            }
            clearError();
            updateTotal();
        });

        donationField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Filter out negative signs and invalid characters
            if (newVal != null && !newVal.isEmpty()) {
                // Check for negative sign and show error
                if (newVal.contains("-")) {
                    showError("❌ Donation cannot be negative!");
                    donationField.setText(oldVal);
                    return;
                }

                // Remove any negative signs
                String filtered = newVal.replaceAll("-", "");
                // Only allow digits and decimal point
                filtered = filtered.replaceAll("[^0-9.]", "");
                // Prevent multiple decimal points
                if (filtered.indexOf('.') != filtered.lastIndexOf('.')) {
                    filtered = oldVal;
                }
                if (!filtered.equals(newVal)) {
                    donationField.setText(filtered);
                }
            }
            clearError();
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
            // Ensure value is non-negative and not infinity/NaN
            if (value < 0 || Double.isInfinite(value) || Double.isNaN(value)) {
                return 0.0;
            }
            // Round to 2 decimal places to prevent floating point precision issues
            return Math.round(value * 100.0) / 100.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @FXML
    public void onConfirm() {
        // Capture amounts BEFORE closing dialog
        tipAmount = parseAmount(tipField.getText());
        donationAmount = parseAmount(donationField.getText());

        // Final validation: ensure non-negative values
        if (tipAmount < 0) tipAmount = 0.0;
        if (donationAmount < 0) donationAmount = 0.0;

        System.out.println("[TipsDonation] Confirm clicked - Tip: " + tipAmount + ", Donation: " + donationAmount);
        System.out.println("[TipsDonation] Validation passed: Both values are non-negative");

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

    /**
     * Show error message to user
     */
    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px; -fx-font-weight: bold;");
            errorLabel.setVisible(true);
        }
        System.out.println("[TipsDonation] ERROR: " + message);
    }

    /**
     * Clear error message
     */
    private void clearError() {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
        }
    }
}

