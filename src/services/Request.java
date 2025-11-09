package services;
import Model.*;

public class Request {

    private static int requestCounter = 1;
    private int requestId;
    private Passenger passenger;
    private Location origin;
    private Location destination;
    private Status status;
    private double distance;
    private int estimatedTime;
    private double estimatedPrice;

    private long dbId;
    public Request(Passenger passenger, Location origin, Location destination, Status status, MapGraph mapGraph) {
        this.requestId = requestCounter++;
        this.passenger = passenger;
        this.origin =origin;
        this.destination = destination;
        this.status = status;

        this.distance = mapGraph.shortestDistance(origin, destination);
        if (this.distance == Double.MAX_VALUE) {
            System.out.println(" ERROR: Request " + this.requestId + " failed. No valid path found from " + origin.getName() + " to " + destination.getName());

            this.distance = 0.0;
            this.estimatedTime = 0;
            this.estimatedPrice = 0.0;
            this.status = Status.Cancelled;

            requestCounter--;
            return;
        }
        this.estimatedTime = calculateEstimatedTime(distance);
        this.estimatedPrice = calculateEstimatedPrice(distance);
    }
    public void setDbId(long id) { this.dbId = id; }

    public long getDatabaseId() { return this.dbId; }

    public int getRequestId() {
        return requestId;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public Location getOrigin() {
        return origin;
    }

    public Location getDestination() {
        return destination;
    }

    public Status getStatus() {
        return status;
    }

    public double getDistance() {
        return distance;
    }

    public int getEstimatedTime() {
        return estimatedTime;
    }

    public double getEstimatedPrice() {
        return estimatedPrice;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    public int calculateEstimatedTime(double distance) {
        double avgSpeedKmPerHour = 60.0;
        double timeInHours = distance / avgSpeedKmPerHour;
        return (int) (timeInHours * 60);
    }

    private double calculateEstimatedPrice(double distance) {
        double baseFare = 11.5;
        double ratePerKm = 4.0;
        return baseFare + (distance * ratePerKm);
    }

    @Override
    public String toString() {
        return "Request{" +
                "requestId=" + requestId +
                ", passenger=" + passenger +
                ", origin=" + origin +
                ", destination=" + destination +
                ", status=" + status +
                ", distance=" + distance +
                ", estimatedTime=" + estimatedTime +
                ", estimatedPrice=" + estimatedPrice +
                '}';
    }
}