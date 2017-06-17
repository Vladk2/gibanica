package models;

/**
 * Created by goran on 17.6.17..
 */
public class FriendsSearchData {
    public String name;
    public String surname;
    public int userId;

    public FriendsSearchData() {}

    public FriendsSearchData(String name, String surname, int userId) {
        this.name = name;
        this.surname = surname;
        this.userId = userId;
    }

    public FriendsSearchData(String name, String surname) {
        this.name = name;
        this.surname = surname;
        userId = -1;
    }
}
