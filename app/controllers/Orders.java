package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import akka.actor.*;
import play.libs.F.*;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.order;
import models.*;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.*;
import play.db.DB;
import views.html.orders;

import javax.xml.crypto.dsig.keyinfo.KeyValue;
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

            String cook_query = "SELECT DISTINCT o.orderId, o.orderDate, o.orderTime, o.foodReady," +
                    "  u.email, ouvd.accepted from baklava.orders as o " +
                    "  LEFT JOIN users AS u ON o.guestId = u.userId" +
                    "  LEFT JOIN orderVictualDrink AS ouvd ON o.orderId = ouvd.orderId" +
                    "  WHERE ouvd.workerId = (Select userId from users where email = ?);";

            String bartender_query = "SELECT DISTINCT o.orderId, o.orderDate, o.orderTime, o.drinkReady," +
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

        /* PROVERI DA LI KONOBARU JOS TRAJE SMENA
        * ukoliko ne traje return forbidden() */

        String myRestName = session("myRestName");
        String loggedUser = session("connected");
        List<VictualAndDrink> menu = new ArrayList<>();

        /* bice poslate informacije o porudzbine preko forme */
        /* svaki element u listi porudzbina ce biti forma za sebe */
        String cookId = "";
        String bartenderId = "";

        try (Connection connection = DB.getConnection()){

            DynamicForm requestData = Form.form().bindFromRequest();
            String orderId = requestData.get("orderId");

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

            result.close();
            stmt.close();

            PreparedStatement order_items = null;

            String items = "select v.name, v.price, ovd.quantity, o.price, o.guestId, v.type from victualsanddrinks as v " +
                    "left join orderVictualDrink as ovd on v.victualsAndDrinksId = ovd.victualDrinkId " +
                    "left join orders as o on o.orderId = ovd.orderId " +
                    "where o.orderId = ?;";

            order_items = connection.prepareStatement(items);
            order_items.setString(1, orderId);

            ResultSet resultSet = order_items.executeQuery();

            Order _order = new Order();
            _order.setOrderId(orderId);
            List<HashMap<String, String>> for_order = new ArrayList<HashMap<String, String>>();

            while(resultSet.next()){
                HashMap<String, String> item = new HashMap<>();
                item.put("name", resultSet.getString(1));
                item.put("price", resultSet.getString(2));
                item.put("quantity", resultSet.getString(3));
                item.put("type", resultSet.getString(6));
                _order.setPrice(new BigDecimal(resultSet.getString(4)));
                _order.setGuestId(resultSet.getString(5));
                for_order.add(item);
            }

            _order.setVictualsDrinks(for_order);

            resultSet.close();
            order_items.close();

            //prazne lista gostiju koja ne treba za edit
            List<User> guests = new ArrayList<>();

            List<User> cooks = new ArrayList<>();

            String get_cooks = "select u.userId, u.name, u.surname from users as u " +
                    "left join workers as w on u.userId = w.userId " +
                    "left join restaurants as r on w.restaurantId = r.restaurantId " +
                    "where u.type = \"chef\" and r.restaurantId = " +
                    "(Select restaurantId from workers " +
                    "where userId = (Select userId from users where email = ?));";


            PreparedStatement preparedStatement2 = connection.prepareStatement(get_cooks);
            preparedStatement2.setString(1, session("connected"));


            ResultSet resultSet1 = preparedStatement2.executeQuery();

            while(resultSet1.next()){
                User cook = new User();
                cook.userId = resultSet1.getString(1);
                cook.name = resultSet1.getString(2);
                cook.surname = resultSet1.getString(3);
                cooks.add(cook);
            }

            resultSet1.close();
            preparedStatement2.close();

            List<User> bartenders = new ArrayList<>();

            String get_bartenders = "select u.userId, u.name, u.surname from users as u " +
                    "left join workers as w on u.userId = w.userId " +
                    "left join restaurants as r on w.restaurantId = r.restaurantId " +
                    "where u.type = \"bartender\" and r.restaurantId = " +
                    "(Select restaurantId from workers " +
                    "where userId = (Select userId from users where email = ?));";

            PreparedStatement preparedStatement3 = connection.prepareStatement(get_bartenders);
            preparedStatement3.setString(1, session("connected"));

            ResultSet resultSet2 = preparedStatement3.executeQuery();

            while(resultSet2.next()){
                User bartender = new User();
                bartender.userId = resultSet2.getString(1);
                bartender.name = resultSet2.getString(2);
                bartender.surname = resultSet2.getString(3);
                bartenders.add(bartender);
            }

            resultSet2.close();
            preparedStatement3.close();
            return ok(order.render(menu,
                    "edit", _order, cookId, bartenderId, guests, cooks, bartenders));

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
                VictualAndDrink vd = new VictualAndDrink(result.getString(1),
                        result.getString(2),
                        result.getDouble(3), result.getString(4));
                menu.add(vd);
            }

            result.close();
            preparedStatement.close();

            // izlistaj imena gostiju koji trenutno imaju rezervisan sto

            String get_guests = "Select * from users where type = \"guest\";";

            List<User> guests = new ArrayList<>();

            PreparedStatement preparedStatement1 = connection.prepareStatement(get_guests);

            ResultSet resultSet = preparedStatement1.executeQuery();

            while(resultSet.next()){
                User guest = new User();
                guest.userId = resultSet.getString(1);
                guest.name = resultSet.getString(2);
                guest.surname = resultSet.getString(3);
                guest.email = resultSet.getString(4);
                guests.add(guest);
            }

            resultSet.close();
            preparedStatement1.close();

            // izlistaj kuvare i sankere kojima ce se dodeliti porudzbina

            List<User> cooks = new ArrayList<>();

            String get_cooks = "select u.userId, u.name, u.surname from users as u " +
                    "left join workers as w on u.userId = w.userId " +
                    "left join restaurants as r on w.restaurantId = r.restaurantId " +
                    "where u.type = \"chef\" and r.restaurantId = " +
                    "(Select restaurantId from workers " +
                    "where userId = (Select userId from users where email = ?));";


            PreparedStatement preparedStatement2 = connection.prepareStatement(get_cooks);
            preparedStatement2.setString(1, session("connected"));


            ResultSet resultSet1 = preparedStatement2.executeQuery();

            while(resultSet1.next()){
                User cook = new User();
                cook.userId = resultSet1.getString(1);
                cook.name = resultSet1.getString(2);
                cook.surname = resultSet1.getString(3);
                System.out.println(resultSet1.getString(2));
                cooks.add(cook);
            }

            System.out.println(cooks.size());

            resultSet1.close();
            preparedStatement2.close();

            List<User> bartenders = new ArrayList<>();

            String get_bartenders = "select u.userId, u.name, u.surname from users as u " +
                    "left join workers as w on u.userId = w.userId " +
                    "left join restaurants as r on w.restaurantId = r.restaurantId " +
                    "where u.type = \"bartender\" and r.restaurantId = " +
                    "(Select restaurantId from workers " +
                    "where userId = (Select userId from users where email = ?));";

            PreparedStatement preparedStatement3 = connection.prepareStatement(get_bartenders);
            preparedStatement3.setString(1, session("connected"));

            ResultSet resultSet3 = preparedStatement3.executeQuery();

            while(resultSet3.next()){
                User bartender = new User();
                bartender.userId = resultSet3.getString(1);
                bartender.name = resultSet3.getString(2);
                bartender.surname = resultSet3.getString(3);
                bartenders.add(bartender);
            }

            System.out.println(bartenders.size());

            resultSet3.close();
            preparedStatement3.close();


            return ok(order.render(menu,
                    "new", new Order(), "", "", guests, cooks, bartenders));

        } catch (SQLException sqle){
            sqle.printStackTrace();
        }

        return internalServerError("Something strange happened");
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

        try (Connection connection = DB.getConnection()) {
            connection.setAutoCommit(false);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            Order request = objectMapper.convertValue(ajax_json, Order.class);


            HashMap<String, String> ord_items = new HashMap<>();

            List<String> order_items = new ArrayList<>();

            for(HashMap<String, String> element: request.getVictualsDrinks()){
                ord_items.put(element.get("name"), element.get("quantity"));
                order_items.add(element.get("name"));
            }
            //upisivanje porudzbine u bazu
            //id porudzbine ce biti isti kao id rezervacije
            //sto jos nije zavrseno !!!!

            if(request.getVictualsDrinks().size() == 0)
                return badRequest("Order needs to have at least one item");

            if(request.getType().equals("edit")){

                // brisanje stavki sa porudzbine

                String delete_all = "delete from orderVictualDrink where orderId = ?;";

                PreparedStatement preparedStatement = connection.prepareStatement(delete_all);
                preparedStatement.setString(1, request.getOrderId());

                preparedStatement.execute();

                preparedStatement.close();

                /* upis novih */
                // ord_item hashmapa gde je kljuc ime a vrednosti amout jela/pica
                // found_items lista stavki koje se vec nalaze na porduzbini

                String new_query = "Insert into orderVictualDrink (orderId, victualDrinkId, isReady, accepted, workerId," +
                        "quantity) values (?, (Select victualsAndDrinksId from victualsanddrinks where name = ?), " +
                        "0, 0, ?, ?);";

                // if workerId null, onda uradi select i nadji bilo kog radnika za taj tip stavke (hrana/pice)
                String workerId = null;

                String get_new_worker = "Select w.userId from workers as w left join v";

                for(int i = 0; i < request.getVictualsDrinks().size(); i++) {
                    PreparedStatement preparedStatement1 = connection.prepareStatement(new_query);
                    preparedStatement1.setString(1, request.getOrderId());
                    preparedStatement1.setString(2, request.getVictualsDrinks().get(i).get("name"));
                    preparedStatement1.setString(3, request.getVictualsDrinks().get(i).get("workerId"));
                    preparedStatement1.setString(4, request.getVictualsDrinks().get(i).get("quantity").trim());

                    preparedStatement1.execute();
                    preparedStatement1.close();

                }

                /* insert novih zavrsen */

                String notification_message = String.format("Order ID: %s\nPrevious orders that you worked " +
                        "on has been updated.", request.getOrderId());
                for(int i = 0; i < request.getVictualsDrinks().size(); i++) {
                    saveNotification(request.getVictualsDrinks().get(i).get("workerId"), notification_message);
                    try {
                        for(Map.Entry<String, ActorRef> websocket : clients_mail.entrySet()){
                            websocket.getValue().tell(notification_message, ActorRef.noSender());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }


                connection.commit();
            } else if(request.getType().equals("new")){

                // vreme kreiranja porudzbine
                Date current_datetime = new Date();
                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                String sqlDate = formatter.format(current_datetime);
                String sqlTime = new SimpleDateFormat("HH:MM:s").format(current_datetime);

                String new_order = "Insert into orders (orderDate, orderTime, guestId, waiterId, restaurantId," +
                        " price) values (?, ?, ?, ?, (Select restaurantId from workers where userId = ? ), ?);";

                PreparedStatement pstmt = connection.prepareStatement(new_order);
                pstmt.setString(1, sqlDate);
                pstmt.setString(2, sqlTime);
                pstmt.setString(3, request.getGuestId());
                pstmt.setString(4, session("userId"));
                pstmt.setString(5, session("userId"));
                pstmt.setBigDecimal(6, request.getPrice());


                pstmt.execute();

                // uzmi id porudzbine koja je tek povezana

                String get_orderId = "Select orderId from orders where waiterId = ? and orderDate = ? and orderTime = ?";
                PreparedStatement getId = connection.prepareCall(get_orderId);
                getId.setString(1, session("userId"));
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

                for(int i = 0; i < request.getVictualsDrinks().size(); i++) {
                    PreparedStatement preparedStatement1 = connection.prepareStatement(new_query);
                    preparedStatement1.setString(1, orderId);
                    preparedStatement1.setString(2, request.getVictualsDrinks().get(i).get("name"));
                    preparedStatement1.setString(3, request.getVictualsDrinks().get(i).get("workerId"));
                    preparedStatement1.setString(4, request.getVictualsDrinks().get(i).get("quantity"));

                    preparedStatement1.execute();
                    preparedStatement1.close();

                }


                connection.commit();

                String notification_message = String.format("Order ID: %s\nThere are new orders waiting " +
                        "for your confirmation.", request.getOrderId());
                for(int i = 0; i < request.getVictualsDrinks().size(); i++) {
                    saveNotification(request.getVictualsDrinks().get(i).get("workerId"), notification_message);
                    try {
                        for(Map.Entry<String, ActorRef> websocket : clients_mail.entrySet()){
                            websocket.getValue().tell(notification_message, ActorRef.noSender());
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

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

            String stmt_update = "select ovd.orderId, ovd.victualDrinkId, ovd.isReady from orderVictualDrink as ovd " +
                    "left join orders as o on ovd.orderId = o.orderId " +
                    "left join restaurants as r on r.restaurantId = o.restaurantId " +
                    "where o.orderId = ? and " +
                    "ovd.victualDrinkId = (Select victualsAndDrinksId from victualsanddrinks where name = ?) for update;";

            //String stmt_update = "Select orderId, victualDrinkId, isReady from orderVictualDrink where orderId = ? and " +
            //        "victualDrinkId = (Select victualsAndDrinksId from victualsanddrinks where name = ?) " +
            //        "and isReady = ? and for update;";

            PreparedStatement preparedStatement =
                    connection.prepareStatement(stmt_update, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            preparedStatement.setString(1, request.get("orderId"));
            preparedStatement.setString(2, request.get("name"));

            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                resultSet.updateString(3, "1");
                resultSet.updateRow();
            }

            resultSet.close();


            preparedStatement.close();

            String notification_message = String.format("%s on Order(ID: %s) is now ready.", request.get("name"),
                    request.get("orderId"));

            String getWaiter = "Select o.waiterId, u.email from orders as o left join users as u " +
                    "on u.userId = o.waiterId where orderId = ?";
            PreparedStatement pstmt = connection.prepareStatement(getWaiter);
            pstmt.setString(1, request.get("orderId"));

            ResultSet rs = pstmt.executeQuery();
            String waiter_id = "";
            String waiter_mail = "";

            while(rs.next()){
                waiter_id = rs.getString(1);
                waiter_mail = rs.getString(2);
            }

            rs.close();
            pstmt.close();

            saveNotification(waiter_id, notification_message);
            try {
                clients_mail.get(waiter_mail).tell(notification_message, ActorRef.noSender());
            } catch (Exception e) {
                e.printStackTrace();
            }

            connection.commit();

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
                String notification_message = "";
                if (session("userType").equals("chef"))
                    notification_message = String.format("Food for order(ID: %s) is now accepted.", request.get("orderId"));
                else if (session("userType").equals("bartender"))
                    notification_message = String.format("Drink for order(ID: %s) is now accepted.", request.get("orderId"));

                String getWaiter = "Select o.waiterId, u.email from orders as o left join users as u " +
                            "on u.userId = o.waiterId where orderId = ?";

                PreparedStatement pstmt = connection.prepareStatement(getWaiter);
                pstmt.setString(1, request.get("orderId"));

                ResultSet rs = pstmt.executeQuery();
                String waiter_id = "";
                String waiter_mail = "";

                while (rs.next()) {
                    waiter_id = rs.getString(1);
                    waiter_mail = rs.getString(2);
                }

                rs.close();
                pstmt.close();

                saveNotification(waiter_id, notification_message);
                try {
                    clients_mail.get(waiter_mail).tell(notification_message, ActorRef.noSender());
                } catch (Exception e) {
                    e.printStackTrace();
                }


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

    @SuppressWarnings("Duplicates")
    public static Result readyOrderAJAX(){
        if(session("userType") == null)
            return redirect("/");
        else {
            if (!session("userType").equals("waiter") && !session("userType").equals("chef")
                    && !session("userType").equals("bartender")) {
                System.out.println(session("userType"));
                return forbidden();
            }
        }

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            /* radice se transakcija sa zakljucavanjem reda koji se updejtuje */
            connection = DB.getConnection();

            connection.setAutoCommit(false);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            HashMap<String, String> request = objectMapper.convertValue(ajax_json, HashMap.class);

            String waiter_statement = "Select orderId, orderReady from orders where orderId = ? " +
                    "and waiterId = ? for update";

            String cook_statement = "Select o.orderId, o.foodReady from orders as o left join orderVictualDrink as ovd " +
                    "on o.orderId = ovd.orderId left join users as u on ovd.workerId = u.userId " +
                    "where o.orderId = ? and ovd.workerId = ? for update";

            String bartender_statement = "Select o.orderId, o.drinkReady from orders as o left join orderVictualDrink as ovd " +
                    "on o.orderId = ovd.orderId left join users as u on ovd.workerId = u.userId " +
                    "where o.orderId = ? and ovd.workerId = ? for update";

            if(session("userType").equals("waiter"))
                preparedStatement = connection.prepareStatement(waiter_statement,
                        ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            else if(session("userType").equals("chef"))
                preparedStatement = connection.prepareStatement(cook_statement,
                         ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            else if(session("userType").equals("bartender"))
                preparedStatement = connection.prepareStatement(bartender_statement,
                         ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            else
                return forbidden();

            preparedStatement.setString(1, request.get("orderId"));
            preparedStatement.setString(2, session("userId"));

            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                resultSet.updateString(2, "1");
                resultSet.updateRow();
            }

            if(session("userType").equals("chef") || session("userType").equals("bartender")) {
                String notification_message = "";
                if (session("userType").equals("chef"))
                    notification_message = String.format("Food for order(ID: %s) is now ready.", request.get("orderId"));
                else if (session("userType").equals("bartender"))
                    notification_message = String.format("Drink for order(ID: %s) is now ready.", request.get("orderId"));

                String getWaiter = "Select o.waiterId, u.email from orders as o left join users as u " +
                            "on u.userId = o.waiterId where orderId = ?";

                PreparedStatement pstmt = connection.prepareStatement(getWaiter);
                pstmt.setString(1, request.get("orderId"));

                ResultSet rs = pstmt.executeQuery();
                String waiter_id = "";
                String waiter_mail = "";

                while (rs.next()) {
                    waiter_id = rs.getString(1);
                    waiter_mail = rs.getString(2);
                }

                rs.close();
                pstmt.close();

                saveNotification(waiter_id, notification_message);
                try {
                    clients_mail.get(waiter_mail).tell(notification_message, ActorRef.noSender());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            preparedStatement.close();

            /* vrati informacije za racun o porudzbini iz baze */

            String orderInfo = "Select orderDate, orderTime, price from orders where orderId = ?";
            String orderItems = "Select v.name, ovd.quantity, v.price from victualsanddrinks as v " +
                    "left join orderVictualDrink as ovd on ovd.victualDrinkId = v.victualsAndDrinksId " +
                    "where ovd.orderId = ?";

            Order order = new Order();

            PreparedStatement stmt1 = connection.prepareStatement(orderInfo);
            stmt1.setString(1, request.get("orderId"));

            ResultSet resultSetO = stmt1.executeQuery();

            while(resultSetO.next()){
                order.setOrderDate(resultSetO.getString(1));
                order.setOrderTime(resultSetO.getString(2));
                order.setPrice(new BigDecimal(resultSetO.getString(3)));
            }

            resultSetO.close();
            stmt1.close();

            List<HashMap<String, String>> victualsDrinks = new ArrayList<>();

            PreparedStatement stmt2 = connection.prepareStatement(orderItems);
            stmt2.setString(1, request.get("orderId"));

            ResultSet resultSetOI = stmt2.executeQuery();

            while(resultSetOI.next()){
                HashMap<String, String> item = new HashMap<>();
                item.put("name", resultSetOI.getString(1));
                item.put("quantity", resultSetOI.getString(2));
                item.put("price", resultSetOI.getString(3));
                victualsDrinks.add(item);
            }

            resultSetOI.close();
            stmt2.close();

            order.setVictualsDrinks(victualsDrinks);

            String jsonized = objectMapper.writeValueAsString(order);

            connection.commit();

            return ok(jsonized);

        } catch (SQLException sql){
            sql.printStackTrace();
            if(connection != null){
                try {
                    connection.rollback();
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (JsonProcessingException e) {
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

    @SuppressWarnings("Duplicates")
    public static Result checkOrderItemStatusAJAX(){
        if(session("userType") == null)
            return redirect("/");
        else {
            if (!session("userType").equals("waiter")) {
                //System.out.println(session("userType"));
                return forbidden();
            }
        }
        try (Connection connection = DB.getConnection()){
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode ajax_json = request().body().asJson();
            HashMap<String, String> request = objectMapper.convertValue(ajax_json, HashMap.class);

            String query = "Select accepted from orderVictualDrink where orderId = ? and victualDrinkId = " +
                    "(Select victualsAndDrinksId from victualsanddrinks where name = ?);";

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, request.get("orderId"));
            preparedStatement.setString(2, request.get("name"));

            ResultSet resultSet = preparedStatement.executeQuery();

            String isAccepted = "";

            while(resultSet.next()){
                isAccepted = resultSet.getString(1).equals("1") ? "true" : "false";
            }

            resultSet.close();
            preparedStatement.close();

            HashMap<String, String> for_response = new HashMap<>();
            for_response.put("isAccepted", isAccepted);

            return ok(objectMapper.writeValueAsString(for_response));

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return internalServerError("Something strange happened");
    }

    @SuppressWarnings("Duplicates")
    public static Result seenNotificationsAJAX() throws SQLException {

        JsonNode j =  request().body().asJson();
        HashMap<String, String> request = Json.fromJson(j, HashMap.class);

        Connection connection = null;
        PreparedStatement stmt = null;

        String query = "update notificationOrders set seen = 1 where userId = ?;";

        try {

            connection = DB.getConnection();
            connection.setAutoCommit(false);
            stmt = connection.prepareStatement(query);
            stmt.setString(1, request.get("userId"));
            stmt.executeUpdate();

            connection.commit();

            return ok();
        }
        catch (SQLException sqle){

            try {
                if (connection != null && !connection.getAutoCommit()) {
                    System.err.print("Transaction is being rolled back\n");
                    connection.rollback();

                }
                if (stmt != null) {
                    stmt.close();
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
        return internalServerError("Something strange happened");

    }

    private static boolean saveNotification(String userId, String message){

        Connection connection = null;

        try {
            connection = DB.getConnection();

            connection.setAutoCommit(false);

            String statement = "Insert into notificationOrders (userId, message, seen) " +
                    "values (?, ?, 0);";

            PreparedStatement preparedStatement = connection.prepareStatement(statement);
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, message);

            preparedStatement.execute();
            preparedStatement.close();

            connection.commit();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if(connection != null){
                try {
                    connection.close();
                    connection.rollback();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        } finally {
            if(connection != null){
                try{
                    connection.close();
                } catch (SQLException e){
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    private static List<String> loadNotifications(String userId){
        List<String> notifications = new ArrayList<>();

        try (Connection connection = DB.getConnection()) {

            String query = "Select message from notificationOrders where userId = ? and seen = 0;";

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, userId);


            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                notifications.add(resultSet.getString(1));
            }

            resultSet.close();
            preparedStatement.close();

        } catch (SQLException e){
            e.printStackTrace();
        }
        return notifications;
    }

    private static ConcurrentHashMap<String, ActorRef> clients_mail = new ConcurrentHashMap<String, ActorRef>();
    private static ConcurrentHashMap<String, String> clients_id = new ConcurrentHashMap<String, String>();

    public static WebSocket<String> proceed() {
        return WebSocket.withActor(OrdersPostman::props);
    }

    public static class OrdersPostman extends UntypedActor {

        public static Props props(ActorRef out) {
            return Props.create(OrdersPostman.class, out);
        }

        private final ActorRef out;
        private String userId;
        private String userMail;

        public OrdersPostman(ActorRef out) {
            this.out = out;
        }

        public void onReceive(Object message) throws Exception {
            if (message instanceof String) {
                ObjectMapper objectMapper = new ObjectMapper();
                HashMap<String, String> request_message = objectMapper.readValue(message.toString(), HashMap.class);
                if(request_message.get("type").equals("connection")) {
                    clients_mail.put(request_message.get("userMail"), out);
                    clients_id.put(request_message.get("userMail"), request_message.get("userId"));
                    this.userId = request_message.get("userId");
                    this.userMail = request_message.get("userMail");

                    for(String notification : loadNotifications(this.userId)){
                        this.out.tell(notification, self());
                    }
                }
                System.out.println(request_message.toString());
                //clients.get(message.toString()).tell("I received your message: " + message, self());
            }
        }

        public void postStop() throws Exception {
            System.out.println("Websocket closing for: " + this.userMail);
            try {
                clients_id.remove(this.userMail);
                clients_mail.remove(this.userMail);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
