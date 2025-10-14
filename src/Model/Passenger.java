package Model;

public class Passenger extends Person {
    private Location currentLocation;
    private Location destination;

    public Passenger(String userSSN, String name, String phoneNumber, String email,
                     double walletBalance, double creditBalance, int accountRating) {
        super(userSSN, name, phoneNumber, email, walletBalance, creditBalance, accountRating);
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }

    public Location getDestination() {
        return destination;
    }

    public void setDestination(Location destination) {
        this.destination = destination;
    }

    public void setWalletBalance(double walletBalance) {
        updateWalletBalance(walletBalance);
    }

    public void setCreditBalance(double creditBalance) {
        updateCreditBalance(creditBalance);
    }

    public boolean canAfford(double amount) {
        double total = getWalletBalance() + getCreditBalance();
        return total >= amount;
    }

    public boolean reduceAmount(double amount, String source) {
        if (!canAfford(amount)) {
            System.out.println("❌ Transaction rejected: Insufficient balance.");
            System.out.println("Available total balance: " + (getWalletBalance() + getCreditBalance()));
            return false;
        }

        switch (source.toLowerCase()) {
            case "wallet":
                if (getWalletBalance() >= amount) {
                    updateWalletBalance(getWalletBalance() - amount);
                    System.out.println("✅ Paid $" + amount + " from wallet.");
                    return true;
                } else {
                    System.out.println("❌ Wallet balance insufficient.");
                    return false;
                }

            case "credit":
                if (getCreditBalance() >= amount) {
                    updateCreditBalance(getCreditBalance() - amount);
                    System.out.println("✅ Paid $" + amount + " from credit.");
                    return true;
                } else {
                    System.out.println("❌ Credit balance insufficient.");
                    return false;
                }

            case "auto": // default behavior (use both)
                if (getWalletBalance() >= amount) {
                    updateWalletBalance(getWalletBalance() - amount);
                } else {
                    double remaining = amount - getWalletBalance();
                    updateWalletBalance(0);
                    updateCreditBalance(getCreditBalance() - remaining);
                }
                System.out.println("✅ Payment successful. Amount deducted: " + amount);
                System.out.println("Remaining wallet: " + getWalletBalance());
                System.out.println("Remaining credit: " + getCreditBalance());
                return true;

            default:
                System.out.println("⚠️ Invalid payment source. Use 'wallet', 'credit', or 'auto'.");
                return false;
        }
    }

    public void RateDriver(RideHistory hist, int rating) {
        if (rating >=1 && rating <=5) {
            hist.setDriverRating(rating);
            System.out.println("Driver rated with: " + rating + " stars");
        }
        else{
            System.out.println("invalid rating must be between 1 and 5");
        }
    }

    @Override
    public void showProfile() {
        System.out.println("Passenger Profile:");
        System.out.println("Name: " + getName());
        System.out.println("SSN: " + getUserSSN());
        System.out.println("Phone: " + getPhoneNumber());
        System.out.println("Email: " + getEmail());
        System.out.println("Wallet Balance: " + getWalletBalance());
        System.out.println("Credit Balance: " + getCreditBalance());
//        System.out.println("Rating: " + getAccountRating());
    }
}



//    public ProblemReport ReportProblem(RideManager manager, Set<ProblemType> types, String details) {
//
//        if (manager == null || !manager.getRequest().getPassenger().equals(this)) {
//            System.out.println("ERROR: Cannot report problem for a ride not linked to this passenger.");
//            return null;
//        }
//
//        ProblemReport report = new ProblemReport(
//            manager,
//            types,
//            details
//        );
//
//        System.out.println("\n✅ Report Submitted!");
//        System.out.println("   Report ID: " + report.getReportId());
//        System.out.println("   Manager Linked.");
//
//        return report;
//    }