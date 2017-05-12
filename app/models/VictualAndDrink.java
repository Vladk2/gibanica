package models;

/**
 * Created by goran on 4/12/17.
 */
public class VictualAndDrink {
    public int VictualAndDrinkId;
    public String name;
    public String description;
    public double price;
    public String tip;

    public VictualAndDrink(String name, String description, double price, String tip) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.tip = tip;
    }

    public VictualAndDrink() {
    }
}
