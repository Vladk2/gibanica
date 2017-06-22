package models;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

/**
 * Created by stefan on 6/19/17.
 */
public class Order {

    private String orderId;
    private String orderDate;
    private String orderTime;

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(String orderTime) {
        this.orderTime = orderTime;
    }

    private String type;
    private BigDecimal price;
    private String guestId;
    private List<HashMap<String, String>> victualsDrinks;

    public String getType(){
        return type;
    }

    public void setType(String type){
        this.type = type;
    }

    public String getGuestId(){
        return guestId;
    }

    public void setGuestId(String guestId){
        this.guestId = guestId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public List<HashMap<String, String>> getVictualsDrinks() {
        return victualsDrinks;
    }

    public void setVictualsDrinks(List<HashMap<String, String>> victualsDrinks) {
        this.victualsDrinks = victualsDrinks;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", price=" + price +
                ", type=" + type +
                ", victualsDrinks=" + victualsDrinks +
                '}';
    }
}