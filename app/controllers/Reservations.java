package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.*;
import org.joda.time.DateTime;
import play.data.DynamicForm;
import play.data.Form;
import play.data.format.Formats;
import play.db.DB;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.reserve;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by goran on 21.6.17..
 */
public class Reservations extends Controller {

    public static Result searchRestaurants() throws SQLException{

        String loggedIn = session("connected");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }


        JsonNode jsonReq = request().body().asJson();
        RestaurantSearchData restaurantSearchData = Json.fromJson(jsonReq, RestaurantSearchData.class);

        Connection connection = null;
        PreparedStatement searchQuery = null;
        ResultSet resultSet = null;
        ArrayList<Restaurant> resultArray = new ArrayList<Restaurant>();

        try {
            connection = DB.getConnection();
            searchQuery = connection.prepareStatement("select * from baklava.restaurants " +
                    "where name like ? or description like ?");
            searchQuery.setString(1, "%" + restaurantSearchData.text + "%");
            searchQuery.setString(2, "%" + restaurantSearchData.text + "%");

            resultSet = searchQuery.executeQuery();

            while (resultSet.next()) {
                resultArray.add(new Restaurant(
                        Integer.toString(resultSet.getInt(1)),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        resultSet.getString(5),
                        Integer.toString(resultSet.getInt(6))
                ));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (searchQuery != null) {
                searchQuery.close();
            }
            if (connection != null) {
                connection.close();
            }
        }

        return ok(Json.toJson(resultArray));
    }


    @SuppressWarnings("Duplicates")
    public static Result reserve1(int restaurantId) {
        String loggedIn = session("connected");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }

        // treba napraviti rezervaciju sa nekim default podacima; forma se apdejtuje u sledecem koraku

        int reservationId = 0;

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DB.getConnection();
            connection.setAutoCommit(false);

