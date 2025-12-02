package DAO;

import utils.DBConnection;
import Model.Location;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocationDAO {

    public static class LocationRow {
        public final int id;
        public final String name;
        public final double latitude;
        public final double longitude;

        public LocationRow(int id, String name, double latitude, double longitude) {
            this.id = id;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        public String toString() {
            return "LocationRow{id=" + id + ", name='" + name +
                    "', latitude=" + latitude + ", longitude=" + longitude + "}";
        }
    }

    // INSERT
    public long insert(Location loc) throws SQLException {

        final String sql =
                "INSERT INTO locations(name, latitude, longitude) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, loc.getName());
            ps.setDouble(2, loc.getLatitude());
            ps.setDouble(3, loc.getLongitude());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    loc.setId((int) id);
                    return id;
                }
            }
        }
        return -1;
    }

    // UPDATE
    public int update(int id, Location loc) throws SQLException {
        final String sql =
                "UPDATE locations SET name=?, latitude=?, longitude=? WHERE id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, loc.getName());
            ps.setDouble(2, loc.getLatitude());
            ps.setDouble(3, loc.getLongitude());
            ps.setInt(4, id);

            return ps.executeUpdate();
        }
    }

    // DELETE
    public int delete(int id) throws SQLException {
        final String sql = "DELETE FROM locations WHERE id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    // SELECT ALL
    public List<LocationRow> showAll() throws SQLException {
        final String sql =
                "SELECT id, name, latitude, longitude FROM locations ORDER BY id";

        List<LocationRow> out = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(new LocationRow(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")
                ));
            }
        }

        return out;
    }

    public List<Location> getPredefinedLocations() {
        List<Location> locations = new ArrayList<>();

        locations.add(new Location("Downtown Cairo", 30.0444, 31.2357));
        locations.add(new Location("Nasr City", 30.0561, 31.3300));
        locations.add(new Location("Maadi", 29.9603, 31.2596));
        locations.add(new Location("Giza", 30.0131, 31.2089));
        locations.add(new Location("New Cairo", 30.0305, 31.4913));
        locations.add(new Location("Hadaeq Al-Qubba", 30.0867, 31.3020));
        locations.add(new Location("El Korba", 30.1127, 31.3270));
        locations.add(new Location("Abbasiya", 30.0670, 31.2759));
        locations.add(new Location("Helmeyet El-Zeitoun", 30.1134, 31.3187));
        locations.add(new Location("El Obour", 30.2289, 31.4553));

        return locations;
    }
}
