package models;

/**
 * Created by vladi on 11-Apr-17.
 */
public class Restaurant {

    public String id;
    public String name;
    public String description;
    public String location;
    public String tel;
    public String rSize;

    public Restaurant(String name, String description, String location, String tel, String rSize) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.tel = tel;
        this.rSize = rSize;
    }

    public Restaurant() {
    }
}
