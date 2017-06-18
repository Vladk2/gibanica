package controllers;

import play.mvc.Controller;
import akka.actor.*;
import play.libs.F.*;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.orders;
import models.*;
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

    public static Result orders() throws  SQLException {

        // iscitaj listu sa imenima hrane i pica za restoran u kome radi konobar ili
        // koji gost trenutno posecuje

            String myRestName = session("myRestName");
            String loggedUser = session("connected");
            List<VictualAndDrink> menu = new ArrayList<>();


             Connection connection = null;

             PreparedStatement stmt = null;
        try {


            connection = DB.getConnection();

            connection.setAutoCommit(false);

            //provera ako je koriscen postojeci email
            stmt = connection.prepareStatement("Select name, description, price, type, restaurantId from victualsanddrinks where restaurantId = " +
                    "(select restaurantId from workers where userId = (select userId from users where email = ?));");
            stmt.setString(1, loggedUser);
            ResultSet result = stmt.executeQuery();


            while (result.next()) {
                VictualAndDrink vd = new VictualAndDrink(result.getString(1), result.getString(2),
                        result.getDouble(3), result.getString(4));
                menu.add(vd);
            }
            connection.commit();

    } catch (SQLException sqle){

        try {
            if (connection != null && !connection.getAutoCommit()) {
                System.err.print("Transaction is being rolled back");
                connection.rollback();
            }
        } catch(SQLException excep) {
            excep.printStackTrace();
        }
        sqle.printStackTrace();
    }
            finally {
        if (stmt != null) {
            stmt.close();
        }

        if(connection != null) {
            connection.setAutoCommit(true);
            connection.close();
        }
    }

        return ok(orders.render(menu));
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
