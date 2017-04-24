package controllers;

import com.google.common.collect.ImmutableMap;
import models.User;
import play.*;
import play.data.DynamicForm;
import play.data.Form;
import play.db.Databases;
import play.db.Database;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by stefan on 4/11/17.
 */

public class Login extends Controller {

    public static Result submit(){
        DynamicForm requestData = Form.form().bindFromRequest();
        User created = new User();
        created.email = requestData.get("email");
        created.password = requestData.get("password");


        Database database = Databases.createFrom(
                "baklava",
                "com.mysql.jdbc.Driver",
                "jdbc:mysql://localhost/baklava",
                ImmutableMap.of(
                        "user", "root",
                        "password", "gibanica"
                )
        );

        Connection connection = database.getConnection();
        try {
            /*ResultSet set = connection.prepareStatement("Select email, password from users where email="
                    + "\"" + created.email + "\"" + " and password=" + "\"" + created.password
                    + "\"" + ";").executeQuery();*/
            ResultSet set = connection.prepareStatement("Select password, email, name, surname, type, verified from users where password="
                    + "\"" + created.password + "\" and email=" + "\"" + created.email + "\"" + ";").executeQuery();
            String email = "";
            String pw = "";
            String tip = "";
            String ime = "";
            String prezime = "";
            int verified = -1;
            while(set.next()){
                pw = set.getString(1);
                email = set.getString(2);
                ime = set.getString(3);
                prezime = set.getString(4);
                tip = set.getString(5);
                verified = set.getInt(6);
            }
            if(pw.equals(created.password) && email.equals(created.email)) {
                session("connected", email);
                session("connectedFName", ime);
                session("connectedLName", prezime);
                session("connectedPass", pw);
                session("userType", tip);
                session("verified", Integer.toString(verified));
                if((session("userType")).equals("rest-manager")){

                    ResultSet set2 = connection.prepareStatement("Select name, description from restaurants where restaurantId = " +
                            "(select restaurantId from restaurantmanagers where userId = (select userId from users where email = " +
                             "\"" + created.email + "\"" + "));").executeQuery();

                    while(set2.next()){
                        session("myRestName", set2.getString(1));
                        session("myRestDesc", set2.getString(2));

                    }
                }
                String loggedUser = session("connected");
                if((tip.equals("bidder") || tip.equals("waiter") || tip.equals("chef") || tip.equals("bartender")) && verified==0)
                    return ok(firstLog.render(email));
                else
                return ok(home.render("Welcome",new play.twirl.api.Html("<center><h2>Welcome, " + loggedUser + "!</h2></center>") )); // login succedded
            }

        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            if(connection != null){
                try {
                    connection.close();
                    System.out.println("Connection closed.");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return ok("Wrong email or password.");
    }

    public static Result logout() {
        session().remove("connected");
        session().remove("connectedFName");
        session().remove("connectedLName");
        session().remove("userType");
        return ok(index.render("bla"));
    }

    public static Result verifyPass() {

        String loggedUser = session("connected");


        DynamicForm requestData = Form.form().bindFromRequest();
        String old_pw = requestData.get("old_password");
        String new_pw = requestData.get("new_password");

        Database database = Databases.createFrom(
                "baklava",
                "com.mysql.jdbc.Driver",
                "jdbc:mysql://localhost/baklava",
                ImmutableMap.of(
                        "user", "root",
                        "password", "gibanica"
                )
        );

        Connection connection = database.getConnection();

        try{
            ResultSet set = connection.prepareStatement("Select password, email from users where password="
                    + "\"" + old_pw + "\" and email=" + "\"" + loggedUser + "\"" + ";").executeQuery();

            String email = "";
            String pw = "";

            while(set.next()){
                pw = set.getString(1);
                email = set.getString(2);
            }

            if (new_pw.equals(old_pw)) {
                return ok("You have to choose a different password");
            }

           else if(pw.equals(old_pw) && email.equals(loggedUser)) {

                if(connection.prepareStatement("Update users" +
                        " set password=" + "\"" + new_pw + "\" , verified = 1" +
                        " Where email=" + "\"" + loggedUser + "\"" + ";").execute()){
                    System.out.println("Password successfully changed!");
                }

                session("verified", "1");
                return ok(home.render("Welcome",new play.twirl.api.Html("<center><h3>Good job, <b>" + loggedUser + "</b>, password has been changed and your account is now verified!</h3></center>") ));
            }



        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            if(connection != null){
                try {
                    connection.close();
                } catch (SQLException q){
                    q.printStackTrace();
                }
            }
        }

        return ok("Wrong password");
    }


}


