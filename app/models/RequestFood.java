package models;


public class RequestFood {


    public String name;
    public int amount;
    public int reqId;

    public RequestFood(String name, int amount) {
        this.name = name;
        this.amount = amount;
    }


    public RequestFood(String name, int amount, int reqId) {
        this.name = name;
        this.amount = amount;
        this.reqId = reqId;
    }




    public RequestFood() {
    }

}
