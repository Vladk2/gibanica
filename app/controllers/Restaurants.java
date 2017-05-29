

package controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.sun.org.apache.xpath.internal.operations.Bool;
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
import java.util.*;


public class Restaurants extends Controller {

    public static String addedRestaurantName;
    public static int addedRestSize;
    public static List<Restaurant> restaurants = new ArrayList<Restaurant>();
    public static boolean showMenuSeatPage = false;

    @SuppressWarnings("Duplicates")
    public static Result restaurants() {

        String loggedUser = session("connected");
        String verified = session("verified");
        String myRestName = session("myRestName");
        restaurants.clear();

        List<VictualAndDrink> menu = new ArrayList<>();
        List<RestSection> seats = new ArrayList<>();
        List<RestSection> sectors = new ArrayList<>();
        List<User> workers = new ArrayList<>();
        String posX = ""; String posY = ""; String sectorColor = "";
        double restSize=0; int noOfSectors=0;
        Restaurant myRestaurant = new Restaurant();
        if(loggedUser == null || verified.equals("0"))
            return redirect("/"); // nema ulogovanog korisnika, vraca na pocetnu stranicu
        else {
            try (Connection connection = DB.getConnection()) {

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


                    ResultSet set2 = connection.prepareStatement("Select name, description, price, type, restaurantId from victualsanddrinks where restaurantId = " +
                            "(select restaurantId from restaurantmanagers where userId = (select userId from users where email = " +
                            "\"" + loggedUser + "\"" + "));").executeQuery();
                    while (set2.next()) {
                        VictualAndDrink vd = new VictualAndDrink(set2.getString(1), set2.getString(2),
                                set2.getDouble(3), set2.getString(4));
                        menu.add(vd);



                    }
                    ResultSet set3 = connection.prepareStatement("Select posX, posY, sectorColor from seatconfig where restaurantId = " +
                            "(select restaurantId from restaurantmanagers where userId = (select userId from users where email = " +
                            "\"" + loggedUser + "\"" + "));").executeQuery();

                    while (set3.next()) {
                        restSize++;
                        posX = set3.getString(1);
                        posY = set3.getString(2);
                        sectorColor = set3.getString(3);
                        RestSection seat = new RestSection(sectorColor, posX, posY);
                        seats.add(seat);

                    }
                    ResultSet set4 = connection.prepareStatement("Select sectorName, sectorColor from sectornames where restaurantId = " +
                            "(select restaurantId from restaurantmanagers where userId = (select userId from users where email = " +
                            "\"" + loggedUser + "\"" + "));").executeQuery();

                    while (set4.next()) {
                        RestSection legend = new RestSection(set4.getString(1), set4.getString(2));
                        sectors.add(legend);
                    }


                    ResultSet set5 = connection.prepareStatement("Select name, surname, email, type from users as u left join workers as w on u.userId = w.userId where w.restaurantId = " +
                            "(select restaurantId from restaurantmanagers where userId = (select userId from users where email = " +
                            "\"" + loggedUser + "\"" + "));").executeQuery();

                    while (set5.next()) {
                        User worker = new User(set5.getString(1), set5.getString(2), set5.getString(3), set5.getString(4));
                        workers.add(worker);
                    }
                }
                // ------------------------------------ rest man. over ----------------------------------------



            } catch (SQLException e){
                e.printStackTrace();
            }
            restSize = Math.sqrt(restSize);
            int intSize = (int)restSize;

            return ok(restaurant.render(loggedUser, restaurants,menu,seats,intSize,sectors,myRestaurant,workers));
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
        try (Connection connection = DB.getConnection()){
            if (connection.prepareStatement("Insert into restaurants (name, description, address, tel, size) " +
                    "values (" + "\"" + rest.name + "\""
                    + ", \"" + rest.description + "\"" + ", \"" + rest.location + "\"" +
                    ", \"" + rest.tel + "\"" + ", \"" + addedRestSize + "\")" + ";").execute()) {

            }
        } catch (SQLException e){
            e.printStackTrace();
        }

        return ok(Json.toJson(rest));
    }

    public static Result addVictualAJAX() {
        JsonNode json = request().body().asJson();
        VictualAndDrink victual = Json.fromJson(json, VictualAndDrink.class);

        try (Connection connection = DB.getConnection()) {

            if (connection.prepareStatement("Insert into victualsanddrinks (name, description, price, type, restaurantId) " +
                    "values (" + "\"" + victual.name + "\""
                    + ", \"" + victual.description + "\"" + ", \"" + victual.price + "\""
                    + ",\"victual\", ( select restaurantId from restaurants where name ="
                    + "\"" + addedRestaurantName + "\"))" + ";").execute()) {
            }
        } catch (SQLException e){
            e.printStackTrace();
        }

        return ok(Json.toJson(victual));
    }

