package Model;

public class RideHistory {
    private int historyId;
    // private Request request;
    private Driver driver;
    private Passenger passenger;
    private int passengerRating;
    private int driverRating;
    public RideHistory(int historyId, Driver driver, Passenger passenger, int passengerRating, int driverRating) {
        this.historyId = historyId;
        this.driver = driver;
        this.passenger = passenger;
        this.passengerRating = 0;
        this.driverRating = 0;
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
//    public static int getRideCounts(List<RideHistory> rides) {
//        int count = 0;
//        for (RideHistory h : rides) {
//            if (h.request != null &&
//                    h.request.getStatus() != null &&
//                    h.request.getStatus().equalsIgnoreCase("completed")) {
//                count++;
//            }
//        }
//        return count;
//    }

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

