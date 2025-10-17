package Model;

public class Payment
{
    private int paymentId;
    private double amount;
    private PaymentType paymentMethod;
    private Option options;

    public Payment(int paymentId, double amount, PaymentType paymentMethod, Option options) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.options = options;
    }

    public void processPayment(Passenger passenger, Driver driver) {
        if (passenger == null || driver == null || amount <= 0) {
            System.out.println("❌ Payment failed: Invalid passenger/driver or invalid amount.");
            return;
        }

        double totalChargeToPassenger = this.amount;
        double totalToDriver = this.amount;

        // Apply options (tips and donations)
        if (options != null) {
            if (options.getDonationAmount() > 0 && options.getDonationOrganization() != null) {
                totalChargeToPassenger += options.getDonationAmount();
            }
            if (options.getTips() > 0) {
                totalChargeToPassenger += options.getTips();
                totalToDriver += options.getTips();
            }
        }

        System.out.println("--- Processing Payment ID: " + paymentId + " ---");

        // Deduct total from passenger
        passenger.reduceAmount(totalChargeToPassenger, paymentMethod.toString());

        // Add total to driver
        driver.addAmount(totalToDriver);

        System.out.println("✅ Payment successful. Total charged to passenger: " + totalChargeToPassenger);
        System.out.println("💰 Driver received: " + totalToDriver);
    }
    public String getPaymentDetails() {
        return "Payment ID: " + paymentId +
                ", Amount: " + amount +
                ", Method: " + paymentMethod.toString();
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
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
