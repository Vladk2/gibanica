package models;

/**
 * Created by goran on 17.6.17..
 */
public class FriendsSearchData {
    public String name;
    public String surname;
    public int userId;
    public boolean sent;
    public boolean received;
    public boolean accepted;

    public FriendsSearchData() {
        this.userId = 0;
        this.sent = false;
        this.received = false;
        this.accepted = false;
    }

    public FriendsSearchData(String name, String surname) {
        this.name = name;
        this.surname = surname;
        this.userId = 0;
        this.sent = false;
        this.received = false;
        this.accepted = false;

    }

    public FriendsSearchData(String name, String surname, int userId, boolean sent, boolean received, boolean accepted) {
        this.name = name;
        this.surname = surname;
        this.userId = userId;
        this.sent = sent;
        this.received = received;
        this.accepted = accepted;

    }

    public FriendsSearchData(String name, String surname, int userId) {
        this.name = name;
        this.surname = surname;
        this.userId = userId;
        this.sent = false;
        this.received = false;
        this.accepted = false;
    }
}
