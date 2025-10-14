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

    public double getWalletBalance() {
        return WalletBalance;
    }

    public double getCreditBalance() {
        return creditBalance;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getCarModel() {
        return carModel;
    }

    public void addAmount(double amount) {
            WalletBalance += amount;
    }
}



//    public Location getCurrentLocation() {
//        return currentLocation;
//    }
//
}
