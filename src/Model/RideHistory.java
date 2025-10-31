package Model;

import services.Request;
import java.util.List;

public class RideHistory {
     private Request request;
    private static int idCounter = 1;
    private int historyId;
    private Driver driver;
    private Passenger passenger;
    private int passengerRating;
    private int driverRating;

    public RideHistory(Driver driver, Passenger passenger, int passengerRating, int driverRating) {
        this.historyId = idCounter++;
        this.driver = driver;
        this.passenger = passenger;
        this.passengerRating = passengerRating;
        this.driverRating = driverRating;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public int getDriverRating() {
        return driverRating;
    }

    public void setDriverRating(int rating) {
        this.driverRating = rating;
    }

    public int getPassengerRating() {
        return passengerRating;
    }

    public void setPassengerRating(int rating) {
        this.passengerRating = rating;
    }
    public Driver getDriver() {
        return driver;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public int getHistoryId() {
        return historyId;
    }

    public static int getRideCounts(List<RideHistory> rides) {
        int count = 0;
        for (RideHistory h : rides) {
            if (h.request != null &&
                    h.request.getStatus() != null &&
                    h.request.getStatus()==Status.Completed) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return "RideHistory{" +
                "historyId=" + historyId +
                ", driver=" + driver +
                ", passenger=" + passenger +
                ", passengerRating=" + passengerRating +
                ", driverRating=" + driverRating +
                '}';
    }
}