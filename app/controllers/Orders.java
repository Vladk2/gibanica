package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.mvc.Controller;
import akka.actor.*;
import play.libs.F.*;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.order;
import models.*;

import java.util.HashMap;
import java.util.*;
import play.db.DB;
import views.html.orders;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by stefan on 6/17/17.
 */
public class Orders extends Controller {

    @SuppressWarnings("Duplicates")
    public static Result orders() {

        if(session("userType") == null)
            return redirect("/");
        else {
            if (!session("userType").equals("waiter") && !session("userType").equals("chef")
                    && !session("userType").equals("bartender")) {
                System.out.println(session("userType"));
                return forbidden();
            }
        }
        try(Connection connection = DB.getConnection()){
            String waiter_query = "SELECT DISTINCT o.orderId, o.orderDate, o.orderTime, o.orderReady," +
                    "  u.email from baklava.orders as o" +
                    "  LEFT JOIN users AS u ON o.guestId = u.userId" +
                    "  LEFT JOIN users AS uw ON o.waiterId = uw.userId" +
                    "  WHERE uw.userId = (Select userId from users where email = ?);";

            String cook_query = "SELECT DISTINCT o.orderId, o.orderDate, o.orderTime, o.orderReady," +
                    "  u.email, ouvd.accepted from baklava.orders as o " +
                    "  LEFT JOIN users AS u ON o.guestId = u.userId" +
                    "  LEFT JOIN orderVictualDrink AS ouvd ON o.orderId = ouvd.orderId" +
                    "  WHERE ouvd.workerId = (Select userId from users where email = ?);";

            String bartender_query = "SELECT DISTINCT o.orderId, o.orderDate, o.orderTime, o.orderReady," +
                    "  u.email, ouvd.accepted from baklava.orders as o " +
                    "  LEFT JOIN users AS u ON o.guestId = u.userId" +
                    "  LEFT JOIN orderVictualDrink AS ouvd ON o.orderId = ouvd.orderId" +
                    "  WHERE ouvd.workerId = (Select userId from users where email = ?);";

            PreparedStatement preparedStatement = null;

            if(session("userType").equals("waiter"))
                preparedStatement = connection.prepareStatement(waiter_query);
            else if(session("userType").equals("chef"))
                preparedStatement = connection.prepareStatement(cook_query);
            else if(session("userType").equals("bartender"))
                preparedStatement = connection.prepareStatement(bartender_query);

            preparedStatement.setString(1, session("connected"));

            ResultSet resultSet = preparedStatement.executeQuery();

            List<HashMap<String, String>> _orders = new ArrayList<HashMap<String, String>>();

            while(resultSet.next()){
                HashMap<String, String> obj = new HashMap<>();
                obj.put("orderId", resultSet.getString(1));
                obj.put("orderTime", resultSet.getString(3));
                obj.put("orderReady", resultSet.getString(4));
                obj.put("guestEmail", resultSet.getString(5));
                if(!session("userType").equals("waiter"))
                    obj.put("orderAccepted", resultSet.getString(6));
                _orders.add(obj);
            }

            return ok(orders.render(_orders));

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return internalServerError("Something strange happened");
    }

    @SuppressWarnings("Duplicates")
    public static Result editOrder() throws SQLException {
        if(session("userType") == null)
            return redirect("/");
        else {
            if (!session("userType").equals("waiter")) {
                System.out.println(session("userType"));
                return forbidden();
            }
        }

        String myRestName = session("myRestName");
        String loggedUser = session("connected");
        List<VictualAndDrink> menu = new ArrayList<>();

        /* bice poslate informacije o porudzbine preko forme */
        /* svaki element u listi porudzbina ce biti forma za sebe */

        try (Connection connection = DB.getConnection()){

            PreparedStatement stmt = null;

            stmt = connection.prepareStatement("Select name, description, price, type, restaurantId from victualsanddrinks where restaurantId = " +
                    "(select restaurantId from workers where userId = (select userId from users where email = ?));");
            stmt.setString(1, loggedUser);
            ResultSet result = stmt.executeQuery();


            while (result.next()) {
                VictualAndDrink vd = new VictualAndDrink(result.getString(1), result.getString(2),
                        result.getDouble(3), result.getString(4));
                menu.add(vd);
            }

            stmt.close();


            //return redirect("/editOrder");
            return ok(order.render(menu,
                    "edit", new Order()));

        } catch (SQLException sqle){
            sqle.printStackTrace();
        }

        return internalServerError("Something strane happened");
    }

    @SuppressWarnings("Duplicates")
    public static Result newOrder() throws SQLException {
        if(session("userType") == null)
            return redirect("/");
        else {
            if (!session("userType").equals("waiter")) {
                //System.out.println(session("userType"));
                return forbidden();
            }
        }

        // iscitaj listu sa imenima hrane i pica za restoran u kome radi konobar ili
        // koji gost trenutno posecuje

            String myRestName = session("myRestName");
            String loggedUser = session("connected");
            List<VictualAndDrink> menu = new ArrayList<>();
            List<HashMap<String, String>> prazna_lista = new ArrayList<HashMap<String, String>>();

        try (Connection connection = DB.getConnection()){

            PreparedStatement preparedStatement = null;

            preparedStatement = connection.prepareStatement("Select name, description, price, type, restaurantId from victualsanddrinks where restaurantId = " +
                    "(select restaurantId from workers where userId = (select userId from users where email = ?));");
            preparedStatement.setString(1, loggedUser);

            ResultSet result = preparedStatement.executeQuery();

            while (result.next()) {
                VictualAndDrink vd = new VictualAndDrink(result.getString(1), result.getString(2),
                        result.getDouble(3), result.getString(4));
                menu.add(vd);
            }

            result.close();

            // izlistaj imena gostiju koji trenutno imaju rezervisan sto

            String get_guests = "";

            // izlistaj kuvare i sankere kojima ce se dodeliti porudzbina

            String get_workers = "select u.name, u.surname from users as u " +
                    "left join workers as w on u.userId = w.userId " +
                    "left join restaurants as r on r.restaurantId = w.restaurantId " +
                    "where u.email = ?;";



            preparedStatement.close();

            return ok(order.render(menu,
                    "new", new Order()));

        } catch (SQLException sqle){
            sqle.printStackTrace();
        }

        return internalServerError("Something strange happened");
    }

    @SuppressWarnings("Duplicates")
    public static Result editOrderAJAX(){
        if(session("userType") == null)
            return redirect("/");
        else {
            if (!session("userType").equals("waiter")) {
                System.out.println(session("userType"));
                return forbidden();
            }
        }
        return ok();
    }

    @SuppressWarnings("Duplicates")
    public static Result createOrderAJAX(){
        if(session("userType") == null)
            return redirect("/");
        else {
            if (!session("userType").equals("waiter")) {
                return forbidden();
            }
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            Order request = objectMapper.convertValue(ajax_json, Order.class);

            //upisivanje porudzbine u bazu
            //id porudzbine ce biti isti kao id rezervacije
            //sto jos nije zavrseno !!!!

            System.out.println(request.toString());

            //nakon snimanja u bazu, salje se notifikacija konobarima i
            //barmenima za porudzbinu
            //u bazi takodje treba dodati boolean kolone koje ce oznacavati
            //da li je konobar ili barmen zavrsio svoj deo porudzbine

            return ok("Order successfully created");
        } catch(Exception e){
            e.printStackTrace();
        }

        return internalServerError("Something strange happened");
    }

    @SuppressWarnings("Duplicates")
    public static Result previewOrderAJAX(){
        if(session("userType") == null)
            return redirect("/");
        else {
            if (!session("userType").equals("waiter") && !session("userType").equals("chef")
                    && !session("userType").equals("bartender")) {
                System.out.println(session("userType"));
                return forbidden();
            }
        }


        try (Connection connection = DB.getConnection()) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            HashMap<String, String> request = objectMapper.convertValue(ajax_json, HashMap.class);

            String cook_query = "select distinct v.name, v.price, ovd.quantity, ovd.isReady, o.orderId from victualsanddrinks as v " +
                    "left join orderVictualDrink as ovd on v.victualsAndDrinksId = ovd.victualDrinkId " +
                    "left join orders as o on ovd.orderId = o.orderId " +
                    "left join users as uc on ovd.workerId = uc.userId " +
                    "where uc.userId = (Select userId from users where email=?) and v.type=\"victual\" " +
                    "and o.orderId=?;";

            String waiter_query = "select distinct v.name, v.price, ovd.quantity, ovd.isReady, o.orderId from victualsanddrinks as v " +
                    "left join orderVictualDrink as ovd on v.victualsAndDrinksId = ovd.victualDrinkId " +
                    "left join orders as o on ovd.orderId = o.orderId " +
                    "left join users as uw on o.waiterId = uw.userId " +
                    "where uw.userId = (select userId from users where email=?) and o.orderId=?;";

            String bartender_query = "select distinct v.name, v.price, ovd.quantity, ovd.isReady, o.orderId from victualsanddrinks as v " +
                    "left join orderVictualDrink as ovd on v.victualsAndDrinksId = ovd.victualDrinkId " +
                    "left join orders as o on ovd.orderId = o.orderId " +
                    "left join users as ub on ovd.workerId = ub.userId " +
                    "where ub.userId = (select userId from users where email=?) and v.type=\"drink\" " +
                    "and o.orderId=?;";

            PreparedStatement preparedStatement = null;

            if(session("userType").equals("waiter"))
                preparedStatement = connection.prepareStatement(waiter_query);
            else if(session("userType").equals("chef"))
                preparedStatement = connection.prepareStatement(cook_query);
            else if(session("userType").equals("bartender"))
                preparedStatement = connection.prepareStatement(bartender_query);
            else
                return forbidden();

            preparedStatement.setString(1, session("connected"));
            preparedStatement.setString(2, request.get("orderId"));

            ResultSet resultSet = preparedStatement.executeQuery();

            HashMap<String, List<HashMap<String, String>>> response =
                    new HashMap<String,List<HashMap<String, String>>>();

            List<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();

            while(resultSet.next()){
                HashMap<String, String> element = new HashMap<>();
                element.put("name", resultSet.getString(1));
                element.put("price", resultSet.getString(2));
                element.put("quantity", resultSet.getString(3));
                element.put("isReady", resultSet.getString(4));
                element.put("orderId", resultSet.getString(5));
                items.add(element);
                System.out.println(element.toString());
            }

            response.put("items", items);

            String json = objectMapper.writeValueAsString(response);

            return ok(json);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception exception){
            exception.printStackTrace();
        }

        return internalServerError("Something strange happened");
    }

