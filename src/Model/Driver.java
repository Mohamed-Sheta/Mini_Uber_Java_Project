package Model;

public class Driver extends Person {
     private String licensePlate;
    private String carModel;
    private boolean active;
    private Location currentLocation;

    public Driver(String licensePlate, String carModel, boolean active, Location currentLocation,
                  String userSSN, String name, String phoneNumber, String email,
                  double walletBalance, double creditBalance, double accountRating) {
        super(userSSN, name, phoneNumber, email, walletBalance, creditBalance, accountRating);
        this.licensePlate = licensePlate;
        this.carModel = carModel;
        this.active = active;
        this.currentLocation = currentLocation;
    }


    @Override
    public void showProfile() {

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

    public void addAmount(double amount) {
        double currentBalance = getWalletBalance(); // جلب الرصيد الحالي باستخدام الـ Getter
        updateWalletBalance(currentBalance + amount); // تحديث الرصيد باستخدام الـ Protected Setter
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
    public Location getCurrentLocation() {
        return currentLocation;
    }
}
//
