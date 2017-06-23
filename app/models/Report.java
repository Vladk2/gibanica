package models;

/**
 * Created by vladi on 23-Jun-17.
 */
public class Report {

    public String date;
    public int visits;
    public double rating;

    public Report(String date, int visits) {
        this.date = date;
        this.visits = visits;
    }
    public Report() {}


}
