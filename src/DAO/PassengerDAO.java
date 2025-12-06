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
}
