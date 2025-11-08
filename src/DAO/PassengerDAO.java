package DAO;
import utils.*;
import Model.Location;
import Model.Passenger;
import Model.RideHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PassengerDAO {

    public static class PassengerRow {
        public final long id;
        public final String userSSN, name, phone, email;
        public final double wallet, credit;
        public final Integer currentLocation; // may be null
        public final int latestDriverRating;
        public PassengerRow(long id, String userSSN, String name, String phone, String email,
                            double wallet, double credit, Integer currentLocation, int latestDriverRating) {
            this.id=id; this.userSSN=userSSN; this.name=name; this.phone=phone; this.email=email;
            this.wallet=wallet; this.credit=credit; this.currentLocation=currentLocation;
            this.latestDriverRating = latestDriverRating;
        }
        @Override public String toString(){ return "PassengerRow{id="+id+", ssn="+userSSN+", name="+name+"}"; }
    }

    // insert باستخدام Passenger (Location currentLocation اختيارى)
    public long insert(Passenger p, Integer currentLocationId) throws SQLException {
        final String sql = "INSERT INTO passengers(user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location,latest_driver_rating) " +
                           "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getUserSSN());
            ps.setString(2, p.getName());
            ps.setString(3, p.getPhoneNumber());
            ps.setString(4, p.getEmail());
            ps.setDouble(5, p.getWalletBalance());
            ps.setDouble(6, p.getCreditBalance());
            if (currentLocationId == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, currentLocationId);
            ps.setInt(8, 0); // latest_driver_rating starts 0, model بيحسب متوسطه من history

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    public int update(long id, Passenger p, Integer currentLocationId, Integer latestDriverRating) throws SQLException {
        final String sql = "UPDATE passengers SET user_ssn=?, name=?, phone_number=?, email=?, " +
                "wallet_balance=?, credit_balance=?, current_location=?, latest_driver_rating=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getUserSSN());
            ps.setString(2, p.getName());
            ps.setString(3, p.getPhoneNumber());
            ps.setString(4, p.getEmail());
            ps.setDouble(5, p.getWalletBalance());
            ps.setDouble(6, p.getCreditBalance());
            if (currentLocationId == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, currentLocationId);
            ps.setInt(8, latestDriverRating == null ? 0 : latestDriverRating);
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
        final String sql = "SELECT id,user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location,latest_driver_rating " +
                "FROM passengers ORDER BY id";
        List<PassengerRow> out = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Integer loc = (Integer) rs.getObject("current_location");
                out.add(new PassengerRow(
                        rs.getLong("id"),
                        rs.getString("user_ssn"),
                        rs.getString("name"),
                        rs.getString("phone_number"),
                        rs.getString("email"),
                        rs.getDouble("wallet_balance"),
                        rs.getDouble("credit_balance"),
                        loc,
                        rs.getInt("latest_driver_rating")
                ));
            }
        }
        return out;
    }
}
