package Model;

public abstract class Person {
    private String userSSN;
    private String name;
    private String phoneNumber;
    private String email;
    private double walletBalance;
    private double creditBalance;
    private int accountRating;
//    there is list of ridehistory

    public Person(String userSSN, String name, String phoneNumber, String email,
                  double walletBalance, double creditBalance, int accountRating) {
        this.userSSN = userSSN;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.walletBalance = walletBalance;
        this.creditBalance = creditBalance;
        this.accountRating = accountRating;
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

    public abstract void showProfile();
}
