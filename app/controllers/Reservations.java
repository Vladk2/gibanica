package controllers;

import akka.actor.ActorRef;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.swing.text.AbstractDocument;
import java.sql.*;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

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

            preparedStatement3 = connection.prepareStatement("Select posX, posY, sectorColor, seatId from seatConfig where restaurantId = ?");
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
            preparedStatement4 = connection.prepareStatement("Select sectorName, sectorColor from sectorNames where restaurantId = ?");
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

                RestSection rs = new RestSection();
                rs.seatId = seatId;
                for(ActorRef klijent : Bids.managerEdit.values()){
                    ObjectMapper mapper = new ObjectMapper();
                    String notifString = mapper.writeValueAsString(seatId);
                    klijent.tell(notifString,  ActorRef.noSender());
                }
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

        session("currentReservationRestaurantId", Integer.toString(restaurantId));
        session("currentReservationReservationId", Integer.toString(reservationId));

        List<VictualAndDrink> menu = new ArrayList<>();
        try {
            Connection connection = DB.getConnection();
            PreparedStatement preparedStatement = null;

            preparedStatement = connection.prepareStatement("Select name, description, price, type, restaurantId from victualsanddrinks where restaurantId = ?");
            preparedStatement.setInt(1, restaurantId);

            ResultSet result = preparedStatement.executeQuery();

            while (result.next()) {
                VictualAndDrink vd = new VictualAndDrink(result.getString(1),
                        result.getString(2),
                        result.getDouble(3), result.getString(4));
                menu.add(vd);
            }

            result.close();
            preparedStatement.close();
            connection.close();

        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return ok(views.html.reserve4.render(userId, restaurantId,
                reservationId, loggedIn, menu, new Order()));
    }


    @SuppressWarnings("Duplicates")
    public static Result createGuestOrder() {
        String loggedIn = session("userId");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }

        String email = session("connected");

        int reservationId = 0;
        int restaurantId = 0;

        try {
            restaurantId = Integer.parseInt(session("currentReservationRestaurantId"));

            reservationId = Integer.parseInt(session("currentReservationReservationId"));
        } catch (Exception e) {return badRequest("no reservations in progress");}

        try (Connection connection = DB.getConnection()){
            connection.setAutoCommit(false);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            Order request = objectMapper.convertValue(ajax_json, Order.class);

            HashMap<String, String> ord_items = new HashMap<>();

            List<String> order_items = new ArrayList<>();

            // random konobar
            PreparedStatement rndKonobar = connection.prepareStatement("" +
                    "select userId from baklava.users where type = 'waiter' and userId in\n" +
                    "    (select userId from workers where restaurantId = ?)\n" +
                    "    order by rand() limit 1");
            rndKonobar.setInt(1, restaurantId);

            ResultSet setKonobar = rndKonobar.executeQuery();
            int konobarId = 0;
            while (setKonobar.next()) {
                konobarId = setKonobar.getInt(1);
            }
            setKonobar.close();
            rndKonobar.close();
            System.out.println("random waiter: " + konobarId);

            // vreme kreiranja porudzbine
            java.util.Date current_datetime = new java.util.Date();
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            String sqlDate = formatter.format(current_datetime);
            String sqlTime = new SimpleDateFormat("HH:MM:s").format(current_datetime);

            String new_order = "Insert into orders (orderDate, orderTime, guestId, waiterId, restaurantId," +
                    " price) values (?, ?, ?, ?, ?, ?);";

            PreparedStatement pstmt = connection.prepareStatement(new_order);
            pstmt.setString(1, sqlDate);
            pstmt.setString(2, sqlTime);
            pstmt.setString(3, loggedIn);
            pstmt.setInt(4, konobarId);
            pstmt.setInt(5, restaurantId);
            pstmt.setBigDecimal(6, request.getPrice());

            pstmt.execute();

            // uzmi id porudzbine koja je tek povezana

            String get_orderId = "Select orderId from orders where waiterId = ? and orderDate = ? and orderTime = ?";
            PreparedStatement getId = connection.prepareCall(get_orderId);
            getId.setInt(1, konobarId);
            getId.setString(2, sqlDate);
            getId.setString(3, sqlTime);

            String orderId = "";

            ResultSet resultSet = getId.executeQuery();

            while(resultSet.next()){
                orderId = resultSet.getString(1);
            }

            resultSet.close();
            getId.close();

            String new_query = "Insert into orderVictualDrink (orderId, victualDrinkId, isReady, accepted, workerId," +
                    "quantity) values (?, (Select victualsAndDrinksId from victualsanddrinks where name = ?), " +
                    "0, 0, ?, ?);";


            int workerIdVictuals = 0;
            int workerIdDrinks = 0;
            for(int i = 0; i < request.getVictualsDrinks().size(); i++) {
                if (request.getVictualsDrinks().get(i).get("type").equals("victual") && workerIdVictuals == 0) {
                    //query za random kuvara
                    PreparedStatement rndQuery = connection.prepareStatement("" +
                            "select userId from baklava.users where type = 'chef' and userId in " +
                            "    (select userId from workers where restaurantId = ?) " +
                                    "    order by rand() limit 1");
                    rndQuery.setInt(1, restaurantId);
                    ResultSet rndSet = rndQuery.executeQuery();
                    while (rndSet.next()) {
                        workerIdVictuals = rndSet.getInt(1);
                    }
                    rndSet.close();
                    rndQuery.close();
                    System.out.println("random chef: " + workerIdVictuals);
                    request.getVictualsDrinks().get(i).put("workerId", Integer.toString(workerIdVictuals));
                }
                else if (request.getVictualsDrinks().get(i).get("type").equals("drink") && workerIdDrinks == 0) {
                    //query za random barmena
                    PreparedStatement rndQuery = connection.prepareStatement("" +
                            "select userId from baklava.users where type = 'bartender' and userId in " +
                            "    (select userId from workers where restaurantId = ?) " +
                            "    order by rand() limit 1");
                    rndQuery.setInt(1, restaurantId);
                    ResultSet rndSet = rndQuery.executeQuery();
                    while (rndSet.next()) {
                        workerIdDrinks = rndSet.getInt(1);
                    }
                    rndSet.close();
                    rndQuery.close();
                    System.out.println("random bartender: " + workerIdDrinks);
                    request.getVictualsDrinks().get(i).put("workerId", Integer.toString(workerIdDrinks));
                }

                PreparedStatement preparedStatement1 = connection.prepareStatement(new_query);
                preparedStatement1.setString(1, orderId);
                preparedStatement1.setString(2, request.getVictualsDrinks().get(i).get("name"));
                if (request.getVictualsDrinks().get(i).get("type").equals("victual")) {
                    preparedStatement1.setInt(3, workerIdVictuals);
                } else if (request.getVictualsDrinks().get(i).get("type").equals("drink")) {
                    preparedStatement1.setInt(3, workerIdDrinks);
                }
                preparedStatement1.setString(4, request.getVictualsDrinks().get(i).get("quantity"));

                preparedStatement1.execute();
                preparedStatement1.close();
            }

            //popuni tabelu reservationOrders - reservationId, userId, orderId, PrepareBeforeArrival
            PreparedStatement reservationOrders = connection.prepareStatement("insert into baklava.reservationOrders " +
                    "(reservationId, userId, orderId) values (?, ?, ?)");
            reservationOrders.setInt(1, reservationId);
            reservationOrders.setString(2, loggedIn);
            reservationOrders.setString(3, orderId);
            reservationOrders.executeUpdate();

            connection.commit();

            System.out.println(request.getVictualsDrinks());

            String notification_message = String.format("Order ID: %s\nThere are new orders waiting " +
                    "for your confirmation.", request.getOrderId());
            for(int i = 0; i < request.getVictualsDrinks().size(); i++) {
                Orders.saveNotification(request.getVictualsDrinks().get(i).get("workerId"), notification_message);
                try {
                    for(Map.Entry<String, ActorRef> websocket : Orders.clients_mail.entrySet()){
                        websocket.getValue().tell(notification_message, ActorRef.noSender());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            connection.setAutoCommit(true);
            connection.close();

            return ok("Order successfully created");
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError("nesto otislo dovraga");
        }
    }

    public static Result getReservations () {
        String loggedIn = session("userId");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }
        String email = session("connected");

        ArrayList<Reservation> reservations = new ArrayList<>();
        Connection connection = null;
        PreparedStatement preparedStatement1 = null;
        PreparedStatement preparedStatement2 = null;
        PreparedStatement preparedStatement3 = null;
        ResultSet resultSet1 = null;
        ResultSet resultSet2 = null;
        ResultSet resultSet3 = null;

        try {
            connection = DB.getConnection();

            preparedStatement1 = connection.prepareStatement("select res.reservationId, res.restaurantId, rest.name, res.startTimestamp, res.endTimestamp " +
                    "from baklava.reservations as res join baklava.restaurants as rest on res.restaurantId = rest.restaurantId " +
                    "where userId = ?");
            preparedStatement1.setInt(1, Integer.parseInt(loggedIn));
            resultSet1 = preparedStatement1.executeQuery();
            int reservationId = 0;
            int seatCount = 0;
            int restaurantId = 0;
            String restaurantName = "";
            Reservation res = null;
            Timestamp start = null;
            Timestamp end = null;
            //ArrayList<VictualAndDrink> victualAndDrinks = null;

            //petlja
            while (resultSet1.next()) {
                reservationId = resultSet1.getInt(1);
                restaurantId = resultSet1.getInt(2);
                restaurantName = resultSet1.getString(3);
                start = resultSet1.getTimestamp(4);
                end = resultSet1.getTimestamp(5);
                preparedStatement2 = connection.prepareStatement("select count(*) from baklava.reservationSeats where reservationId = ?");
                preparedStatement2.setInt(1, reservationId);
                resultSet2 = preparedStatement2.executeQuery();
                while (resultSet2.next()) {
                    seatCount = resultSet2.getInt(1);
                }
                //victualAndDrinks = new ArrayList<>();

                // preparedStatement3 = connection.prepareStatement("select victualDrinkId, quantity, name, price")

                res = new Reservation(reservationId, Integer.parseInt(loggedIn), restaurantId, start, end,
                        null, seatCount, restaurantName);
                reservations.add(res);
                resultSet2.close();
                preparedStatement2.close();
            }


            System.out.println(Json.toJson(reservations));
            return ok(Json.toJson(reservations));
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            try {
                connection.close();
            } catch(Exception e) {}
            try {
                connection.close();
            } catch(Exception e) {}
        }
        return internalServerError("nesto je otislo dovraga");

    }

    public static Result cancelReservation() {
        String loggedIn = session("connected");
        if (loggedIn == null) {
            return redirect("/"); // nije ulogovan
        }

        JsonNode jsonReq = request().body().asJson();
        CancelReservation cancelReservation = Json.fromJson(jsonReq, CancelReservation.class);

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = DB.getConnection();
            connection.setAutoCommit(false);

            String[] queries = ("delete from baklava.reservationSeats where reservationId = ?;" +
                    "delete from baklava.reservationGuests where reservationId = ?;" +
                    "delete from baklava.orderVictualDrink where orderid in (select orderId from baklava.reservationOrders where reservationId = ?);" +
                    "delete from baklava.reservationOrders where reservationid = ?;" +
                    "delete from baklava.orders where orderid in (select orderId from baklava.reservationOrders where reservationId = ?);" +
                    "delete from baklava.reservations where reservationId = ?").split(";");

            for (String query : queries) {
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, cancelReservation.reservationId);
                preparedStatement.executeUpdate();
                preparedStatement.close();
            }

            connection.commit();

            connection.close();

            return ok("rezervacija ponistena");
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
                connection.setAutoCommit(true);
                connection.close();
            } catch (Exception e1) {}
            return internalServerError("nesto se pokvarilo...");
        }
        finally {
            try {
                preparedStatement.close();
            } catch (Exception e) {}
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (Exception e) {}
        }

    }

    public static Result inviteFriend() {
        return badRequest();
    }

    public static Result cancelInvite() {
        return badRequest();
    }
}
