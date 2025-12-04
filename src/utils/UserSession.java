package utils;

import Model.Driver;
import Model.Passenger;
import Model.Person;

/**
 * Singleton class to manage user session across the application
 * Stores the currently logged-in user and provides thread-safe access
 */
public class

UserSession {

    private static UserSession instance;
    private Person currentUser;
    private boolean isDriver;
    private long userId;

    // Private constructor to prevent instantiation
    private UserSession() {
        this.currentUser = null;
        this.isDriver = false;
        this.userId = -1;
    }

    /**
     * Get the singleton instance of UserSession
     * Thread-safe implementation using synchronized
     */
    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Set the current logged-in user
     * @param user The logged-in Person (Passenger or Driver)
     */
    public void setCurrentUser(Person user) {
        this.currentUser = user;
        this.isDriver = (user instanceof Driver);
        this.userId = -1; // Will be set from database
        System.out.println("[UserSession] User set: " + user.getEmail() + " | Is Driver: " + isDriver);
    }

    /**
     * Set the current logged-in user with ID
     * @param user The logged-in Person
     * @param userId The user's database ID
     */
    public void setCurrentUser(Person user, long userId) {
        this.currentUser = user;
        this.isDriver = (user instanceof Driver);
        this.userId = userId;
        System.out.println("[UserSession] User set: " + user.getEmail() + " | ID: " + userId + " | Is Driver: " + isDriver);
    }

    /**
     * Get the current logged-in user
     * @return Current Person object or null if not logged in
     */
    public Person getCurrentUser() {
        return currentUser;
    }

    /**
     * Get the current user's database ID
     * @return User ID or -1 if not set
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Set the user ID
     * @param userId The database ID
     */
    public void setUserId(long userId) {
        this.userId = userId;
    }

    /**
     * Check if a user is currently logged in
     * @return true if user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Check if the current user is a driver
     * @return true if driver, false if passenger or not logged in
     */
    public boolean isDriver() {
        return isDriver && currentUser instanceof Driver;
    }

    /**
     * Check if the current user is a passenger
     * @return true if passenger, false if driver or not logged in
     */
    public boolean isPassenger() {
        return !isDriver && currentUser instanceof Passenger;
    }

    /**
     * Get the current user as a Driver
     * @return Driver object or null if user is not a driver
     */
    public Driver getDriver() {
        if (isDriver && currentUser instanceof Driver) {
            return (Driver) currentUser;
        }
        return null;
    }

    /**
     * Get the current user as a Passenger
     * @return Passenger object or null if user is not a passenger
     */
    public Passenger getPassenger() {
        if (!isDriver && currentUser instanceof Passenger) {
            return (Passenger) currentUser;
        }
        return null;
    }

    /**
     * Update the current user object
     * Useful after profile updates or wallet changes
     * @param updatedUser The updated Person object
     */
    public void updateCurrentUser(Person updatedUser) {
        if (currentUser != null && updatedUser != null) {
            if (currentUser.getEmail().equals(updatedUser.getEmail())) {
                this.currentUser = updatedUser;
                this.isDriver = (updatedUser instanceof Driver);
                System.out.println("[UserSession] User updated: " + updatedUser.getEmail());
            } else {
                System.err.println("[UserSession] Update failed: Email mismatch");
            }
        }
    }

    /**
     * Clear the current session (logout)
     */
    public void clearSession() {
        System.out.println("[UserSession] Clearing session for user: " +
                (currentUser != null ? currentUser.getEmail() : "none"));
        this.currentUser = null;
        this.isDriver = false;
        this.userId = -1;
    }

    /**
     * Get user's email safely
     * @return Email or empty string if not logged in
     */
    public String getUserEmail() {
        return (currentUser != null) ? currentUser.getEmail() : "";
    }

    /**
     * Get user's name safely
     * @return Name or empty string if not logged in
     */
    public String getUserName() {
        return (currentUser != null) ? currentUser.getName() : "";
    }

    /**
     * Get user's wallet balance safely
     * @return Wallet balance or 0.0 if not logged in
     */
    public double getWalletBalance() {
        return (currentUser != null) ? currentUser.getWalletBalance() : 0.0;
    }

    /**
     * Update user's wallet balance in session
     * @param newBalance The new balance amount
     */
    public void updateWalletBalance(double newBalance) {
        if (currentUser != null) {
            currentUser.updateWalletBalance(newBalance);
            System.out.println("[UserSession] Wallet updated: " + newBalance + " EGP");
        }
    }

    /**
     * Check if user session is valid
     * @return true if user is logged in with valid ID
     */
    public boolean isValidSession() {
        return currentUser != null && userId > 0;
    }

    /**
     * Print session info for debugging
     */
    public void printSessionInfo() {
        System.out.println("=== UserSession Info ===");
        System.out.println("Logged In: " + isLoggedIn());
        System.out.println("User: " + (currentUser != null ? currentUser.getEmail() : "none"));
        System.out.println("User ID: " + userId);
        System.out.println("Is Driver: " + isDriver);
        System.out.println("Is Passenger: " + isPassenger());
        System.out.println("Wallet Balance: " + getWalletBalance() + " EGP");
        System.out.println("========================");
    }
}
