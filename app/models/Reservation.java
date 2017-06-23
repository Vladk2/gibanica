package models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by goran on 23.6.17..
 */
public class Reservation {

    public int reservationId;
    public int userId;
    public int restaurantId;
    public Timestamp startTimestamp;
    public Timestamp endTimestamp;
    public ArrayList<VictualAndDrink> orderVictualDrink;
    public int seatCount;
    public String name;

    public Reservation() {}

    public Reservation(int reservationId, int userId, int restaurantId, Timestamp startTimestamp, Timestamp endTimestamp,
                       ArrayList<VictualAndDrink> orderVictualDrink, int seatCount, String name) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.orderVictualDrink = orderVictualDrink;
        this.seatCount = seatCount;
        this.name = name;
    }

}
