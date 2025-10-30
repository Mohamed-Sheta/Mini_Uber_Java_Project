package com.mycompany.uper;

public class Edge {
    protected Location from ;
    protected Location to;
    protected double distance;
    protected int estimated_time;

    public Edge(Location from, Location to, double distance, int estimated_time) {
        this.from = from;
        this.to = to;
        this.distance = distance;
        this.estimated_time = estimated_time;
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

    public int getEstimated_time() {
        return estimated_time;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "from=" + from +
                ", to=" + to +
                ", distance=" + distance +
                ", estimated_time=" + estimated_time +
                '}';
    }
}