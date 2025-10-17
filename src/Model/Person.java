package Model;
import java.util.ArrayList;
import java.util.List;

public abstract class Person {
    private String userSSN;
    private String name;
    private String phoneNumber;
    private String email;
    private double walletBalance;
    private double creditBalance;
    private double accountRating;
    private List<RideHistory> rideHistory;
//    there is list of ridehistory

    public Person(String userSSN, String name, String phoneNumber, String email,
                  double walletBalance, double creditBalance, double accountRating) {
        this.userSSN = userSSN;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.walletBalance = walletBalance;
        this.creditBalance = creditBalance;
        this.accountRating = accountRating;
        this.rideHistory = new ArrayList<RideHistory>();
    }

    public String getUserSSN() {
        return userSSN;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public double getCreditBalance() {
        return creditBalance;
    }

//    public int getAccountRating() {
//        return accountRating;
//    add ride history
//    }

    protected void updateWalletBalance(double walletBalance) {
        this.walletBalance = walletBalance;
    }

    protected void updateCreditBalance(double creditBalance) {
        this.creditBalance = creditBalance;
    }

    public double getAverageRating() {
        System.out.println("rideHistory is null: " + (rideHistory == null));
        int total = 0;
        int count = 0;
        for (RideHistory h : rideHistory) {
            if (this instanceof Driver) {
                // تقييم الراكب للسائق
                total += h.getPassengerRating();
                if (h.getPassengerRating() > 0) count++;
            } else if (this instanceof Passenger) {
                // تقييم السائق للراكب
                total += h.getDriverRating();
                if (h.getDriverRating() > 0) count++;
            }
        }
        return count == 0 ? 0 : (double) total / count;
    }

    public abstract void showProfile();
}