            preparedStatement = connection.prepareStatement("insert into baklava.reservations " +
                    "(userId, restaurantId) values (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, Integer.parseInt(session("userId")));
            preparedStatement.setInt(2, restaurantId);

            preparedStatement.executeUpdate();

            connection.commit();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if(generatedKeys.next()) {
                    reservationId = generatedKeys.getInt(1);
                    generatedKeys.close();
                }
                else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }



        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return internalServerError();
        }
        finally {
            try {
                connection.close();
            } catch (SQLException e) {}
            try {
                preparedStatement.close();
            } catch (SQLException e) {}
        }

        return ok(reserve.render(1, 0, reservationId, restaurantId, "foobar", loggedIn, null, 0, null));
    }

    @SuppressWarnings("Duplicates")
    public static Result reserve2() {
        String loggedIn = session("connected");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }

        DynamicForm requestData = new DynamicForm().bindFromRequest();
        // System.out.println(requestData.get());

        int reservationId = Integer.parseInt(requestData.get("reservationId"));
        int restaurantId = Integer.parseInt(requestData.get("restaurantId"));
        int hours = Integer.parseInt(requestData.get("hours"));
        DateTime dateTime = new DateTime(requestData.get("dateAndTime"));
        java.sql.Timestamp startTimestamp = new java.sql.Timestamp(dateTime.getMillis());
        java.sql.Timestamp endTimestamp = new java.sql.Timestamp(dateTime.getMillis() + hours * 1000*60*60);
        //System.out.println(startDate + " : " + endDate + " : " + startTime + " : " + endTime);


        Connection connection = null;
        PreparedStatement preparedStatement1 = null;
        PreparedStatement preparedStatement2 = null;
        PreparedStatement preparedStatement3 = null;
        PreparedStatement preparedStatement4 = null;
        ResultSet set1 = null;
        ResultSet set2 = null;
        ResultSet set3 = null;
        ResultSet set4 = null;


        try {

            connection = DB.getConnection();
            connection.setAutoCommit(false);
            // update date and time
            preparedStatement1 = connection.prepareStatement("update baklava.reservations " +
                    "set startTimestamp = ?, endTimestamp = ? where reservationId = ?");
            preparedStatement1.setTimestamp(1, startTimestamp);
            preparedStatement1.setTimestamp(2, endTimestamp);
            preparedStatement1.setInt(3, reservationId);

            preparedStatement1.executeUpdate();
            connection.commit();


        } catch (SQLException e){
            e.printStackTrace();
            return internalServerError();
        } finally {
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (Exception e) {e.printStackTrace();}
            try {
                preparedStatement1.close();
            } catch (Exception e) {e.printStackTrace();}
        }

        //sad mi treba raspored stolova iz baze

        //List<VictualAndDrink> menu = new ArrayList<>();
        List<RestSection> seats = new ArrayList<>();
        List<RestSection> sectors = new ArrayList<>();
        String posX = ""; String posY = ""; String sectorColor = "";
        HashMap<Integer, RestSection> seatsMap = new HashMap<Integer, RestSection>();
        double restSize=0; int noOfSectors=0;
        int seatId = 0;

        try {
           connection = DB.getConnection();

            preparedStatement3 = connection.prepareStatement("Select posX, posY, sectorColor, seatId from seatconfig where restaurantId = ?");
            preparedStatement3.setInt(1, restaurantId);

            set3 = preparedStatement3.executeQuery();

            while (set3.next()) {
                restSize++;
                posX = set3.getString(1);
                posY = set3.getString(2);
                sectorColor = set3.getString(3);
                seatId = set3.getInt(4);
                RestSection seat = new RestSection(sectorColor, posX, posY, "free", seatId);
                seats.add(seat);
                seatsMap.put(seat.seatId, seat);
            }
            preparedStatement4 = connection.prepareStatement("Select sectorName, sectorColor from sectornames where restaurantId = ?");
            preparedStatement4.setInt(1, restaurantId);
            set4 = preparedStatement4.executeQuery();


            while (set4.next()) {
                RestSection legend = new RestSection(set4.getString(1), set4.getString(2));
                sectors.add(legend);
            }

        } catch (SQLException e){
            e.printStackTrace();
        }
        finally {
            try {
                preparedStatement3.close();
            } catch (Exception e) {e.printStackTrace();}
            try {
                preparedStatement4.close();
            } catch (Exception e) {e.printStackTrace();}
            try {
                set3.close();
            } catch (Exception e) {e.printStackTrace();}
            try {
                set4.close();
            } catch (Exception e) {e.printStackTrace();}
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (Exception e) {e.printStackTrace();}
        }

        restSize = Math.sqrt(restSize);
        int intSize = (int)restSize;

        ArrayList<RestSection> reservedSeats = new ArrayList<>();

        //onda mi trebaju rezervisani stolovi za to vreme
        try {
            connection = DB.getConnection();
            // trebaju mi seatId za svaki sto gde je vreme u rezervaciji vezanoj za taj sto izmedju moja dva vremena
            preparedStatement1 = connection.prepareStatement("" +
                    "select seatId from baklava.reservationSeats " +
                    "where (reservationId in " +
                    "(select reservationId from reservations " +
                    "where restaurantId = ? " +
                    "and ((startTimestamp between ? and ?) " +
                    "or (endTimestamp between ? and ?) " +
                    "or (? between startTimestamp and endTimestamp)" +
                    "or (? between startTimestamp and endTimestamp))))");
            preparedStatement1.setInt(1, restaurantId);
            preparedStatement1.setTimestamp(2, startTimestamp);
            preparedStatement1.setTimestamp(3, endTimestamp);
            preparedStatement1.setTimestamp(4, startTimestamp);
            preparedStatement1.setTimestamp(5, endTimestamp);
            preparedStatement1.setTimestamp(6, startTimestamp);
            preparedStatement1.setTimestamp(7, endTimestamp);

            set1 = preparedStatement1.executeQuery();

            while (set1.next()) {
                seatId = set1.getInt(1);
                RestSection seat = new RestSection("reserved", seatId);
                reservedSeats.add(seat);
                //onda podesavam sta je rezervisano
                seatsMap.get(seatId).status = "reserved";
            }

            seats = new ArrayList<RestSection>(seatsMap.values());

            System.out.println(reservedSeats);

        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerError();
        } finally {
            try {
                preparedStatement1.close();
            } catch (Exception e) {e.printStackTrace();}
            try {
                set1.close();
            } catch (Exception e) {e.printStackTrace();}
            try {
                connection.close();
            } catch (Exception e) {e.printStackTrace();}
        }


        //onda raspored stolova sa oznacenim rezervisanim saljem templejtu


        return ok(reserve.render(2, 0,reservationId, restaurantId, "foobar", loggedIn, seats, intSize, sectors));
    }


    @SuppressWarnings("Duplicates")
    public static Result reserve3() throws Exception{
        String loggedIn = session("connected");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }

        DynamicForm requestData = new DynamicForm().bindFromRequest();
        //System.out.println(requestData.get());
        int restaurantId = Integer.parseInt(requestData.get("restaurantIdInput"));
        int reservationId = Integer.parseInt(requestData.get("reservationIdInput"));
        String[] seatIds = requestData.get("seatIdsInput").replace('[', '\0').replace(']', '\0').replace('"', '\0').split(",");
        ArrayList<Integer> seatIdsInt = new ArrayList<>();
        for (String seatId : seatIds) {
            seatId = seatId.trim();
            if (!seatId.equals("")) {
                System.out.println(seatId);
                seatIdsInt.add(Integer.parseInt(seatId));
            }
        }

        //treba upisati dobijene seatIds u reservationSeats

        Connection connection = null;
        PreparedStatement preparedStatement1 = null;
        PreparedStatement preparedStatement2 = null;
        ResultSet resultSet1 = null;
        ResultSet resultSet2 = null;

        try {
            connection = DB.getConnection();
            connection.setAutoCommit(false);

            preparedStatement1 = connection.prepareStatement("insert into baklava.reservationSeats " +
                    "(seatId, reservationId) values (?, ?)");

            for (int seatId : seatIdsInt) {
                preparedStatement1.clearParameters();
                preparedStatement1.setInt(1, seatId);
                preparedStatement1.setInt(2, reservationId);
                preparedStatement1.executeUpdate();
            }

            connection.commit();
        }
        catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
            return internalServerError();
        }
        finally {
            try {
                preparedStatement1.close();
            } catch (Exception e) {e.printStackTrace();}
//            try {
//                resultSet1.close();
//            } catch (Exception e) {e.printStackTrace();}
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (Exception e) {e.printStackTrace();}
        }

        // sad treba preusmeriti na formu 3 - prijatelji

        return ok(reserve.render(3, Integer.parseInt(session("userId")), reservationId, restaurantId, "foobar", loggedIn, null, 0, null));
    }


    public static Result reserve4(String userIdStr, String restaurantIdStr, String reservationIdStr) {
        String loggedIn = session("connected");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }

        int userId = 0, restaurantId = 0, reservationId = 0;

        try {
            userId = Integer.parseInt(userIdStr);
            restaurantId = Integer.parseInt(restaurantIdStr);
            reservationId = Integer.parseInt(reservationIdStr);
        } catch (Exception e) {}

        if (userId == 0 || restaurantId == 0 || reservationId == 0) {
            return notFound();
        }

        return badRequest();
    }


    public static Result inviteFriend() {

        return badRequest();
    }

    public static Result cancelInvite() {
        return badRequest();
    }
}
