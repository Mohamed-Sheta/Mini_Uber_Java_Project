package services;
import Model.Driver;
import Model.Option;
import Model.Passenger;
import Model.PaymentType;

public class Payment
{
    private static int paymentCounter = 1;
    private int paymentId;
    private double amount;
    private PaymentType paymentMethod;
    private Option options;

    private static final double COMPANY_COMMISSION = 0.08;

    public Payment(double amount, PaymentType paymentMethod, Option options) {
        this.paymentId = paymentCounter++;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.options = options;
    }

    public static boolean canAfford(Passenger passenger, double amount) {
        double total = passenger.getWalletBalance() + passenger.getCreditBalance();
        return total >= amount;
    }

    private boolean reduceAmount(Passenger passenger, double amount, String source) {
        if (!canAfford(passenger, amount)) {
            System.out.println(" Transaction rejected: Insufficient balance.");
            return false;
        }

        switch (source.toLowerCase()) {
            case "wallet":
                if (passenger.getWalletBalance() >= amount) {
                    passenger.updateWalletBalance(passenger.getWalletBalance() - amount);
                    return true;
                } else {
                    System.out.println(" Wallet balance insufficient.");
                    return false;
                }

            case "credit":
                if (passenger.getCreditBalance() >= amount) {
                    passenger.updateCreditBalance(passenger.getCreditBalance() - amount);
                    return true;
                } else {
                    System.out.println(" Credit balance insufficient.");
                    return false;
                }

            case "auto":
                if (passenger.getWalletBalance() >= amount) {
                    passenger.updateWalletBalance(passenger.getWalletBalance() - amount);
                } else {
                    double remaining = amount - passenger.getWalletBalance();
                    passenger.updateWalletBalance(0);
                    passenger.updateCreditBalance(passenger.getCreditBalance() - remaining);
                }
                return true;

            default:
                System.out.println(" Invalid payment source.");
                return false;
        }
    }

    public void addAmountToDriver(Driver driver, double amount) {
        driver.updateWalletBalance(driver.getWalletBalance() + amount);
    }


    private void processPayment(Passenger passenger, Driver driver) {
        if (passenger == null || driver == null || amount <= 0) {
            System.out.println(" Payment failed: Invalid passenger/driver or invalid amount.");
            return;
        }

        double commission = amount * COMPANY_COMMISSION;
        double amountAfterCommission = amount - commission;//driver

        double totalChargeToPassenger = amount;//passanger
        double driverNetAmount = amountAfterCommission;

        System.out.println("--- Processing Payment ID: " + paymentId + " ---");

        if (!reduceAmount(passenger, totalChargeToPassenger, paymentMethod.toString())) {
            System.out.println(" Payment Cancelled.");
            return;
        }

        addAmountToDriver(driver, driverNetAmount);

        System.out.println(" Payment successful. Passenger Paid: " + totalChargeToPassenger);
        System.out.println(" Driver received (after 8% cut): " + driverNetAmount);
        System.out.println(" Company commission: " + commission);
    }
    
    public void updateProcessPayment(Passenger passenger, Driver driver) {  // Used when passeneger want to add tips/donation
        if (passenger == null || driver == null) {
            System.out.println(" Invalid Passenger or Driver.");
            return;
        }

        double tips = 0;
        double donation = 0;

        if (options != null) {
            tips = options.getTips();
            donation = options.getDonationAmount();
        }

        double newTotal = amount + tips + donation;
        System.out.println(" Updating payment... Old Amount: " + amount +
                           " | Tips: " + tips + " | Donation: " + donation);

        this.amount = newTotal;

        System.out.println(" New total amount to be paid: " + newTotal);

        processPayment(passenger, driver);
    }

    public String getPaymentDetails() {
        return "Payment ID: " + paymentId +
                ", Amount: " + amount +
                ", Method: " + paymentMethod.toString();
    }

    public int getPaymentId() {
        return paymentId;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentType getPaymentMethod() {
        return paymentMethod;
    }

    public Option getOptions() {
        return options;
    }
}