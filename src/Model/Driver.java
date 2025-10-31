package Model;

import java.util.List;

public class Driver extends Person {
     private String licensePlate;
    private String carModel;
    private boolean active;

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

    public void RatePassenger(RideHistory hist, int rating) {
        if (rating >= 1 && rating <= 5) {
            hist.setPassengerRating(rating);
            System.out.println("Passenger rated with: " + rating + " stars");
        }
        else{
            System.out.println("invalid rating must be between 1 and 5");
        }
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