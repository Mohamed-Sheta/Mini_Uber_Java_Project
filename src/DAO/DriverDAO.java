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
        if (d.getEmail() == null || !d.getEmail().contains("@"))
            throw new IllegalArgumentException("Driver email must be valid.");
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
        if (d.getEmail() == null || !d.getEmail().contains("@"))
            throw new IllegalArgumentException("Driver email must be valid.");
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
                    double walletBalance = rs.getDouble("wallet_balance");
                    double creditBalance = rs.getDouble("credit_balance");
                    boolean active = rs.getBoolean("active");

                    // Use full constructor with shouldHashPassword=false to avoid re-hashing
                    Driver driver = new Driver(
                        licensePlate, carModel, active,
                        userSSN, name, phoneNumber, emailAddr,
                        walletBalance, creditBalance,
                        null, // currentLocation
                        new java.util.ArrayList<>(), // rideHistory
                        hashedPassword // already hashed password
                    );

                    return driver;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting driver by email: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Get driver ID by email
    public Long getDriverIdByEmail(String email) {
        final String sql = "SELECT id FROM drivers WHERE email=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting driver ID by email: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get driver by ID - Always fetches fresh data from database
     * Use this method to get updated driver data after balance changes
     * @param driverId the driver's ID
     * @return Driver object with latest data from database, or null if not found
     */
    public Driver getDriverById(long driverId) {
        final String sql = "SELECT id,user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location,license_plate,car_model,active,password " +
                "FROM drivers WHERE id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, driverId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String userSSN = rs.getString("user_ssn");
                    String name = rs.getString("name");
                    String phoneNumber = rs.getString("phone_number");
                    String emailAddr = rs.getString("email");
                    String hashedPassword = rs.getString("password");
                    String licensePlate = rs.getString("license_plate");
                    String carModel = rs.getString("car_model");
                    double walletBalance = rs.getDouble("wallet_balance");
                    double creditBalance = rs.getDouble("credit_balance");
                    boolean active = rs.getBoolean("active");

                    // Use full constructor with shouldHashPassword=false to avoid re-hashing
                    Driver driver = new Driver(
                        licensePlate, carModel, active,
                        userSSN, name, phoneNumber, emailAddr,
                        walletBalance, creditBalance,
                        null, // currentLocation
                        new java.util.ArrayList<>(), // rideHistory
                        hashedPassword // already hashed password
                    );

                    System.out.println("[DriverDAO] ✅ Fetched fresh driver data by ID: " + driverId +
                                      " (wallet=" + String.format("%.2f", walletBalance) + " EGP)");

                    return driver;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DriverDAO] Error getting driver by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Set driver to ONLINE status (active = true) after ride completion
     * @param driverId the driver's ID
     * @return true if successful, false otherwise
     */
    public boolean setDriverOnline(long driverId) {
        final String sql = "UPDATE drivers SET active = true WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("[DriverDAO] ✅ Driver ID " + driverId + " set to ONLINE (active=true)");
                return true;
            } else {
                System.err.println("[DriverDAO] ❌ Failed to update driver status - no rows affected");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[DriverDAO] Error setting driver online: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update driver's wallet balance after completing a ride
     * @param driverId the driver's ID
     * @param fareAmount the ride fare amount to add to wallet
     * @return true if successful, false otherwise
     */
    public boolean updateDriverWalletAfterRide(long driverId, double fareAmount) {
        final String sql = "UPDATE drivers SET wallet_balance = wallet_balance + ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, fareAmount);
            ps.setLong(2, driverId);
            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("[DriverDAO] ✅ Driver wallet updated: +" + String.format("%.2f", fareAmount) + " EGP (ID: " + driverId + ")");
                return true;
            } else {
                System.err.println("[DriverDAO] ❌ Failed to update driver wallet - no rows affected");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[DriverDAO] Error updating driver wallet: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Get driver's current wallet balance from database
     * ALWAYS queries the database for the latest value
     * @param driverId the driver's ID
     * @return current wallet balance, or -1 if error, or 0 if not found
     */
    public double getDriverBalance(long driverId) {
        System.out.println("[DriverDAO.getDriverBalance] Called with driver ID: " + driverId);
        final String sql = "SELECT wallet_balance FROM drivers WHERE id = ?";
        try (Connection con = DBConnection.getConnection()) {
            System.out.println("[DriverDAO.getDriverBalance] Database connection established: " + (con != null));

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, driverId);
                System.out.println("[DriverDAO.getDriverBalance] Executing query: " + sql);
                System.out.println("[DriverDAO.getDriverBalance] With parameter: driver_id = " + driverId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double balance = rs.getDouble("wallet_balance");
                        System.out.println("[DriverDAO.getDriverBalance] ✅ SUCCESS: Retrieved balance = $" +
                                          String.format("%.2f", balance) + " for driver ID: " + driverId);
                        return balance;
                    } else {
                        System.err.println("[DriverDAO.getDriverBalance] ⚠️ WARNING: No driver found with ID: " + driverId);
                        System.err.println("[DriverDAO.getDriverBalance] ⚠️ The driver may not exist in the database!");
                        return 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[DriverDAO.getDriverBalance] ❌ SQL ERROR: " + e.getMessage());
            System.err.println("[DriverDAO.getDriverBalance] ❌ SQL State: " + e.getSQLState());
            System.err.println("[DriverDAO.getDriverBalance] ❌ Error Code: " + e.getErrorCode());
            e.printStackTrace();
            return -1; // Return -1 to indicate error
        }
    }

    /**
     * Update driver's balance to a specific value
     * Use this after calculating the new balance (currentBalance + fare)
     * @param driverId the driver's ID
     * @param newBalance the new balance value to set
     * @return true if successful, false otherwise
     */
    public boolean updateDriverBalance(long driverId, double newBalance) {
        System.out.println("[DriverDAO.updateDriverBalance] Called with driver ID: " + driverId + ", new balance: $" + String.format("%.2f", newBalance));
        final String sql = "UPDATE drivers SET wallet_balance = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection()) {
            System.out.println("[DriverDAO.updateDriverBalance] Database connection established: " + (con != null));

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDouble(1, newBalance);
                ps.setLong(2, driverId);

                System.out.println("[DriverDAO.updateDriverBalance] Executing update: " + sql);
                System.out.println("[DriverDAO.updateDriverBalance] Parameters: wallet_balance = $" +
                                 String.format("%.2f", newBalance) + ", driver_id = " + driverId);

                int rowsUpdated = ps.executeUpdate();
                System.out.println("[DriverDAO.updateDriverBalance] Rows affected: " + rowsUpdated);

                if (rowsUpdated > 0) {
                    System.out.println("[DriverDAO.updateDriverBalance] ✅✅✅ SUCCESS! ✅✅✅");
                    System.out.println("[DriverDAO.updateDriverBalance] ✅ Driver balance updated to: $" +
                                     String.format("%.2f", newBalance) + " (ID: " + driverId + ")");

                    // Verify the update by reading back the value
                    String verifySQL = "SELECT wallet_balance FROM drivers WHERE id = ?";
                    try (PreparedStatement verifyPS = con.prepareStatement(verifySQL)) {
                        verifyPS.setLong(1, driverId);
                        try (ResultSet rs = verifyPS.executeQuery()) {
                            if (rs.next()) {
                                double verifiedBalance = rs.getDouble("wallet_balance");
                                System.out.println("[DriverDAO.updateDriverBalance] ✅ VERIFIED: Balance in DB is now: $" +
                                                 String.format("%.2f", verifiedBalance));
                                if (Math.abs(verifiedBalance - newBalance) < 0.01) {
                                    System.out.println("[DriverDAO.updateDriverBalance] ✅ VERIFICATION PASSED: Values match!");
                                } else {
                                    System.err.println("[DriverDAO.updateDriverBalance] ⚠️ WARNING: Verification mismatch!");
                                    System.err.println("[DriverDAO.updateDriverBalance] ⚠️ Expected: $" + String.format("%.2f", newBalance) +
                                                     ", Got: $" + String.format("%.2f", verifiedBalance));
                                }
                            }
                        }
                    }

                    return true;
                } else {
                    System.err.println("[DriverDAO.updateDriverBalance] ❌ FAILED: No rows affected!");
                    System.err.println("[DriverDAO.updateDriverBalance] ❌ This means no driver with ID " + driverId + " exists in the database!");
                    System.err.println("[DriverDAO.updateDriverBalance] ❌ Please verify the driver exists:");
                    System.err.println("[DriverDAO.updateDriverBalance]    SQL: SELECT COUNT(*) FROM drivers WHERE id = " + driverId);
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DriverDAO.updateDriverBalance] ❌ SQL ERROR: " + e.getMessage());
            System.err.println("[DriverDAO.updateDriverBalance] ❌ SQL State: " + e.getSQLState());
            System.err.println("[DriverDAO.updateDriverBalance] ❌ Error Code: " + e.getErrorCode());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get driver's current wallet balance
     * @param driverId the driver's ID
     * @return wallet balance, or -1 if error
     */
    public double getDriverWalletBalance(long driverId) {
        final String sql = "SELECT wallet_balance FROM drivers WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double balance = rs.getDouble("wallet_balance");
                    System.out.println("[DriverDAO] Retrieved driver balance: " + String.format("%.2f", balance) + " EGP (ID: " + driverId + ")");
                    return balance;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DriverDAO] Error getting driver wallet balance: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Cross-table validation: Check if email exists in passengers table
     * Used during driver registration to prevent duplicate accounts across tables
     * @param email the email to check
     * @return true if email exists in passengers table, false otherwise
     */
    public boolean emailExistsInPassengers(String email) {
        final String sql = "SELECT id FROM passengers WHERE email=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // Returns true if email found
            }
        } catch (SQLException e) {
            System.err.println("Error checking email in passengers table: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
