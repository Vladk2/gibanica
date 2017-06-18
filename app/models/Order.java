package models;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

/**
 * Created by stefan on 6/19/17.
 */
public class Order {

    private String orderId;
    private BigDecimal price;
    private List<HashMap<String, String>> victualsDrinks;

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
                ", victualsDrinks=" + victualsDrinks +
                '}';
    }
}