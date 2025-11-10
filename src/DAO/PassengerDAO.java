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

        public PassengerRow(long id, String userSSN, String name, String phone, String email,
                            double wallet, double credit, String currentLocation) {
            this.id=id; this.userSSN=userSSN; this.name=name; this.phone=phone; this.email=email;
            this.wallet=wallet; this.credit=credit; this.currentLocation=currentLocation;
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
        if (p.getEmail() == null || !p.getEmail().endsWith("@gmail.com"))
            throw new IllegalArgumentException("Email must end with '@gmail.com'.");
        if (p.getWalletBalance() < 0 || p.getCreditBalance() < 0)
            throw new IllegalArgumentException("Balances cannot be negative.");
        final String sql = "INSERT INTO passengers(user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location) " +
                "VALUES (?,?,?,?,?,?,?)";

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
        if (p.getEmail() == null || !p.getEmail().endsWith("@gmail.com"))
            throw new IllegalArgumentException("Email must end with '@gmail.com'.");
        if (p.getWalletBalance() < 0 || p.getCreditBalance() < 0)
            throw new IllegalArgumentException("Balances cannot be negative.");
        final String sql = "UPDATE passengers SET user_ssn=?, name=?, phone_number=?, email=?, " +
                "wallet_balance=?, credit_balance=?, current_location=? WHERE id=?";

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

            ps.setLong(8, id);

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
        final String sql = "SELECT id,user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location " +
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
                        rs.getString("current_location") // Now String
                ));
            }
        }
        return out;
    }
}
