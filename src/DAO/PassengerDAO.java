package DAO;
import utils.*;
import Model.Passenger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PassengerDAO {

    public static class PassengerRow {
        public final long id;
        public final String userSSN, name, phone, email;
        public final double wallet, credit;
        public final String currentLocation;
        public final String password;

        public PassengerRow(long id, String userSSN, String name, String phone, String email,
                            double wallet, double credit, String currentLocation, String password) {
            this.id=id; this.userSSN=userSSN; this.name=name; this.phone=phone; this.email=email;
            this.wallet=wallet; this.credit=credit; this.currentLocation=currentLocation;
            this.password=password;
        }

        @Override public String toString(){
            return "PassengerRow{id="+id+", ssn="+userSSN+", name="+name+"}";
        }
    }

    public long insert(Passenger p, String currentLocationName) throws SQLException {
        if (p.getName() == null || p.getName().trim().isEmpty() || p.getName().length() > 50)
            throw new IllegalArgumentException("Passenger name must be between 1 and 50 characters.");
        if (p.getPhoneNumber() == null || !p.getPhoneNumber().matches("\\d{11}"))
            throw new IllegalArgumentException("Phone number must be exactly 11 digits.");
        if (p.getEmail() == null || !p.getEmail().contains("@"))
            throw new IllegalArgumentException("Email must be valid.");
        if (p.getWalletBalance() < 0 || p.getCreditBalance() < 0)
            throw new IllegalArgumentException("Balances cannot be negative.");
        final String sql = "INSERT INTO passengers(user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location,password) " +
                "VALUES (?,?,?,?,?,?,?,?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getUserSSN());
            ps.setString(2, p.getName());
            ps.setString(3, p.getPhoneNumber());
            ps.setString(4, p.getEmail());
            ps.setDouble(5, p.getWalletBalance());
            ps.setDouble(6, p.getCreditBalance());

            if (currentLocationName == null)
                ps.setNull(7, Types.VARCHAR);
            else
                ps.setString(7, currentLocationName);

            // Password should already be hashed by the controller or model
            if (p.getPassword() != null && !p.getPassword().isEmpty()) {
                ps.setString(8, p.getPassword());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    public int update(long id, Passenger p, String currentLocationName) throws SQLException {
        if (p.getName() == null || p.getName().trim().isEmpty() || p.getName().length() > 50)
            throw new IllegalArgumentException("Passenger name must be between 1 and 50 characters.");
        if (p.getPhoneNumber() == null || !p.getPhoneNumber().matches("\\d{11}"))
            throw new IllegalArgumentException("Phone number must be exactly 11 digits.");
        if (p.getEmail() == null || !p.getEmail().contains("@"))
            throw new IllegalArgumentException("Email must be valid.");
        if (p.getWalletBalance() < 0 || p.getCreditBalance() < 0)
            throw new IllegalArgumentException("Balances cannot be negative.");
        final String sql = "UPDATE passengers SET user_ssn=?, name=?, phone_number=?, email=?, " +
                "wallet_balance=?, credit_balance=?, current_location=?, password=? WHERE id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, p.getUserSSN());
            ps.setString(2, p.getName());
            ps.setString(3, p.getPhoneNumber());
            ps.setString(4, p.getEmail());
            ps.setDouble(5, p.getWalletBalance());
            ps.setDouble(6, p.getCreditBalance());

            if (currentLocationName == null)
                ps.setNull(7, Types.VARCHAR);
            else
                ps.setString(7, currentLocationName);

            // Password should already be hashed by the controller or model
            if (p.getPassword() != null && !p.getPassword().isEmpty()) {
                ps.setString(8, p.getPassword());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }

            ps.setLong(9, id);

            return ps.executeUpdate();
        }
    }

    public int delete(long id) throws SQLException {
        final String sql = "DELETE FROM passengers WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    public List<PassengerRow> showAll() throws SQLException {
        final String sql = "SELECT id,user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location,password " +
                "FROM passengers ORDER BY id";

        List<PassengerRow> out = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(new PassengerRow(
                        rs.getLong("id"),
                        rs.getString("user_ssn"),
                        rs.getString("name"),
                        rs.getString("phone_number"),
                        rs.getString("email"),
                        rs.getDouble("wallet_balance"),
                        rs.getDouble("credit_balance"),
                        rs.getString("current_location"),
                        rs.getString("password")
                ));
            }
        }
        return out;
    }

    // Save method for easy registration
    public boolean save(Passenger p) {
        try {
            long id = insert(p, null); // null location for new registrations
            return id > 0;
        } catch (SQLException e) {
            System.err.println("Error saving passenger: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Get passenger by email for login
    public Passenger getByEmail(String email) {
        final String sql = "SELECT id,user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location,password " +
                "FROM passengers WHERE email=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String userSSN = rs.getString("user_ssn");
                    String name = rs.getString("name");
                    String phoneNumber = rs.getString("phone_number");
                    String emailAddr = rs.getString("email");
                    String hashedPassword = rs.getString("password"); // Already hashed in DB
                    double walletBalance = rs.getDouble("wallet_balance");
                    double creditBalance = rs.getDouble("credit_balance");

                    // Use full constructor with already-hashed password
                    Passenger passenger = new Passenger(
                        userSSN, name, phoneNumber, emailAddr,
                        walletBalance, creditBalance,
                        null, // currentLocation
                        new java.util.ArrayList<>(), // rideHistory
                        hashedPassword // already hashed password
                    );

                    return passenger;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting passenger by email: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Cross-table validation: Check if email exists in drivers table
     * Used during passenger registration to prevent duplicate accounts across tables
     * @param email the email to check
     * @return true if email exists in drivers table, false otherwise
     */
    public boolean emailExistsInDrivers(String email) {
        final String sql = "SELECT id FROM drivers WHERE email=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Returns true if email found
            }
        } catch (SQLException e) {
            System.err.println("Error checking email in drivers table: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get passenger ID by email
     * @param email the email to search for
     * @return the passenger ID or -1 if not found
     */
    public long getIdByEmail(String email) {
        final String sql = "SELECT id FROM passengers WHERE email=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting passenger ID by email: " + e.getMessage());
            e.printStackTrace();
        }
        return -1L;
    }

    /**
     * Get the current wallet balance for a passenger
     * @param passengerId the passenger ID
     * @return the wallet balance, or -1 if passenger not found
     */
    public double getWalletBalance(long passengerId) {
        final String sql = "SELECT wallet_balance FROM passengers WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, passengerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("wallet_balance");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting wallet balance: " + e.getMessage());
            e.printStackTrace();
        }
        return -1.0;
    }

    /**
     * Deduct an amount from passenger's wallet balance
     * @param passengerId the passenger ID
     * @param amount the amount to deduct (must be positive)
     * @return true if successful, false otherwise
     */
    public boolean deductFromWallet(long passengerId, double amount) {
        if (amount <= 0) {
            System.err.println("Cannot deduct negative or zero amount");
            return false;
        }

        final String sql = "UPDATE passengers SET wallet_balance = wallet_balance - ? WHERE id = ? AND wallet_balance >= ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setLong(2, passengerId);
            ps.setDouble(3, amount); // Ensure balance is sufficient

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deducting from wallet: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update only the passenger's current_location
     * Used when passenger requests a ride (set to pickup location) or completes a ride (set to destination)
     * @param passengerId the passenger ID
     * @param locationName the location name (or null to clear)
     * @return true if successful, false otherwise
     */
    public boolean updateCurrentLocation(long passengerId, String locationName) {
        final String sql = "UPDATE passengers SET current_location = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (locationName == null) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, locationName);
            }
            ps.setLong(2, passengerId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("[PassengerDAO] ✅ Updated current_location for passenger ID " + passengerId + " to: " + locationName);
                return true;
            } else {
                System.err.println("[PassengerDAO] ⚠️ No passenger found with ID: " + passengerId);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("[PassengerDAO] ❌ Error updating current_location: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Record a tip or donation transaction
     * @param passengerId the passenger ID
     * @param amount the transaction amount (will be stored as negative)
     * @param type the transaction type ('TIP' or 'DONATION')
     * @return true if successful, false otherwise
     */
    public boolean recordTransaction(long passengerId, double amount, String type) {
        // Note: Since there's no dedicated transactions table for user transactions,
        // we'll log this for now. In a production system, you'd create a transactions table.
        System.out.println("[Transaction] Passenger ID: " + passengerId +
                         ", Amount: -" + amount + " EGP, Type: " + type +
                         ", Timestamp: " + java.time.LocalDateTime.now());

        // For now, just return true. In production, you'd insert into a transactions table:
        // INSERT INTO user_transactions (passenger_id, amount, type, timestamp) VALUES (?, ?, ?, NOW())
        return true;
    }
}
