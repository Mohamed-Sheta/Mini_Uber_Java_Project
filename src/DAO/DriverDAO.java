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
        public final Integer currentLocation;
        public final String licensePlate, carModel;
        public final boolean active;
        public final int latestPassengerRating;

        public DriverRow(long id, String userSSN, String name, String phone, String email,
                         double wallet, double credit, Integer currentLocation,
                         String licensePlate, String carModel, boolean active, int latestPassengerRating) {
            this.id=id; this.userSSN=userSSN; this.name=name; this.phone=phone; this.email=email;
            this.wallet=wallet; this.credit=credit; this.currentLocation=currentLocation;
            this.licensePlate=licensePlate; this.carModel=carModel; this.active=active;
            this.latestPassengerRating=latestPassengerRating;
        }
        @Override public String toString(){ return "DriverRow{id="+id+", plate="+licensePlate+", name="+name+"}"; }
    }

    public long insert(Driver d, Integer currentLocationId) throws SQLException {
        final String sql = "INSERT INTO drivers(user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location,license_plate,car_model,active,latest_passenger_rating) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getUserSSN());
            ps.setString(2, d.getName());
            ps.setString(3, d.getPhoneNumber());
            ps.setString(4, d.getEmail());
            ps.setDouble(5, d.getWalletBalance());
            ps.setDouble(6, d.getCreditBalance());
            if (currentLocationId == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, currentLocationId);
            ps.setString(8, d.getLicensePlate());
            ps.setString(9, d.getCarModel());
            ps.setBoolean(10, d.isActive());
            ps.setInt(11, 0);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        }
    }

    public int update(long id, Driver d, Integer currentLocationId, Integer latestPassengerRating) throws SQLException {
        final String sql = "UPDATE drivers SET user_ssn=?, name=?, phone_number=?, email=?, wallet_balance=?, credit_balance=?, " +
                "current_location=?, license_plate=?, car_model=?, active=?, latest_passenger_rating=? WHERE id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, d.getUserSSN());
            ps.setString(2, d.getName());
            ps.setString(3, d.getPhoneNumber());
            ps.setString(4, d.getEmail());
            ps.setDouble(5, d.getWalletBalance());
            ps.setDouble(6, d.getCreditBalance());
            if (currentLocationId == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, currentLocationId);
            ps.setString(8, d.getLicensePlate());
            ps.setString(9, d.getCarModel());
            ps.setBoolean(10, d.isActive());
            ps.setInt(11, latestPassengerRating == null ? 0 : latestPassengerRating);
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
        final String sql = "SELECT id,user_ssn,name,phone_number,email,wallet_balance,credit_balance,current_location,license_plate,car_model,active,latest_passenger_rating FROM drivers ORDER BY id";
        List<DriverRow> out = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Integer loc = (Integer) rs.getObject("current_location");
                out.add(new DriverRow(
                        rs.getLong("id"),
                        rs.getString("user_ssn"),
                        rs.getString("name"),
                        rs.getString("phone_number"),
                        rs.getString("email"),
                        rs.getDouble("wallet_balance"),
                        rs.getDouble("credit_balance"),
                        loc,
                        rs.getString("license_plate"),
                        rs.getString("car_model"),
                        rs.getBoolean("active"),
                        rs.getInt("latest_passenger_rating")
                ));
            }
        }
        return out;
    }
}
