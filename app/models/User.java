package models;

/**
 * Created by stefan on 4/11/17.
 */
public class User {
    public String name;
    public String surname;
    public String email;
    public String password;
    public String tip;
    public int verified;

    public User() {}

    public User(String name, String surname, String email, String tip){

        this.name = name;
        this.surname = surname;
        this.email = email;
        this.tip = tip;
    }
}
