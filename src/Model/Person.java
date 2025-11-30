package Model;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private String password;
    private Location currentLocation;
    private List<RideHistory> rideHistory;

    public Person(String userSSN, String name, String phoneNumber, String email,
                  double walletBalance, double creditBalance,
                  Location currentLocation, List<RideHistory> rideHistory, String password) {

        this.userSSN = userSSN;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.walletBalance = walletBalance;
        this.creditBalance = creditBalance;
        this.currentLocation = currentLocation;
        this.rideHistory = rideHistory != null ? rideHistory : new ArrayList<>();
        this.accountRating = getAccountRating();
        // Hash password only if not null or empty
        if (password != null && !password.isEmpty()) {
            setPassword(hashPassword(password));
        } else {
            setPassword(null);
        }
    }

    public String getUserSSN() { return userSSN; }
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public double getWalletBalance() { return walletBalance; }
    public double getCreditBalance() { return creditBalance; }
    public Location getCurrentLocation() {return currentLocation;}
    public List<RideHistory> getRideHistory() {return rideHistory;}
    
    public double getAccountRating() {
        this.accountRating = getAverageRating();
        return accountRating;
    }

    public void updateWalletBalance(double walletBalance) {this.walletBalance = walletBalance;}
    public void updateCreditBalance(double creditBalance) {this.creditBalance = creditBalance;}

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public double getAverageRating() {
        System.out.println("rideHistory is null: " + (rideHistory == null));
        int total = 0;
        int count = 0;
        for (RideHistory h : rideHistory) {
            if (this instanceof Driver) {
                total += h.getPassengerRating();
                if (h.getPassengerRating() > 0) count++;
            } else if (this instanceof Passenger) {
                total += h.getDriverRating();
                if (h.getDriverRating() > 0) count++;
            }
        }
        return count == 0 ? 0 : (double) total / count;
    }

    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return ""; // Return empty string for null/empty passwords
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public abstract void showProfile();
}