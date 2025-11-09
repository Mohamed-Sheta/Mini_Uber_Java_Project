package Model;

import java.util.Objects;

public class Location {
    private  int counter = 1;
    private int id;
    private String name;

    public Location(int id, String name) {
        this.id = id;
        this.name = name;
    }
    public Location(String name) {
        this.id = counter++;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) { this.id = id; }
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Location{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location location)) return false;
        return id == location.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void setName(String name) {
        this.name = name;
    }
}