    public static Result editVictualAJAX() {

        JsonNode json = request().body().asJson();
        VictualAndDrink victual = Json.fromJson(json, VictualAndDrink.class);
        String myRestName = session("myRestName");

        try (Connection connection = DB.getConnection()){

            if (connection.prepareStatement("Insert into victualsanddrinks (name, description, price, type, restaurantId) " +
                    "values (" + "\"" + victual.name + "\""
                    + ", \"" + victual.description + "\"" + ", \"" + victual.price + "\""
                    + ",\"victual\", ( select restaurantId from restaurants where name ="
                    + "\"" + myRestName + "\"))" + ";").execute()) {
            }
        } catch (SQLException e){
            e.printStackTrace();
        }

        return ok(Json.toJson(victual));
    }


    public static Result addDrinkAJAX() {
        JsonNode json = request().body().asJson();
        VictualAndDrink drink = Json.fromJson(json, VictualAndDrink.class);

        try (Connection connection = DB.getConnection()) {

            if (connection.prepareStatement("Insert into victualsanddrinks (name, description, price, type, restaurantId) " +
                    "values (" + "\"" + drink.name + "\""
                    + ", \"" + drink.description + "\"" + ", \"" + drink.price + "\""
                    + ",\"drink\", ( select restaurantId from restaurants where name ="
                    + "\"" + addedRestaurantName + "\"))" + ";").execute()) {
            }
        } catch (SQLException e){
            e.printStackTrace();
        }

        return ok(Json.toJson(drink));
    }

    public static Result editDrinkAJAX() {
        JsonNode json = request().body().asJson();
        VictualAndDrink drink = Json.fromJson(json, VictualAndDrink.class);
        String myRestName = session("myRestName");

        try (Connection connection = DB.getConnection()) {

            if (connection.prepareStatement("Insert into victualsanddrinks (name, description, price, type, restaurantId) " +
                    "values (" + "\"" + drink.name + "\""
                    + ", \"" + drink.description + "\"" + ", \"" + drink.price + "\""
                    + ",\"drink\", ( select restaurantId from restaurants where name ="
                    + "\"" + myRestName + "\"))" + ";").execute()) {
            }
        } catch (SQLException e){
            e.printStackTrace();
        }

        return ok(Json.toJson(drink));

    }

    public static Result rateRestaurantAJAX() {
        JsonNode ajax_json = request().body().asJson();

        String mark;

        if (ajax_json != null)
            mark = ajax_json.toString();
        else
            return badRequest();

        StringBuilder stringBuilder = new StringBuilder();

        if(mark.length() > 0) {
            String[] tokens = mark.split(":");
            String key = tokens[0];
            for(int i = 0; i < key.length(); i++){
                if(key.charAt(i) == '{' || key.charAt(i) == '\"'){
                    continue;
                }
                stringBuilder.append(key.charAt(i));
            }
        }

        int rating = Character.getNumericValue(stringBuilder.toString().charAt(stringBuilder.toString().length() - 1));

        System.out.println("RATING: " + rating);

        return ok();
    }

    public static Result removeVictualOrDrink() {

        String myRestName = session("myRestName");
        JsonNode json = request().body().asJson();
        VictualAndDrink vd = Json.fromJson(json, VictualAndDrink.class);

        System.out.println("NAME = "+vd.name+"\nRESTID = " + myRestName);
        try (Connection connection = DB.getConnection()) {

            connection.prepareStatement("delete from victualsanddrinks where name=" +
                    "\"" + vd.name + "\""
                    + "and restaurantId= ( select restaurantId from restaurants where name =\"" + myRestName + "\")" + ";").execute();


        } catch (SQLException e){
            e.printStackTrace();
        }

        return ok();
    }

    @SuppressWarnings("Duplicates")
    public static Result addSeatSection() {

        JsonNode json = request().body().asJson();
        RestSection section = Json.fromJson(json, RestSection.class);
        if(section.sectionColor != null) {

        try (Connection connection = DB.getConnection()){

            if (connection.prepareStatement("Insert into sectornames (sectorName, sectorColor, restaurantId)" +
                    "values (" + "\"" + section.sectionName + "\""
                    + ", \"" + section.sectionColor + "\"" + ", ( select restaurantId from restaurants where name ="
                    + "\"" + addedRestaurantName + "\"))" + ";").execute()) {
            }
        } catch (SQLException e){
            e.printStackTrace();
        }

        return ok(Json.toJson(section));
        }
        else  return status(409, "No more section colors");
    }

