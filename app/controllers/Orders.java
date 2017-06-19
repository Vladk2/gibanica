package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.mvc.Controller;
import akka.actor.*;
import play.libs.F.*;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.orders;
import models.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.*;
import play.db.DB;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by stefan on 6/17/17.
 */
public class Orders extends Controller {

    @SuppressWarnings("Duplicate")
    public static Result editOrder() throws SQLException {
        String myRestName = session("myRestName");
        String loggedUser = session("connected");
        List<VictualAndDrink> menu = new ArrayList<>();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode ajax_json = request().body().asJson();
        Order response = objectMapper.convertValue(ajax_json, Order.class);
        System.out.println("AJAX STRING: " + ajax_json.toString());
        System.out.println("EDIT ORDER: " + response.toString());

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

            return ok(orders.render(menu, "edit", response));

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

            return ok(orders.render(menu, "new", new Order()));

        } catch (SQLException sqle){
            sqle.printStackTrace();
        }

        return badRequest("Something strange happened");
    }

    public static Result editOrderAJAX(){
        return ok();
    }

    public static Result getOrderInfoAJAX(){

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

            System.out.println("AJAX STRING: " + ajax_json.toString());
            System.out.println("CREATE ORDER:" + response.toString());

            //nakon snimanja u bazu, salje se notifikacija konobarima i
            //barmenima za porudzbinu
            //u bazi takodje treba dodati boolean kolone koje ce oznacavati
            //da li je konobar ili barmen zavrsio svoj deo porudzbine

            //return ok("Uspesno kreirana porudzbina");
            return ok(ajax_json);
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
