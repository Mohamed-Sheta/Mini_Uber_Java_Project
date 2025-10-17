package Model;
import java.util.Objects;

public class Location {
    private int id;
    private String name;
    private double latitude;
    private double longitude;

    public void setId(int id) {
        this.id = id;
    }

    public Location(int id, String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Location(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "Location{" +
                "name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location location)) return false;

        return Double.compare(getLatitude(), location.getLatitude()) == 0 &&
                Double.compare(getLongitude(), location.getLongitude()) == 0 &&
                Objects.equals(getName(), location.getName());
    }

}