    @SuppressWarnings("Duplicates")
    public static Result updateOrderItemAJAX(){
        if(session("userType") == null)
            return redirect("/");
        else {
            if (!session("userType").equals("chef")
                    && !session("userType").equals("bartender")) {
                System.out.println(session("userType"));
                return forbidden();
            }
        }

        Connection connection = null;

        try {
            /* radice se transakcija sa zakljucavanjem reda koji se updejtuje */
            connection = DB.getConnection();

            connection.setAutoCommit(false);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            HashMap<String, String> request = objectMapper.convertValue(ajax_json, HashMap.class);

            System.out.println(request.toString());

            String stmt_update = "Select orderId, victualDrinkId, isReady from orderVictualDrink where orderId = ? and " +
                    "victualDrinkId = (Select victualsAndDrinksId from victualsanddrinks where name = ?) " +
                    "and isReady = ? for update;";

            PreparedStatement preparedStatement =
                    connection.prepareStatement(stmt_update, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            preparedStatement.setString(1, request.get("orderId"));
            preparedStatement.setString(2, request.get("name"));
            preparedStatement.setString(3, "0");

            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                resultSet.updateString(3, "1");
                resultSet.updateRow();
            }

            connection.commit();
            preparedStatement.close();
            return ok("Successfully updated");
        } catch (SQLException sqle){
            if(connection != null)
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            sqle.printStackTrace();
        } catch (Exception exc){
            exc.printStackTrace();
        } finally {
            if(connection != null)
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }

        return internalServerError("Something strange happened");
    }

