package models;

/**
 * Created by vladi on 21-Jun-17.
 */
public class Notification {

    public int notificationId;
    public int userId;
    public String message;


    public Notification(int notificationId, int userId, String message) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.message = message;
    }

    public Notification() {}
}
