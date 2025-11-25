package DAO;
import utils.*;
import Model.Driver;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DriverDAO {

    public static class DriverRow {
        public final long id;
        public final String userSSN, name, phone, email;
        public final double wallet, credit;
        public final String currentLocation;
        public final String licensePlate, carModel;
        public final boolean active;
        public final String password;

        public DriverRow(long id, String userSSN, String name, String phone, String email,
                         double wallet, double credit, String currentLocation,
                         String licensePlate, String carModel, boolean active, String password) {

            this.id=id; this.userSSN=userSSN; this.name=name; this.phone=phone; this.email=email;
            this.wallet=wallet; this.credit=credit; this.currentLocation=currentLocation;
            this.licensePlate=licensePlate; this.carModel=carModel; this.active=active;
            this.password=password;
        }

        @Override public String toString(){
            return "DriverRow{id="+id+", plate="+licensePlate+", name="+name+"}";
        }
    }

    // INSERT
    public long insert(Driver d, String currentLocationName) throws SQLException {
        if (d.getName() == null || d.getName().trim().isEmpty() || d.getName().length() > 50)
            throw new IllegalArgumentException("Driver name must be between 1 and 50 characters.");
        if (d.getPhoneNumber() == null || !d.getPhoneNumber().matches("\\d{11}"))
            throw new IllegalArgumentException("Driver phone must be exactly 11 digits.");
        if (d.getEmail() == null || !d.getEmail().endsWith("@gmail.com"))
            throw new IllegalArgumentException("Driver email must end with '@gmail.com'.");
        if (d.getWalletBalance() < 0 || d.getCreditBalance() < 0)
            throw new IllegalArgumentException("Driver balances cannot be negative.");
        if (d.getLicensePlate() == null || d.getLicensePlate().trim().isEmpty())
            throw new IllegalArgumentException("License plate cannot be empty.");
        if (d.getCarModel() == null || d.getCarModel().trim().isEmpty())
            throw new IllegalArgumentException("Car model cannot be empty.");
        final String sql = "INSERT INTO drivers(user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location,license_plate,car_model,active,password) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, d.getUserSSN());
            ps.setString(2, d.getName());
            ps.setString(3, d.getPhoneNumber());
            ps.setString(4, d.getEmail());
            ps.setDouble(5, d.getWalletBalance());
            ps.setDouble(6, d.getCreditBalance());

            if (currentLocationName == null)
                ps.setNull(7, Types.VARCHAR);
            else
                ps.setString(7, currentLocationName);

            ps.setString(8, d.getLicensePlate());
            ps.setString(9, d.getCarModel());
            ps.setBoolean(10, d.isActive());

            // Password should already be hashed by the controller or model
            if (d.getPassword() != null && !d.getPassword().isEmpty()) {
                ps.setString(11, d.getPassword());
            } else {
                ps.setNull(11, Types.VARCHAR);
            }

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    // UPDATE
    public int update(long id, Driver d, String currentLocationName) throws SQLException {
        if (d.getName() == null || d.getName().trim().isEmpty() || d.getName().length() > 50)
            throw new IllegalArgumentException("Driver name must be between 1 and 50 characters.");
        if (d.getPhoneNumber() == null || !d.getPhoneNumber().matches("\\d{11}"))
            throw new IllegalArgumentException("Driver phone must be exactly 11 digits.");
        if (d.getEmail() == null || !d.getEmail().endsWith("@gmail.com"))
            throw new IllegalArgumentException("Driver email must end with '@gmail.com'.");
        if (d.getWalletBalance() < 0 || d.getCreditBalance() < 0)
            throw new IllegalArgumentException("Driver balances cannot be negative.");
        final String sql = "UPDATE drivers SET user_ssn=?, name=?, phone_number=?, email=?, wallet_balance=?, credit_balance=?, current_location=?, license_plate=?, car_model=?, active=?, password=? WHERE id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, d.getUserSSN());
            ps.setString(2, d.getName());
            ps.setString(3, d.getPhoneNumber());
            ps.setString(4, d.getEmail());
            ps.setDouble(5, d.getWalletBalance());
            ps.setDouble(6, d.getCreditBalance());

            if (currentLocationName == null)
                ps.setNull(7, Types.VARCHAR);
            else
                ps.setString(7, currentLocationName);

            ps.setString(8, d.getLicensePlate());
            ps.setString(9, d.getCarModel());
            ps.setBoolean(10, d.isActive());

            // Password should already be hashed by the controller or model
            if (d.getPassword() != null && !d.getPassword().isEmpty()) {
                ps.setString(11, d.getPassword());
            } else {
                ps.setNull(11, Types.VARCHAR);
            }

            ps.setLong(12, id);

            return ps.executeUpdate();
        }
    }

    public int delete(long id) throws SQLException {
        final String sql = "DELETE FROM drivers WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    public List<DriverRow> showAll() throws SQLException {
        final String sql = "SELECT id,user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location,license_plate,car_model,active,password FROM drivers ORDER BY id";
        List<DriverRow> out = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(new DriverRow(
                        rs.getLong("id"),
                        rs.getString("user_ssn"),
                        rs.getString("name"),
                        rs.getString("phone_number"),
                        rs.getString("email"),
                        rs.getDouble("wallet_balance"),
                        rs.getDouble("credit_balance"),
                        rs.getString("current_location"),
                        rs.getString("license_plate"),
                        rs.getString("car_model"),
                        rs.getBoolean("active"),
                        rs.getString("password")
                ));
            }
        }
        return out;
    }

    // Save method for easy registration
    public boolean save(Driver d) {
        try {
            long id = insert(d, null); // null location for new registrations
            return id > 0;
        } catch (SQLException e) {
            System.err.println("Error saving driver: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Get driver by email for login
    public Driver getByEmail(String email) {
        final String sql = "SELECT id,user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location,license_plate,car_model,active,password " +
                "FROM drivers WHERE email=?";

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
                    String licensePlate = rs.getString("license_plate");
                    String carModel = rs.getString("car_model");

                    // Create Driver with empty password first to avoid double hashing
                    Driver driver = new Driver(userSSN, name, phoneNumber, emailAddr, "", licensePlate, carModel);

                    // Set the already-hashed password directly (bypassing the hash in constructor)
                    driver.setPassword(hashedPassword);

                    // Set wallet and credit balance
                    driver.updateWalletBalance(rs.getDouble("wallet_balance"));
                    driver.updateCreditBalance(rs.getDouble("credit_balance"));

                    return driver;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting driver by email: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