    @SuppressWarnings("Duplicates")
    public static Result editSeatSection() {

        String myRestName = session("myRestName");
        JsonNode json = request().body().asJson();
        RestSection section = Json.fromJson(json, RestSection.class);
        if(section.sectionColor != null) {
            try (Connection connection = DB.getConnection()){

                if (connection.prepareStatement("Insert into sectornames (sectorName, sectorColor, restaurantId)" +
                        "values (" + "\"" + section.sectionName + "\""
                        + ", \"" + section.sectionColor + "\"" + ", ( select restaurantId from restaurants where name ="
                        + "\"" + myRestName + "\"))" + ";").execute()) {
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return ok(Json.toJson(section));
        }
        else  return status(409, "No more section colors");
    }

    public static Result removeSector() {
        String myRestName = session("myRestName");
        JsonNode json = request().body().asJson();
        RestSection section = Json.fromJson(json, RestSection.class);
        //  String color = json.get("sectorColor").textValue();   bad request ??

        try ( Connection connection = DB.getConnection()){

            connection.prepareStatement("delete from sectornames where sectorColor=" +
                    "\"" + section.sectionColor + "\""
                    + "and restaurantId= ( select restaurantId from restaurants where name =\"" + myRestName + "\")" + ";").execute();

            connection.prepareStatement("Update seatconfig " +
                    "set sectorColor = 'none' where sectorColor = \"" + section.sectionColor + "\" and restaurantId = ( select restaurantId from restaurants where name = " +
                    "\"" + myRestName + "\")" + ";").execute();

        } catch (SQLException e){
            e.printStackTrace();
        }

        return ok(Json.toJson(section));
    }


    public static Result saveSeatConf() {


        JsonNode j =  request().body().asJson();

        ObjectMapper mapper = new ObjectMapper();
        Map<ArrayList, ArrayList> result = mapper.convertValue(j, Map.class);
        showMenuSeatPage = false;

        try (Connection connection = DB.getConnection()){
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

    public static Result editSeatConf() {

        JsonNode j =  request().body().asJson();

        String myRestName = session("myRestName");
        ObjectMapper mapper = new ObjectMapper();
        Map<ArrayList, ArrayList> result = mapper.convertValue(j, Map.class);

        try (Connection connection = DB.getConnection()){
            for(ArrayList value : result.values()) {
                for (Object s : value) {

                    String polje = s.toString();
                    String[] deo = polje.split(":");
                    String posX = deo[1];
                    String posY = deo[2];
                    String[] deo2 = polje.split("\\|");
                    String boja = deo2[1];

                    if (connection.prepareStatement("Update seatconfig " +
                            "set sectorColor = \"" + boja + "\" where posX = \"" + posX + "\" and posY = \"" + posY + "\" and restaurantId = ( select restaurantId from restaurants where name = " +
                            "\"" + myRestName + "\")" + ";").execute()) {
                    }
                }

            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }


        return ok();
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

    @SuppressWarnings("Duplicates")
    public static Result EditMenuAndSeating() {
        String loggedUser = session("connected");
        String verified = session("verified");
        String userType = session("userType");
        List<VictualAndDrink> menu = new ArrayList<>();
        List<RestSection> seats = new ArrayList<>();
        List<RestSection> sectors = new ArrayList<>();
        String posX = ""; String posY = ""; String sectorColor = "";
        double restSize=0; int noOfSectors=0;
        if(!(userType.equals("rest-manager")) || loggedUser == null || verified.equals("0"))
            return redirect("/");

        try (Connection connection = DB.getConnection()) {

                ResultSet set2 = connection.prepareStatement("Select name, description, price, type, restaurantId from victualsanddrinks where restaurantId = " +
                        "(select restaurantId from restaurantmanagers where userId = (select userId from users where email = " +
                        "\"" + loggedUser + "\"" + "));").executeQuery();
                while (set2.next()) {
                    VictualAndDrink vd = new VictualAndDrink(set2.getString(1), set2.getString(2),
                            set2.getDouble(3), set2.getString(4));
                    menu.add(vd);

                }
                ResultSet set3 = connection.prepareStatement("Select posX, posY, sectorColor from seatconfig where restaurantId = " +
                        "(select restaurantId from restaurantmanagers where userId = (select userId from users where email = " +
                        "\"" + loggedUser + "\"" + "));").executeQuery();

                while (set3.next()) {
                    restSize++;
                    posX = set3.getString(1);
                    posY = set3.getString(2);
                    sectorColor = set3.getString(3);
                    RestSection seat = new RestSection(sectorColor, posX, posY);
                    seats.add(seat);

                }
                ResultSet set4 = connection.prepareStatement("Select sectorName, sectorColor from sectornames where restaurantId = " +
                        "(select restaurantId from restaurantmanagers where userId = (select userId from users where email = " +
                        "\"" + loggedUser + "\"" + "));").executeQuery();

                while (set4.next()) {
                    RestSection legend = new RestSection(set4.getString(1), set4.getString(2));
                    sectors.add(legend);
                }




        } catch (SQLException e){
            e.printStackTrace();
        }

        restSize = Math.sqrt(restSize);
        int intSize = (int)restSize;
        return ok(editMenuAndSeating.render(menu,seats,intSize,sectors));
    }


    public static Result restAddedFlash() {
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

        try (Connection connection = DB.getConnection()) {
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

        try (Connection connection = DB.getConnection()) {
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

        try (Connection connection = DB.getConnection()) {
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
        }

        return ok(home.render("Welcome",new play.twirl.api.Html("<center>Bidder has been added successfully!</center>") ));


    }

    public static Result addEmployee() {
        String myRestName = session("myRestName");
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

        try (Connection connection = DB.getConnection()) {
            if(connection.prepareStatement("Insert into users (name, surname, email, password, verified, type) " +
                    "values (" + "\"" + created.name + "\""
                    + ", \"" + created.surname + "\""
                    + ", \"" + created.email + "\""
                    + ", \"" + created.password + "\""
                    + ", 0"
                    + ", \"" + created.type +"\")" + ";").execute()) {
                System.out.println("Added to users table");
            }
            if(connection.prepareStatement("Insert into workers (userId, birthDate, clothNo, shoesNo, restaurantId) " +
                    "values ((select userId from users where email ="
                    + "\"" + created.email + "\")"
                    + ", \"" + created.birthDate + "\""
                    + ", \"" + created.clothNo + "\""
                    + ", \"" + created.shoeNo + "\"" + ", (select restaurantId from restaurants where name =" + "\"" + myRestName + "\"));").execute()) {

                System.out.println("Added to workers table");
            }
        } catch (SQLException e){
            e.printStackTrace();
        }

        return ok(home.render("Welcome",new play.twirl.api.Html("<center>Employee has been added successfully!</center>") ));

    }


    public static Result editRestaurant() {

        String oldName = session("myRestName");
        JsonNode json = request().body().asJson();
        Restaurant rest = Json.fromJson(json, Restaurant.class);
        for(Restaurant restoran : restaurants) {
            if (restoran.name.equals(rest.name) && !(restoran.name.equals(oldName))) {
                return status(409, "Already used restName");
            }
        }

        try (Connection connection = DB.getConnection()){

            if(connection.prepareStatement("Update restaurants" +
                    " set name =" + "\"" + rest.name + "\"" +
                    ", description =" + "\"" + rest.description + "\"" + ", tel =" + "\"" + rest.tel + "\"" +
                    ", address = " + "\"" + rest.location + "\" where name = " + "\"" + oldName + "\"" + ";").execute()){
                System.out.println("Restaurant's info successfully changed!");

            }

        } catch (SQLException e){
            e.printStackTrace();
        }

        session("myRestName", rest.name);


        return ok();
    }

    public static Result restEditedFlash() {
        flash("editRestSuccess", "Your restaurant info has been successfully changed.");
        return redirect("/restaurants");

    }

    public static Result addWorkTime() {

        JsonNode json = request().body().asJson();
        WorkTime wt = Json.fromJson(json, WorkTime.class);

        try (Connection connection = DB.getConnection()){

            ResultSet set = connection.prepareStatement("Select date from workingday where userId = " +
                    "(select userId from users where email = " + "\"" + wt.workerEmail + "\"" + ");").executeQuery();
            while (set.next()) {
                String datum = set.getString(1);
                if(datum.equals(wt.date))
                    return status(409, "Worker already works on that day");
            }

            if (connection.prepareStatement("Insert into workingday (date, startTime, endTime,sectorName,userId)" +
                    "values (" + "\"" + wt.date + "\""
                    + ", \"" + wt.startTime + "\"" + ", \"" + wt.endTime + "\""  + ", \"" + wt.workingSector + "\""  +
                    ",(select userId from users where email = " + "\"" + wt.workerEmail + "\"" + "));").execute()) {
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ok();
    }

}


