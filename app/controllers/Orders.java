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

        try(Connection connection = DB.getConnection()){
            String waiter_query = "SELECT DISTINCT u.email, uw.email, o.orderDate, o.orderTime, o.waiterId," +
                    "  vd.name, vd.description, vd.price, uc.email, ub.email from baklava.orders as o" +
                    "  LEFT JOIN users AS u ON o.guestId = u.userId" +
                    "  LEFT JOIN users AS uw ON o.waiterId = uw.userId" +
                    "  LEFT JOIN orderVictualDrink AS ouvd ON o.orderId = ouvd.orderId" +
                    "  LEFT JOIN users as uc on ouvd.cookId = uc.userId" +
                    "  LEFT JOIN users as ub on ouvd.bartenderId = ub.userId" +
                    "  LEFT JOIN victualsanddrinks AS vd ON ouvd.victualDrinkId = vd.victualsAndDrinksId" +
                    "  WHERE uw.userId = (Select userId from users where email = ?);";

            String cook_query = "SELECT DISTINCT u.email, uw.email, o.orderDate, o.orderTime, o.waiterId," +
                    "  vd.name, vd.description, vd.price, uc.email, ub.email from baklava.orders as o" +
                    "  LEFT JOIN users AS u ON o.guestId = u.userId" +
                    "  LEFT JOIN users AS uw ON o.waiterId = uw.userId" +
                    "  LEFT JOIN orderVictualDrink AS ouvd ON o.orderId = ouvd.orderId" +
                    "  LEFT JOIN users as uc on ouvd.cookId = uc.userId" +
                    "  LEFT JOIN victualsanddrinks AS vd ON ouvd.victualDrinkId = vd.victualsAndDrinksId" +
                    "  WHERE uw.userId = (Select userId from users where email = ?);";

            String bartender_query = "SELECT DISTINCT u.email, uw.email, o.orderDate, o.orderTime, o.waiterId," +
                    "  vd.name, vd.description, vd.price, uc.email, ub.email from baklava.orders as o" +
                    "  LEFT JOIN users AS u ON o.guestId = u.userId" +
                    "  LEFT JOIN users AS uw ON o.waiterId = uw.userId" +
                    "  LEFT JOIN orderVictualDrink AS ouvd ON o.orderId = ouvd.orderId" +
                    "  LEFT JOIN users as ub on ouvd.bartenderId = ub.userId" +
                    "  LEFT JOIN victualsanddrinks AS vd ON ouvd.victualDrinkId = vd.victualsAndDrinksId" +
                    "  WHERE ub.userId = (Select userId from users where email = ?);";

            PreparedStatement preparedStatement = connection.prepareStatement(waiter_query);
            preparedStatement.setString(1, session("connected"));

            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                System.out.println(resultSet.getString(1));
                System.out.println(resultSet.getString(2));
                System.out.println(resultSet.getString(3));
                System.out.println(resultSet.getString(4));
                System.out.println(resultSet.getString(5));
                System.out.println(resultSet.getString(6));
                System.out.println(resultSet.getString(7));
                System.out.println(resultSet.getString(8));
                System.out.println(resultSet.getString(9));
                System.out.println(resultSet.getString(10));
            }

            List<HashMap<String, String>> _orders = new ArrayList<HashMap<String, String>>();



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
            return ok(order.render(menu, "edit", new Order()));

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

            return ok(order.render(menu, "new", new Order()));

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
            Order response = objectMapper.convertValue(ajax_json, Order.class);

            //upisivanje porudzbine u bazu
            //id porudzbine ce biti isti kao id rezervacije
            //sto jos nije zavrseno !!!!

            System.out.println(response.toString());

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
