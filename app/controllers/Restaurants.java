

package controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.*;


import play.data.DynamicForm;
import play.data.Form;
import play.db.DB;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.*;


public class Restaurants extends Controller {

    public static String addedRestaurantName;
    public static int addedRestSize;
    public static List<Restaurant> restaurants = new ArrayList<Restaurant>();
    public static boolean showMenuSeatPage = false;
    public static int myRestId;

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

                String query = "Select restaurantId, name, description, address, tel, size from restaurants";
                ResultSet set = connection.prepareStatement(query).executeQuery();
                while(set.next()){
                    Restaurant restaurant =
                            new Restaurant(set.getString(1), set.getString(2),
                                    set.getString(3), set.getString(4), set.getString(5),
                                    set.getString(6));
                    if(restaurant.name.equals(myRestName) && (session("userType")).equals("rest-manager"))
                        myRestaurant = restaurant;
                    restaurants.add(restaurant);
                }
                //----------------------------- if rest manager ------------------------------------------------------

                if((session("userType")).equals("rest-manager")) {

                    myRestId = Integer.parseInt(myRestaurant.id);
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

    public static Result getWorkersRating() throws SQLException {

        JsonNode json = request().body().asJson();
        User worker = Json.fromJson(json, User.class);
        double ocena = 0;
        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;

            stmt = connection.prepareStatement("select rr.serviceRating, u.name from restaurantsRating as rr left join orders as o on o.restaurantId = rr.restaurantId left join orderVictualDrink as ovd on " +
                    "ovd.orderId = o.orderId left join workers as w on w.userId = ovd.workerId left join users as u on u.userId = w.userId where rr.restaurantId = ? and u.email = ?;");
            stmt.setInt(1, myRestId);
            stmt.setString(2, worker.email);

            ResultSet result = stmt.executeQuery();
            int n = 0;

            while (result.next()) {
                ocena += result.getInt(1);
                n++;
            }
            ocena /= n;
        }catch(SQLException sqle) {
            sqle.printStackTrace();
        }

        Report r = new Report();
        r.rating = ocena;

        return ok(Json.toJson(r));
    }

    public static Result getMealRating() throws SQLException {

        JsonNode json = request().body().asJson();
        VictualAndDrink vd = Json.fromJson(json, VictualAndDrink.class);
        double ocena = 0;
        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;

            stmt = connection.prepareStatement("select rr.mealRating from restaurantsRating as rr left join orders as o on o.restaurantId = rr.restaurantId left join orderVictualDrink as ovd " +
                    "on o.orderId = ovd.orderId left join victualsanddrinks as v on ovd.victualDrinkId = v.victualsAndDrinksId where v.name = ? and o.restaurantId = ?;");
            stmt.setInt(2, myRestId);
            stmt.setString(1, vd.name);

            ResultSet result = stmt.executeQuery();
            int n = 0;

            while (result.next()) {
                ocena += result.getInt(1);
                n++;
            }
            ocena /= n;
        }catch(SQLException sqle) {
            sqle.printStackTrace();
        }

        Report r = new Report();
        r.rating = ocena;

