package Model;

public class Request {
    private static int reqId=1000;
    public Passenger p;
    String status;
    public Location Origin;
    public Location destination;
    public double distance;
    double estimatedprice;
    boolean can;
    public static void setReqId(){
        reqId++;
    }
    public void  pymenttransaction(){
        double amount=distance*11.5;
        if(p.canAfford(amount)){
            System.out.println("requst is being proceed"+amount);

        }
        else{
            System.out.println("cannot afford the price of the trip"+p.getWalletBalance()+p.getCreditBalance());
        }
    }
    public void set_Distnace(){
        MapGraph h=new MapGraph();
        distance=h.shortestDistance(Origin,destination);
    }
}