    @SuppressWarnings("Duplicates")
    public static Result acceptOrderAJAX(){
        if(session("userType") == null)
            return redirect("/");
        else {
            if (!session("userType").equals("chef")
                    && !session("userType").equals("bartender")) {
                System.out.println(session("userType"));
                return forbidden();
            }
        }

        Connection connection = null;

        try {
            connection = DB.getConnection();

            connection.setAutoCommit(false);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            HashMap<String, String> request = objectMapper.convertValue(ajax_json, HashMap.class);

            System.out.println(request.toString());

            String stmt_update = "Update orderVictualDrink set accepted = 1 where orderId = ? and workerId = ?;";

            PreparedStatement preparedStatement =
                    connection.prepareStatement(stmt_update, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            preparedStatement.setString(1, request.get("orderId"));
            preparedStatement.setString(2, request.get("employee"));

            if(preparedStatement.executeUpdate() > 0) {
                connection.commit();
                preparedStatement.close();

                return ok("Order has been accepted");
            }
            else
                return internalServerError("Something strange happened");

        } catch (SQLException e) {
            if(connection != null){
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
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

        return internalServerError("Something strange happened");
    }

    private static ConcurrentHashMap<String, ActorRef> clients = new ConcurrentHashMap<String, ActorRef>();

    public static WebSocket<String> proceed() {
        return WebSocket.withActor(OrdersPostman::props);
    }

    public static class OrdersPostman extends UntypedActor {

        public static Props props(ActorRef out) {
            return Props.create(OrdersPostman.class, out);
        }

        private final ActorRef out;
        private String user;

        public OrdersPostman(ActorRef out) {
            this.out = out;
        }

        public void onReceive(Object message) throws Exception {
            if (message instanceof String) {
                ObjectMapper objectMapper = new ObjectMapper();
                HashMap<String, String> request_message = objectMapper.readValue(message.toString(), HashMap.class);
                if(request_message.get("type").equals("connection")) {
                    clients.put(request_message.get("user"), out);
                    this.user = request_message.get("user");
                }
                //clients.get("konobar@konobar.com").tell("Pozdraviii", self());
                //clients.get(message.toString()).tell("I received your message: " + message, self());
            }
        }

        public void postStop() throws Exception {
            System.out.println("Websocket closing for: " + this.user);
            clients.remove(this.user);
        }
    }
}
