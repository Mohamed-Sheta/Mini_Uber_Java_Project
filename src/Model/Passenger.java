package Model;
import services.Payment;
import services.Request;
import services.RideManager;

import java.util.List;
import java.util.Set;

public class Passenger extends Person {
    private int latestDriverRating = 0;

    public Passenger(String userSSN, String name, String phoneNumber, String email, double walletBalance, double creditBalance, Location currentLocation, List<RideHistory> rideHistory) {
        super(userSSN, name, phoneNumber, email, walletBalance, creditBalance, currentLocation, rideHistory);
    }

    public void setWalletBalance(double walletBalance) {
        updateWalletBalance(walletBalance);
    }

    public void setCreditBalance(double creditBalance) {
        updateCreditBalance(creditBalance);
    }

    public void RateDriver(int rating) {
        if (rating >= 1 && rating <= 5) {
            this.latestDriverRating = rating;
            System.out.println("Driver rated with: " + rating + " stars");
        } else {
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

        System.out.println("\n Report Submitted!");
        System.out.println("   Report ID: " + report.getReportId());
        System.out.println("   Linked to RideManager successfully.");
    }

    public Request request_ride(Location origin, Location destination, MapGraph mapGraph) {
        if (origin == null || destination == null||mapGraph == null) {
            System.out.println("Error: Origin, destination, and map cannot be null.");
            return null;
        }

        if (origin.equals(destination)) {
            System.out.println("Error: Origin and destination cannot be the same.");
            return null;
        }

        Request request = new Request(this, origin, destination, Status.Pending, mapGraph);
        if (request == null) {
            System.out.println(" Ride request could not be created (internal error).");
            return null;
        }

        if (Double.isNaN(request.getDistance()) || request.getDistance() <= 0) {
            System.out.println("No valid path found from " + origin.getName() + " to " + destination.getName());
            return null;
        }
        double estimatedPrice = request.getEstimatedPrice();

        if (!Payment.canAfford(this, estimatedPrice)) {
            System.out.println(" Cannot request ride. Insufficient funds!");
            System.out.println("Required: " + estimatedPrice + " EGP | Available: " +
                    (getWalletBalance() + getCreditBalance()) + " EGP");
            return null;
        }

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

    public void cancelRide(RideManager manager) {
        if (manager == null) {
            System.out.println(" Error: RideManager cannot be null.");
            return;
        }

        Request request = manager.getRequest();
        if (request == null || !request.getPassenger().equals(this)) {
            System.out.println(" Error: No ride found for this passenger in the given RideManager.");
            return;
        }

        Status status = request.getStatus();
        if (status == Status.Completed) {
            System.out.println(" Cannot cancel a completed ride!");
            return;
        }
        if (status == Status.Cancelled) {
            System.out.println(" Ride is already cancelled.");
            return;
        }

        final double TOTAL_PENALTY = 20.0;
        final double DRIVER_SHARE = 10.0;
        final double COMPANY_SHARE = 10.0;

        double wallet = this.getWalletBalance();
        double credit = this.getCreditBalance();

        if (wallet >= TOTAL_PENALTY) {
            this.updateWalletBalance(wallet - TOTAL_PENALTY);
        } else if (credit >= TOTAL_PENALTY) {
            this.updateCreditBalance(credit - TOTAL_PENALTY);
        } else {
            this.updateWalletBalance(wallet - TOTAL_PENALTY);
        }

        request.updateStatus(Status.Cancelled);
        System.out.println(" Ride cancelled successfully. Request ID: " + request.getRequestId());
        System.out.println(" Total cancellation penalty deducted from passenger: " + TOTAL_PENALTY + " EGP");

        Driver driver = manager.getCurrentDriver();
        if (driver != null) {
            System.out.println("ℹ Notifying driver " + driver.getName() + " about ride cancellation.");
            manager.getPaymentProcessor().addAmountToDriver(driver, DRIVER_SHARE);
            System.out.println( DRIVER_SHARE + " EGP added to driver's wallet from cancellation fee.");
        }

        System.out.println( COMPANY_SHARE + " EGP goes to the company from cancellation fee.");
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
