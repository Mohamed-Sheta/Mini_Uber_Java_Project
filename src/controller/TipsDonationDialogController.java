package controller;

import DAO.PassengerDAO;
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

    // DAO for wallet operations
    private final PassengerDAO passengerDAO = new PassengerDAO();
    private long passengerId = -1;

    public void setRideInfo(double rideCost, Passenger passenger, Driver driver) {
        this.baseCost = rideCost;
        this.passenger = passenger;
        this.driver = driver;

        // Get passenger ID for wallet operations
        if (passenger != null && passenger.getEmail() != null) {
            this.passengerId = passengerDAO.getIdByEmail(passenger.getEmail());
            System.out.println("[TipsDonation] Passenger ID: " + passengerId);
        }

        rideCostLabel.setText(String.format("%.2f EGP", rideCost));
        updateTotal();

        // Add real-time input validation to prevent negative values and enforce max limit
        tipField.textProperty().addListener((obs, oldVal, newVal) -> {
            final double MAX_TIP = 50.0;

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

                // Check if value exceeds maximum limit
                if (!filtered.isEmpty()) {
                    try {
                        double value = Double.parseDouble(filtered);
                        if (value > MAX_TIP) {
                            showError(String.format("❌ Tip cannot exceed %.0f EGP!", MAX_TIP));
                            tipField.setText(oldVal);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        // Invalid number format, revert to old value
                        tipField.setText(oldVal);
                        return;
                    }
                }

                if (!filtered.equals(newVal)) {
                    tipField.setText(filtered);
                }
            }
            clearError();
            updateTotal();
        });

        donationField.textProperty().addListener((obs, oldVal, newVal) -> {
            final double MAX_DONATION = 50.0;

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

                // Check if value exceeds maximum limit
                if (!filtered.isEmpty()) {
                    try {
                        double value = Double.parseDouble(filtered);
                        if (value > MAX_DONATION) {
                            showError(String.format("❌ Donation cannot exceed %.0f EGP!", MAX_DONATION));
                            donationField.setText(oldVal);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        // Invalid number format, revert to old value
                        donationField.setText(oldVal);
                        return;
                    }
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
        // Maximum allowed values
        final double MAX_TIP = 50.0;
        final double MAX_DONATION = 50.0;

        // Capture amounts BEFORE validation
        tipAmount = parseAmount(tipField.getText());
        donationAmount = parseAmount(donationField.getText());

        // Final validation: ensure non-negative values
        if (tipAmount < 0) tipAmount = 0.0;
        if (donationAmount < 0) donationAmount = 0.0;

        System.out.println("[TipsDonation] Confirm clicked - Tip: " + tipAmount + ", Donation: " + donationAmount + ", Ride Cost: " + baseCost);

        // Validate maximum limits
        if (tipAmount > MAX_TIP) {
            showError(String.format("❌ Tip cannot exceed %.2f EGP. Please enter a valid amount.", MAX_TIP));
            System.err.println("[TipsDonation] ERROR: Tip exceeds maximum limit: " + tipAmount + " > " + MAX_TIP);
            return;
        }

        if (donationAmount > MAX_DONATION) {
            showError(String.format("❌ Donation cannot exceed %.2f EGP. Please enter a valid amount.", MAX_DONATION));
            System.err.println("[TipsDonation] ERROR: Donation exceeds maximum limit: " + donationAmount + " > " + MAX_DONATION);
            return;
        }

        // Compute total cost including ride cost
        double totalCost = baseCost + tipAmount + donationAmount;
        System.out.println("[TipsDonation] Total cost breakdown:");
        System.out.println("[TipsDonation]   - Ride Cost: " + baseCost + " EGP");
        System.out.println("[TipsDonation]   - Tip: " + tipAmount + " EGP");
        System.out.println("[TipsDonation]   - Donation: " + donationAmount + " EGP");
        System.out.println("[TipsDonation]   - TOTAL: " + totalCost + " EGP");

        // Validate passenger ID
        if (passengerId <= 0) {
            showError("❌ Cannot process transaction: Invalid passenger ID");
            System.err.println("[TipsDonation] ERROR: Invalid passenger ID: " + passengerId);
            return;
        }

        // Check wallet balance against TOTAL cost (ride + tip + donation)
        double currentBalance = passengerDAO.getWalletBalance(passengerId);
        System.out.println("[TipsDonation] Current wallet balance: " + currentBalance + " EGP");
        System.out.println("[TipsDonation] Total cost required: " + totalCost + " EGP");

        if (currentBalance < 0) {
            showError("❌ Error retrieving wallet balance");
            System.err.println("[TipsDonation] ERROR: Could not retrieve wallet balance");
            return;
        }

        if (currentBalance < totalCost) {
            showError(String.format("❌ Insufficient balance. You only have %.2f EGP", currentBalance));
            System.err.println("[TipsDonation] ERROR: Insufficient balance - Has: " + currentBalance +
                             ", Needs: " + totalCost);
            return;
        }

        System.out.println("[TipsDonation] ✓ Balance validation passed");

        // Deduct the TOTAL cost from wallet (ride + tip + donation)
        System.out.println("[TipsDonation] Deducting total cost: " + totalCost + " EGP");
        boolean deductionSuccess = passengerDAO.deductFromWallet(passengerId, totalCost);

        if (!deductionSuccess) {
            showError("❌ Failed to process payment");
            System.err.println("[TipsDonation] ERROR: Failed to deduct total cost from wallet");
            return;
        }

        System.out.println("[TipsDonation] ✓ Total cost deducted successfully");

        // Record transactions for each component
        // 1. Record ride cost transaction
        passengerDAO.recordTransaction(passengerId, baseCost, "RIDE");
        System.out.println("[TipsDonation] ✓ Ride cost transaction recorded: " + baseCost + " EGP");

        // 2. Record tip transaction (if > 0)
        if (tipAmount > 0) {
            passengerDAO.recordTransaction(passengerId, tipAmount, "TIP");
            System.out.println("[TipsDonation] ✓ Tip transaction recorded: " + tipAmount + " EGP");
        }

        // 3. Record donation transaction (if > 0)
        if (donationAmount > 0) {
            passengerDAO.recordTransaction(passengerId, donationAmount, "DONATION");
            System.out.println("[TipsDonation] ✓ Donation transaction recorded: " + donationAmount + " EGP");
        }

        // Update passenger's local wallet balance
        if (passenger != null) {
            double newBalance = currentBalance - totalCost;
            passenger.setWalletBalance(newBalance);
            System.out.println("[TipsDonation] ✓ Passenger local balance updated: " + newBalance + " EGP");
        }

        System.out.println("[TipsDonation] ✓ All transactions completed successfully");
        System.out.println("[TipsDonation] ✓ Final breakdown: Ride=" + baseCost + ", Tip=" + tipAmount + ", Donation=" + donationAmount);

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

