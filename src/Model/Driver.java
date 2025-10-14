package Model;

public class Driver extends Person {
    private String licensePlate;
    private String carModel;
    private boolean active;
    private Location currentLocation;

    public Driver(String licensePlate, String carModel, boolean active, String User_SSN, String name, String PhoneNumber, String Email, float WalletBalance, float creditBalance, int AccountRating) {
        super(User_SSN, name, PhoneNumber, Email, WalletBalance, creditBalance, AccountRating);
        this.licensePlate = licensePlate;
        this.carModel = carModel;
        this.active = active;
    }


    @Override
    public void showProfile() {

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
}



//    public Location getCurrentLocation() {
//        return currentLocation;
//    }
//
