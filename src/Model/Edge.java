package Model;

public class Edge {
    protected Location from ;
    protected Location to;
    protected double distance;

    public Edge(Location from, Location to, double distance) {
        this.from = from;
        this.to = to;
        this.distance = distance;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    public double getDistance() {
        return distance;
    }


    @Override
    public String toString() {
        return "Edge{" +
                "from=" + from +
                ", to=" + to +
                ", distance=" + distance +
                '}';
    }
}