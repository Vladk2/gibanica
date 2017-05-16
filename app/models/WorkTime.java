package models;

import java.sql.Date;

/**
 * Created by stefan on 5/14/17.
 */
public class WorkTime {
    public String date;
    public String startTime;
    public String endTime;
    public String workingSector;
    public String workerEmail;

    public WorkTime(String _date, String _startTime, String _endTime){
        this.date = _date;
        this.startTime = _startTime;
        this.endTime = _endTime;
    }

    public WorkTime(String _date, String _startTime, String _endTime, String workingSector){
        this.date = _date;
        this.startTime = _startTime;
        this.endTime = _endTime;
        this.workingSector = workingSector;
    }

    public WorkTime(String _date, String _startTime, String _endTime, String workingSector, String workerEmail){
        this.date = _date;
        this.startTime = _startTime;
        this.endTime = _endTime;
        this.workingSector = workingSector;
        this.workerEmail = workerEmail;
    }

    public WorkTime() {}

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
