

package controllers;


import com.google.common.collect.ImmutableMap;
import models.Restaurant;
import models.RestaurantManager;
import models.User;
import models.Employee;
import play.*;


import play.data.DynamicForm;
import play.data.Form;
import play.db.Database;
import play.db.Databases;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Restaurants extends Controller {

    public static Result restaurants() {

        String loggedUser = session("connected");
        String verified = session("verified");

        List<Restaurant> restaurants = new ArrayList<Restaurant>();

        //if(loggedUser == null || verified.equals("0"))
        if(5 == 6)
            return redirect("/"); // nema ulogovanog korisnika, vraca na pocetnu stranicu
        else {
            Database database = Databases.createFrom (
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
                String query = "Select name, description from restaurants";
                ResultSet set = connection.prepareStatement(query).executeQuery();

                while(set.next()){
                    Restaurant restaurant =
                    new Restaurant(set.getString(1), set.getString(2));

                    restaurants.add(restaurant);
                }

            } catch (SQLException e){
                e.printStackTrace();
            } finally {
                if(connection != null){
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            return ok(restaurant.render(loggedUser, restaurants));
        }
    }



    public static Result addRestaurant() {
        DynamicForm requestData = Form.form().bindFromRequest();
        Restaurant created = new Restaurant();
        created.name = requestData.get("rname");
        created.description = requestData.get("rdesc");

        //TODO: staticko polje database, pravi se konekcija prilikom pokretanja aplikacije.
        // posto driver ima connection pool, moze na vise mesta da se radi get connection
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
            if(connection.prepareStatement("Insert into restaurants (name, description) " +
                    "values (" + "\"" + created.name + "\""
                    + ", \"" + created.description + "\")" + ";").execute()) {
                System.out.println("Success");
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            if(connection != null){
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }


        return ok(home.render("Welcome",new play.twirl.api.Html("<center>Restaurant has been added successfully!</center>") ));

    }

    public static Result addRestaurantManager() {
        DynamicForm requestData = Form.form().bindFromRequest();
        RestaurantManager created = new RestaurantManager();
        created.name = requestData.get("fname");
        created.surname = requestData.get("lname");
        created.email = requestData.get("email");
        created.password = requestData.get("pass");
        created.restaurant = requestData.get("forRest");
        System.out.println("\n " + created.restaurant + "\ngfsdgfdgsfsf");
        //TODO: staticko polje database, pravi se konekcija prilikom pokretanja aplikacije.
        // posto driver ima connection pool, moze na vise mesta da se radi get connection
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
            if(connection.prepareStatement("Insert into users (name, surname, email, password, verified, type) " +
                    "values (" + "\"" + created.name + "\""
                    + ", \"" + created.surname + "\""
                    + ", \"" + created.email + "\""
                    + ", \"" + created.password + "\""
                    + ", 1"
                    + ", \"rest-manager\")" + ";").execute()) {
                System.out.println("Success-added to users table");
            }
            if(connection.prepareStatement("Insert into restaurantmanagers (restaurantId, userId) " +
                    "values (( select restaurantId from restaurants where name ="
                    + "\"" + created.restaurant + "\"),(select userId from users where email ="
                    + "\"" + created.email + "\"));").execute()) {
                System.out.println("Success-added to rest-managers table");
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            if(connection != null){
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }


        return ok(home.render("Welcome",new play.twirl.api.Html("<center>Restaurant manager has been added successfully!</center>") ));


    }

    public static Result addSystemManager() {
        DynamicForm requestData = Form.form().bindFromRequest();
        User created = new User();
        created.name = requestData.get("fname");
        created.surname = requestData.get("lname");
        created.email = requestData.get("email");
        created.password = requestData.get("pass");

        //TODO: staticko polje database, pravi se konekcija prilikom pokretanja aplikacije.
        // posto driver ima connection pool, moze na vise mesta da se radi get connection
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
            if(connection.prepareStatement("Insert into users (name, surname, email, password, verified, type) " +
                    "values (" + "\"" + created.name + "\""
                    + ", \"" + created.surname + "\""
                    + ", \"" + created.email + "\""
                    + ", \"" + created.password + "\""
                    + ", 1"
                    + ", \"system-manager\")" + ";").execute()) {
                System.out.println("Success");
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            if(connection != null){
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }


        return ok(home.render("Welcome",new play.twirl.api.Html("<center>System manager has been added successfully!</center>") ));

    }

    public static Result addBidder() {
        DynamicForm requestData = Form.form().bindFromRequest();
        User created = new User();
        created.name = requestData.get("fname");
        created.surname = requestData.get("lname");
        created.email = requestData.get("email");
        created.password = requestData.get("pass");

        //TODO: staticko polje database, pravi se konekcija prilikom pokretanja aplikacije
        // posto driver ima connection pool, moze na vise mesta da se radi get connection
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
            if(connection.prepareStatement("Insert into users (name, surname, email, password, verified, type) " +
                    "values (" + "\"" + created.name + "\""
                    + ", \"" + created.surname + "\""
                    + ", \"" + created.email + "\""
                    + ", \"" + created.password + "\""
                    + ", 0"
                    + ", \"bidder\")" + ";").execute()) {
                System.out.println("Success");
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            if(connection != null){
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }


        return ok(home.render("Welcome",new play.twirl.api.Html("<center>Bidder has been added successfully!</center>") ));


    }

    public static Result addEmployee() {
        DynamicForm requestData = Form.form().bindFromRequest();
        Employee created = new Employee();
        created.name = requestData.get("fname");
        created.surname = requestData.get("lname");
        created.email = requestData.get("email");
        created.password = requestData.get("pass");
        created.type = requestData.get("type");
        created.birthDate = requestData.get("birthDate");
        created.clothNo = requestData.get("clothNo");
        created.shoeNo = requestData.get("shoesNo");

        //TODO: staticko polje database, pravi se konekcija prilikom pokretanja aplikacije
        // posto driver ima connection pool, moze na vise mesta da se radi get connection
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
            if(connection.prepareStatement("Insert into users (name, surname, email, password, verified, type) " +
                    "values (" + "\"" + created.name + "\""
                    + ", \"" + created.surname + "\""
                    + ", \"" + created.email + "\""
                    + ", \"" + created.password + "\""
                    + ", 0"
                    + ", \"" + created.type +"\")" + ";").execute()) {
                System.out.println("Added to users table");
            }
            if(connection.prepareStatement("Insert into workers (userId, birthDate, clothNo, shoesNo) " +
                    "values ((select userId from users where email ="
                    + "\"" + created.email + "\")"
                    + ", \"" + created.birthDate + "\""
                    + ", \"" + created.clothNo + "\""
                    + ", \"" + created.shoeNo + "\")" + ";").execute()) {

                System.out.println("Added to workers table");
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            if(connection != null){
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }


        return ok(home.render("Welcome",new play.twirl.api.Html("<center>Employee has been added successfully!</center>") ));

    }


    public static Result editRestaurant() {

        DynamicForm requestData = Form.form().bindFromRequest();
        String restName = requestData.get("rname");
        String restDesc = requestData.get("rdesc");
        String oldName = session("myRestName");
        //TODO: staticko polje database, pravi se konekcija prilikom pokretanja aplikacije
        // posto driver ima connection pool, moze na vise mesta da se radi get connection
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

                if(connection.prepareStatement("Update restaurants" +
                        " set name =" + "\"" + restName + "\"" +
                        ", description =" + "\"" + restDesc + "\" where name = " + "\"" + oldName + "\"" + ";").execute()){
                    System.out.println("Restaurant's info successfully changed!");

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

        session("myRestName", restName);
        session("myRestDesc", restDesc);
        return ok(home.render("Welcome",new play.twirl.api.Html("<center>You have successfully edited your restaurant's info!</center>") ));
    }

}