        return ok(Json.toJson(r));
    }

    public static Result report() throws Exception {


        String tip = session("userType");
        String verified = session("verified");
        String userId = session("userId");
        String loggedUser = session("connected");
        String myRestName = session("myRestName");
        // ---- prikazi ovu stranicu samo ako je korisnik ulogovani i verifikovani rest-manager.
        if (loggedUser == null || verified.equals("0"))
            return redirect("/");
        else if (!tip.equals("rest-manager"))
            return redirect("/");
        // ----------------------------

        List<Notification> notifikacije = new ArrayList<>();
        double ocena_rest=0;
        double prihod=0;
        int n=0;
        int nn=0;
        List<Report> raport = new ArrayList<>();
        List<User> workers = new ArrayList<>();
        List<VictualAndDrink> meals = new ArrayList<>();
        try (Connection connection = DB.getConnection()) {

            PreparedStatement stmt = null;

            stmt = connection.prepareStatement("Select rating from restaurantsRating where restaurantId = ?;");
            stmt.setInt(1, myRestId);
            ResultSet result = stmt.executeQuery();

            while (result.next()) {
              ocena_rest += result.getInt(1);
              n++;

            }
            ocena_rest = ocena_rest / n;


            stmt = connection.prepareStatement("Select price from orders where restaurantId = ?;");
            stmt.setInt(1, myRestId);
            result = stmt.executeQuery();

            while (result.next()) {
                prihod += result.getInt(1);
                nn++;

            }
            prihod /= nn;

            stmt = connection.prepareStatement("Select name, surname, email, type from users as u left join workers as w on u.userId = w.userId where w.restaurantId" +
                    " = (select restaurantId from restaurantManagers where userId = (select userId from users where email = ?));");

            stmt.setString(1, loggedUser);
            result = stmt.executeQuery();

            while (result.next()) {
                User worker = new User(result.getString(1), result.getString(2), result.getString(3), result.getString(4));
                workers.add(worker);

            }

            stmt = connection.prepareStatement("Select name from victualsAndDrinks where restaurantId = ?;");

            stmt.setInt(1, myRestId);
            result = stmt.executeQuery();

            while (result.next()) {
                VictualAndDrink vd = new VictualAndDrink();
                vd.name=(result.getString(1));
                meals.add(vd);

            }


            stmt = connection.prepareStatement("Select visitDate from visits where restaurantId = ?;");
            stmt.setInt(1, myRestId);
            result = stmt.executeQuery();
            List<String> datumi = new ArrayList<>();
            List<String> sedmica = new ArrayList<>();

            while (result.next()) {
                datumi.add(result.getString(1));
            }
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat format2 = new SimpleDateFormat("EEE, MMM d");
            Date date = new Date();
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            c.add(Calendar.DATE, -7);
            for(int i = 0; i < 7; i++){
                c.add(Calendar.DATE, +1);
                String dan = format1.format(c.getTime());
                sedmica.add(dan);
            }
            for(String dan : sedmica){
                int brPoseta = 0;
                System.out.print("DAN " + myRestId);
                for(String datum : datumi){
                    System.out.print("\nDATUM  " + datum);
                    if(dan.equals(datum)){
                        brPoseta++;
                    }
                }
                    Date date2 = format1.parse(dan);
                    String dan2 = format2.format(date2);
                    Report r = new Report(dan2, brPoseta);
                    raport.add(r);
                    ObjectMapper mapper = new ObjectMapper();
                    String notifString = mapper.writeValueAsString(r);
                    System.out.print("OHOH: " + notifString);

            }

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }


        return ok(report.render(ocena_rest, raport, workers, meals, prihod));
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


    public static Result saveSeatConf() throws SQLException {


        JsonNode j =  request().body().asJson();

        ObjectMapper mapper = new ObjectMapper();
        Map<ArrayList, ArrayList> result = mapper.convertValue(j, Map.class);
        showMenuSeatPage = false;
        Connection connection = null;
        PreparedStatement insertSeat = null;
        StringJoiner sj = new StringJoiner(", ");
        for(ArrayList value : result.values()) {
            for (Object s : value) {
                StringBuilder build = new StringBuilder("Insert into seatconfig (posX, posY, sectorColor, restaurantId) values ");
                String elem = "(?, ?, ?, ?)";
                sj.add(elem);
            }
        }


        try {


            connection = DB.getConnection();

            connection.setAutoCommit(false);

            for(ArrayList value : result.values()) {
                for (Object s : value) {
                    String polje = s.toString();
                    String[] deo = polje.split(":");
                    String posX = deo[1];
                    String posY = deo[2];
                    String[] deo2 = polje.split("\\|");
                    String boja = deo2[1];


                    insertSeat = connection.prepareStatement("Insert into seatconfig (posX, posY, sectorColor, restaurantId)" +
                            " values (?,?,?, ( select restaurantId from restaurants where name = ?));");

                    insertSeat.setString(1, posX);
                    insertSeat.setString(2, posY);
                    insertSeat.setString(3, boja);
                    insertSeat.setString(4, addedRestaurantName);
                    insertSeat.executeUpdate();

                }

            }

        }
        catch (SQLException sqle){

            try {
                if (connection != null && !connection.getAutoCommit()) {
                    System.err.print("Transaction is being rolled back");
                    connection.rollback();
                }
                if (insertSeat != null) {
                    insertSeat.close();
                }

            } catch(SQLException excep) {
                excep.printStackTrace();
            }
            sqle.printStackTrace();
        }
        finally {

            if(connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }

        return ok();
    }
   // update seatconfig set sectorColor = ? where posX = ? and posY = ? and restaurantId = (select restaurantId from restaurants where name = ?);
    public static Result editSeatConf() throws SQLException  {

        JsonNode j =  request().body().asJson();

        String myRestName = session("myRestName");
        ObjectMapper mapper = new ObjectMapper();
        Map<ArrayList, ArrayList> result = mapper.convertValue(j, Map.class);
        Connection connection = null;
        PreparedStatement updateSeat = null;

        String query = "update seatconfig set sectorColor = ? where posX = ? and posY = ? and restaurantId = (select restaurantId from restaurants where name = ?);";
        long start = System.currentTimeMillis();
        try {

            connection = DB.getConnection();
            connection.setAutoCommit(false);
            int i = 0;
            updateSeat = connection.prepareStatement(query);
            for(ArrayList value : result.values()) {
                for (Object s : value) {
                    String polje = s.toString();
                    String[] deo = polje.split(":");
                    String posX = deo[1];
                    String posY = deo[2];
                    String[] deo2 = polje.split("\\|");
                    String boja = deo2[1];

                    updateSeat.setString(i + 1, boja );
                    updateSeat.setString(i + 2, posX );
                    updateSeat.setString(i + 3, posY );
                    updateSeat.setString(i + 4, myRestName );

                    updateSeat.addBatch();


                }

            }
            updateSeat.executeBatch();
            //  System.out.println("REZ JE " + rez);
            connection.commit();
            long end = System.currentTimeMillis();
            System.out.println("Batch time was " + (end - start));

        }
        catch (SQLException sqle){

            try {
                if (connection != null && !connection.getAutoCommit()) {
                    System.err.print("Transaction is being rolled back\n");
                    connection.rollback();

                }
                if (updateSeat != null) {
                    updateSeat.close();
                }

            } catch(SQLException excep) {
                excep.printStackTrace();
            }
            sqle.printStackTrace();
        }
        finally {

            if(connection != null) {

                connection.close();
            }
        }

        return ok();
    }

    public static Result addGroceries() throws java.io.IOException, SQLException {


        JsonNode j =  request().body().asJson();

        ObjectMapper mapper = new ObjectMapper();
        Map<ArrayList, ArrayList> result = mapper.convertValue(j, Map.class);

        showMenuSeatPage = false;
        Connection connection = null;
        PreparedStatement addGroceries = null;


        try {

            connection = DB.getConnection();
            connection.setAutoCommit(false);
            addGroceries = connection.prepareStatement("insert into requestedFood (name, amount, requestId) values (?, ?, ?);");

            for (ArrayList value : result.values()) {
                for (Object s : value) {
                    String jsonString = mapper.writeValueAsString(s);
                    RequestFood req = mapper.readValue(jsonString, RequestFood.class);
                    addGroceries.setString(1, req.name);
                    addGroceries.setInt(2, req.amount);
                    addGroceries.setInt(3, req.reqId);

                    addGroceries.addBatch();


                }
            }
            addGroceries.executeBatch();
        }
        catch (SQLException sqle){

            try {
                if (connection != null && !connection.getAutoCommit()) {
                    System.err.print("Transaction is being rolled back");
                    connection.rollback();

                }
                if (addGroceries != null) {
                    addGroceries.close();
                }

            } catch(SQLException excep) {
                excep.printStackTrace();
            }
            sqle.printStackTrace();
        }
        finally {

            if(connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }
        return ok();
    }


    public static Result addRequest() throws SQLException {

        String myRestName = session("myRestName");
        JsonNode json = request().body().asJson();
        Request req = Json.fromJson(json, Request.class);
        int reqId;

        Connection connection = null;
        PreparedStatement insertReq = null;
        PreparedStatement getId = null;

        try {


            connection = DB.getConnection();

            connection.setAutoCommit(false);


            insertReq = connection.prepareStatement("insert into requests (fromDate, dueDate, restaurantId, isActive) values (?,?,(select restaurantId from restaurants where name = ?), true);");
            insertReq.setString(1, req.dateFrom);
            insertReq.setString(2, req.dateTo);
            insertReq.setString(3, myRestName);
            insertReq.executeUpdate();

            getId = connection.prepareStatement("select last_insert_id();");
            ResultSet result = getId.executeQuery();
            result.next();
            reqId = result.getInt(1);
            System.out.print("\nID JE " + reqId + "\n");
            req.reqId = reqId;
        }

        catch (SQLException sqle){

            try {
                if (connection != null && !connection.getAutoCommit()) {
                    System.err.print("Transaction is being rolled back");
                    connection.rollback();
                    return status(409, "Error while writing in db");

                }
                if (insertReq != null) {
                    insertReq.close();
                }


            } catch(SQLException excep) {
                excep.printStackTrace();
            }
            sqle.printStackTrace();
        }
        finally {

            if(connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }

        return ok(Json.toJson(req));
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
        PreparedStatement stmt = null;
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
                ResultSet set3 = connection.prepareStatement("Select posX, posY, sectorColor, seatId from seatconfig where restaurantId = " +
                        "(select restaurantId from restaurantmanagers where userId = (select userId from users where email = " +
                        "\"" + loggedUser + "\"" + "));").executeQuery();
                List<Integer> rezervisani = new ArrayList<>();
                String imaRezervaciju = "free";
                while (set3.next()) {
                    stmt = connection.prepareStatement("select seatId from baklava.reservationSeats where (reservationId in (select reservationId from reservations where restaurantId = ?));");
                    stmt.setInt(1, myRestId);
                    ResultSet result = stmt.executeQuery();
                    imaRezervaciju = "free";
                    while (result.next()) {
                        rezervisani.add(result.getInt(1));
                    }
                    restSize++;
                    posX = set3.getString(1);
                    posY = set3.getString(2);
                    sectorColor = set3.getString(3);
                    for(Integer seat : rezervisani){
                        if(seat == set3.getInt(4))
                            imaRezervaciju = "reserved";

                    }
                    RestSection seat = new RestSection(sectorColor, posX, posY, imaRezervaciju);
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


