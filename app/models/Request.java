package models;

import java.sql.Date;


public class Request {



    public int reqId;
    public String dateFrom;
    public String dateTo;
    public String restName;
    public int acceptedOfferId;

    public Request(String dateFrom, String dateTo) {
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }


    public Request(int reqId, String dateFrom, String dateTo, String restName) {
        this.reqId = reqId;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.restName = restName;
    }

    public Request(int reqId, String dateFrom, String dateTo, String restName, int acceptedOfferId) {
        this.reqId = reqId;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.restName = restName;
        this.acceptedOfferId = acceptedOfferId;
    }

    public Request() {
    }

}



