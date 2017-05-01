package models;

/**
 * Created by goran on 4/12/17.
 */
public class VictualAndDrink {
    public int VictualAndDrinkId;
    public String name;
    public String description;
    public double price;

    public VictualAndDrink(String name, String description, double price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public VictualAndDrink() {}
}
