package models;

/**
 * Created by goran on 23.6.17..
 */
public class CancelReservation {
    public int userId;
    public int reservationId;

    public CancelReservation() {
    }

    public CancelReservation(int userId, int reservationId) {
        this.userId = userId;
        this.reservationId = reservationId;
    }
}
