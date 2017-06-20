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

        return badRequest("Something strange happened");
    }

    @SuppressWarnings("Duplicate")
    public static Result editOrder() throws SQLException {
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

        return badRequest("Something strane happened");
    }

    @SuppressWarnings("Duplicate")
    public static Result newOrder() throws  SQLException {

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

        return badRequest("Something strange happened");
    }

    public static Result editOrderAJAX(){
        return ok();
    }

    public static Result createOrderAJAX(){
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

        return badRequest("Nesto cudno");
    }

    public static Result previewOrderAJAX(){

        try (Connection connection = DB.getConnection()) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            HashMap<String, Integer> request = objectMapper.convertValue(ajax_json, HashMap.class);

            System.out.println(request.toString());

            String query = "";

            PreparedStatement preparedStatement = null;

            return ok(ajax_json);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception exception){
            exception.printStackTrace();
        }

        return badRequest("Something strange happened");
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
