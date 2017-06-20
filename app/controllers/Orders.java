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
                    "  u.email from baklava.orders as o " +
                    "  LEFT JOIN users AS u ON o.guestId = u.userId" +
                    "  LEFT JOIN orderVictualDrink AS ouvd ON o.orderId = ouvd.orderId" +
                    "  WHERE ouvd.cookId = (Select userId from users where email = ?);";

            String bartender_query = "SELECT DISTINCT o.orderId, o.orderDate, o.orderTime, o.orderReady," +
                    "  u.email from baklava.orders as o " +
                    "  LEFT JOIN users AS u ON o.guestId = u.userId" +
                    "  LEFT JOIN orderVictualDrink AS ouvd ON o.orderId = ouvd.orderId" +
                    "  WHERE ouvd.bartenderId = (Select userId from users where email = ?);";

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
            if (!session("userType").equals("waiter") && !session("userType").equals("chef")
                    && !session("userType").equals("bartender")) {
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
    public static Result newOrder() throws  SQLException {
        if(session("userType") == null)
            return redirect("/");
        else {
            if (!session("userType").equals("waiter") && !session("userType").equals("chef")
                    && !session("userType").equals("bartender")) {
                System.out.println(session("userType"));
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
            if (!session("userType").equals("waiter") && !session("userType").equals("chef")
                    && !session("userType").equals("bartender")) {
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
            if (!session("userType").equals("waiter") && !session("userType").equals("chef")
                    && !session("userType").equals("bartender")) {
                System.out.println(session("userType"));
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
                    "left join users as uc on ovd.cookId = uc.userId " +
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
                    "left join users as ub on ovd.bartenderId = ub.userId " +
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
            if (!session("userType").equals("waiter") && !session("userType").equals("chef")
                    && !session("userType").equals("bartender")) {
                System.out.println(session("userType"));
                return forbidden();
            }
        }

        try (Connection connection = DB.getConnection()) {
            /* radice se transakcija sa zakljucavanjem reda koji se updejtuje */

            connection.setAutoCommit(false);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            HashMap<String, String> request = objectMapper.convertValue(ajax_json, HashMap.class);

            System.out.println(request.toString());

            String statement = "Update orderVictualDrink set isReady = 1 where orderId = ? and " +
                    "victualDrinkId = (Select victualsAndDrinksId from victualsanddrinks where name = ?);";

            PreparedStatement preparedStatement = connection.prepareStatement(statement);
            preparedStatement.setString(1, request.get("orderId"));
            preparedStatement.setString(2, request.get("name"));

            if(preparedStatement.executeUpdate() > 0) {
                connection.commit();
                return ok("Successfully updated");
            }
            else
                return internalServerError();
        } catch (SQLException sqle){
            sqle.printStackTrace();
        } catch (Exception exc){
            exc.printStackTrace();
        }

        return internalServerError("Something strange happened");
    }

    private static HashMap<String, ActorRef> clients = new HashMap<String, ActorRef>();

    public static WebSocket<String> proceed() {
        return WebSocket.withActor(OrdersPostman::props);
    }

    public static class OrdersPostman extends UntypedActor {

        public static Props props(ActorRef out) {
            return Props.create(OrdersPostman.class, out);
        }

        private final ActorRef out;

        public OrdersPostman(ActorRef out) {
            this.out = out;
        }

        public void onReceive(Object message) throws Exception {
            if (message instanceof String) {
                clients.put(message.toString(), out);
                clients.get(message.toString()).tell("I received your message: " + message, self());
            }
        }
    }
}
