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

    public Request(Passenger passenger, Location origin, Location destination, Status status, MapGraph mapGraph) {
        this.requestId = requestCounter++;
        this.passenger = passenger;
        this.origin = passenger.getCurrentLocation();
        this.destination = destination;
        this.status = status;

        this.distance = mapGraph.shortestDistance(origin, destination);

        this.estimatedTime = calculateEstimatedTime(distance);
        this.estimatedPrice = calculateEstimatedPrice(distance);
    }

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
        double baseFare = 10.0;
        double ratePerKm = 4.0;
        double ratePerMinute = 0.5;

        return baseFare + (distance * ratePerKm) + (estimatedTime * ratePerMinute);
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
