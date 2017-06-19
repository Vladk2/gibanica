package models;

import java.sql.Date;


public class Request {


    public int reqId;
    public String dateFrom;
    public String dateTo;

    public Request(String dateFrom, String dateTo) {
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }


    public Request(int reqId, String dateFrom, String dateTo) {
        this.reqId = reqId;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    public Request() {
    }

}



