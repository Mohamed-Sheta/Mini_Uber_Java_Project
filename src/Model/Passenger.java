package Model;
import services.Request;

import java.util.List;
import java.util.Set;

public class Passenger extends Person {
    private int latestDriverRating = 0;
    
    public Passenger(String userSSN, String name, String phoneNumber, String email, double walletBalance, double creditBalance, double accountRating, Location currentLocation, List<RideHistory> rideHistory) {
        super(userSSN, name, phoneNumber, email, walletBalance, creditBalance, accountRating, currentLocation, rideHistory);
    }

    public void setWalletBalance(double walletBalance) {
        updateWalletBalance(walletBalance);
    }

    public void setCreditBalance(double creditBalance) {
        updateCreditBalance(creditBalance);
    }

    public void RateDriver(int rating) {
        if (rating >=1 && rating <=5) {
            this.latestDriverRating = rating;
            System.out.println("Driver rated with: " + rating + " stars");
        }
        else{
            System.out.println("invalid rating must be between 1 and 5");
        }
    }

    public int getLatestDriverRating() {
        return latestDriverRating;
    }

    
    public void ReportProblem(RideManager manager, Set<ProblemType> types, String details) {
        if (manager == null) {
            System.out.println("ERROR: RideManager cannot be null.");
            return;
        }

        Request currentRequest = manager.getRequest();
        if (currentRequest == null || !currentRequest.getPassenger().equals(this)) {
            System.out.println("ERROR: Cannot report problem for a ride not linked to this passenger.");
            return;
        }

        ProblemReport report = new ProblemReport(manager, types, details);

        System.out.println("\n✅ Report Submitted!");
        System.out.println("   Report ID: " + report.getReportId());
        System.out.println("   Linked to RideManager successfully.");
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
        System.out.println("Rating: " + getAccountRating());
    }
}