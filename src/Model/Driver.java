package Model;

import services.Request;

import java.util.*;

public class Driver extends Person {
     private String licensePlate;
    private String carModel;
    private boolean active;
    private int latestPassengerRating = 0;
    public Driver(String licensePlate, String carModel, boolean active, String userSSN, String name, String phoneNumber, String email, double walletBalance, double creditBalance, double accountRating, Location currentLocation, List<RideHistory> rideHistory) {
        super(userSSN, name, phoneNumber, email, walletBalance, creditBalance, accountRating, currentLocation, rideHistory);
        this.licensePlate = licensePlate;
        this.carModel = carModel;
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getCarModel() {
        return carModel;
    }

    public void RatePassenger(int rating) {
        if (rating >= 1 && rating <= 5) {
            this.latestPassengerRating = rating;
            System.out.println("Passenger rated with: " + rating + " stars");
        }
        else{
            System.out.println("invalid rating must be between 1 and 5");
        }
    }

    public void viewRideRequests(Queue<Request> requests) {
        if (requests.isEmpty()) {
            System.out.println("🚫 No ride requests available.");
            return;
        }

        System.out.println("\n📋 Ride Requests Sorted by Distance then Time:");

        List<Request> list = new ArrayList<>(requests);

        Collections.sort(list, Comparator
                .comparingDouble(Request::getDistance)
                .thenComparingInt(Request::getEstimatedTime));

        for (Request req : list) {
            System.out.println("- Passenger: " + req.getPassenger().getName()
                    + " | Distance: " + req.getDistance() + " km"
                    + " | Time: " + req.getEstimatedTime() + " min"
                    + " | Status: " + req.getStatus());
        }

        requests.clear();
        requests.addAll(list);
    }

    public boolean Accept_Request(Queue<Request> requests) {
        if (requests.isEmpty()) {
            System.out.println("⚠ No ride requests available to accept.");
            return false;
        }

        Request req = requests.peek();
        req.updateStatus(Status.Accepted);
        System.out.println("✅ Request Accepted by Driver for Passenger: "
                + req.getPassenger().getName()
                + " | Distance: " + req.getDistance() + " km"
                + " | Estimated Time: " + req.getEstimatedTime() + " min");

        requests.remove(req);

        return true;
    }

    public int getLatestPassengerRating() {
        return latestPassengerRating;
    }
    @Override
    public void showProfile() {
        System.out.println("Driver Profile:");
        System.out.println("Name: " + getName());
        System.out.println("SSN: " + getUserSSN());
        System.out.println("Phone: " + getPhoneNumber());
        System.out.println("Email: " + getEmail());
        System.out.println("Wallet Balance: $" + getWalletBalance());
        System.out.println("Credit Balance: $" + getCreditBalance());
        System.out.println("License Plate: " + licensePlate);
        System.out.println("Car Model: " + carModel);
        System.out.println("Active: " + active);
        System.out.println("Current Location: " + (getCurrentLocation() != null ? getCurrentLocation().getName() : "Not set"));
    }
}