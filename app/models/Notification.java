package models;

/**
 * Created by vladi on 21-Jun-17.
 */
public class Notification {

    public int notificationId;
    public int userId;
    public String message;
    public int aboutReq;

    public Notification(int notificationId, int userId, String message, int aboutReq, String signal) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.message = message;
        this.aboutReq = aboutReq;
        this.signal = signal;
    }

    public String signal;


    public Notification(int notificationId, int userId, String message, int aboutReq) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.message = message;
        this.aboutReq = aboutReq;
    }



    public Notification() {}
}
