package Model;

import services.Request;

import java.util.List;

public class Passenger extends Person {

    public Passenger(String userSSN, String name, String phoneNumber, String email, double walletBalance, double creditBalance, double accountRating, Location currentLocation, List<RideHistory> rideHistory) {
        super(userSSN, name, phoneNumber, email, walletBalance, creditBalance, accountRating, currentLocation, rideHistory);
    }

    public void setWalletBalance(double walletBalance) {
        updateWalletBalance(walletBalance);
    }

    public void setCreditBalance(double creditBalance) {
        updateCreditBalance(creditBalance);
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

    public Request request_ride(Location origin, Location destination, MapGraph mapGraph) {
        if (origin == null || destination == null || mapGraph == null) {
            System.out.println("Error: Origin, destination, and map cannot be null.");
            return null;
        }

        if (origin.equals(destination)) {
            System.out.println("Error: Origin and destination cannot be the same.");
            return null;
        }

        // Create a new ride request with PENDING status
        Request request = new Request(this, origin, destination, Status.Pending, mapGraph);

        System.out.println("\nRide Request Submitted Successfully!");
        System.out.println("Request ID: " + request.getRequestId());
        System.out.println("From: " + origin);
        System.out.println("To: " + destination);
        System.out.println("Estimated Distance: " + String.format("%.2f", request.getDistance()) + " km");
        System.out.println("Estimated Time: " + request.getEstimatedTime() + " minutes");
        System.out.println("Estimated Price: $" + String.format("%.2f", request.getEstimatedPrice()));
        System.out.println("Status: " + request.getStatus());

        return request;
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