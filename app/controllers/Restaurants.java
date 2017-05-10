

package controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import models.*;
import play.*;


import play.data.DynamicForm;
import play.data.Form;
import play.db.DB;
import play.db.Database;
import play.db.Databases;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Restaurants extends Controller {

    public static String addedRestaurantName;
    public static int addedRestSize;
    public static List<Restaurant> restaurants = new ArrayList<Restaurant>();
    public static boolean showMenuSeatPage = false;

    public static Result restaurants() {

        String loggedUser = session("connected");
        String verified = session("verified");
        String myRestName = session("myRestName");
        restaurants.clear();
        String thisRestId = "";
        List<VictualAndDrink> menu = new ArrayList<>();
        List<RestSection> seats = new ArrayList<>();
        List<RestSection> sectors = new ArrayList<>();
        String posX = ""; String posY = ""; String sectorColor = "";
        double restSize=0; int noOfSectors=0;
        Restaurant myRestaurant = new Restaurant();
        if(loggedUser == null || verified.equals("0"))

            return redirect("/"); // nema ulogovanog korisnika, vraca na pocetnu stranicu
        else {
            Connection connection = DB.getConnection();
            try {
                String query = "Select name, description, address, tel, size from restaurants";
                ResultSet set = connection.prepareStatement(query).executeQuery();
                while(set.next()){
                    Restaurant restaurant =
                            new Restaurant(set.getString(1), set.getString(2),
                                    set.getString(3), set.getString(4), set.getString(5));
                    if(restaurant.name.equals(myRestName) && (session("userType")).equals("rest-manager"))
                        myRestaurant = restaurant;
                    restaurants.add(restaurant);
                }
                //----------------------------- if rest manager ------------------------------------------------------

                if((session("userType")).equals("rest-manager")) {

                }



                    ResultSet set2 = connection.prepareStatement("Select name, description, price, type, restaurantId from victualsanddrinks where restaurantId = " +
                            "(select restaurantId from restaurantManagers where userId = (select userId from users where email = " +
                            "\"" + loggedUser + "\"" + "));").executeQuery();
                    while (set2.next()) {
                        VictualAndDrink vd = new VictualAndDrink(set2.getString(1), set2.getString(2),
                                set2.getDouble(3), set2.getString(4));
                        menu.add(vd);
                        thisRestId = set2.getString(5);


                    }
                    ResultSet set3 = connection.prepareStatement("Select posX, posY, sectorColor from seatconfig where restaurantId = " +
                            "\"" + thisRestId + "\"" + ";").executeQuery();

                    while (set3.next()) {
                        restSize++;
                        posX = set3.getString(1);
                        posY = set3.getString(2);
                        sectorColor = set3.getString(3);
                        RestSection seat = new RestSection(sectorColor, posX, posY);
                        seats.add(seat);

                    }
                        ResultSet set4 = connection.prepareStatement("Select sectorName, sectorColor from sectornames where restaurantId = " +
                                "\"" + thisRestId + "\"" + ";").executeQuery();

                        while (set4.next()) {
                            RestSection legend = new RestSection(set4.getString(1),set4.getString(2));
                            sectors.add(legend);
                    }

                // ------------------------------------ rest man. over ----------------------------------------



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
            restSize = Math.sqrt(restSize);
            int intSize = (int)restSize;
            return ok(restaurant.render(loggedUser, restaurants,menu,seats,intSize,sectors,myRestaurant));
        }
    }



    public static Result addRestInfoAJAX() {
        JsonNode json = request().body().asJson();
        Restaurant rest = Json.fromJson(json, Restaurant.class);
        addedRestaurantName = rest.name;
        addedRestSize = 8;
        showMenuSeatPage = true;
        if(rest.rSize.equals("small")){
            addedRestSize = 8;
        }
        else if(rest.rSize.equals("medium")){
            addedRestSize = 10;
        }
       else if(rest.rSize.equals("large")){
            addedRestSize = 14;
        }
        else if(rest.rSize.equals("extra large")){
            addedRestSize = 20;
        }
        for(Restaurant restoran : restaurants) {
            if (restoran.name.equals(addedRestaurantName)) {
                return status(409, "Already used restName");
            }
        }
        restaurants.add(rest);
        Connection connection = DB.getConnection();
        try {
            if (connection.prepareStatement("Insert into restaurants (name, description, address, tel, size) " +
                    "values (" + "\"" + rest.name + "\""
                    + ", \"" + rest.description + "\"" + ", \"" + rest.location + "\"" +
                    ", \"" + rest.tel + "\"" + ", \"" + addedRestSize + "\")" + ";").execute()) {

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
        return ok(Json.toJson(rest));
    }

    public static Result addVictualAJAX() {
        JsonNode json = request().body().asJson();
        VictualAndDrink victual = Json.fromJson(json, VictualAndDrink.class);

        Connection connection = DB.getConnection();

        try {

            if (connection.prepareStatement("Insert into victualsAndDrinks (name, description, price, type, restaurantId) " +
                    "values (" + "\"" + victual.name + "\""
                    + ", \"" + victual.description + "\"" + ", \"" + victual.price + "\""
                    + ",\"victual\", ( select restaurantId from restaurants where name ="
                    + "\"" + addedRestaurantName + "\"))" + ";").execute()) {
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

        return ok(Json.toJson(victual));
    }

    public static Result addDrinkAJAX() {
        JsonNode json = request().body().asJson();
        VictualAndDrink drink = Json.fromJson(json, VictualAndDrink.class);

        Connection connection = DB.getConnection();
        try {

            if (connection.prepareStatement("Insert into victualsAndDrinks (name, description, price, type, restaurantId) " +
                    "values (" + "\"" + drink.name + "\""
                    + ", \"" + drink.description + "\"" + ", \"" + drink.price + "\""
                    + ",\"drink\", ( select restaurantId from restaurants where name ="
                    + "\"" + addedRestaurantName + "\"))" + ";").execute()) {
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

        return ok(Json.toJson(drink));
    }

    public static Result saveSeatConf() {


        JsonNode j =  request().body().asJson();

        ObjectMapper mapper = new ObjectMapper();
        Map<ArrayList, ArrayList> result = mapper.convertValue(j, Map.class);
        Connection connection = DB.getConnection();
        showMenuSeatPage = false;

        try {
        for(ArrayList value : result.values()) {
            for (Object s : value) {
                String polje = s.toString();
                String[] deo = polje.split(":");
                String posX = deo[1];
                String posY = deo[2];
                String[] deo2 = polje.split("\\|");
                String boja = deo2[1];

                    if (connection.prepareStatement("Insert into seatconfig (posX, posY, sectorColor, restaurantId) " +
                            "values (" + "\"" + posX + "\""
                            + ", \"" + posY + "\"" + ", \"" + boja + "\", ( select restaurantId from restaurants where name =" +
                            "\"" + addedRestaurantName + "\"))" + ";").execute()) {
                    }
                }

            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return ok();
    }

    public static Result addSeatSection() {

        JsonNode json = request().body().asJson();
        RestSection section = Json.fromJson(json, RestSection.class);
        Connection connection = DB.getConnection();
        try {

            if (connection.prepareStatement("Insert into sectornames (sectorName, sectorColor, restaurantId)" +
                    "values (" + "\"" + section.sectionName + "\""
                    + ", \"" + section.sectionColor + "\"" + ", ( select restaurantId from restaurants where name ="
                    + "\"" + addedRestaurantName + "\"))" + ";").execute()) {
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

        return ok(Json.toJson(section));
    }

    public static Result restManagerHome() {

        String loggedUser = session("connected");
        String verified = session("verified");

        List<VictualAndDrink> meals = new ArrayList<>();

        if(loggedUser == null || verified.equals("0"))
            return redirect("/"); // nema ulogovanog korisnika, vraca na pocetnu stranicu
        else {
          /*  Connection connection = DB.getConnection();
            try {
                String query = "Select name, description, price from victualsanddrinks";
                ResultSet set = connection.prepareStatement(query).executeQuery();

                while(set.next()){
                    Restaurant restaurant =
                            new Restaurant(set.getString(1), set.getString(2));

                    meals.add(restaurant);
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
            } */

            return ok(restManagerHome.render(loggedUser));
        }
    }

    public static Result AddMenuAndSeating() {
        String loggedUser = session("connected");
        String verified = session("verified");
        if(showMenuSeatPage == false || loggedUser == null || verified.equals("0"))
            return redirect("/");
        return ok(addMenuAndSeating.render(addedRestSize));

    }

    public static Result saveMenuAndSeat() {
        flash("addRestSuccess", "A new restaurant has been added.");
        return redirect("/restaurants");

    }

    public static Result addRestaurantManager() {
        DynamicForm requestData = Form.form().bindFromRequest();
        RestaurantManager created = new RestaurantManager();
        created.name = requestData.get("fname");
        created.surname = requestData.get("lname");
        created.email = requestData.get("email");
        created.password = requestData.get("pass");
        created.restaurant = requestData.get("forRestSelect");
        System.out.println("\n " + created.restaurant + "\n");

        Connection connection = DB.getConnection();
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

        Connection connection = DB.getConnection();
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

        Connection connection = DB.getConnection();
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

        Connection connection = DB.getConnection();
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

        Connection connection = DB.getConnection();
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


        return ok(home.render("Welcome",new play.twirl.api.Html("<center>You have successfully edited your restaurant's info!</center>") ));
    }

}


